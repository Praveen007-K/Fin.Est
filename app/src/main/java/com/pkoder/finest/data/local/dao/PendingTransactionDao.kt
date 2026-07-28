package com.pkoder.finest.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pkoder.finest.data.local.entities.PendingTransactionEntity

@Dao
interface PendingTransactionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE) // IGNORE avoids re-inserting duplicates
    suspend fun insert(entity: PendingTransactionEntity)

    @Query("SELECT * FROM pending_transactions ORDER BY timestamp DESC")
    suspend fun getAll(): List<PendingTransactionEntity>

    @Query("DELETE FROM pending_transactions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM pending_transactions")
    suspend fun clearAll()
}
