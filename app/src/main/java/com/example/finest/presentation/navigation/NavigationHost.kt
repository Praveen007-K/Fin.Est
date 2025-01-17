package com.example.finest.presentation.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.finest.presentation.screens.details.Details
import com.example.finest.presentation.screens.transaction.Transaction
import kotlinx.serialization.Serializable

@Composable
fun NavigationHost(){
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = TransactionType) {
        composable<TransactionType> { Transaction(navController) }
        composable<Details> { Details() }
    }
}

@Serializable
object TransactionType

@Serializable
object Details