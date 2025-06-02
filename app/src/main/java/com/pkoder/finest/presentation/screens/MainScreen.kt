// MainScreen.kt
package com.pkoder.finest.presentation.screens

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.*
import androidx.compose.foundation.layout.*
import com.pkoder.finest.presentation.navigation.BottomNavigationBar
import com.pkoder.finest.presentation.navigation.NavigationGraph
import com.pkoder.finest.presentation.viewmodel.FinanceViewModel
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun MainScreen(viewModel: FinanceViewModel = hiltViewModel()) {
    val navController = rememberNavController()

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
