package com.pkoder.finest.auth.presentation.viewmodel

import android.util.Log
import androidx.credentials.Credential
import androidx.credentials.CustomCredential
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.pkoder.finest.auth.data.repository.AuthRepository
import com.pkoder.finest.auth.data.repository.AuthResult
import com.pkoder.finest.data.repository.FinanceRepository
import com.pkoder.finest.data.repository.UserProfileRepository
import com.pkoder.finest.notification.TransactionNotifier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val financeRepository: FinanceRepository,
    private val userProfileRepository: UserProfileRepository,
    private val notifier: TransactionNotifier
) : ViewModel() {

    // Seeded from FirebaseAuth, so a returning user never sees the sign-in screen.
    private val _authState = MutableStateFlow<AuthResult>(
        repository.getCurrentUser()?.let { AuthResult.Success(it) } ?: AuthResult.SignedOut
    )
    val authState = _authState.asStateFlow()

    fun handleSignIn(credential: Credential) {
        viewModelScope.launch {
            _authState.value = AuthResult.Loading

            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                try {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val result = repository.signInWithGoogleToken(googleIdTokenCredential.idToken)
                    _authState.value = result

                    if (result is AuthResult.Success) {
                        // Write the users/{uid} document the entry sub-collections live under.
                        userProfileRepository.saveUser(result.userData)
                        // Pull this account's data and push anything queued offline. A failure
                        // here must not turn a successful sign-in into an error — the dashboard
                        // can retry with pull-to-refresh.
                        runCatching { financeRepository.syncNow() }
                            .onFailure { Log.w(TAG, "Initial sync failed: ${it.message}") }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Credential processing failed: ${e.message}", e)
                    _authState.value =
                        AuthResult.Error(e.localizedMessage ?: "Failed to process Google credential.")
                }
            } else {
                Log.e(TAG, "Expected GoogleIdTokenCredential but got: ${credential.type}")
                _authState.value = AuthResult.Error("Unexpected credential type: ${credential.type}")
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            repository.signOut()
            financeRepository.clearLocalData()
            // The pending queue is gone, so its notifications must go too.
            notifier.cancelAll()
            _authState.value = AuthResult.SignedOut
        }
    }

    private companion object {
        const val TAG = "AuthViewModel"
    }
}
