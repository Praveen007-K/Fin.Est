package com.pkoder.finest.auth.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.pkoder.finest.auth.domain.model.UserData
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth
) : AuthRepository {

    override fun getCurrentUser(): UserData? {
        return auth.currentUser?.toUserData()
    }

    override suspend fun signInWithGoogleToken(idToken: String): AuthResult {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val user = authResult.user?.toUserData()
                ?: return AuthResult.Error("Firebase user is null after sign-in.")
            AuthResult.Success(user)
        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "An unknown authentication error occurred.")
        }
    }

    override suspend fun signOut() {
        auth.signOut()
    }

    private fun com.google.firebase.auth.FirebaseUser.toUserData(): UserData {
        return UserData(
            userId = uid,
            username = displayName.toString(),
            email = email.toString()
        )
    }
}