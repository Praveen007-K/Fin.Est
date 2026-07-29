package com.pkoder.finest.presentation.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pkoder.finest.presentation.components.charts.Sparkline
import com.pkoder.finest.presentation.ui.theme.Mono
import com.pkoder.finest.presentation.ui.theme.Spacing
import com.pkoder.finest.presentation.ui.theme.moneyColors
import com.pkoder.finest.presentation.util.asMoney
import com.pkoder.finest.presentation.util.asMoneyWhole
import com.pkoder.finest.presentation.util.asRelativeDay
import com.pkoder.finest.presentation.util.asSignedMoney
import com.pkoder.finest.presentation.util.asTime
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The dashboard's headline: what's left this period, with a trend chip and a sparkline of how it got
 * there. A wallet glyph sits behind it at 10% opacity — the design's one decorative flourish.
 *
 * @param trendFraction change against the previous comparable period, e.g. `0.024` for +2.4%. Null
 *   when there is no previous period to compare with, in which case no chip is shown rather than a
 *   meaningless "0%".
 * @param sparkline running balance across the period, oldest first. Fewer than two points draws
 *   nothing.
 */
@Composable
fun BalanceHeroCard(
    label: String,
    balance: Double,
    modifier: Modifier = Modifier,
    trendFraction: Double? = null,
    sparkline: List<Double> = emptyList()
) {
    val positive = balance >= 0
    val accent = if (positive) moneyColors.income else moneyColors.expense

    GlassCard(modifier = modifier.fillMaxWidth()) {
        Box {
            Icon(
                imageVector = Icons.Default.AccountBalanceWallet,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(Spacing.sm)
                    .size(120.dp)
            )

            Column(modifier = Modifier.padding(Spacing.card)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.sm),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = balance.asMoney(),
                            style = MaterialTheme.typography.displaySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.animateContentSize()
                        )
                        Text(
                            text = if (positive) "LEFT OVER" else "OVERSPENT",
                            style = Mono.labelWide,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.padding(start = Spacing.md)
                    ) {
                        if (trendFraction != null) {
                            val up = trendFraction >= 0
                            MonoChip(
                                text = (if (up) "+" else "−") +
                                    "${(abs(trendFraction) * 100).roundToInt()}%",
                                color = if (up) moneyColors.income else moneyColors.expense,
                                leadingIcon = if (up) Icons.Default.ArrowUpward
                                else Icons.Default.ArrowDownward
                            )
                        }
                        if (sparkline.size >= 2) {
                            Sparkline(
                                values = sparkline,
                                color = accent,
                                modifier = Modifier
                                    .padding(top = Spacing.sm)
                                    .width(96.dp)
                                    .height(32.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * One half of the income/expenses pair: a tinted disc, an all-caps mono label and the amount in
 * tabular figures.
 */
@Composable
fun SummaryTile(
    label: String,
    amount: Double,
    isExpense: Boolean,
    modifier: Modifier = Modifier
) {
    val accent = if (isExpense) moneyColors.expense else moneyColors.income

    GlassCard(modifier = modifier, shape = MaterialTheme.shapes.medium) {
        Column(
            modifier = Modifier.padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            TonalIcon(
                icon = if (isExpense) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                tint = accent,
                size = 40.dp
            )
            Column {
                Text(
                    text = label.uppercase(),
                    style = Mono.labelWide,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = amount.asMoneyWhole(),
                    style = Mono.tile,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = Spacing.xs)
                )
            }
        }
    }
}

/**
 * One transaction line, used by the dashboard's recents and by history.
 *
 * Only income is tinted — the design keeps expense amounts in plain `on-surface` and lets the minus
 * sign and the category disc carry the direction, so a list of spending does not read as a wall of
 * warnings.
 *
 * The time sits under the amount rather than after the subtitle: on a phone, `bank • method • time`
 * in one line ellipsised as soon as the row also carried a delete button, and the right-hand column
 * has the width to spare.
 *
 * @param synced false shows a cloud-off marker, so a row that only exists on this device is never
 *   silently presented as saved.
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
    val visual = categoryVisual(title, isExpense)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TonalIcon(
            icon = visual.icon,
            tint = visual.tint,
            contentDescription = if (isExpense) "Expense" else "Income"
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = Spacing.lg)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = Mono.label,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.padding(start = Spacing.sm)
        ) {
            Text(
                text = amount.asSignedMoney(isExpense),
                style = Mono.amount,
                color = if (isExpense) MaterialTheme.colorScheme.onSurface else moneyColors.income,
                maxLines = 1
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                if (!synced) {
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = "Not synced yet",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp)
                    )
                }
                Text(
                    text = timestamp.asTime(),
                    style = Mono.label,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }

        if (trailing != null) trailing()
    }
}

/** Day header for grouped transaction lists: all-caps day on the left, the day's net on the right. */
@Composable
fun DayHeader(timestamp: Long, total: Double, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.xs, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = timestamp.asRelativeDay().uppercase(),
            style = Mono.labelWide,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = total.asMoney(),
            style = Mono.label,
            color = if (total >= 0) moneyColors.income else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
