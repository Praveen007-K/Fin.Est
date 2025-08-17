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
        Log.d(TAG, "Current user UID: $it")
        firestore.collection(USERS_COLLECTION).document(it)
    } ?: run { Log.d(TAG, "Current user is null."); null }

    suspend fun insertDebit(debitEntry: DebitEntryEntity) {
        Log.d(TAG, "Attempting to insert debit to Firestore.")
        try {
            getUserDocument()?.collection(DEBIT_ENTRIES_COLLECTION)?.add(debitEntry)?.await()
            Log.d(TAG, "Debit successfully added to Firestore.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add debit to Firestore: ${e.message}", e)
        }
        debitDao.insert(debitEntry)
    }

    suspend fun insertCredit(creditEntry: CreditEntryEntity) {
        Log.d(TAG, "Attempting to insert credit to Firestore.")
        try {
            getUserDocument()?.collection(CREDIT_ENTRIES_COLLECTION)?.add(creditEntry)?.await()
            Log.d(TAG, "Credit successfully added to Firestore.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add credit to Firestore: ${e.message}", e)
        }
        creditDao.insert(creditEntry)
    }

    suspend fun getAllDebits(): List<DebitEntryEntity> {
        Log.d(TAG, "Attempting to get all debits from Firestore.")
        val userDoc = getUserDocument()
        if (userDoc != null) {
            try {
                val snapshot = userDoc.collection(DEBIT_ENTRIES_COLLECTION).get().await()
                val debits = snapshot.toObjects(DebitEntryEntity::class.java)
                debitDao.insertAll(debits)
                Log.d(TAG, "Debits successfully fetched from Firestore and inserted into local DB.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get debits from Firestore: ${e.message}", e)
            }
        } else {
            Log.d(TAG, "Cannot get debits from Firestore: User document is null.")
        }
        return debitDao.getAllDebits()
    }

    suspend fun getAllCredits(): List<CreditEntryEntity> {
        Log.d(TAG, "Attempting to get all credits from Firestore.")
        val userDoc = getUserDocument()
        if (userDoc != null) {
            try {
                val snapshot = userDoc.collection(CREDIT_ENTRIES_COLLECTION).get().await()
                val credits = snapshot.toObjects(CreditEntryEntity::class.java)
                creditDao.insertAll(credits)
                Log.d(TAG, "Credits successfully fetched from Firestore and inserted into local DB.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get credits from Firestore: ${e.message}", e)
            }
        } else {
            Log.d(TAG, "Cannot get credits from Firestore: User document is null.")
        }
        return creditDao.getAllCredits()
    }
}
