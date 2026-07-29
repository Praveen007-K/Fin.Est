package com.pkoder.finest.domain.model

/**
 * The vocabulary the entry forms and the SMS review sheet share.
 *
 * These used to be duplicated inside each tab composable, which let the manual forms and the SMS
 * parser drift apart (the parser can emit "NEFT"/"IMPS"/"Bank Transfer", which no dropdown offered).
 */
object TransactionOptions {

    val expenseCategories = listOf(
        "Housing", "Food", "Transport", "Utilities", "Dependents",
        "Entertainment", "Health", "Finance", "Shopping", "Uncategorized"
    )

    val paymentMethods = listOf(
        "UPI", "Cash", "Card", "Credit Card", "Debit Card",
        "Net Banking", "NEFT", "IMPS", "RTGS", "ATM", "Bank Transfer"
    )

    val banks = listOf("SBI", "BOB", "HDFC")

    val incomeSources = listOf("Salary", "Freelance", "Gift", "Interest", "Refund", "Other")

    /** Dropdowns must still show a value the parser produced but the list doesn't contain. */
    fun withCurrent(options: List<String>, current: String): List<String> =
        if (current.isBlank() || current in options) options else options + current
}
