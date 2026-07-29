package com.pkoder.finest.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pkoder.finest.data.local.entities.PendingTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingTransactionDao {

    /**
     * IGNORE dedupes re-delivered SMS: the id is derived from sender + body + SMS timestamp, so
     * the same message always maps to the same row.
     *
     * @return the new rowId, or -1 when the row already existed.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: PendingTransactionEntity): Long

    @Query("SELECT * FROM pending_transactions ORDER BY timestamp DESC")
    fun observePending(): Flow<List<PendingTransactionEntity>>

    @Query("SELECT * FROM pending_transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): PendingTransactionEntity?

    @Update
    suspend fun update(entity: PendingTransactionEntity)

    @Query("DELETE FROM pending_transactions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM pending_transactions")
    suspend fun clearAll()
}
