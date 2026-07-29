package com.pkoder.finest.presentation.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Money semantics, kept outside the Material scheme.
 *
 * The design system assigns them explicitly: "Mint Green for positive trends and Soft Pink for
 * negative trends". Containers are those two accents at 12% over the charcoal canvas — the design's
 * "secondary colours at 10% opacity" rule — so a tinted avatar never reads as a solid button.
 */
@Immutable
data class MoneyColors(
    val income: Color,
    val incomeContainer: Color,
    val onIncomeContainer: Color,
    val expense: Color,
    val expenseContainer: Color,
    val onExpenseContainer: Color,
    val chartPalette: List<Color>
)

/**
 * Categorical chart tones: the three brand accents first, then muted extensions that still read on
 * `#131313` without competing with mint.
 */
private val CharcoalChartPalette = listOf(
    Charcoal.primary,
    Charcoal.secondary,
    Charcoal.tertiaryFixed,
    Charcoal.primaryFixedDim,
    Color(0xFFB9A5D6),
    Color(0xFF8FC5A9),
    Color(0xFFE0A98A),
    Color(0xFF9BB8CE)
)

val CharcoalMoneyColors = MoneyColors(
    income = Charcoal.primary,
    incomeContainer = Charcoal.primary.copy(alpha = 0.12f),
    onIncomeContainer = Charcoal.primary,
    expense = Charcoal.secondary,
    expenseContainer = Charcoal.secondary.copy(alpha = 0.12f),
    onExpenseContainer = Charcoal.secondary,
    chartPalette = CharcoalChartPalette
)

val LocalMoneyColors = staticCompositionLocalOf { CharcoalMoneyColors }

/** Shorthand: `moneyColors.expense` inside any composable under [FinEstTheme]. */
val moneyColors: MoneyColors
    @Composable @ReadOnlyComposable get() = LocalMoneyColors.current
