package com.pkoder.finest.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.pkoder.finest.data.local.dao.CreditEntryDao
import com.pkoder.finest.data.local.dao.DebitEntryDao
import com.pkoder.finest.data.local.entities.CreditEntryEntity
import com.pkoder.finest.data.local.entities.DebitEntryEntity
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FinanceRepository @Inject constructor(
    private val debitDao: DebitEntryDao,
    private val creditDao: CreditEntryDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    companion object {
        private const val USERS_COLLECTION = "users"
        private const val DEBIT_ENTRIES_COLLECTION = "debit_entries"
        private const val CREDIT_ENTRIES_COLLECTION = "credit_entries"
        private const val TAG = "FinanceRepository"
    }

    private fun getUserDocument() = auth.currentUser?.uid?.let {
        firestore.collection(USERS_COLLECTION).document(it)
    } ?: run { Log.d(TAG, "User is null"); null }

    suspend fun insertDebit(debitEntry: DebitEntryEntity) {
        try {
            val docRef = getUserDocument()
                ?.collection(DEBIT_ENTRIES_COLLECTION)
                ?.add(debitEntry)
                ?.await()
            // Save to Room with Firestore doc ID to prevent duplicates
            val entryWithId = debitEntry.copy(firestoreId = docRef?.id ?: "")
            debitDao.insert(entryWithId)
            Log.d(TAG, "Debit inserted with ID: ${docRef?.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to insert debit: ${e.message}", e)
            // Still save locally even if Firestore fails
            debitDao.insert(debitEntry.copy(firestoreId = "local_${System.currentTimeMillis()}"))
        }
    }

    suspend fun insertCredit(creditEntry: CreditEntryEntity) {
        try {
            val docRef = getUserDocument()
                ?.collection(CREDIT_ENTRIES_COLLECTION)
                ?.add(creditEntry)
                ?.await()
            val entryWithId = creditEntry.copy(firestoreId = docRef?.id ?: "")
            creditDao.insert(entryWithId)
            Log.d(TAG, "Credit inserted with ID: ${docRef?.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to insert credit: ${e.message}", e)
            creditDao.insert(creditEntry.copy(firestoreId = "local_${System.currentTimeMillis()}"))
        }
    }

    suspend fun getAllDebits(): List<DebitEntryEntity> {
        val userDoc = getUserDocument()
        if (userDoc != null) {
            try {
                val snapshot = userDoc.collection(DEBIT_ENTRIES_COLLECTION).get().await()
                // Map Firestore doc ID into each entity before inserting
                val debits = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(DebitEntryEntity::class.java)?.copy(firestoreId = doc.id)
                }
                debitDao.insertAll(debits)
                Log.d(TAG, "Fetched ${debits.size} debits from Firestore")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch debits: ${e.message}", e)
            }
        }
        return debitDao.getAllDebits()
    }

    suspend fun getAllCredits(): List<CreditEntryEntity> {
        val userDoc = getUserDocument()
        if (userDoc != null) {
            try {
                val snapshot = userDoc.collection(CREDIT_ENTRIES_COLLECTION).get().await()
                val credits = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(CreditEntryEntity::class.java)?.copy(firestoreId = doc.id)
                }
                creditDao.insertAll(credits)
                Log.d(TAG, "Fetched ${credits.size} credits from Firestore")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch credits: ${e.message}", e)
            }
        }
        return creditDao.getAllCredits()
    }

    suspend fun clearLocalData() {
        debitDao.clearAll()
        creditDao.clearAll()
        Log.d(TAG, "Local data cleared")
    }
}