package com.pkoder.finest.auth.presentation.viewmodel

import androidx.credentials.Credential
import androidx.credentials.CustomCredential
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.pkoder.finest.auth.data.repository.AuthRepository
import com.pkoder.finest.auth.data.repository.AuthResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository
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
                    _authState.value = repository.signInWithGoogleToken(idToken)
                } catch (e: Exception) {
                    _authState.value = AuthResult.Error(e.localizedMessage ?: "Failed to process Google credential.")
                }
            } else {
                _authState.value = AuthResult.Error("Received an unexpected credential type.")
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            repository.signOut()
            _authState.value = AuthResult.Error("Not signed in.")
        }
    }
}