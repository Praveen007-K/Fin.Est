package com.pkoder.finest.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.pkoder.finest.presentation.viewmodel.SmsViewModel

data class BottomNavItem(val route: String, val icon: ImageVector, val label: String)

@Composable
fun BottomNavigationBar(navController: NavController, smsViewModel: SmsViewModel) {
    val pending by smsViewModel.pendingTransactions.collectAsState()
    val pendingCount = pending.size

    val items = listOf(
        BottomNavItem(NavRoutes.HOME, Icons.Default.Home, "Home"),
        BottomNavItem(NavRoutes.STATS, Icons.Default.Build, "Stats"),
        BottomNavItem(NavRoutes.HISTORY, Icons.AutoMirrored.Filled.List, "History"),
        BottomNavItem(NavRoutes.REVIEW, Icons.Default.Notifications, "Review"),
        BottomNavItem(NavRoutes.ABOUT, Icons.Default.Info, "About")
    )

    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry.value?.destination?.route

    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    if (item.route == NavRoutes.REVIEW && pendingCount > 0) {
                        BadgedBox(badge = { Badge { Text("$pendingCount") } }) {
                            Icon(item.icon, contentDescription = item.label)
                        }
                    } else {
                        Icon(item.icon, contentDescription = item.label)
                    }
                },
                label = { Text(item.label) }
            )
        }
    }
}