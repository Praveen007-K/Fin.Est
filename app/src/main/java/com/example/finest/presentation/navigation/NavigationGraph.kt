package com.example.finest.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.finest.presentation.screens.HomeScreen
import com.example.finest.presentation.screens.StatsScreen

@Composable
fun NavigationGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController, startDestination = NavRoutes.HOME) {
        composable(NavRoutes.HOME) { HomeScreen() }
        composable(NavRoutes.STATS) { StatsScreen() }
    }
}


