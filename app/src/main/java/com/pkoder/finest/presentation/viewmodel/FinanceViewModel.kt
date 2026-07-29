package com.pkoder.finest.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pkoder.finest.data.local.entities.CreditEntryEntity
import com.pkoder.finest.data.local.entities.DebitEntryEntity
import com.pkoder.finest.data.repository.FinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Entries come straight from Room as flows, so every screen sees the same data and no screen has
 * to ask for a reload. [refresh] only deals with the network.
 */
@HiltViewModel
class FinanceViewModel @Inject constructor(
    private val repository: FinanceRepository
) : ViewModel() {

    val debits: StateFlow<List<DebitEntryEntity>> = repository.observeDebits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val credits: StateFlow<List<CreditEntryEntity>> = repository.observeCredits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        refresh()
    }

    /** Uploads anything queued offline, then pulls the server state. */
    fun refresh() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                repository.syncNow()
            } catch (e: Exception) {
                _errorMessage.value = "Couldn't reach the cloud. Your data is saved on this device."
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun consumeErrorMessage() {
        _errorMessage.value = null
    }

    fun addDebit(debit: DebitEntryEntity) {
        viewModelScope.launch { repository.insertDebit(debit) }
    }

    fun addCredit(credit: CreditEntryEntity) {
        viewModelScope.launch { repository.insertCredit(credit) }
    }

    fun deleteDebit(firestoreId: String) {
        viewModelScope.launch { repository.deleteDebit(firestoreId) }
    }

    fun deleteCredit(firestoreId: String) {
        viewModelScope.launch { repository.deleteCredit(firestoreId) }
    }

    /** Rows hidden while their undo window is open. */
    private val _pendingDeletions = MutableStateFlow<Set<String>>(emptySet())
    val pendingDeletions: StateFlow<Set<String>> = _pendingDeletions.asStateFlow()

    /**
     * Hides a row, waits for [awaitUndo] to report whether the user took it back, then either
     * restores it or deletes for real — so an accidental swipe costs nothing and no document id
     * is churned in Firestore.
     *
     * Deliberately runs in `viewModelScope`, not the screen's: leaving the history tab while the
     * snackbar is still up must not abandon the deletion half-done.
     */
    fun deleteWithUndo(id: String, isExpense: Boolean, awaitUndo: suspend () -> Boolean) {
        viewModelScope.launch {
            _pendingDeletions.value = _pendingDeletions.value + id
            val undone = awaitUndo()
            if (!undone) {
                if (isExpense) repository.deleteDebit(id) else repository.deleteCredit(id)
            }
            _pendingDeletions.value = _pendingDeletions.value - id
        }
    }

    fun updateDebit(debitEntry: DebitEntryEntity) {
        viewModelScope.launch { repository.updateDebit(debitEntry) }
    }

    fun updateCredit(creditEntry: CreditEntryEntity) {
        viewModelScope.launch { repository.updateCredit(creditEntry) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
