package com.pkoder.finest.auth

import android.content.IntentSender

data class SignInLaunchResult(
    val success: Boolean,
    val errorMessage: String? = null,
    val intentSender: IntentSender? = null
)


