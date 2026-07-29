package com.pkoder.finest.auth.presentation.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.pkoder.finest.R
import com.pkoder.finest.auth.data.repository.AuthResult
import com.pkoder.finest.auth.presentation.viewmodel.AuthViewModel
import com.pkoder.finest.presentation.ui.theme.Spacing
import kotlinx.coroutines.launch

@Composable
fun SignInScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onSignInSuccess: () -> Unit
) {
    val context = LocalContext.current
    // Walk the ContextWrapper chain instead of casting: a plain `context as Activity` crashes in
    // previews and anywhere the composable is hosted outside an Activity.
    val activity = remember(context) { context.findActivity() }
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val authState by viewModel.authState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthResult.Success -> onSignInSuccess()
            // Failures from the Firebase exchange land here rather than in a raw Toast.
            is AuthResult.Error -> snackbarHostState.showSnackbar(state.message)
            else -> Unit
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.xl),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_app_logo),
                    contentDescription = null,
                    modifier = Modifier.size(96.dp)
                )
                Spacer(modifier = Modifier.height(Spacing.xl))
                Text(text = "Fin.Est", style = MaterialTheme.typography.displaySmall)
                Text(
                    text = "Track every rupee — by hand, or straight from your bank SMS.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = Spacing.sm)
                )
                Spacer(modifier = Modifier.height(Spacing.xxl))

                if (authState is AuthResult.Loading) {
                    CircularProgressIndicator()
                } else {
                    Button(
                        onClick = {
                            val host = activity ?: return@Button
                            scope.launch {
                                try {
                                    val googleSignInOption = GetSignInWithGoogleOption.Builder(
                                        context.getString(R.string.web_client_id)
                                    ).build()
                                    val request = GetCredentialRequest.Builder()
                                        .addCredentialOption(googleSignInOption)
                                        .build()

                                    val result = credentialManager.getCredential(host, request)
                                    viewModel.handleSignIn(result.credential)
                                } catch (e: NoCredentialException) {
                                    // No Google account on the device, or none the user will share.
                                    Log.w(TAG, "No credential available", e)
                                    snackbarHostState.showSnackbar(
                                        "No Google account available. Add one in Settings and try again."
                                    )
                                } catch (e: GetCredentialCancellationException) {
                                    Log.d(TAG, "Sign-in cancelled by user")
                                } catch (e: GetCredentialException) {
                                    Log.e(TAG, "Sign-in failed: ${e.javaClass.simpleName}", e)
                                    snackbarHostState.showSnackbar(e.toUserMessage())
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Continue with Google")
                    }
                }
            }
        }
    }
}

private const val TAG = "SignInScreen"

private fun Context.findActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

/** Raw exception text means nothing to the user; map what we can recognise. */
private fun GetCredentialException.toUserMessage(): String = when {
    message?.contains("Network", ignoreCase = true) == true ->
        "No connection. Check your internet and try again."
    else -> "Couldn't sign in. Please try again."
}
