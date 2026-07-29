package com.pkoder.finest.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.pkoder.finest.presentation.viewmodel.SmsViewModel

private data class BottomNavItem(val route: String, val icon: ImageVector, val label: String)

/**
 * Four destinations: adding an entry moved to the centre FAB and the profile to the top bar, which
 * keeps the bar inside Material's 3–5 item guidance and puts the common action in thumb reach.
 */
@Composable
fun BottomNavigationBar(navController: NavController, smsViewModel: SmsViewModel) {
    val pending by smsViewModel.pendingTransactions.collectAsState()

    val items = listOf(
        BottomNavItem(NavRoutes.HOME, Icons.Default.Home, "Home"),
        BottomNavItem(NavRoutes.HISTORY, Icons.AutoMirrored.Filled.ReceiptLong, "History"),
        BottomNavItem(NavRoutes.STATS, Icons.Default.PieChart, "Insights"),
        BottomNavItem(NavRoutes.REVIEW, Icons.Default.Notifications, "Review")
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        items.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    if (item.route == NavRoutes.REVIEW && pending.isNotEmpty()) {
                        BadgedBox(badge = { Badge { Text("${pending.size}") } }) {
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
