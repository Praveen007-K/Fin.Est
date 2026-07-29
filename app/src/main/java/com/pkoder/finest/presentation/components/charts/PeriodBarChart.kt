package com.pkoder.finest.presentation.components.charts

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pkoder.finest.presentation.ui.theme.Mono
import com.pkoder.finest.presentation.ui.theme.Spacing

data class BarDatum(
    val label: String,
    val value: Double,
    /** Shown in the tooltip above the tallest bar; usually the formatted amount. */
    val valueLabel: String
)

/**
 * Bucketed totals — weeks within a month, months within a quarter, and so on.
 *
 * The design highlights only the peak bucket: that bar is mint with a mint tooltip pill above it,
 * everything else recedes to a charcoal track. Built from layout composables rather than a Canvas so
 * the labels are real `Text` — they scale with the user's font size and TalkBack can read them.
 */
@Composable
fun PeriodBarChart(
    data: List<BarDatum>,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primaryContainer,
    chartHeight: Dp = 160.dp
) {
    if (data.isEmpty()) return
    val max = data.maxOf { it.value }.takeIf { it > 0.0 } ?: return
    val peak = data.indexOfFirst { it.value == max }

    Column(modifier = modifier.fillMaxWidth()) {
        // The tooltip is reserved in the layout for every column so bar tops stay on one baseline.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            data.forEachIndexed { index, datum ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    if (index == peak) {
                        Text(
                            text = datum.valueLabel,
                            style = Mono.label,
                            color = MaterialTheme.colorScheme.onPrimary,
                            maxLines = 1,
                            // Unbounded: a full amount is wider than one column, and clipping it
                            // to the bar width turned "₹27,350" into "₹27,3".
                            modifier = Modifier
                                .wrapContentWidth(unbounded = true)
                                .background(accent, CircleShape)
                                .padding(horizontal = Spacing.sm, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.Bottom
        ) {
            data.forEachIndexed { index, datum ->
                val fraction by animateFloatAsState(
                    targetValue = (datum.value / max).toFloat().coerceIn(0f, 1f),
                    animationSpec = tween(durationMillis = 600),
                    label = "barHeight"
                )
                val isPeak = index == peak

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = "${datum.label}: ${datum.valueLabel}" },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .height(chartHeight)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.62f)
                                // A floor keeps a tiny-but-nonzero bucket visible.
                                .fillMaxHeight(fraction.coerceAtLeast(0.03f))
                                .background(
                                    color = if (isPeak) accent
                                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                                )
                        )
                    }
                    Text(
                        text = datum.label,
                        style = Mono.label,
                        color = if (isPeak) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = Spacing.sm)
                    )
                }
            }
            // Keeps a single bar from stretching across the whole card.
            if (data.size == 1) Box(modifier = Modifier.width(0.dp).weight(2f))
        }
    }
}
