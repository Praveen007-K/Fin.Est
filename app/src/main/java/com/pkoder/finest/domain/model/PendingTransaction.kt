package com.pkoder.finest.domain

import java.util.UUID

data class PendingTransaction(
    val id: String = UUID.randomUUID().toString(),
    val type: TransactionType,
    val amount: Double,
    val bank: String,
    val paymentMethod: String = "SMS",
    val category: String = "Uncategorized",
    val source: String = "",
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val rawSms: String = ""
)

enum class TransactionType { DEBIT, CREDIT }