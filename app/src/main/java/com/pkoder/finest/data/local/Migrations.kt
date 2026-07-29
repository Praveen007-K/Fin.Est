package com.pkoder.finest.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v3 → v4: adds the `synced` flag used by [com.pkoder.finest.data.repository.FinanceRepository.syncNow].
 *
 * Rows created while offline before this version carry a `local_<millis>` id and were never
 * uploaded, so they are back-filled as unsynced and the sync queue picks them up on the next
 * refresh. Everything else already lives in Firestore and is marked synced.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE debit_entries ADD COLUMN synced INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE credit_entries ADD COLUMN synced INTEGER NOT NULL DEFAULT 1")
        db.execSQL("UPDATE debit_entries SET synced = 0 WHERE firestoreId LIKE 'local_%'")
        db.execSQL("UPDATE credit_entries SET synced = 0 WHERE firestoreId LIKE 'local_%'")
    }
}
