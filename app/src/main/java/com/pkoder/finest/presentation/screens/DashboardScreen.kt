package com.pkoder.finest.presentation.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.dp
import com.pkoder.finest.data.local.entities.CreditEntryEntity
import com.pkoder.finest.data.local.entities.DebitEntryEntity
import com.pkoder.finest.presentation.components.BalanceHeroCard
import com.pkoder.finest.presentation.components.EmptyState
import com.pkoder.finest.presentation.components.SectionHeader
import com.pkoder.finest.presentation.components.TransactionRow
import com.pkoder.finest.presentation.ui.theme.Spacing
import com.pkoder.finest.presentation.util.Period
import com.pkoder.finest.presentation.util.periodStart
import com.pkoder.finest.presentation.viewmodel.FinanceViewModel
import com.pkoder.finest.presentation.viewmodel.SmsViewModel

/**
 * Landing screen: where the month stands, anything waiting for review, and the latest activity.
 * Entry forms moved into the FAB sheet, so the first thing the user sees is their money rather
 * than an empty form.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    financeViewModel: FinanceViewModel,
    smsViewModel: SmsViewModel,
    onSeeAllTransactions: () -> Unit,
    onOpenReview: () -> Unit,
    onAddEntry: () -> Unit
) {
    val debits by financeViewModel.debits.collectAsState()
    val credits by financeViewModel.credits.collectAsState()
    val pending by smsViewModel.pendingTransactions.collectAsState()
    val isRefreshing by financeViewModel.isRefreshing.collectAsState()

    val monthStart = remember(debits, credits) { periodStart(Period.MONTH) }
    val monthExpense = debits.filter { it.timestamp >= monthStart }.sumOf { it.amount }
    val monthIncome = credits.filter { it.timestamp >= monthStart }.sumOf { it.amount }

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
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            item {
                BalanceHeroCard(
                    periodLabel = Period.MONTH.label,
                    income = monthIncome,
                    expense = monthExpense
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
                    actionLabel = if (recent.isNotEmpty()) "See all" else null,
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
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            recent.forEachIndexed { index, activity ->
                                TransactionRow(
                                    title = activity.title,
                                    subtitle = activity.subtitle,
                                    amount = activity.amount,
                                    timestamp = activity.timestamp,
                                    isExpense = activity.isExpense,
                                    synced = activity.synced
                                )
                                if (index != recent.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = Spacing.lg)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewPromptCard(count: Int, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = Spacing.md)
            ) {
                Text(
                    text = if (count == 1) "1 payment needs review"
                    else "$count payments need review",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "Captured from your bank SMS",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null
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
            subtitle = listOfNotNull(it.bank.takeIf(String::isNotBlank), it.paymentMethod.takeIf(String::isNotBlank))
                .joinToString(" · ")
                .ifBlank { null },
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
