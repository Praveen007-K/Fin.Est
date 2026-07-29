package com.pkoder.finest.di

import com.pkoder.finest.data.repository.FinanceRepository
import com.pkoder.finest.notification.TransactionNotifier
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope

/**
 * Dependencies for broadcast receivers.
 *
 * Field injection via `@AndroidEntryPoint` would require calling `super.onReceive()`, which Kotlin
 * cannot express (the base method is abstract), so receivers pull what they need from the
 * singleton component instead. Either way they share the app's single Room instance rather than
 * building their own.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReceiverEntryPoint {
    fun financeRepository(): FinanceRepository
    fun transactionNotifier(): TransactionNotifier

    @ApplicationScope
    fun applicationScope(): CoroutineScope
}
