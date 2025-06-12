package com.pkoder.finest.presentation.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewModelScope
import com.pkoder.finest.auth.SignInViewModel
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue

@Composable
fun SignInScreen(
    viewModel: SignInViewModel,
    onSignInSuccess: () -> Unit
) {
    // The launcher is used to start the Google Sign-In activity and get the result.
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        // When the result comes back, we check if it's OK.
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { intent ->
                // The result intent is passed to the ViewModel for processing.
                viewModel.handleSignInResult(intent)
            }
        }
        // You could add an else block here to handle cancellation or errors.
    }

    // Observe the user state from the ViewModel.
    val user by viewModel.user.collectAsState()

    // This effect runs when the 'user' state changes.
    // If the user is not null (sign-in was successful), we navigate away.
    LaunchedEffect(user) {
        if (user != null) {
            onSignInSuccess()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Button(onClick = {
            // Launch a coroutine to call the ViewModel's suspend function.
            viewModel.viewModelScope.launch {
                val signInResult = viewModel.signIn()
                signInResult.pendingIntent.intentSender?.let { intentSender ->
                    // Launch the Google Sign-In UI.
                    val request = IntentSenderRequest.Builder(intentSender).build()
                    launcher.launch(request)
                }
            }
        }) {
            Text("Sign in with Google")
        }
    }
}
