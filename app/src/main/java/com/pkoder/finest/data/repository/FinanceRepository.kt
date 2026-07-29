package com.pkoder.finest.data.repository

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.pkoder.finest.data.local.dao.CreditEntryDao
import com.pkoder.finest.data.local.dao.DebitEntryDao
import com.pkoder.finest.data.local.dao.PendingTransactionDao
import com.pkoder.finest.data.local.entities.CreditEntryEntity
import com.pkoder.finest.data.local.entities.DebitEntryEntity
import com.pkoder.finest.data.local.entities.PendingTransactionEntity
import com.pkoder.finest.domain.model.PendingTransaction
import com.pkoder.finest.domain.model.TransactionType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room is the single source of truth the UI observes; Firestore is a replica kept in step by
 * [syncNow].
 *
 * Writes are **local-first**: the document id is generated client-side, the row is stored in Room
 * immediately with `synced = false`, and only then pushed to Firestore. That keeps the UI instant
 * and offline-correct, and because the id is fixed up front a retried upload overwrites the same
 * document instead of creating a duplicate.
 */
@Singleton
class FinanceRepository @Inject constructor(
    private val debitDao: DebitEntryDao,
    private val creditDao: CreditEntryDao,
    private val pendingDao: PendingTransactionDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    companion object {
        private const val USERS_COLLECTION = "users"
        private const val DEBIT_ENTRIES_COLLECTION = "debit_entries"
        private const val CREDIT_ENTRIES_COLLECTION = "credit_entries"
        private const val TAG = "FinanceRepository"

        /**
         * Firestore acks writes only against the server, so an await would hang for as long as
         * the device is offline. Cap it and let the sync queue retry instead.
         */
        private const val REMOTE_WRITE_TIMEOUT_MS = 8_000L
    }

    private fun getUserDocument(): DocumentReference? = auth.currentUser?.uid?.let {
        firestore.collection(USERS_COLLECTION).document(it)
    } ?: run { Log.d(TAG, "User is null"); null }

    // ── Observation (single source of truth) ──────────────────────────────────

    fun observeDebits(): Flow<List<DebitEntryEntity>> = debitDao.observeDebits()

    fun observeCredits(): Flow<List<CreditEntryEntity>> = creditDao.observeCredits()

    fun observePending(): Flow<List<PendingTransaction>> =
        pendingDao.observePending().map { rows -> rows.map { it.toDomain() } }

    // ── Writes ───────────────────────────────────────────────────────────────

    suspend fun insertDebit(debitEntry: DebitEntryEntity) {
        val collection = getUserDocument()?.collection(DEBIT_ENTRIES_COLLECTION)
        // document() generates an id offline, without touching the network.
        val id = collection?.document()?.id ?: UUID.randomUUID().toString()
        val local = debitEntry.copy(firestoreId = id, synced = false)
        debitDao.insert(local)

        if (collection == null) return
        val pushed = pushRemote("debit $id") { collection.document(id).set(local) }
        if (pushed) debitDao.update(local.copy(synced = true))
    }

    suspend fun insertCredit(creditEntry: CreditEntryEntity) {
        val collection = getUserDocument()?.collection(CREDIT_ENTRIES_COLLECTION)
        val id = collection?.document()?.id ?: UUID.randomUUID().toString()
        val local = creditEntry.copy(firestoreId = id, synced = false)
        creditDao.insert(local)

        if (collection == null) return
        val pushed = pushRemote("credit $id") { collection.document(id).set(local) }
        if (pushed) creditDao.update(local.copy(synced = true))
    }

    suspend fun updateDebit(debitEntry: DebitEntryEntity) {
        val collection = getUserDocument()?.collection(DEBIT_ENTRIES_COLLECTION)
        val pushed = collection != null && pushRemote("debit ${debitEntry.firestoreId}") {
            collection.document(debitEntry.firestoreId).set(debitEntry)
        }
        // Unsynced rows are picked up by syncNow(), which re-sends the whole document.
        debitDao.update(debitEntry.copy(synced = pushed))
    }

    suspend fun updateCredit(creditEntry: CreditEntryEntity) {
        val collection = getUserDocument()?.collection(CREDIT_ENTRIES_COLLECTION)
        val pushed = collection != null && pushRemote("credit ${creditEntry.firestoreId}") {
            collection.document(creditEntry.firestoreId).set(creditEntry)
        }
        creditDao.update(creditEntry.copy(synced = pushed))
    }

    suspend fun deleteDebit(firestoreId: String) {
        val neverUploaded = debitDao.getById(firestoreId)?.synced == false
        val collection = getUserDocument()?.collection(DEBIT_ENTRIES_COLLECTION)
        if (!neverUploaded && collection != null) {
            pushRemote("delete debit $firestoreId") { collection.document(firestoreId).delete() }
        }
        debitDao.deleteById(firestoreId)
    }

    suspend fun deleteCredit(firestoreId: String) {
        val neverUploaded = creditDao.getById(firestoreId)?.synced == false
        val collection = getUserDocument()?.collection(CREDIT_ENTRIES_COLLECTION)
        if (!neverUploaded && collection != null) {
            pushRemote("delete credit $firestoreId") { collection.document(firestoreId).delete() }
        }
        creditDao.deleteById(firestoreId)
    }

    /**
     * Runs a Firestore write with a timeout. Returns true only when the server acked it.
     *
     * Note `Task<Void>.await()` yields null on success, so the timeout result cannot be tested
     * for nullness directly — the lambda returns an explicit flag instead.
     */
    private suspend fun pushRemote(label: String, block: () -> Task<Void>): Boolean =
        try {
            withTimeoutOrNull(REMOTE_WRITE_TIMEOUT_MS) {
                block().await()
                true
            } == true
        } catch (e: CancellationException) {
            throw e // never swallow cancellation of the calling scope
        } catch (e: Exception) {
            Log.e(TAG, "Remote write failed ($label): ${e.message}", e)
            false
        }

    // ── Sync ─────────────────────────────────────────────────────────────────

    /**
     * Pushes everything still marked unsynced, then pulls the server state and drops local rows
     * that were deleted elsewhere. Throws if a pull failed, so the caller can surface it.
     */
    suspend fun syncNow() {
        val userDoc = getUserDocument() ?: return

        uploadUnsyncedDebits(userDoc)
        uploadUnsyncedCredits(userDoc)

        // One collection failing must not stop the other; the first error is reported at the end.
        var firstError: Exception? = null
        try {
            pullDebits(userDoc)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch debits: ${e.message}", e)
            firstError = e
        }
        try {
            pullCredits(userDoc)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch credits: ${e.message}", e)
            if (firstError == null) firstError = e
        }
        firstError?.let { throw it }
    }

    private suspend fun uploadUnsyncedDebits(userDoc: DocumentReference) {
        val collection = userDoc.collection(DEBIT_ENTRIES_COLLECTION)
        for (row in debitDao.getUnsynced()) {
            // Legacy rows from before client-generated ids: give them a real document id.
            val isLegacyLocalId = row.firestoreId.startsWith(LEGACY_LOCAL_ID_PREFIX)
            val targetId = if (isLegacyLocalId) collection.document().id else row.firestoreId
            val uploaded = row.copy(firestoreId = targetId, synced = true)
            if (!pushRemote("debit $targetId") { collection.document(targetId).set(uploaded) }) continue

            if (isLegacyLocalId) debitDao.deleteById(row.firestoreId)
            debitDao.insert(uploaded)
            Log.d(TAG, "Uploaded pending debit $targetId")
        }
    }

    private suspend fun uploadUnsyncedCredits(userDoc: DocumentReference) {
        val collection = userDoc.collection(CREDIT_ENTRIES_COLLECTION)
        for (row in creditDao.getUnsynced()) {
            val isLegacyLocalId = row.firestoreId.startsWith(LEGACY_LOCAL_ID_PREFIX)
            val targetId = if (isLegacyLocalId) collection.document().id else row.firestoreId
            val uploaded = row.copy(firestoreId = targetId, synced = true)
            if (!pushRemote("credit $targetId") { collection.document(targetId).set(uploaded) }) continue

            if (isLegacyLocalId) creditDao.deleteById(row.firestoreId)
            creditDao.insert(uploaded)
            Log.d(TAG, "Uploaded pending credit $targetId")
        }
    }

    private suspend fun pullDebits(userDoc: DocumentReference) {
        val snapshot = userDoc.collection(DEBIT_ENTRIES_COLLECTION).get().await()
        val remote = snapshot.documents.mapNotNull { doc ->
            doc.toObject(DebitEntryEntity::class.java)?.copy(firestoreId = doc.id, synced = true)
        }
        // Never let the server copy clobber a local edit that has not been pushed yet.
        val unsynced = debitDao.getUnsyncedIds().toHashSet()
        debitDao.insertAll(remote.filterNot { it.firestoreId in unsynced })

        // Offline, get() is served from Firestore's cache. That snapshot is not authoritative, so
        // pruning against it could delete rows that are alive on the server.
        if (!snapshot.metadata.isFromCache) {
            val prune = idsToPrune(debitDao.getSyncedIds(), remote.map { it.firestoreId })
            if (prune.isNotEmpty()) {
                debitDao.deleteByIds(prune)
                Log.d(TAG, "Pruned ${prune.size} debits deleted elsewhere")
            }
        }
        Log.d(TAG, "Fetched ${remote.size} debits (fromCache=${snapshot.metadata.isFromCache})")
    }

    private suspend fun pullCredits(userDoc: DocumentReference) {
        val snapshot = userDoc.collection(CREDIT_ENTRIES_COLLECTION).get().await()
        val remote = snapshot.documents.mapNotNull { doc ->
            doc.toObject(CreditEntryEntity::class.java)?.copy(firestoreId = doc.id, synced = true)
        }
        val unsynced = creditDao.getUnsyncedIds().toHashSet()
        creditDao.insertAll(remote.filterNot { it.firestoreId in unsynced })

        // See pullDebits: a cache-backed snapshot must not drive deletions.
        if (!snapshot.metadata.isFromCache) {
            val prune = idsToPrune(creditDao.getSyncedIds(), remote.map { it.firestoreId })
            if (prune.isNotEmpty()) {
                creditDao.deleteByIds(prune)
                Log.d(TAG, "Pruned ${prune.size} credits deleted elsewhere")
            }
        }
        Log.d(TAG, "Fetched ${remote.size} credits (fromCache=${snapshot.metadata.isFromCache})")
    }

    /** Called on sign-out so the next user never sees the previous one's rows. */
    suspend fun clearLocalData() {
        debitDao.clearAll()
        creditDao.clearAll()
        pendingDao.clearAll()
        Log.d(TAG, "Local data cleared")
    }

    // ── Pending (SMS-derived) transactions ───────────────────────────────────

    /** @return false when this message was already queued (duplicate broadcast, re-parse, …). */
    suspend fun insertPending(transaction: PendingTransaction): Boolean =
        pendingDao.insert(transaction.toEntity()) != -1L

    suspend fun getPending(id: String): PendingTransaction? = pendingDao.getById(id)?.toDomain()

    /** Persists edits made in the review screen before the entry is approved. */
    suspend fun updatePending(transaction: PendingTransaction) {
        pendingDao.update(transaction.toEntity())
    }

    suspend fun deletePending(id: String) {
        pendingDao.deleteById(id)
    }

    /**
     * Turns a reviewed SMS transaction into a real entry. Shared by the review screen and the
     * notification action, so both behave identically.
     */
    suspend fun approvePending(transaction: PendingTransaction) {
        when (transaction.type) {
            TransactionType.DEBIT -> insertDebit(
                DebitEntryEntity(
                    category = transaction.category.ifBlank { "Uncategorized" },
                    paymentMethod = transaction.paymentMethod,
                    bank = transaction.bank,
                    amount = transaction.amount,
                    description = transaction.description.ifBlank { null },
                    timestamp = transaction.timestamp
                )
            )

            TransactionType.CREDIT -> insertCredit(
                CreditEntryEntity(
                    source = transaction.source.ifBlank { transaction.bank },
                    amount = transaction.amount,
                    timestamp = transaction.timestamp
                )
            )
        }
        deletePending(transaction.id)
    }

    /** Approves straight from a notification action, where only the id is known. */
    suspend fun approvePending(id: String): Boolean {
        val pending = getPending(id) ?: return false
        approvePending(pending)
        return true
    }
}

private const val LEGACY_LOCAL_ID_PREFIX = "local_"

private fun PendingTransactionEntity.toDomain() = PendingTransaction(
    id = id,
    type = TransactionType.valueOf(type),
    amount = amount,
    bank = bank,
    paymentMethod = paymentMethod,
    category = category,
    source = source,
    description = description,
    timestamp = timestamp,
    rawSms = rawSms
)

private fun PendingTransaction.toEntity() = PendingTransactionEntity(
    id = id,
    type = type.name,
    amount = amount,
    bank = bank,
    paymentMethod = paymentMethod,
    category = category,
    source = source,
    description = description,
    timestamp = timestamp,
    rawSms = rawSms
)
