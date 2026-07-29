package com.pkoder.finest.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pkoder.finest.data.local.entities.CreditEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CreditEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(creditEntry: CreditEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(credits: List<CreditEntryEntity>)

    /** Single source of truth for the UI — re-emits on every local write. */
    @Query("SELECT * FROM credit_entries ORDER BY timestamp DESC")
    fun observeCredits(): Flow<List<CreditEntryEntity>>

    @Query("SELECT * FROM credit_entries WHERE firestoreId = :id LIMIT 1")
    suspend fun getById(id: String): CreditEntryEntity?

    /** Rows created while offline, waiting to be uploaded. */
    @Query("SELECT * FROM credit_entries WHERE synced = 0")
    suspend fun getUnsynced(): List<CreditEntryEntity>

    @Query("SELECT firestoreId FROM credit_entries WHERE synced = 1")
    suspend fun getSyncedIds(): List<String>

    @Query("SELECT firestoreId FROM credit_entries WHERE synced = 0")
    suspend fun getUnsyncedIds(): List<String>

    @Query("DELETE FROM credit_entries")
    suspend fun clearAll()

    @Update
    suspend fun update(creditEntry: CreditEntryEntity)

    @Query("DELETE FROM credit_entries WHERE firestoreId = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM credit_entries WHERE firestoreId IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)
}
