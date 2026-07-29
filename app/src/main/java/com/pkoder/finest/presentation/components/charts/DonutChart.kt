package com.pkoder.finest.presentation.components.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pkoder.finest.presentation.ui.theme.Mono
import com.pkoder.finest.presentation.ui.theme.Spacing

data class DonutSlice(
    val label: String,
    val value: Double,
    val color: Color
)

/**
 * The wealth-distribution ring from the Insights design: a thick band of brand tones with the total
 * in tabular figures at the centre.
 *
 * The reference mock hangs leader-line callouts off each slice; those need collision handling to stay
 * legible on a narrow screen, so the amounts live in the legend under the chart instead — same
 * information, no overlap.
 */
@Composable
fun DonutChart(
    slices: List<DonutSlice>,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 34.dp,
    centerLabel: String? = null,
    centerCaption: String? = null
) {
    val total = slices.sumOf { it.value }
    if (total <= 0.0) return

    // Re-animates whenever the data changes, so switching the insights period feels responsive.
    val sweepProgress = remember(slices) { Animatable(0f) }
    LaunchedEffect(slices) {
        sweepProgress.animateTo(1f, tween(durationMillis = 650, easing = FastOutSlowInEasing))
    }

    val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .aspectRatio(1f)
                .padding(strokeWidth / 2)
        ) {
            val stroke = Stroke(width = strokeWidth.toPx())
            val arcSize = Size(size.minDimension, size.minDimension)
            val topLeft = Offset(
                (size.width - arcSize.width) / 2f,
                (size.height - arcSize.height) / 2f
            )

            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke
            )

            var startAngle = -90f
            slices.forEach { slice ->
                val fullSweep = (slice.value / total * 360f).toFloat()
                val sweep = fullSweep * sweepProgress.value
                // Small gap between slices; skip it when the slice is already hairline thin.
                val gap = if (fullSweep > 6f) 1.5f else 0f
                drawArc(
                    color = slice.color,
                    startAngle = startAngle,
                    sweepAngle = (sweep - gap).coerceAtLeast(0f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = stroke
                )
                startAngle += fullSweep
            }
        }

        if (centerLabel != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = strokeWidth + Spacing.lg)
            ) {
                if (centerCaption != null) {
                    Text(
                        text = centerCaption.uppercase(),
                        style = Mono.labelWide,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                Text(
                    text = centerLabel,
                    style = Mono.displayLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = Spacing.xs)
                )
            }
        }
    }
}

/**
 * Dot + label + share, laid out as the design's three-up legend under the ring.
 *
 * Anything past [maxItems] is folded into a single "Other" entry rather than dropped, so the shares
 * on screen always add up to 100% — a top-3 legend that silently omits four more categories reads as
 * if those were all there is.
 */
@Composable
fun DonutLegend(
    slices: List<DonutSlice>,
    modifier: Modifier = Modifier,
    maxItems: Int = 3
) {
    val total = slices.sumOf { it.value }
    if (total <= 0.0) return

    val shown = if (slices.size <= maxItems) {
        slices
    } else {
        val head = slices.take(maxItems - 1)
        val rest = slices.drop(maxItems - 1)
        head + DonutSlice(
            label = "Other (${rest.size})",
            value = rest.sumOf { it.value },
            color = MaterialTheme.colorScheme.outline
        )
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        shown.forEach { slice ->
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .size(8.dp)
                        .background(slice.color, CircleShape)
                )
                Column {
                    Text(
                        text = slice.label,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${Math.round(slice.value / total * 100)}%",
                        style = Mono.label,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        // Pads a one- or two-slice legend so the columns keep the same width as a full row.
        repeat(maxItems - shown.size) {
            Box(modifier = Modifier.weight(1f))
        }
    }
}
