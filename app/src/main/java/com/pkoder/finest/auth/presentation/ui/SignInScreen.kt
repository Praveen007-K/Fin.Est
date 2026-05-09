package com.pkoder.finest.auth.presentation.ui

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
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
    val activity = context as Activity
    val credentialManager = CredentialManager.create(context)
    val authState by viewModel.authState.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(authState) {
        if (authState is AuthResult.Success) {
            onSignInSuccess()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (authState) {
            is AuthResult.Loading -> {
                CircularProgressIndicator()
            }
            else -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Fin.Est",
                        style = MaterialTheme.typography.headlineLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        scope.launch {  // ← THIS IS MISSING IN YOUR CODE
                            Log.d("SignInScreen", "Button clicked, launching sign in")
                            try {
                                val googleSignInOption = GetSignInWithGoogleOption.Builder(
                                    context.getString(R.string.web_client_id)
                                ).build()

                                val request = GetCredentialRequest.Builder()
                                    .addCredentialOption(googleSignInOption)
                                    .build()

                                Log.d("SignInScreen", "Requesting credential...")
                                val result = credentialManager.getCredential(activity, request)
                                Log.d("SignInScreen", "Credential received: ${result.credential.type}")
                                viewModel.handleSignIn(result.credential)

                            } catch (e: Exception) {
                                Log.e("SignInScreen", "Sign-in error: ${e.javaClass.simpleName} - ${e.message}", e)
                                val errorMsg = when {
                                    e.message?.contains("Network", ignoreCase = true) == true -> "Network error. Check internet."
                                    e.message?.contains("USER_CANCELLED", ignoreCase = true) == true -> "Sign-in cancelled."
                                    else -> "${e.javaClass.simpleName}: ${e.message}"
                                }
                                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                            }
                        }  // ← CLOSE scope.launch HERE
                    }) {
                        Text("Sign in with Google")
                    }
                }
            }
        }
    }
}