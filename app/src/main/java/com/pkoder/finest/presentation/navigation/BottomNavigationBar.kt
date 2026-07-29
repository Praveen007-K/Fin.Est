package com.pkoder.finest.presentation.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.InsertChart
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.pkoder.finest.presentation.ui.theme.Mono
import com.pkoder.finest.presentation.ui.theme.Spacing
import com.pkoder.finest.presentation.viewmodel.SmsViewModel

private data class BottomNavItem(val route: String, val icon: ImageVector, val label: String)

/**
 * Four destinations: adding an entry lives on the centre FAB and the profile in the top bar, which
 * keeps the bar inside Material's 3–5 item guidance and puts the common action in thumb reach.
 *
 * Hand-rolled rather than a `NavigationBar` because the design wraps the whole selected item — icon
 * *and* label — in a mint rounded rectangle, while Material's indicator is a pill behind the icon
 * only. Insets are applied here since a custom bar gets none from `Scaffold`.
 */
@Composable
fun BottomNavigationBar(navController: NavController, smsViewModel: SmsViewModel) {
    val pending by smsViewModel.pendingTransactions.collectAsState()

    val items = listOf(
        BottomNavItem(NavRoutes.HOME, Icons.Default.Home, "Home"),
        BottomNavItem(NavRoutes.HISTORY, Icons.Default.History, "History"),
        BottomNavItem(NavRoutes.STATS, Icons.Default.InsertChart, "Insights"),
        BottomNavItem(NavRoutes.REVIEW, Icons.Default.RateReview, "Review")
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val selected = currentRoute == item.route
                NavItem(
                    item = item,
                    selected = selected,
                    badgeCount = if (item.route == NavRoutes.REVIEW) pending.size else 0,
                    onClick = {
                        if (!selected) {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun NavItem(
    item: BottomNavItem,
    selected: Boolean,
    badgeCount: Int,
    onClick: () -> Unit
) {
    val container by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainer,
        label = "navContainer"
    )
    val content by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "navContent"
    )

    Column(
        modifier = Modifier
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .background(container, MaterialTheme.shapes.medium)
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        if (badgeCount > 0) {
            BadgedBox(badge = { Badge { Text("$badgeCount") } }) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.label,
                    tint = content,
                    modifier = Modifier.size(24.dp)
                )
            }
        } else {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = content,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(text = item.label, style = Mono.label, color = content)
    }
}
