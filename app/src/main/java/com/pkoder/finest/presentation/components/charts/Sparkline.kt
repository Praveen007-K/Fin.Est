package com.pkoder.finest.presentation.components.charts

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The thin trend line in the balance hero — "thin, precise 2pt strokes" per the design system.
 *
 * Values are normalised against their own min/max, so this shows the *shape* of the run rather than
 * absolute amounts; the number beside it carries the magnitude. A flat run draws down the middle
 * instead of dividing by a zero range.
 */
@Composable
fun Sparkline(
    values: List<Double>,
    color: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 2.dp
) {
    if (values.size < 2) return

    val progress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 700),
        label = "sparkline"
    )

    val min = values.min()
    val max = values.max()
    val range = (max - min).takeIf { it > 0.0 }

    Canvas(modifier = modifier) {
        val stepX = size.width / (values.size - 1)
        val points = values.mapIndexed { index, value ->
            val normalised = range?.let { (value - min) / it } ?: 0.5
            // Inset by the stroke so the extremes are not clipped at the edges.
            val usable = size.height - strokeWidth.toPx()
            Offset(
                x = index * stepX,
                y = (strokeWidth.toPx() / 2f) + (1f - normalised.toFloat()) * usable
            )
        }

        // Reveal left-to-right by only drawing the segments inside the animated window.
        val visible = (points.size * progress).toInt().coerceAtLeast(2)
        val path = Path().apply {
            moveTo(points[0].x, points[0].y)
            for (index in 1 until visible) lineTo(points[index].x, points[index].y)
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = strokeWidth.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}
