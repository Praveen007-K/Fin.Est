package com.pkoder.finest.auth.presentation.ui

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
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch

@Composable
fun SignInScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onSignInSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = CredentialManager.create(context)

    val authState by viewModel.authState.collectAsState()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (val state = authState) {
            is AuthResult.Success -> {
                // If sign-in is successful, trigger the navigation callback.
                LaunchedEffect(Unit) {
                    onSignInSuccess()
                }
            }
            is AuthResult.Error -> {
                // Show the sign-in button if there's an error or user is signed out.
                Button(onClick = {
                    coroutineScope.launch {
                        val googleIdOption = GetGoogleIdOption.Builder()
                            .setFilterByAuthorizedAccounts(false)
                            .setServerClientId(context.getString(R.string.web_client_id))
                            .build()

                        val request = GetCredentialRequest.Builder()
                            .addCredentialOption(googleIdOption)
                            .build()

                        try {
                            val result = credentialManager.getCredential(context, request)
                            viewModel.handleSignIn(result.credential)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Sign-in was cancelled or failed.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }) {
                    Text("Sign in with Google")
                }
            }
            is AuthResult.Loading -> {
                // Show a loading indicator during the sign-in process.
                CircularProgressIndicator()
            }
        }
    }
}