package com.example.finest.presentation.screens.tabs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.finest.presentation.viewmodel.FinanceViewModel


@Composable
fun CreditStatsTab() {
    val viewModel: FinanceViewModel = hiltViewModel()
    val credits by viewModel.credits.collectAsState()
    val total = credits.sumOf { it.amount }
    val grouped = credits.groupBy { it.source }
        .mapValues { it.value.sumOf { entry -> entry.amount } }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("💰 Total Credit: ₹${"%.2f".format(total)}", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(16.dp))
        Text("📊 Breakdown by Source:", style = MaterialTheme.typography.titleMedium)
        grouped.forEach { (source, amount) ->
            Text("- $source: ₹${"%.2f".format(amount)}")
        }
    }
}