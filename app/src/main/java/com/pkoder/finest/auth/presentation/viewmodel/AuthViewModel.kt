package com.pkoder.finest.auth.presentation.viewmodel

import androidx.credentials.Credential
import androidx.credentials.CustomCredential
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.pkoder.finest.auth.data.repository.AuthRepository
import com.pkoder.finest.auth.data.repository.AuthResult
import com.pkoder.finest.data.repository.FinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val financeRepository: FinanceRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthResult>(
        repository.getCurrentUser()?.let { AuthResult.Success(it) } ?: AuthResult.Error("Not signed in.")
    )
    val authState = _authState.asStateFlow()

    fun handleSignIn(credential: Credential) {
        viewModelScope.launch {
            _authState.value = AuthResult.Loading

            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                try {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken
                    android.util.Log.d("AuthViewModel", "Successfully extracted idToken, attempting Firebase sign-in...")
                    _authState.value = repository.signInWithGoogleToken(idToken)
                    android.util.Log.d("AuthViewModel", "Firebase sign-in result: ${_authState.value}")
                } catch (e: Exception) {
                    android.util.Log.e("AuthViewModel", "Credential processing failed: ${e.message}", e)
                    _authState.value = AuthResult.Error(e.localizedMessage ?: "Failed to process Google credential.")
                }
            } else {
                android.util.Log.e("AuthViewModel", "Expected GoogleIdTokenCredential but got: ${credential.type}")
                _authState.value = AuthResult.Error("Received an unexpected credential type: ${credential.type}")
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            repository.signOut()
            financeRepository.clearLocalData()
            _authState.value = AuthResult.Error("Not signed in.")
        }
    }
}