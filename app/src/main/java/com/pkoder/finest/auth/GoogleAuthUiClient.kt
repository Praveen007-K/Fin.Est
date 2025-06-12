package com.pkoder.finest.auth

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.*
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.pkoder.finest.domain.model.UserData
import kotlinx.coroutines.tasks.await

class GoogleAuthUiClient(
    private val context: Context,
    private val oneTapClient: SignInClient
) {
    private val auth = Firebase.auth

    suspend fun signIn(): SignInLaunchResult {
        return try {
            val result = oneTapClient.beginSignIn(
                BeginSignInRequest.builder()
                    .setGoogleIdTokenRequestOptions(
                        BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                            .setSupported(true)
                            .setServerClientId("YOUR_WEB_CLIENT_ID")
                            .setFilterByAuthorizedAccounts(false)
                            .build()
                    )
                    .build()
            ).await()
            SignInLaunchResult(success = true, intentSender = result.pendingIntent.intentSender)
        } catch (e: Exception) {
            SignInLaunchResult(success = false, errorMessage = e.localizedMessage)
        }
    }

    suspend fun signInWithIntent(intent: Intent): UserData? {
        val credential = oneTapClient.getSignInCredentialFromIntent(intent)
        val googleIdToken = credential.googleIdToken
        val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
        val user = auth.signInWithCredential(firebaseCredential).await().user
        return user?.let {
            UserData(
                userId = it.uid,
                username = it.displayName ?: "",
                email = it.email ?: "",
                profilePictureUrl = it.photoUrl?.toString()
            )
        }
    }

    fun signOut() {
        auth.signOut()
    }

    val currentUser: UserData?
        get() = auth.currentUser?.let {
            UserData(
                userId = it.uid,
                username = it.displayName ?: "",
                email = it.email ?: "",
                profilePictureUrl = it.photoUrl?.toString()
            )
        }
}


