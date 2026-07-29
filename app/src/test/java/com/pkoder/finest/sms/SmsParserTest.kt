package com.pkoder.finest.sms

import com.pkoder.finest.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** [SmsParser] is a pure object, so it is the cheapest place to lock down capture behaviour. */
class SmsParserTest {

    private val timestamp = 1_754_000_000_000L

    @Test
    fun `parses SBI debit with currency prefix`() {
        val parsed = SmsParser.parse(
            sender = "JD-SBIINB-S",
            body = "Dear UPI user A/C X1234 debited by Rs.1,250.50 on date 12Aug25 trf to SWIGGY Refno 123456",
            timestampMillis = timestamp
        )

        requireNotNull(parsed)
        assertEquals(TransactionType.DEBIT, parsed.type)
        assertEquals(1250.50, parsed.amount, 0.001)
        assertEquals("SBI", parsed.bank)
        assertEquals("UPI", parsed.paymentMethod)
        assertEquals("Uncategorized", parsed.category)
        assertEquals(timestamp, parsed.timestamp)
    }

    @Test
    fun `parses amount without currency prefix via fallback`() {
        val parsed = SmsParser.parse(
            "SBIINB",
            "Your A/c XX987 debited by 5849.25 on 01Sep25",
            timestamp
        )

        requireNotNull(parsed)
        assertEquals(5849.25, parsed.amount, 0.001)
    }

    @Test
    fun `parses HDFC credit and uses bank as source`() {
        val parsed = SmsParser.parse(
            "AD-HDFCBK",
            "INR 45,000.00 credited to your A/c XX4455 on 01Sep25 by NEFT",
            timestamp
        )

        requireNotNull(parsed)
        assertEquals(TransactionType.CREDIT, parsed.type)
        assertEquals(45000.0, parsed.amount, 0.001)
        assertEquals("HDFC", parsed.bank)
        assertEquals("HDFC", parsed.source)
        assertEquals("NEFT", parsed.paymentMethod)
        assertTrue(parsed.category.isEmpty())
    }

    @Test
    fun `resolves bank from body when sender is a numeric shortcode`() {
        val parsed = SmsParser.parse(
            "51234",
            "Rs.500 Dr. from A/C XX11 to upi@ybl -Bank of Baroda",
            timestamp
        )

        requireNotNull(parsed)
        assertEquals("BOB", parsed.bank)
        assertEquals(TransactionType.DEBIT, parsed.type)
    }

    @Test
    fun `debit wins over credit for UPI Dr Cr pairs`() {
        // BOB phrases UPI transfers as "Dr. ... Cr. ..." — the Cr. half is the recipient's.
        val parsed = SmsParser.parse(
            "BOBSMS",
            "Rs.200 Dr. from A/C XX11 and Cr. to merchant@okhdfcbank",
            timestamp
        )

        requireNotNull(parsed)
        assertEquals(TransactionType.DEBIT, parsed.type)
    }

    @Test
    fun `unknown bank is ignored`() {
        assertNull(
            SmsParser.parse("VM-AXISBK", "Rs.100 debited from your account", timestamp)
        )
    }

    @Test
    fun `message with no amount is ignored`() {
        assertNull(
            SmsParser.parse("SBIINB", "Your SBI account statement is ready", timestamp)
        )
    }

    @Test
    fun `extracts the UPI id as the description`() {
        val parsed = SmsParser.parse(
            "SBIUPI",
            "Rs.75.00 debited, trf to swiggy.orders@okicici Refno 99",
            timestamp
        )

        requireNotNull(parsed)
        assertEquals("swiggy.orders@okicici", parsed.description)
    }

    @Test
    fun `dedupe id is stable for the same message and differs when anything changes`() {
        val body = "Rs.100 debited from A/C XX1 -SBI"
        val first = SmsParser.dedupeId("SBIINB", body, timestamp)
        val second = SmsParser.dedupeId("SBIINB", body, timestamp)

        assertEquals(first, second)
        assertNotEquals(first, SmsParser.dedupeId("SBIINB", body, timestamp + 1))
        assertNotEquals(first, SmsParser.dedupeId("HDFCBK", body, timestamp))
        assertNotEquals(first, SmsParser.dedupeId("SBIINB", body + " x", timestamp))
    }

    @Test
    fun `identical message parsed twice produces the same id so the insert dedupes`() {
        val sender = "SBIINB"
        val body = "Rs.100 debited from A/C XX1"

        val first = SmsParser.parse(sender, body, timestamp)
        val second = SmsParser.parse(sender, body, timestamp)

        assertEquals(first!!.id, second!!.id)
    }
}
