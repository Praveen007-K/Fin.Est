package com.pkoder.finest.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.InsertChart
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pkoder.finest.data.local.entities.CreditEntryEntity
import com.pkoder.finest.data.local.entities.DebitEntryEntity
import com.pkoder.finest.presentation.components.BalanceHeroCard
import com.pkoder.finest.presentation.components.EmptyState
import com.pkoder.finest.presentation.components.GlassCard
import com.pkoder.finest.presentation.components.SectionHeader
import com.pkoder.finest.presentation.components.SummaryTile
import com.pkoder.finest.presentation.components.TonalIcon
import com.pkoder.finest.presentation.components.TransactionRow
import com.pkoder.finest.presentation.ui.theme.Charcoal
import com.pkoder.finest.presentation.ui.theme.Mono
import com.pkoder.finest.presentation.ui.theme.Spacing
import com.pkoder.finest.presentation.ui.theme.moneyColors
import com.pkoder.finest.presentation.viewmodel.FinanceViewModel
import com.pkoder.finest.presentation.viewmodel.SmsViewModel
import java.util.Calendar

/**
 * Landing screen: where the month stands, anything waiting for review, and the latest activity.
 *
 * The hero's trend chip and sparkline are derived here rather than stored — the trend compares this
 * month's net against last month's, and the sparkline is the running net day by day through the
 * current month.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    financeViewModel: FinanceViewModel,
    smsViewModel: SmsViewModel,
    onSeeAllTransactions: () -> Unit,
    onOpenReview: () -> Unit,
    onOpenInsights: () -> Unit,
    onAddEntry: () -> Unit
) {
    val debits by financeViewModel.debits.collectAsState()
    val credits by financeViewModel.credits.collectAsState()
    val pending by smsViewModel.pendingTransactions.collectAsState()
    val isRefreshing by financeViewModel.isRefreshing.collectAsState()

    val month = remember(debits, credits) { monthSummary(debits, credits) }
    val recent = remember(debits, credits) { recentActivity(debits, credits, limit = 6) }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = financeViewModel::refresh,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Spacing.screen,
                end = Spacing.screen,
                top = Spacing.sm,
                bottom = Spacing.xxl
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.gutter)
        ) {
            item {
                BalanceHeroCard(
                    label = "Net this month",
                    balance = month.net,
                    trendFraction = month.trendFraction,
                    sparkline = month.runningNet
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.lg)
                ) {
                    SummaryTile(
                        label = "Income",
                        amount = month.income,
                        isExpense = false,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryTile(
                        label = "Expenses",
                        amount = month.expense,
                        isExpense = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                QuickActions(
                    onAddEntry = onAddEntry,
                    onSeeAllTransactions = onSeeAllTransactions,
                    onOpenInsights = onOpenInsights,
                    onOpenReview = onOpenReview,
                    pendingCount = pending.size
                )
            }

            if (pending.isNotEmpty()) {
                item {
                    ReviewPromptCard(count = pending.size, onClick = onOpenReview)
                }
            }

            item {
                SectionHeader(
                    title = "Recent activity",
                    actionLabel = if (recent.isNotEmpty()) "VIEW ALL" else null,
                    onAction = onSeeAllTransactions.takeIf { recent.isNotEmpty() }
                )
            }

            if (recent.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = Spacing.xl)) {
                        EmptyState(
                            message = "Nothing logged yet",
                            icon = Icons.Default.Add,
                            hint = "Add your first expense or income to see it here.",
                            actionLabel = "Add transaction",
                            onAction = onAddEntry
                        )
                    }
                }
            } else {
                // One card per row, not a single card with dividers: the design gives every
                // transaction its own tonal panel so the list reads as discrete objects.
                items(recent.size) { index ->
                    val activity = recent[index]
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        TransactionRow(
                            title = activity.title,
                            subtitle = activity.subtitle,
                            amount = activity.amount,
                            timestamp = activity.timestamp,
                            isExpense = activity.isExpense,
                            synced = activity.synced
                        )
                    }
                }
            }
        }
    }
}

/**
 * The design's four-up action row. Only the first is filled — one primary action per screen — and
 * every one of them goes somewhere real.
 */
@Composable
private fun QuickActions(
    onAddEntry: () -> Unit,
    onSeeAllTransactions: () -> Unit,
    onOpenInsights: () -> Unit,
    onOpenReview: () -> Unit,
    pendingCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        QuickAction(
            icon = Icons.Default.Add,
            label = "Add",
            onClick = onAddEntry,
            filled = true
        )
        QuickAction(
            icon = Icons.Default.ArrowDownward,
            label = "History",
            onClick = onSeeAllTransactions
        )
        QuickAction(
            icon = Icons.Default.InsertChart,
            label = "Insights",
            onClick = onOpenInsights
        )
        QuickAction(
            icon = Icons.Default.RateReview,
            label = if (pendingCount > 0) "Review ($pendingCount)" else "Review",
            onClick = onOpenReview,
            accent = if (pendingCount > 0) MaterialTheme.colorScheme.primary else null
        )
    }
}

