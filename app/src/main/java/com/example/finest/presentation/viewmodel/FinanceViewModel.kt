package com.example.finest.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finest.data.local.entities.CreditEntryEntity
import com.example.finest.data.local.entities.DebitEntryEntity
import com.example.finest.data.repository.FinanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FinanceViewModel(private val repository: FinanceRepository) : ViewModel() {

    private val _debits = MutableStateFlow<List<DebitEntryEntity>>(emptyList())
    val debits: StateFlow<List<DebitEntryEntity>> = _debits

    private val _credits = MutableStateFlow<List<CreditEntryEntity>>(emptyList())
    val credits: StateFlow<List<CreditEntryEntity>> = _credits

    fun loadAllEntries() {
        viewModelScope.launch {
            _debits.value = repository.getAllDebits()
            _credits.value = repository.getAllCredits()
        }
    }

    fun addDebit(debit: DebitEntryEntity) {
        viewModelScope.launch {
            repository.insertDebit(debit)
            loadAllEntries()
        }
    }

    fun addCredit(credit: CreditEntryEntity) {
        viewModelScope.launch {
            repository.insertCredit(credit)
            loadAllEntries()
        }
    }
}
