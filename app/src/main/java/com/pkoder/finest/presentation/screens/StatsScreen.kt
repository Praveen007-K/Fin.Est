package com.pkoder.finest.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pkoder.finest.presentation.components.BreakdownRow
import com.pkoder.finest.presentation.components.DropdownPill
import com.pkoder.finest.presentation.components.EmptyState
import com.pkoder.finest.presentation.components.GlassCard
import com.pkoder.finest.presentation.components.SummaryTile
import com.pkoder.finest.presentation.components.charts.BarDatum
import com.pkoder.finest.presentation.components.charts.DonutChart
import com.pkoder.finest.presentation.components.charts.DonutLegend
import com.pkoder.finest.presentation.components.charts.DonutSlice
import com.pkoder.finest.presentation.components.charts.PeriodBarChart
import com.pkoder.finest.presentation.ui.theme.Mono
import com.pkoder.finest.presentation.ui.theme.Spacing
import com.pkoder.finest.presentation.ui.theme.moneyColors
import com.pkoder.finest.presentation.util.Period
import com.pkoder.finest.presentation.util.asMoney
import com.pkoder.finest.presentation.util.asMoneyWhole
import com.pkoder.finest.presentation.util.buckets
import com.pkoder.finest.presentation.util.periodStart
import com.pkoder.finest.presentation.viewmodel.FinanceViewModel

/**
 * Insights: totals for the chosen period, a category (or source) donut, and the trend across the
 * period's buckets. Charts are Compose-native, so they follow the theme and animate with the rest of
 * the UI.
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
    val trend: List<BarDatum> = remember(periodDebits, periodCredits, showExpenses, period) {
        val timed = if (showExpenses) {
            periodDebits.map { it.timestamp to it.amount }
        } else {
            periodCredits.map { it.timestamp to it.amount }
        }
        period.buckets(timed).map { BarDatum(it.label, it.total, it.total.asMoneyWhole()) }
    }

    val total = if (showExpenses) totalExpense else totalIncome
    val slices = grouped.mapIndexed { index, (label, value) ->
        DonutSlice(label, value, palette[index % palette.size])
    }

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
            // Centred title block, as in the Insights reference.
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (showExpenses) "Spending Insights" else "Income Insights",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Where your money moved · ${period.label.lowercase()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = Spacing.xs)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
                    SegmentedButton(
                        selected = showExpenses,
                        onClick = { showExpenses = true },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        colors = mintSegmentColors(),
                        // The design has no tick on the active segment; the fill carries it.
                        icon = {}
                    ) { Text("Expenses", style = Mono.label) }
                    SegmentedButton(
                        selected = !showExpenses,
                        onClick = { showExpenses = false },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        colors = mintSegmentColors(),
                        icon = {}
                    ) { Text("Income", style = Mono.label) }
                }
                DropdownPill(
                    selected = period.label,
                    options = Period.entries.map { it.label },
                    onSelect = { label -> period = Period.entries.first { it.label == label } }
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.lg)
            ) {
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
                    hint = "Pick a wider date range to see more.",
                    modifier = Modifier.height(320.dp)
                )
            }
            return@LazyColumn
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(Spacing.card)) {
                    // Fixed square, centred: letting the canvas take the card width pushed the
                    // ring flush against both edges.
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        DonutChart(
                            slices = slices,
                            centerLabel = total.asMoneyWhole(),
                            centerCaption = if (showExpenses) "Total spent" else "Total earned",
                            modifier = Modifier.size(240.dp)
                        )
                    }
                    DonutLegend(
                        slices = slices,
                        modifier = Modifier.padding(top = Spacing.xl)
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(vertical = Spacing.lg)
                    )
                    Text(
                        text = if (showExpenses) "WHERE IT WENT" else "WHERE IT CAME FROM",
                        style = Mono.labelWide,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column(modifier = Modifier.padding(top = Spacing.sm)) {
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

        if (trend.size > 1) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(Spacing.card)) {
                        Text(
                            text = "TREND",
                            style = Mono.labelWide,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        PeriodBarChart(
                            data = trend,
                            chartHeight = 140.dp,
                            modifier = Modifier.padding(top = Spacing.lg)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun mintSegmentColors() = SegmentedButtonDefaults.colors(
    activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
    activeContentColor = MaterialTheme.colorScheme.onPrimary,
    activeBorderColor = MaterialTheme.colorScheme.primaryContainer,
    inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    inactiveBorderColor = MaterialTheme.colorScheme.outlineVariant
)
