package com.pkoder.finest.sms

import com.pkoder.finest.domain.model.PendingTransaction
import com.pkoder.finest.domain.model.TransactionType
import java.security.MessageDigest

object SmsParser {

    private val bankSenderMap = mapOf(
        "SBIINB" to "SBI", "SBICRD" to "SBI", "SBIUPI" to "SBI",
        "HDFCBK" to "HDFC", "HDFCCC" to "HDFC", "HDFCUP" to "HDFC",
        "BOBSMS" to "BOB", "BOBNOT" to "BOB", "BOBPAY" to "BOB"
    )

    // BOB sends from numeric shortcodes containing "BOB" at end of body
    private val bankBodyMap = mapOf(
        "BOB" to "BOB",
        "Bank of Baroda" to "BOB",
        "SBIINB" to "SBI",
        "State Bank" to "SBI",
        "HDFC Bank" to "HDFC"
    )

    private val debitKeywords = listOf(
        "debited", "debit", "withdrawn", "spent", "paid", "payment of",
        "transferred", "charged", "purchase", "sent",
        "dr.", " dr ", "dr from"  // BOB uses "Dr. from A/C"
    )

    private val creditKeywords = listOf(
        "credited", "credit", "received", "deposited", "refund", "cashback",
        "cr.", " cr ", "cr to"
    )

    // Handles: Rs.1.00 / Rs. 1.00 / Rs 1.00 / INR 1.00 / ₹1.00
    private val amountRegex = Regex(
        """(?:Rs\.?\s*|INR\s*|₹\s*)([\d,]+(?:\.\d{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    // Fallback: handles SBI-style "debited by 5849.25" (no currency prefix)
    private val amountFallbackRegex = Regex(
        """(?:debited|credited|debit|credit|spent|sent)\s+(?:by|with|of)\s+([\d,]+(?:\.\d{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    // UPI ID pattern: word chars before @ and after (e.g. name@okaxis, q12345@ybl)
    private val upiIdRegex = Regex("""[\w.]{3,}@[a-zA-Z]{2,}""")

    /**
     * @param timestampMillis when the *SMS* was sent (`SmsMessage.timestampMillis`), not when it
     *   was parsed — a message can be delivered long after the transaction happened.
     */
    fun parse(sender: String, body: String, timestampMillis: Long): PendingTransaction? {
        val bank = resolveBank(sender, body) ?: return null
        val lowerBody = body.lowercase()

        // For UPI transfers, "Dr." = debit from your account — treat as DEBIT
        // Even if "Cr." appears, it refers to the recipient's credit
        val type = when {
            debitKeywords.any { lowerBody.contains(it.lowercase()) } -> TransactionType.DEBIT
            creditKeywords.any { lowerBody.contains(it.lowercase()) } -> TransactionType.CREDIT
            else -> return null
        }

        val amount = (amountRegex.find(body) ?: amountFallbackRegex.find(body))
            ?.groupValues?.get(1)
            ?.replace(",", "")
            ?.toDoubleOrNull() ?: return null

        val paymentMethod = resolvePaymentMethod(lowerBody)
        val description = extractDescription(body)

        return PendingTransaction(
            id = dedupeId(sender, body, timestampMillis),
            type = type,
            amount = amount,
            bank = bank,
            paymentMethod = paymentMethod,
            category = if (type == TransactionType.DEBIT) "Uncategorized" else "",
            source = if (type == TransactionType.CREDIT) bank else "",
            description = description,
            timestamp = timestampMillis,
            rawSms = body
        )
    }

    /**
     * Stable id for one physical message, so a duplicated broadcast — or a re-parse after a
     * restart — collides with the existing row instead of adding a second pending entry.
     */
    fun dedupeId(sender: String, body: String, timestampMillis: Long): String =
        MessageDigest.getInstance("SHA-256")
            .digest("$sender|$body|$timestampMillis".toByteArray())
            .take(16)
            .joinToString("") { "%02x".format(it) }

    private fun resolveBank(sender: String, body: String): String? {
        val upperSender = sender.uppercase()
        val upperBody = body.uppercase()

        // Try sender ID first
        bankSenderMap.entries.firstOrNull { upperSender.contains(it.key) }
            ?.value?.let { return it }

        // Fallback: scan body for bank identifiers (handles numeric senders)
        bankBodyMap.entries.firstOrNull { upperBody.contains(it.key.uppercase()) }
            ?.value?.let { return it }

        return null
    }

    private fun resolvePaymentMethod(body: String): String = when {
        body.contains("upi") || upiIdRegex.containsMatchIn(body) -> "UPI"  // match UPI ID pattern
        body.contains("neft") -> "NEFT"
        body.contains("imps") -> "IMPS"
        body.contains("rtgs") -> "RTGS"
        body.contains("atm") -> "ATM"
        body.contains("credit card") || body.contains("cc ") -> "Credit Card"
        body.contains("debit card") || body.contains("dc ") -> "Debit Card"
        else -> "Bank Transfer"
    }

    private fun extractDescription(body: String): String {
        // Extract UPI ID (e.g. 94006XXXXX@ptaxis)
        val upiRegex = Regex("""[\w.]+@[\w]+""")
        upiRegex.find(body)?.value?.let { return it }

        // Fallback: grab text after "to" or "from"
        val refRegex = Regex("""(?:to|from)\s+([A-Za-z0-9@.\-_ ]{3,30})""", RegexOption.IGNORE_CASE)
        return refRegex.find(body)?.groupValues?.get(1)?.trim() ?: ""
    }
}