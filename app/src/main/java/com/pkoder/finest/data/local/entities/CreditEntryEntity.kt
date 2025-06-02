package com.pkoder.finest.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "credit_entries")
data class CreditEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val source: String,
    val amount: Double,
    val timestamp: Long = System.currentTimeMillis()
)
