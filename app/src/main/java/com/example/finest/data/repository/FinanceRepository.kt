package com.example.finest.data.repository

import com.example.finest.data.local.dao.CreditEntryDao
import com.example.finest.data.local.dao.DebitEntryDao
import com.example.finest.data.local.entities.CreditEntryEntity
import com.example.finest.data.local.entities.DebitEntryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FinanceRepository(
    private val debitDao: DebitEntryDao,
    private val creditDao: CreditEntryDao
) {

    suspend fun insertDebit(debitEntry: DebitEntryEntity) {
        debitDao.insert(debitEntry)
    }

    suspend fun insertCredit(creditEntry: CreditEntryEntity) {
        creditDao.insert(creditEntry)
    }

    suspend fun getAllDebits(): List<DebitEntryEntity> {
        return debitDao.getAllDebits()
    }

    suspend fun getAllCredits(): List<CreditEntryEntity> {
        return creditDao.getAllCredits()
    }
}
