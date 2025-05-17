package com.example.finest.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.finest.presentation.screens.HomeScreen
import com.example.finest.presentation.screens.StatsScreen

@Composable
fun NavigationGraph(navController: NavHostController) {
    NavHost(navController, startDestination = NavRoutes.HOME) {
        composable(NavRoutes.HOME) {
            HomeScreen() // This contains your Credit/Debit tabs
        }
        composable(NavRoutes.STATS) {
            StatsScreen()
        }
    }
}
