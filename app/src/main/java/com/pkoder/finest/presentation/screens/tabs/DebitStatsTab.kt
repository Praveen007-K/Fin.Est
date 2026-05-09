package com.pkoder.finest.presentation.screens.tabs

import android.graphics.Color
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.pkoder.finest.presentation.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DebitStatsTab() {
    val viewModel: FinanceViewModel = hiltViewModel()
    val debits by viewModel.debits.collectAsState()
    val total = debits.sumOf { it.amount }
    val groupedByCategory = debits.groupBy { it.category }
        .mapValues { it.value.sumOf { entry -> entry.amount } }
    val groupedByMonth = debits.groupBy {
        SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date(it.timestamp))
    }.mapValues { it.value.sumOf { entry -> entry.amount } }
        .entries.sortedBy { it.key }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Total Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Total Expenses",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    "₹${"%.2f".format(total)}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        if (debits.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No expense data yet", style = MaterialTheme.typography.bodyMedium)
            }
            return@Column
        }

        // Pie Chart — Category Breakdown
        Text("Breakdown by Category", style = MaterialTheme.typography.titleMedium)
        Card(modifier = Modifier.fillMaxWidth()) {
            AndroidView(
                factory = { context ->
                    PieChart(context).apply {
                        description.isEnabled = false
                        isDrawHoleEnabled = true
                        holeRadius = 40f
                        setHoleColor(Color.TRANSPARENT)
                        setUsePercentValues(true)
                        legend.isEnabled = true
                        setEntryLabelColor(Color.WHITE)
                        setEntryLabelTextSize(11f)
                    }
                },
                update = { chart ->
                    val entries = groupedByCategory.map { (category, amount) ->
                        PieEntry(amount.toFloat(), category)
                    }
                    val dataSet = PieDataSet(entries, "").apply {
                        colors = ColorTemplate.MATERIAL_COLORS.toList()
                        valueTextColor = Color.WHITE
                        valueTextSize = 11f
                    }
                    chart.data = PieData(dataSet)
                    chart.invalidate()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .padding(8.dp)
            )
        }

        // Category List
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                groupedByCategory.forEach { (category, amount) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(category, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "₹${"%.2f".format(amount)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                }
            }
        }

        // Bar Chart — Monthly Spending
        if (groupedByMonth.isNotEmpty()) {
            Text("Monthly Spending", style = MaterialTheme.typography.titleMedium)
            Card(modifier = Modifier.fillMaxWidth()) {
                AndroidView(
                    factory = { context ->
                        BarChart(context).apply {
                            description.isEnabled = false
                            setDrawGridBackground(false)
                            setDrawBarShadow(false)
                            setScaleEnabled(false)
                            legend.isEnabled = false
                            xAxis.apply {
                                position = XAxis.XAxisPosition.BOTTOM
                                granularity = 1f
                                setDrawGridLines(false)
                            }
                            axisLeft.apply {
                                setDrawGridLines(true)
                                axisMinimum = 0f
                            }
                            axisRight.isEnabled = false
                        }
                    },
                    update = { chart ->
                        val labels = groupedByMonth.map { it.key }
                        val entries = groupedByMonth.mapIndexed { index, entry ->
                            BarEntry(index.toFloat(), entry.value.toFloat())
                        }
                        val dataSet = BarDataSet(entries, "Monthly").apply {
                            colors = ColorTemplate.MATERIAL_COLORS.toList()
                            valueTextSize = 10f
                        }
                        chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
                        chart.data = BarData(dataSet)
                        chart.invalidate()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .padding(8.dp)
                )
            }
        }
    }
}