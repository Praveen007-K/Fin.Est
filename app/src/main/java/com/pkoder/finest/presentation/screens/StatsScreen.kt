package com.pkoder.finest.presentation.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pkoder.finest.presentation.components.BreakdownRow
import com.pkoder.finest.presentation.components.EmptyState
import com.pkoder.finest.presentation.components.SectionHeader
import com.pkoder.finest.presentation.components.SummaryTile
import com.pkoder.finest.presentation.components.charts.BarDatum
import com.pkoder.finest.presentation.components.charts.DonutChart
import com.pkoder.finest.presentation.components.charts.DonutSlice
import com.pkoder.finest.presentation.components.charts.MonthlyBarChart
import com.pkoder.finest.presentation.ui.theme.Spacing
import com.pkoder.finest.presentation.ui.theme.moneyColors
import com.pkoder.finest.presentation.util.Period
import com.pkoder.finest.presentation.util.asMonthYear
import com.pkoder.finest.presentation.util.asMoney
import com.pkoder.finest.presentation.util.periodStart
import com.pkoder.finest.presentation.viewmodel.FinanceViewModel
import java.util.Calendar

/**
 * Insights: totals for the chosen period, a category (or source) donut, and the month-over-month
 * trend. Charts are Compose-native, so they follow the theme and animate with the rest of the UI.
 */
@Composable
fun StatsScreen(viewModel: FinanceViewModel) {
    val debits by viewModel.debits.collectAsState()
    val credits by viewModel.credits.collectAsState()

    var showExpenses by remember { mutableStateOf(true) }
    var period by remember { mutableStateOf(Period.MONTH) }

    val start = remember(period) { periodStart(period) }
    val periodDebits = debits.filter { it.timestamp >= start }
    val periodCredits = credits.filter { it.timestamp >= start }

    val totalExpense = periodDebits.sumOf { it.amount }
    val totalIncome = periodCredits.sumOf { it.amount }

    val palette = moneyColors.chartPalette

    // Both tables are reduced to (label, amount) / (timestamp, amount) pairs first: the two
    // entity types share no supertype, so grouping them directly would not type-check.
    val labelled: List<Pair<String, Double>> = remember(periodDebits, periodCredits, showExpenses) {
        if (showExpenses) {
            periodDebits.map { it.category.ifBlank { "Uncategorized" } to it.amount }
        } else {
            periodCredits.map { it.source.ifBlank { "Other" } to it.amount }
        }
    }
    val grouped: List<Pair<String, Double>> = remember(labelled) {
        labelled.groupBy { it.first }
            .mapValues { (_, rows) -> rows.sumOf { it.second } }
            .entries
            .sortedByDescending { it.value }
            .map { it.key to it.value }
    }
    val monthly = remember(periodDebits, periodCredits, showExpenses) {
        val timed = if (showExpenses) {
            periodDebits.map { it.timestamp to it.amount }
        } else {
            periodCredits.map { it.timestamp to it.amount }
        }
        monthlyTotals(timed)
    }

    val total = if (showExpenses) totalExpense else totalIncome
    val accent = if (showExpenses) moneyColors.expense else moneyColors.income

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Spacing.screen,
            end = Spacing.screen,
            top = Spacing.sm,
            bottom = Spacing.xxl
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = showExpenses,
                    onClick = { showExpenses = true },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) { Text("Expenses") }
                SegmentedButton(
                    selected = !showExpenses,
                    onClick = { showExpenses = false },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) { Text("Income") }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Period.entries.forEach { option ->
                    FilterChip(
                        selected = period == option,
                        onClick = { period = option },
                        label = { Text(option.label) }
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                SummaryTile(
                    label = "Income",
                    amount = totalIncome,
                    isExpense = false,
                    modifier = Modifier.weight(1f)
                )
                SummaryTile(
                    label = "Expenses",
                    amount = totalExpense,
                    isExpense = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (grouped.isEmpty()) {
            item {
                EmptyState(
                    message = if (showExpenses) "No expenses in this period" else "No income in this period",
                    icon = Icons.Default.Info,
                    hint = "Pick a wider date range to see more."
                )
            }
            return@LazyColumn
        }

        item {
            SectionHeader(title = if (showExpenses) "Where it went" else "Where it came from")
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(Spacing.lg)) {
                    DonutChart(
                        slices = grouped.mapIndexed { index, (label, value) ->
                            DonutSlice(label, value, palette[index % palette.size])
                        },
                        centerLabel = total.asMoney(),
                        centerCaption = period.label,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                    )
                    Column(modifier = Modifier.padding(top = Spacing.lg)) {
                        grouped.forEachIndexed { index, (label, value) ->
                            BreakdownRow(
                                label = label,
                                value = value.asMoney(),
                                fraction = if (total > 0) (value / total).toFloat() else 0f,
                                color = palette[index % palette.size]
                            )
                        }
                    }
                }
            }
        }

        if (monthly.size > 1) {
            item { SectionHeader(title = "Month by month") }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    MonthlyBarChart(
                        data = monthly,
                        barColor = accent,
                        modifier = Modifier.padding(Spacing.lg)
                    )
                }
            }
        }
    }
}

/** Chronological monthly totals, labelled `MMM` with the amount above each bar. */
private fun monthlyTotals(entries: List<Pair<Long, Double>>): List<BarDatum> =
    entries
        .groupBy { (timestamp, _) -> monthKey(timestamp) }
        .toSortedMap()
        .map { (_, rows) ->
            val total = rows.sumOf { it.second }
            BarDatum(
                label = rows.first().first.asMonthYear().substringBefore(' '),
                value = total,
                valueLabel = total.asMoney().substringBefore('.')
            )
        }

/** Sortable `yyyyMM` key. */
private fun monthKey(timestamp: Long): Int {
    val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
    return calendar.get(Calendar.YEAR) * 100 + calendar.get(Calendar.MONTH)
}
