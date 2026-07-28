package com.pkoder.finest.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.pkoder.finest.data.local.dao.CreditEntryDao
import com.pkoder.finest.data.local.dao.DebitEntryDao
import com.pkoder.finest.data.local.dao.PendingTransactionDao
import com.pkoder.finest.data.local.entities.CreditEntryEntity
import com.pkoder.finest.data.local.entities.DebitEntryEntity
import com.pkoder.finest.data.local.entities.PendingTransactionEntity

@Database(
    entities = [DebitEntryEntity::class, CreditEntryEntity::class, PendingTransactionEntity::class],
    version = 3,
    exportSchema = false
)
abstract class FinanceDatabase : RoomDatabase() {
    abstract fun debitEntryDao(): DebitEntryDao
    abstract fun creditEntryDao(): CreditEntryDao
    abstract fun pendingTransactionDao(): PendingTransactionDao
}
