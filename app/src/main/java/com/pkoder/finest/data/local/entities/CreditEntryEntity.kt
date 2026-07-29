package com.pkoder.finest.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude

@Entity(tableName = "credit_entries")
data class CreditEntryEntity(
    // See DebitEntryEntity for why firestoreId and synced are excluded from Firestore.
    @PrimaryKey @get:Exclude val firestoreId: String = "",
    val source: String = "",
    val amount: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "1") @get:Exclude val synced: Boolean = true
)
