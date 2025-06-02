package com.pkoder.finest.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.pkoder.finest.data.local.entities.CreditEntryEntity

@Dao
interface CreditEntryDao {
    @Insert
    suspend fun insert(creditEntry: CreditEntryEntity)

    @Query("SELECT * FROM credit_entries ORDER BY timestamp DESC")
    suspend fun getAllCredits(): List<CreditEntryEntity>
}
