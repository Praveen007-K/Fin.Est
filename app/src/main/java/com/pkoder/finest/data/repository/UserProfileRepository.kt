package com.pkoder.finest.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.pkoder.finest.auth.domain.model.UserData
import kotlinx.coroutines.tasks.await

class UserProfileRepository(private val db: FirebaseFirestore) {
    suspend fun saveUser(user: UserData) {
        db.collection("users").document(user.userId).set(user).await()
    }

    suspend fun getUser(userId: String): UserData? {
        return db.collection("users").document(userId).get().await().toObject(UserData::class.java)
    }
}
