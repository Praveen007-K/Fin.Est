package com.pkoder.finest

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.pkoder.finest.notification.TransactionNotifier
import com.pkoder.finest.presentation.screens.MainScreen
import com.pkoder.finest.presentation.ui.theme.FinEstTheme
import com.pkoder.finest.presentation.viewmodel.FinanceViewModel
import com.pkoder.finest.presentation.viewmodel.SmsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val financeViewModel: FinanceViewModel by viewModels()
    private val smsViewModel: SmsViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.filterValues { !it }.keys.forEach { denied ->
            Log.w(TAG, "Permission denied: $denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FinEstTheme {
                MainScreen(financeViewModel, smsViewModel)
            }
        }

        handleNotificationTap(intent)

        // SMS capture happens in the manifest-declared receiver; it just needs the runtime grant.
        // Notifications are optional — capture still works if the user says no.
        requestMissingPermissions()
    }

    // launchMode is singleTop, so a notification tap while the app is already open arrives here.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationTap(intent)
    }

    private fun handleNotificationTap(intent: Intent?) {
        intent?.getStringExtra(TransactionNotifier.EXTRA_PENDING_ID)
            ?.let { smsViewModel.requestReview(it) }
    }

    private fun requestMissingPermissions() {
        val wanted = buildList {
            add(Manifest.permission.RECEIVE_SMS)
            add(Manifest.permission.READ_SMS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        val missing = wanted.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) permissionLauncher.launch(missing.toTypedArray())
    }

    private companion object {
        const val TAG = "MainActivity"
    }
}
