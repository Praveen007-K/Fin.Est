// MainScreen.kt
package com.pkoder.finest.presentation.screens

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import com.pkoder.finest.presentation.navigation.BottomNavigationBar
import com.pkoder.finest.presentation.navigation.NavigationGraph
import com.pkoder.finest.presentation.viewmodel.FinanceViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.pkoder.finest.presentation.navigation.AppDrawer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: FinanceViewModel = hiltViewModel()) {
    val navController = rememberNavController()

    AppDrawer {
        Scaffold(
            bottomBar = {
                BottomNavigationBar(navController)
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                NavigationGraph(navController)
            }
        }
    }
}
