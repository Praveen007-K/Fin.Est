package com.pkoder.finest.auth

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.identity.BeginSignInResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// Data classes to represent the user and sign-in result
data class User(val id: String, val name: String?, val email: String?)
data class SignInResult(val user: User?, val error: String? = null)

// A simple repository interface. The actual implementation would handle the
// Google Sign-In client logic.
interface SignInRepository {
    suspend fun beginSignIn(): BeginSignInResult
    suspend fun handleSignInResult(intent: Intent): SignInResult
}

// --- ViewModel ---
// This is a complete example of how the ViewModel should be structured.
@HiltViewModel
class SignInViewModel @Inject constructor(
    private val repository: SignInRepository // Injected via Hilt
) : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user = _user.asStateFlow()

    /**
     * This public function starts the sign-in process.
     * It's called from the UI and returns the result needed to launch the sign-in flow.
     */
    suspend fun signIn(): BeginSignInResult {
        return repository.beginSignIn()
    }

    /**
     * This public function handles the result returned from the Google Sign-In intent.
     */
    fun handleSignInResult(intent: Intent) {
        viewModelScope.launch {
            val result = repository.handleSignInResult(intent)
            _user.value = result.user
            // You can also handle result.error here, e.g., show a toast
        }
    }
}
