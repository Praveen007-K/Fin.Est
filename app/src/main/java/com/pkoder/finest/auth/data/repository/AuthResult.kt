package com.pkoder.finest.auth.data.repository

import com.pkoder.finest.auth.domain.model.UserData

sealed class AuthResult {
    data class Success(val userData: UserData) : AuthResult()
    data class Error(val message: String) : AuthResult()
    object Loading : AuthResult()
}

interface AuthRepository {
    fun getCurrentUser(): UserData?
    suspend fun signInWithGoogleToken(idToken: String): AuthResult
    suspend fun signOut()
}