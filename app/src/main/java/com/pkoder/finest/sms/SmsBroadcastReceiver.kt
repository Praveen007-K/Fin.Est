package com.pkoder.finest.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import androidx.room.Room
import com.pkoder.finest.data.local.FinanceDatabase
import com.pkoder.finest.domain.model.PendingTransaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsBroadcastReceiver : BroadcastReceiver() {

    companion object {
        var onTransactionParsed: ((PendingTransaction) -> Unit)? = null
        private const val TAG = "SmsBroadcastReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        messages.forEach { sms ->
            val sender = sms.originatingAddress ?: return@forEach
            val body = sms.messageBody ?: return@forEach
            Log.d(TAG, "SMS from $sender: $body")

            val parsed = SmsParser.parse(sender, body) ?: return@forEach
            Log.d(TAG, "Parsed transaction: $parsed")

            // Persist to DB so the transaction survives app restarts / closed state
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = Room.databaseBuilder(
                        context.applicationContext,
                        FinanceDatabase::class.java,
                        "finance_db"
                    ).fallbackToDestructiveMigration(dropAllTables = true).build()

                    db.pendingTransactionDao().insert(
                        com.pkoder.finest.data.local.entities.PendingTransactionEntity(
                            id = parsed.id,
                            type = parsed.type.name,
                            amount = parsed.amount,
                            bank = parsed.bank,
                            paymentMethod = parsed.paymentMethod,
                            category = parsed.category,
                            source = parsed.source,
                            description = parsed.description,
                            timestamp = parsed.timestamp,
                            rawSms = parsed.rawSms
                        )
                    )
                    Log.d(TAG, "Pending transaction saved to DB: ${parsed.id}")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to save pending transaction: ${e.message}", e)
                } finally {
                    pendingResult.finish()
                }
            }

            // Also notify UI immediately if app is open
            onTransactionParsed?.invoke(parsed)
        }
    }
}