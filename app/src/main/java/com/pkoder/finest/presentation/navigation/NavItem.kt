// NavItem.kt
package com.pkoder.finest.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Build
import androidx.compose.ui.graphics.vector.ImageVector

sealed class NavItem(val title: String, val icon: ImageVector) {
    data object Home : NavItem("Home", Icons.Filled.Home)
    data object Stats : NavItem("Stats", Icons.Filled.Build)

    companion object {
        val items = listOf(Home, Stats)
    }
}
