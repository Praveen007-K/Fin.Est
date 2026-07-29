package com.pkoder.finest.presentation.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Money semantics live outside the Material scheme.
 *
 * The app uses dynamic colour, so `primary`/`error` change from device to device — income and
 * expense must not. These are fixed, contrast-checked pairs for light and dark, plus a categorical
 * palette for charts that stays readable in both.
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

private val LightChartPalette = listOf(
    Color(0xFF3D5AFE), Color(0xFF00897B), Color(0xFFEF6C00), Color(0xFF8E24AA),
    Color(0xFF0288D1), Color(0xFFC2185B), Color(0xFF558B2F), Color(0xFF5D4037)
)

private val DarkChartPalette = listOf(
    Color(0xFF9FA8FF), Color(0xFF56C9BC), Color(0xFFFFB264), Color(0xFFD69AE8),
    Color(0xFF76C7F2), Color(0xFFF48FB1), Color(0xFFA5D267), Color(0xFFBCAAA4)
)

val LightMoneyColors = MoneyColors(
    income = Color(0xFF14663A),
    incomeContainer = Color(0xFFCFF0DC),
    onIncomeContainer = Color(0xFF03301A),
    expense = Color(0xFFA8342A),
    expenseContainer = Color(0xFFFBDDD9),
    onExpenseContainer = Color(0xFF41100B),
    chartPalette = LightChartPalette
)

val DarkMoneyColors = MoneyColors(
    income = Color(0xFF7BDBA4),
    incomeContainer = Color(0xFF11341F),
    onIncomeContainer = Color(0xFFB6F0CC),
    expense = Color(0xFFFFB4AB),
    expenseContainer = Color(0xFF3D1512),
    onExpenseContainer = Color(0xFFFFDAD5),
    chartPalette = DarkChartPalette
)

val LocalMoneyColors = staticCompositionLocalOf { LightMoneyColors }

/** Shorthand: `moneyColors.expense` inside any composable under [FinEstTheme]. */
val moneyColors: MoneyColors
    @Composable @ReadOnlyComposable get() = LocalMoneyColors.current
