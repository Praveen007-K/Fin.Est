package com.pkoder.finest.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "debit_entries")
data class DebitEntryEntity(
    @PrimaryKey val firestoreId: String = "",
    val category: String = "",
    val paymentMethod: String = "",
    val bank: String = "",
    val amount: Double = 0.0,
    val description: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)