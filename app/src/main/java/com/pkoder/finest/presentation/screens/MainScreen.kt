package com.pkoder.finest.presentation.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pkoder.finest.presentation.navigation.BottomNavigationBar
import com.pkoder.finest.presentation.navigation.NavigationGraph
import com.pkoder.finest.presentation.navigation.NavRoutes
import com.pkoder.finest.presentation.viewmodel.FinanceViewModel
import com.pkoder.finest.presentation.viewmodel.SmsViewModel

@Composable
fun MainScreen(
    viewModel: FinanceViewModel = hiltViewModel(),
    smsViewModel: SmsViewModel = hiltViewModel()   // hoisted so all children share one instance
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Only show bottom nav when user is logged in
    val showBottomBar = currentRoute != NavRoutes.SIGN_IN

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavigationBar(navController, smsViewModel)
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            NavigationGraph(navController, smsViewModel = smsViewModel)
        }
    }
}