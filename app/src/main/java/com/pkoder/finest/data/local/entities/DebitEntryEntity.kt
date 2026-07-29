package com.pkoder.finest.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude

@Entity(tableName = "debit_entries")
data class DebitEntryEntity(
    // Firestore doc id doubles as the Room primary key, so re-syncing is idempotent.
    // @get:Exclude keeps it out of the remote document (it is the document's own id).
    @PrimaryKey @get:Exclude val firestoreId: String = "",
    val category: String = "",
    val paymentMethod: String = "",
    val bank: String = "",
    val amount: Double = 0.0,
    val description: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    // false only for rows created while offline; syncNow() uploads them and flips this.
    // Local-only bookkeeping, so it is excluded from Firestore too — rows read back from
    // the server fall back to the default (true).
    @ColumnInfo(defaultValue = "1") @get:Exclude val synced: Boolean = true
)
