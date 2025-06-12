package com.pkoder.finest.di

import android.content.Context
import androidx.room.Room
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.pkoder.finest.data.local.FinanceDatabase
import com.pkoder.finest.data.local.dao.CreditEntryDao
import com.pkoder.finest.data.local.dao.DebitEntryDao
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

    @Provides
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

//    @Provides
//    @Singleton
//    fun provideFinanceRepository(
//        debitDao: DebitEntryDao,
//        creditDao: CreditEntryDao
//    ): FinanceRepository = FinanceRepository(debitDao, creditDao)
}
