package com.pkoder.finest.domain.model

data class UserData(
    val userId: String,
    val username: String,
    val email: String,
    val profilePictureUrl: String? = null
)