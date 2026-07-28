package com.pkoder.finest.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_transactions")
data class PendingTransactionEntity(
    @PrimaryKey val id: String,
    val type: String,           // "DEBIT" or "CREDIT"
    val amount: Double,
    val bank: String,
    val paymentMethod: String,
    val category: String,
    val source: String,
    val description: String,
    val timestamp: Long,
    val rawSms: String
)
