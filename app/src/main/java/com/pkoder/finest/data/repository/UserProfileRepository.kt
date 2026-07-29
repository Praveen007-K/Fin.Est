package com.pkoder.finest.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.pkoder.finest.auth.domain.model.UserData
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Writes the `users/{uid}` profile document that the entry sub-collections hang off.
 *
 * The class existed but was never provided by Hilt and never called, so that parent document was
 * only ever implied. It is now written on each successful sign-in.
 */
@Singleton
class UserProfileRepository @Inject constructor(
    private val db: FirebaseFirestore
) {
    suspend fun saveUser(user: UserData) {
        try {
            db.collection(USERS).document(user.userId).set(user).await()
            Log.d(TAG, "Saved profile for ${user.userId}")
        } catch (e: Exception) {
            // Non-fatal: the profile doc is metadata, entries are stored independently.
            Log.e(TAG, "Failed to save profile: ${e.message}", e)
        }
    }

    suspend fun getUser(userId: String): UserData? = try {
        db.collection(USERS).document(userId).get().await().toObject(UserData::class.java)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to read profile: ${e.message}", e)
        null
    }

    private companion object {
        const val USERS = "users"
        const val TAG = "UserProfileRepository"
    }
}
