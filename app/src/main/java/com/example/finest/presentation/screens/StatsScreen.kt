// StatsScreen.kt
package com.example.finest.presentation.screens

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.finest.data.dummy.DummyData
import com.example.finest.domain.model.CreditEntry
import com.example.finest.domain.model.DebitEntry

@Composable
fun StatsScreen() {
    val tabs = listOf("Credit Stats", "Debit Stats")
    var selectedTabIndex by remember { mutableStateOf(0) }

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
            0 -> CreditStatsTab()
            1 -> DebitStatsTab()
        }
    }
}
@Composable
fun CreditStatsTab() {
    val credits = DummyData.dummyCredits
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



@Composable
fun DebitStatsTab() {
    val debits = DummyData.dummyDebits
    val total = debits.sumOf { it.amount }
    val grouped = debits.groupBy { it.category }
        .mapValues { it.value.sumOf { entry -> entry.amount } }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("💸 Total Debit: ₹${"%.2f".format(total)}", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(16.dp))
        Text("📊 Breakdown by Category:", style = MaterialTheme.typography.titleMedium)
        grouped.forEach { (category, amount) ->
            Text("- $category: ₹${"%.2f".format(amount)}")
        }
    }
}
