package com.pkoder.finest.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.pkoder.finest.data.local.entities.DebitEntryEntity

@Dao
interface DebitEntryDao {
    @Insert
    suspend fun insert(debitEntry: DebitEntryEntity)

    @Query("SELECT * FROM debit_entries ORDER BY timestamp DESC")
    suspend fun getAllDebits(): List<DebitEntryEntity>
}
