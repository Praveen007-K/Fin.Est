package com.pkoder.finest.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pkoder.finest.presentation.screens.AboutScreen
import com.pkoder.finest.presentation.screens.HomeScreen
import com.pkoder.finest.presentation.screens.SignInScreen
import com.pkoder.finest.presentation.screens.StatsScreen
import androidx.hilt.navigation.compose.hiltViewModel
import com.pkoder.finest.auth.SignInViewModel

@Composable
fun NavigationGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController, startDestination = NavRoutes.HOME) {
        composable(NavRoutes.HOME) { HomeScreen() }
        composable(NavRoutes.STATS) { StatsScreen() }
        composable(NavRoutes.ABOUT) { AboutScreen() }
        composable(NavRoutes.SIGN_IN) {
            val viewModel: SignInViewModel = hiltViewModel()
            SignInScreen(
                viewModel = viewModel,
                onSignInSuccess = { navController.navigate(NavRoutes.HOME) }
            )
        }
    }
}

