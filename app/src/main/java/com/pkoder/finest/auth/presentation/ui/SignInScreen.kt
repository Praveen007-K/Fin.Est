package com.pkoder.finest.auth.presentation.ui

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.pkoder.finest.R
import com.pkoder.finest.auth.data.repository.AuthResult
import com.pkoder.finest.auth.presentation.viewmodel.AuthViewModel

@Composable
fun SignInScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onSignInSuccess: () -> Unit
) {
    val context = LocalContext.current
    val credentialManager = CredentialManager.create(context)

    val authState by viewModel.authState.collectAsState()
    var signInTriggered by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.signOut() // Force sign out on screen entry
    }

    LaunchedEffect(signInTriggered) {
        Log.d("SignInScreen", "LaunchedEffect triggered. signInTriggered: $signInTriggered")
        if (signInTriggered) {
            Log.d("SignInScreen", "Attempting to get credential...")
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(context.getString(R.string.web_client_id))
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            try {
                val result = credentialManager.getCredential(context, request)
                Log.d("SignInScreen", "Credential obtained: ${result.credential.type}")
                viewModel.handleSignIn(result.credential)
            } catch (e: Exception) {
                Log.e("SignInScreen", "Sign-in failed", e)
                Toast.makeText(context, "Sign-in was cancelled or failed.", Toast.LENGTH_SHORT).show()
            }
            signInTriggered = false
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (authState) {
            is AuthResult.Success -> {
                LaunchedEffect(Unit) {
                    onSignInSuccess()
                }
            }
            is AuthResult.Error -> {
                Button(onClick = {
                    Log.d("SignInScreen", "Sign-in button clicked!")
                    signInTriggered = true
                }) {
                    Text("Sign in with Google")
                }
            }
            is AuthResult.Loading -> {
                CircularProgressIndicator()
            }
        }
    }
}