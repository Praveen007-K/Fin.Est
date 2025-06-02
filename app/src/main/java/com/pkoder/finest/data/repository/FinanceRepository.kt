package com.pkoder.finest.data.repository

import com.pkoder.finest.data.local.dao.CreditEntryDao
import com.pkoder.finest.data.local.dao.DebitEntryDao
import com.pkoder.finest.data.local.entities.CreditEntryEntity
import com.pkoder.finest.data.local.entities.DebitEntryEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FinanceRepository @Inject constructor(
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
