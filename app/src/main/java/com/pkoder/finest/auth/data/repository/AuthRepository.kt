package com.pkoder.finest.auth.data.repository

import com.pkoder.finest.auth.domain.model.UserData

interface AuthRepository {
    fun getCurrentUser(): UserData?
    suspend fun signInWithGoogleToken(idToken: String): AuthResult
    suspend fun signOut()
}
