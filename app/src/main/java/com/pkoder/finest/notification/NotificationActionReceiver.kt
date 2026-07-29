package com.pkoder.finest.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.pkoder.finest.data.repository.FinanceRepository
import com.pkoder.finest.di.ReceiverEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch

/**
 * Handles the Approve / Reject buttons on a captured-payment notification, without opening the app.
 *
 * Approve goes through the same [FinanceRepository.approvePending] the review screen uses, so an
 * approval made offline is stored locally and uploaded by the next sync.
 */
class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingId = intent.getStringExtra(TransactionNotifier.EXTRA_PENDING_ID) ?: return
        val approve = when (intent.action) {
            ACTION_APPROVE -> true
            ACTION_REJECT -> false
            else -> return
        }

        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            ReceiverEntryPoint::class.java
        )
        val repository = entryPoint.financeRepository()

        // Dismiss straight away: the user already told us what to do.
        entryPoint.transactionNotifier().cancel(pendingId)

        val pendingResult = goAsync()
        entryPoint.applicationScope().launch {
            try {
                if (approve) {
                    val approved = repository.approvePending(pendingId)
                    Log.d(TAG, "Approved $pendingId from notification: $approved")
                } else {
                    repository.deletePending(pendingId)
                    Log.d(TAG, "Rejected $pendingId from notification")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Notification action failed for $pendingId: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_APPROVE = "com.pkoder.finest.action.APPROVE_PENDING"
        const val ACTION_REJECT = "com.pkoder.finest.action.REJECT_PENDING"
        private const val TAG = "NotificationAction"
    }
}
