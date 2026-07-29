package com.pkoder.finest.auth.data.repository

import com.pkoder.finest.auth.domain.model.UserData

sealed class AuthResult {
    data class Success(val userData: UserData) : AuthResult()

    /** A real failure worth telling the user about. */
    data class Error(val message: String) : AuthResult()

    data object Loading : AuthResult()

    /** Nobody is signed in — the normal starting state, not an error. */
    data object SignedOut : AuthResult()
}
