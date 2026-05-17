package com.pkoder.finest.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.pkoder.finest.domain.model.PendingTransaction

// We use a companion object callback since BroadcastReceiver can't easily
// inject into ViewModel — the Activity/App registers a listener
class SmsBroadcastReceiver : BroadcastReceiver() {

    companion object {
        var onTransactionParsed: ((com.pkoder.finest.domain.model.PendingTransaction) -> Unit)? = null
        private const val TAG = "SmsBroadcastReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        messages.forEach { sms ->
            val sender = sms.originatingAddress ?: return@forEach
            val body = sms.messageBody ?: return@forEach
            Log.d(TAG, "SMS from $sender: $body")

            val parsed = SmsParser.parse(sender, body)
            if (parsed != null) {
                Log.d(TAG, "Parsed transaction: $parsed")
                onTransactionParsed?.invoke(parsed)
            }
        }
    }
}