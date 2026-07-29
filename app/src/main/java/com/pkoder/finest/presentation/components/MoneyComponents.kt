package com.pkoder.finest.presentation.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pkoder.finest.presentation.ui.theme.Spacing
import com.pkoder.finest.presentation.ui.theme.moneyColors
import com.pkoder.finest.presentation.util.asMoney
import com.pkoder.finest.presentation.util.asRelativeDay
import com.pkoder.finest.presentation.util.asSignedMoney
import com.pkoder.finest.presentation.util.asTime

/** The dashboard's headline: what's left this period, with the two totals behind it. */
@Composable
fun BalanceHeroCard(
    periodLabel: String,
    income: Double,
    expense: Double,
    modifier: Modifier = Modifier
) {
    val balance = income - expense
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(Spacing.xl)) {
            Text(text = periodLabel, style = MaterialTheme.typography.labelLarge)
            Text(
                text = balance.asMoney(),
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier
                    .padding(top = Spacing.xs)
                    .animateContentSize()
            )
            Text(
                text = if (balance >= 0) "Left over" else "Overspent",
                style = MaterialTheme.typography.bodySmall
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                SummaryTile(
                    label = "Income",
                    amount = income,
                    isExpense = false,
                    modifier = Modifier.weight(1f)
                )
                SummaryTile(
                    label = "Expenses",
                    amount = expense,
                    isExpense = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun SummaryTile(
    label: String,
    amount: Double,
    isExpense: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = moneyColors
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (isExpense) colors.expenseContainer else colors.incomeContainer,
            contentColor = if (isExpense) colors.onExpenseContainer else colors.onIncomeContainer
        )
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isExpense) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(start = Spacing.xs)
                )
            }
            Text(
                text = amount.asMoney(),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = Spacing.xs)
            )
        }
    }
}

/**
 * One transaction line, used by the dashboard's recents and by history.
 *
 * @param synced false shows a cloud-off marker, so a row that only exists on this device is
 *   never silently presented as saved.
 */
@Composable
fun TransactionRow(
    title: String,
    subtitle: String?,
    amount: Double,
    timestamp: Long,
    isExpense: Boolean,
    modifier: Modifier = Modifier,
    synced: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val colors = moneyColors
    val accent = if (isExpense) colors.expense else colors.income

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (isExpense) colors.expenseContainer else colors.incomeContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isExpense) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                contentDescription = if (isExpense) "Expense" else "Income",
                tint = accent,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = Spacing.md)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = listOfNotNull(subtitle?.takeIf { it.isNotBlank() }, timestamp.asTime())
                    .joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = amount.asSignedMoney(isExpense),
                style = MaterialTheme.typography.titleSmall,
                color = accent,
                maxLines = 1
            )
            if (!synced) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = "Not synced yet",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "On device",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 2.dp)
                    )
                }
            }
        }

        if (trailing != null) trailing()
    }
}

/** Sticky-ish day header for grouped transaction lists. */
@Composable
fun DayHeader(timestamp: Long, total: Double, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = timestamp.asRelativeDay(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = total.asMoney(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
