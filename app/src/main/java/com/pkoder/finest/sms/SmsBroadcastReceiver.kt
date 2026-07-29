package com.pkoder.finest.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.pkoder.finest.di.ReceiverEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch

/**
 * Declared in the manifest only. `SMS_RECEIVED` is exempt from the implicit-broadcast
 * restrictions, so one manifest receiver covers foreground, background and force-stopped states —
 * registering a second one at runtime just delivered every message twice.
 *
 * The UI needs no callback from here: the row goes into Room and the review screen observes that
 * table.
 */
class SmsBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsBroadcastReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            ?.mapNotNull { sms ->
                val sender = sms.originatingAddress ?: return@mapNotNull null
                val body = sms.messageBody ?: return@mapNotNull null
                SmsPart(sender, body, sms.timestampMillis)
            }
            ?: return

        val transactions = joinSmsParts(parts).mapNotNull { message ->
            Log.d(TAG, "SMS from ${message.sender}: ${message.body}")
            SmsParser.parse(message.sender, message.body, message.timestampMillis)
        }
        if (transactions.isEmpty()) return

        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            ReceiverEntryPoint::class.java
        )
        val repository = entryPoint.financeRepository()
        val notifier = entryPoint.transactionNotifier()

        // goAsync keeps the receiver alive while the insert runs; the work itself lives in the
        // application scope so it outlives this receiver instance.
        val pendingResult = goAsync()
        entryPoint.applicationScope().launch {
            try {
                transactions.forEach { transaction ->
                    // Only alert for something actually queued — a duplicate SMS is ignored by
                    // the insert and must not buzz the user a second time.
                    if (repository.insertPending(transaction)) {
                        Log.d(TAG, "Pending transaction saved: ${transaction.id}")
                        notifier.notifyPending(transaction)
                    } else {
                        Log.d(TAG, "Duplicate SMS ignored: ${transaction.id}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save pending transaction: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
