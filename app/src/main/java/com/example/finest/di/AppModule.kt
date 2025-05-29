package com.example.finest.di

import android.content.Context
import androidx.room.Room
import com.example.finest.data.local.FinanceDatabase
import com.example.finest.data.local.dao.CreditEntryDao
import com.example.finest.data.local.dao.DebitEntryDao
import com.example.finest.data.repository.FinanceRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFinanceDatabase(@ApplicationContext context: Context): FinanceDatabase =
        Room.databaseBuilder(context, FinanceDatabase::class.java, "finance_db").build()

    @Provides
    @Singleton
    fun provideDebitEntryDao(database: FinanceDatabase): DebitEntryDao = database.debitEntryDao()

    @Provides
    @Singleton
    fun provideCreditEntryDao(database: FinanceDatabase): CreditEntryDao = database.creditEntryDao()

//    @Provides
//    @Singleton
//    fun provideFinanceRepository(
//        debitDao: DebitEntryDao,
//        creditDao: CreditEntryDao
//    ): FinanceRepository = FinanceRepository(debitDao, creditDao)
}
