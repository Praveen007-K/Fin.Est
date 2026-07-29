package com.pkoder.finest.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.pkoder.finest.MainActivity
import com.pkoder.finest.R
import com.pkoder.finest.domain.model.PendingTransaction
import com.pkoder.finest.domain.model.TransactionType
import com.pkoder.finest.presentation.util.asMoney
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tells the user a payment was captured and offers the two things they'd otherwise open the app
 * for: approve it as-is, or reject it. Tapping the body opens Review on that transaction so the
 * details (category, payment method) can be corrected first.
 */
@Singleton
class TransactionNotifier @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val manager = NotificationManagerCompat.from(context)

    /** Safe to call repeatedly; creating an existing channel is a no-op. */
    fun createChannel() {
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_HIGH)
                .setName(context.getString(R.string.notification_channel_payments_name))
                .setDescription(context.getString(R.string.notification_channel_payments_description))
                .build()
        )
    }

    fun notifyPending(pending: PendingTransaction) {
        // Posting without the permission is a silent no-op on Android 13+; bail out early so the
        // rest of the capture flow is unaffected.
        if (!manager.areNotificationsEnabled()) return

        val isDebit = pending.type == TransactionType.DEBIT
        val title = context.getString(
            if (isDebit) R.string.notification_payment_debit_title
            else R.string.notification_payment_credit_title,
            pending.amount.asMoney(),
            pending.bank
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_payment)
            .setContentTitle(title)
            .setContentText(context.getString(R.string.notification_payment_body))
            .setStyle(NotificationCompat.BigTextStyle().bigText(pending.rawSms))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(reviewIntent(pending.id))
            .addAction(
                0,
                context.getString(R.string.notification_action_reject),
                actionIntent(pending.id, NotificationActionReceiver.ACTION_REJECT)
            )
            .addAction(
                0,
                context.getString(R.string.notification_action_approve),
                actionIntent(pending.id, NotificationActionReceiver.ACTION_APPROVE)
            )
            .build()

        manager.notify(notificationId(pending.id), notification)
    }

    /** Called once a transaction is resolved, so the shade never shows a stale card. */
    fun cancel(pendingId: String) = manager.cancel(notificationId(pendingId))

    fun cancelAll() = manager.cancelAll()

    private fun reviewIntent(pendingId: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_PENDING_ID, pendingId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            requestCode(pendingId, "open"),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun actionIntent(pendingId: String, action: String): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_PENDING_ID, pendingId)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode(pendingId, action),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    companion object {
        const val CHANNEL_ID = "captured_payments"
        const val EXTRA_PENDING_ID = "com.pkoder.finest.extra.PENDING_ID"

        /** Stable per transaction, so re-posting updates the same notification. */
        fun notificationId(pendingId: String): Int = pendingId.hashCode()

        private fun requestCode(pendingId: String, suffix: String): Int =
            (pendingId + suffix).hashCode()
    }
}
