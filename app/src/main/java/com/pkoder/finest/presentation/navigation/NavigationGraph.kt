package com.pkoder.finest.presentation.navigation

import androidx.compose.material3.SnackbarHostState
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
import com.pkoder.finest.presentation.screens.DashboardScreen
import com.pkoder.finest.presentation.screens.HistoryScreen
import com.pkoder.finest.presentation.screens.ReviewScreen
import com.pkoder.finest.presentation.screens.StatsScreen
import com.pkoder.finest.presentation.viewmodel.FinanceViewModel
import com.pkoder.finest.presentation.viewmodel.SmsViewModel

@Composable
fun NavigationGraph(
    navController: NavHostController,
    // Hoisted by MainScreen: every destination shares one instance, so data written on one screen
    // is visible on the others without a reload.
    financeViewModel: FinanceViewModel,
    smsViewModel: SmsViewModel,
    snackbarHostState: SnackbarHostState,
    onAddEntry: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel()
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
        composable(NavRoutes.HOME) {
            DashboardScreen(
                financeViewModel = financeViewModel,
                smsViewModel = smsViewModel,
                onSeeAllTransactions = {
                    navController.navigate(NavRoutes.HISTORY) { launchSingleTop = true }
                },
                onOpenReview = {
                    navController.navigate(NavRoutes.REVIEW) { launchSingleTop = true }
                },
                onOpenInsights = {
                    navController.navigate(NavRoutes.STATS) { launchSingleTop = true }
                },
                onAddEntry = onAddEntry
            )
        }
        composable(NavRoutes.HISTORY) {
            HistoryScreen(financeViewModel, snackbarHostState)
        }
        composable(NavRoutes.STATS) { StatsScreen(financeViewModel) }
        composable(NavRoutes.REVIEW) { ReviewScreen(smsViewModel) }
        composable(NavRoutes.ABOUT) {
            AboutScreen(
                authViewModel = authViewModel,
                financeViewModel = financeViewModel,
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
