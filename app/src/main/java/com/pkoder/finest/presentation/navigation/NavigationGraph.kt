package com.pkoder.finest.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pkoder.finest.presentation.screens.HomeScreen
import com.pkoder.finest.auth.presentation.ui.SignInScreen
import com.pkoder.finest.presentation.screens.StatsScreen
import androidx.hilt.navigation.compose.hiltViewModel
import com.pkoder.finest.auth.presentation.viewmodel.AuthViewModel
import com.pkoder.finest.presentation.screens.AboutScreen

@Composable
fun NavigationGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController, startDestination = NavRoutes.SIGN_IN) {
        composable(NavRoutes.HOME) { HomeScreen() }
        composable(NavRoutes.STATS) { StatsScreen() }
        composable(NavRoutes.ABOUT) { AboutScreen() }
        composable(NavRoutes.SIGN_IN) {
            val viewModel: AuthViewModel = hiltViewModel()
            SignInScreen(
                viewModel = viewModel,
                onSignInSuccess = { navController.navigate(NavRoutes.HOME) }
            )
        }
    }
}

