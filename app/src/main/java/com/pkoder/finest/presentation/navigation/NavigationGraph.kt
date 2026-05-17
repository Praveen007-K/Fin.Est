package com.pkoder.finest.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.pkoder.finest.auth.data.repository.AuthResult
import com.pkoder.finest.auth.presentation.ui.SignInScreen
import com.pkoder.finest.auth.presentation.viewmodel.AuthViewModel
import com.pkoder.finest.presentation.screens.AboutScreen
import com.pkoder.finest.presentation.screens.HistoryScreen
import com.pkoder.finest.presentation.screens.HomeScreen
import com.pkoder.finest.presentation.screens.ReviewScreen
import com.pkoder.finest.presentation.screens.StatsScreen
import com.pkoder.finest.presentation.viewmodel.SmsViewModel

@Composable
fun NavigationGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel = hiltViewModel(),
    smsViewModel: SmsViewModel
) {
    val authState by authViewModel.authState.collectAsState()

    // Determine start destination based on auth state
    val startDestination = when (authState) {
        is AuthResult.Success -> NavRoutes.HOME
        else -> NavRoutes.SIGN_IN
    }

    NavHost(navController, startDestination = startDestination) {
        composable(NavRoutes.SIGN_IN) {
            SignInScreen(
                viewModel = authViewModel,
                onSignInSuccess = {
                    navController.navigate(NavRoutes.HOME) {
                        // Clear back stack so user can't go back to login
                        popUpTo(NavRoutes.SIGN_IN) { inclusive = true }
                    }
                }
            )
        }
        composable(NavRoutes.HOME) { HomeScreen() }
        composable(NavRoutes.STATS) { StatsScreen() }
        composable(NavRoutes.REVIEW) { ReviewScreen(smsViewModel) }
        composable(NavRoutes.HISTORY) { HistoryScreen() }
        composable(NavRoutes.ABOUT) {
            AboutScreen(
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate(NavRoutes.SIGN_IN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}