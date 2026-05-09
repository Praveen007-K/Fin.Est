package com.pkoder.finest.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pkoder.finest.data.local.entities.CreditEntryEntity

@Dao
interface CreditEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(creditEntry: CreditEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(credits: List<CreditEntryEntity>)

    @Query("SELECT * FROM credit_entries ORDER BY timestamp DESC")
    suspend fun getAllCredits(): List<CreditEntryEntity>

    @Query("DELETE FROM credit_entries")
    suspend fun clearAll()
}
