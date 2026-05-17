package com.pkoder.finest

import android.Manifest
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.provider.Telephony
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.ContextCompat
import com.pkoder.finest.presentation.screens.MainScreen
import com.pkoder.finest.presentation.ui.theme.FinEstTheme
import com.pkoder.finest.presentation.viewmodel.FinanceViewModel
import com.pkoder.finest.presentation.viewmodel.SmsViewModel
import com.pkoder.finest.sms.SmsBroadcastReceiver
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val financeViewModel: FinanceViewModel by viewModels()
    private val smsViewModel: SmsViewModel by viewModels()
    private val smsReceiver = SmsBroadcastReceiver()
    private var smsReceiverRegistered = false

    private val smsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.RECEIVE_SMS] == true &&
                permissions[Manifest.permission.READ_SMS] == true
        if (granted) {
            registerSmsReceiver()
        } else {
            Log.w("MainActivity", "SMS permissions denied — transaction SMS will not be tracked")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FinEstTheme {
                MainScreen(financeViewModel, smsViewModel)
            }
        }

        // Wire parsed SMS into SmsViewModel
        SmsBroadcastReceiver.onTransactionParsed = { transaction ->
            smsViewModel.addPending(transaction)
        }

        // Request SMS permissions at runtime (required on Android 6+)
        val alreadyGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    this, Manifest.permission.READ_SMS
                ) == PackageManager.PERMISSION_GRANTED

        if (alreadyGranted) {
            registerSmsReceiver()
        } else {
            smsPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.READ_SMS
                )
            )
        }
    }

    private fun registerSmsReceiver() {
        if (!smsReceiverRegistered) {
            val filter = IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)
            registerReceiver(smsReceiver, filter)
            smsReceiverRegistered = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (smsReceiverRegistered) {
            unregisterReceiver(smsReceiver)
        }
        SmsBroadcastReceiver.onTransactionParsed = null // prevent leaks
    }
}