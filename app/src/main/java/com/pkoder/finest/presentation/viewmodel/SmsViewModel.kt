package com.pkoder.finest.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pkoder.finest.data.local.entities.CreditEntryEntity
import com.pkoder.finest.data.local.entities.DebitEntryEntity
import com.pkoder.finest.data.repository.FinanceRepository
import com.pkoder.finest.domain.model.PendingTransaction
import com.pkoder.finest.domain.model.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SmsViewModel @Inject constructor(
    private val repository: FinanceRepository
) : ViewModel() {

    private val _pendingTransactions = MutableStateFlow<List<PendingTransaction>>(emptyList())
    val pendingTransactions: StateFlow<List<PendingTransaction>> = _pendingTransactions

    init {
        // Load any pending transactions that were saved while the app was closed
        viewModelScope.launch {
            _pendingTransactions.value = repository.getAllPending()
        }
    }

    fun addPending(transaction: PendingTransaction) {
        // Avoid duplicates by ID (DB already deduplicates via IGNORE, this covers the live callback)
        val exists = _pendingTransactions.value.any { it.id == transaction.id }
        if (!exists) {
            _pendingTransactions.value = _pendingTransactions.value + transaction
        }
    }

    fun approve(transaction: PendingTransaction) {
        viewModelScope.launch {
            when (transaction.type) {
                TransactionType.DEBIT -> repository.insertDebit(
                    DebitEntryEntity(
                        category = transaction.category,
                        paymentMethod = transaction.paymentMethod,
                        bank = transaction.bank,
                        amount = transaction.amount,
                        description = transaction.description,
                        timestamp = transaction.timestamp
                    )
                )
                TransactionType.CREDIT -> repository.insertCredit(
                    CreditEntryEntity(
                        source = transaction.source.ifBlank { transaction.bank },
                        amount = transaction.amount,
                        timestamp = transaction.timestamp
                    )
                )
            }
            dismiss(transaction.id)
        }
    }

    fun dismiss(id: String) {
        viewModelScope.launch {
            repository.deletePending(id)  // Remove from DB
            _pendingTransactions.value = _pendingTransactions.value.filter { it.id != id }
        }
    }

    fun dismissAll() {
        viewModelScope.launch {
            _pendingTransactions.value.forEach { repository.deletePending(it.id) }
            _pendingTransactions.value = emptyList()
        }
    }

    val pendingCount: Int get() = _pendingTransactions.value.size
}