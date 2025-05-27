package com.example.finest.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "debit_entries")
data class DebitEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val category: String,
    val paymentMethod: String,
    val bank: String,
    val amount: Double,
    val description: String?,
    val timestamp: Long = System.currentTimeMillis()
)
