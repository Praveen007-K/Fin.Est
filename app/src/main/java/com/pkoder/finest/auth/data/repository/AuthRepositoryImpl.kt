package com.pkoder.finest.auth.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.pkoder.finest.auth.domain.model.UserData
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth
) : AuthRepository {

    companion object {
        private const val TAG = "AuthRepositoryImpl"
    }

    override fun getCurrentUser(): UserData? {
        return auth.currentUser?.toUserData().also {
            Log.d(TAG, "getCurrentUser: ${if (it != null) "User found: ${it.email}" else "No user"}")
        }
    }

    override suspend fun signInWithGoogleToken(idToken: String): AuthResult {
        return try {
            Log.d(TAG, "Attempting to sign in with Google token (length: ${idToken.length})")
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            Log.d(TAG, "Created Firebase credential, calling Firebase Auth...")
            val authResult = auth.signInWithCredential(credential).await()
            Log.d(TAG, "Firebase sign-in successful")
            val user = authResult.user?.toUserData()
                ?: return AuthResult.Error("Firebase user is null after sign-in.")
            Log.d(TAG, "User data extracted: ${user.email}")
            AuthResult.Success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Sign-in failed: ${e.javaClass.simpleName} - ${e.message}", e)
            AuthResult.Error(e.localizedMessage ?: "An unknown authentication error occurred.")
        }
    }

    override suspend fun signOut() {
        try {
            auth.signOut()
            Log.d(TAG, "Sign out successful")
        } catch (e: Exception) {
            Log.e(TAG, "Sign out failed: ${e.message}", e)
        }
    }

    private fun com.google.firebase.auth.FirebaseUser.toUserData(): UserData {
        return UserData(
            userId = uid,
            username = displayName.toString(),
            email = email.toString()
        )
    }
}