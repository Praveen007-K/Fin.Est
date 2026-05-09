package com.pkoder.finest.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pkoder.finest.data.local.entities.DebitEntryEntity

@Dao
interface DebitEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(debitEntry: DebitEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(debits: List<DebitEntryEntity>)

    @Query("SELECT * FROM debit_entries ORDER BY timestamp DESC")
    suspend fun getAllDebits(): List<DebitEntryEntity>

    @Query("DELETE FROM debit_entries")
    suspend fun clearAll()

    @Update
    suspend fun update(debitEntry: DebitEntryEntity)

    @Query("DELETE FROM debit_entries WHERE firestoreId = :id")
    suspend fun deleteById(id: String)
}
