package com.pkoder.finest.di

import android.content.Context
import androidx.room.Room
import com.pkoder.finest.data.local.FinanceDatabase
import com.pkoder.finest.data.local.MIGRATION_3_4
import com.pkoder.finest.data.local.dao.CreditEntryDao
import com.pkoder.finest.data.local.dao.DebitEntryDao
import com.pkoder.finest.data.local.dao.PendingTransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFinanceDatabase(@ApplicationContext context: Context): FinanceDatabase =
        Room.databaseBuilder(context, FinanceDatabase::class.java, "finance_db")
            // Real migrations only — a destructive fallback would silently drop entries that
            // exist offline plus the whole pending-SMS queue.
            .addMigrations(MIGRATION_3_4)
            .build()

    /**
     * Lives as long as the process. Used by broadcast receivers, which have no lifecycle of
     * their own and must not leak work into a scope that dies with onReceive().
     */
    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Provides
    @Singleton
    fun provideDebitEntryDao(database: FinanceDatabase): DebitEntryDao = database.debitEntryDao()

    @Provides
    @Singleton
    fun provideCreditEntryDao(database: FinanceDatabase): CreditEntryDao = database.creditEntryDao()

    @Provides
    @Singleton
    fun providePendingTransactionDao(database: FinanceDatabase): PendingTransactionDao = database.pendingTransactionDao()

}
