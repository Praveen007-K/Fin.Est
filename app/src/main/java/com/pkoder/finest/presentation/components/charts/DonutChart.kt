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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pkoder.finest.presentation.ui.theme.Spacing

data class DonutSlice(
    val label: String,
    val value: Double,
    val color: Color
)

/**
 * Compose-native replacement for the MPAndroidChart pie: themes itself, animates in, and needs no
 * `AndroidView` interop.
 */
@Composable
fun DonutChart(
    slices: List<DonutSlice>,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 26.dp,
    centerLabel: String? = null,
    centerCaption: String? = null
) {
    val total = slices.sumOf { it.value }
    if (total <= 0.0) return

    // Re-animates whenever the data changes, so switching stats period feels responsive.
    val sweepProgress = remember(slices) { Animatable(0f) }
    LaunchedEffect(slices) {
        sweepProgress.animateTo(1f, tween(durationMillis = 650, easing = FastOutSlowInEasing))
    }

    val trackColor = MaterialTheme.colorScheme.surfaceVariant

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
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = centerLabel,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
                if (centerCaption != null) {
                    Text(
                        text = centerCaption,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** Colour swatch + label + value row, shared by both stats tabs. */
@Composable
fun ChartLegendRow(
    color: Color,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}