@Composable
private fun QuickAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    accent: Color? = null
) {
    val outline = accent ?: MaterialTheme.colorScheme.outlineVariant
    val iconTint = accent ?: MaterialTheme.colorScheme.onSurface

    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.xs, vertical = Spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .then(
                    if (filled) {
                        Modifier.background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    } else {
                        Modifier.border(1.dp, outline, CircleShape)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (filled) MaterialTheme.colorScheme.onPrimary else iconTint,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = label,
            style = Mono.label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ReviewPromptCard(count: Int, onClick: () -> Unit) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        containerColor = Charcoal.tertiaryFixed.copy(alpha = 0.10f),
        borderColor = Charcoal.tertiaryFixed.copy(alpha = 0.35f),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TonalIcon(
                icon = Icons.Default.RateReview,
                tint = Charcoal.tertiaryFixed,
                size = 40.dp
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = Spacing.lg)
            ) {
                Text(
                    text = if (count == 1) "1 payment needs review"
                    else "$count payments need review",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "CAPTURED FROM YOUR BANK SMS",
                    style = Mono.label,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Charcoal.tertiaryFixed
            )
        }
    }
}

/** Flattened view of both tables for the recents list. */
private data class Activity(
    val title: String,
    val subtitle: String?,
    val amount: Double,
    val timestamp: Long,
    val isExpense: Boolean,
    val synced: Boolean
)

private fun recentActivity(
    debits: List<DebitEntryEntity>,
    credits: List<CreditEntryEntity>,
    limit: Int
): List<Activity> {
    val expenses = debits.map {
        Activity(
            title = it.category,
            subtitle = listOfNotNull(
                it.bank.takeIf(String::isNotBlank),
                it.paymentMethod.takeIf(String::isNotBlank)
            ).joinToString(" • ").ifBlank { null },
            amount = it.amount,
            timestamp = it.timestamp,
            isExpense = true,
            synced = it.synced
        )
    }
    val incomes = credits.map {
        Activity(
            title = it.source,
            subtitle = null,
            amount = it.amount,
            timestamp = it.timestamp,
            isExpense = false,
            synced = it.synced
        )
    }
    return (expenses + incomes).sortedByDescending { it.timestamp }.take(limit)
}

/** Everything the hero and the two tiles need, computed once per data change. */
private data class MonthSummary(
    val income: Double,
    val expense: Double,
    val net: Double,
    /** Net change against last month, as a fraction; null when last month had no activity. */
    val trendFraction: Double?,
    /** Running net for each day elapsed this month, oldest first. */
    val runningNet: List<Double>
)

private fun monthSummary(
    debits: List<DebitEntryEntity>,
    credits: List<CreditEntryEntity>,
    now: Long = System.currentTimeMillis()
): MonthSummary {
    val thisMonthStart = startOfMonth(now)
    val lastMonthStart = startOfMonth(thisMonthStart - 1)

    val income = credits.filter { it.timestamp >= thisMonthStart }.sumOf { it.amount }
    val expense = debits.filter { it.timestamp >= thisMonthStart }.sumOf { it.amount }
    val net = income - expense

    val lastNet = credits.filter { it.timestamp in lastMonthStart until thisMonthStart }
        .sumOf { it.amount } -
        debits.filter { it.timestamp in lastMonthStart until thisMonthStart }.sumOf { it.amount }

    // A zero baseline makes the percentage meaningless, so no chip is shown at all.
    val trend = if (lastNet == 0.0) null else (net - lastNet) / kotlin.math.abs(lastNet)

    val daysElapsed = Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.DAY_OF_MONTH)
    val perDay = DoubleArray(daysElapsed)
    fun add(timestamp: Long, delta: Double) {
        if (timestamp < thisMonthStart) return
        val day = Calendar.getInstance().apply { timeInMillis = timestamp }
            .get(Calendar.DAY_OF_MONTH) - 1
        if (day in perDay.indices) perDay[day] += delta
    }
    credits.forEach { add(it.timestamp, it.amount) }
    debits.forEach { add(it.timestamp, -it.amount) }

    var running = 0.0
    val runningNet = perDay.map { running += it; running }

    return MonthSummary(income, expense, net, trend, runningNet)
}

private fun startOfMonth(timestamp: Long): Long =
    Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
