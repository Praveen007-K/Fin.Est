package com.pkoder.finest.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pkoder.finest.data.repository.FinanceRepository
import com.pkoder.finest.domain.model.PendingTransaction
import com.pkoder.finest.notification.TransactionNotifier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Pending SMS transactions are observed from Room, so rows written by the SMS receiver — a
 * separate component that also runs while the app is closed — show up with no callback wiring.
 */
@HiltViewModel
class SmsViewModel @Inject constructor(
    private val repository: FinanceRepository,
    private val notifier: TransactionNotifier
) : ViewModel() {

    val pendingTransactions: StateFlow<List<PendingTransaction>> = repository.observePending()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Set when the user arrives from a notification: the review screen navigates to it, highlights
     * that transaction and opens it for editing.
     */
    private val _reviewRequest = MutableStateFlow<String?>(null)
    val reviewRequest: StateFlow<String?> = _reviewRequest.asStateFlow()

    fun requestReview(pendingId: String) {
        _reviewRequest.value = pendingId
    }

    fun consumeReviewRequest() {
        _reviewRequest.value = null
    }

    /** Persists review-screen edits without approving yet. */
    fun update(transaction: PendingTransaction) {
        viewModelScope.launch { repository.updatePending(transaction) }
    }

    fun approve(transaction: PendingTransaction) {
        viewModelScope.launch {
            repository.approvePending(transaction)
            notifier.cancel(transaction.id)
        }
    }

    fun dismiss(id: String) {
        viewModelScope.launch {
            repository.deletePending(id)
            notifier.cancel(id)
        }
    }

    fun dismissAll() {
        viewModelScope.launch {
            pendingTransactions.value.forEach {
                repository.deletePending(it.id)
                notifier.cancel(it.id)
            }
        }
    }
}
