package com.pkoder.finest.presentation.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.pkoder.finest.presentation.screens.tabs.CreditScreen
import com.pkoder.finest.presentation.screens.tabs.DebitScreen
import com.pkoder.finest.presentation.viewmodel.FinanceViewModel

@Composable
fun HomeScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Debit", "Credit")
    val viewModel: FinanceViewModel = hiltViewModel()

    // Refresh entries when HomeScreen loads
    LaunchedEffect(Unit) {
        viewModel.loadAllEntries()
    }

    Scaffold(
        topBar = {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> DebitScreen()
                1 -> CreditScreen()
            }
        }
    }
}