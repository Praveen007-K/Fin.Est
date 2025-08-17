package com.pkoder.finest.presentation.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.pkoder.finest.presentation.navigation.AppDrawer
import com.pkoder.finest.presentation.navigation.BottomNavigationBar
import com.pkoder.finest.presentation.navigation.NavigationGraph
import com.pkoder.finest.presentation.viewmodel.FinanceViewModel

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
