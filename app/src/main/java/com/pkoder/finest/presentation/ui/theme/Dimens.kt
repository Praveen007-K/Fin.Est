package com.pkoder.finest.presentation.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * One spacing scale for the whole app, on the design system's strict 8px grid.
 *
 * [screen] and [card] are the two the design calls out by name (`container_padding_mobile` and the
 * card's inner padding); the rest are the grid steps everything else lands on.
 */
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp

    /** Horizontal padding shared by every screen's content. */
    val screen = 20.dp

    /** Inner padding for a feature card (the hero, the chart cards). */
    val card = 24.dp

    /** Vertical gap between top-level sections. */
    val gutter = 24.dp
}

/**
 * "Large, inviting radii": 20dp cards against 12dp inputs and pill buttons.
 *
 * `large` is the card radius the design fixes at 20dp, so a plain `Card` picks it up by default.
 */
val FinEstShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp)
)
