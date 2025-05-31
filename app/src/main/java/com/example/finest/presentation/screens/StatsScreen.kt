// StatsScreen.kt
package com.example.finest.presentation.screens

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.finest.presentation.screens.tabs.CreditStatsTab
import com.example.finest.presentation.screens.tabs.DebitStatsTab
import com.example.finest.presentation.viewmodel.FinanceViewModel

@Composable
fun StatsScreen() {
    val tabs = listOf("Debit Stats", "Credit Stats")
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val viewModel: FinanceViewModel = hiltViewModel()

    LaunchedEffect(Unit) {
        viewModel.loadAllEntries()
    }


    Column {
        TabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedTabIndex) {
            0 -> DebitStatsTab()
            1 -> CreditStatsTab()
        }
    }
}





