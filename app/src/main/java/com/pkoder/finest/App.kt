package com.pkoder.finest

import android.app.Application
import com.pkoder.finest.notification.TransactionNotifier
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class App : Application() {

    @Inject
    lateinit var notifier: TransactionNotifier

    override fun onCreate() {
        super.onCreate()
        // The SMS receiver can fire before any activity exists, so the channel has to be ready
        // as soon as the process starts.
        notifier.createChannel()
    }
}
