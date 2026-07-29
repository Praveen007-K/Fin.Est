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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pkoder.finest.presentation.ui.theme.Spacing

data class BarDatum(
    val label: String,
    val value: Double,
    /** Shown above the bar; usually the formatted amount. */
    val valueLabel: String
)

/**
 * Month-over-month bars.
 *
 * Built from layout composables rather than a Canvas so the labels are real `Text` — they scale
 * with the user's font size and are readable by TalkBack.
 */
@Composable
fun MonthlyBarChart(
    data: List<BarDatum>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    chartHeight: Dp = 160.dp
) {
    if (data.isEmpty()) return
    val max = data.maxOf { it.value }.takeIf { it > 0.0 } ?: return

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.Bottom
    ) {
        data.forEach { datum ->
            val fraction by animateFloatAsState(
                targetValue = (datum.value / max).toFloat().coerceIn(0f, 1f),
                animationSpec = tween(durationMillis = 600),
                label = "barHeight"
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = "${datum.label}: ${datum.valueLabel}" },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = datum.valueLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
                Box(
                    modifier = Modifier
                        .padding(vertical = Spacing.xs)
                        .height(chartHeight)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            // A floor keeps a tiny-but-nonzero month visible.
                            .fillMaxHeight(fraction.coerceAtLeast(0.02f))
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(barColor)
                    )
                }
                Text(
                    text = datum.label,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
            }
        }
        // Keeps a single bar from stretching across the whole card.
        if (data.size == 1) Box(modifier = Modifier.width(0.dp).weight(2f))
    }
}
