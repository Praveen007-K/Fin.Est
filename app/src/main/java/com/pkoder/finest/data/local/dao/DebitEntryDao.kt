package com.pkoder.finest.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pkoder.finest.data.local.entities.DebitEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DebitEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(debitEntry: DebitEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(debits: List<DebitEntryEntity>)

    /** Single source of truth for the UI — re-emits on every local write. */
    @Query("SELECT * FROM debit_entries ORDER BY timestamp DESC")
    fun observeDebits(): Flow<List<DebitEntryEntity>>

    @Query("SELECT * FROM debit_entries WHERE firestoreId = :id LIMIT 1")
    suspend fun getById(id: String): DebitEntryEntity?

    /** Rows created while offline, waiting to be uploaded. */
    @Query("SELECT * FROM debit_entries WHERE synced = 0")
    suspend fun getUnsynced(): List<DebitEntryEntity>

    @Query("SELECT firestoreId FROM debit_entries WHERE synced = 1")
    suspend fun getSyncedIds(): List<String>

    @Query("SELECT firestoreId FROM debit_entries WHERE synced = 0")
    suspend fun getUnsyncedIds(): List<String>

    @Query("DELETE FROM debit_entries")
    suspend fun clearAll()

    @Update
    suspend fun update(debitEntry: DebitEntryEntity)

    @Query("DELETE FROM debit_entries WHERE firestoreId = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM debit_entries WHERE firestoreId IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)
}
