package com.pkoder.finest.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Work
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.pkoder.finest.presentation.ui.theme.Charcoal
import com.pkoder.finest.presentation.ui.theme.moneyColors

/**
 * The reference designs give every row a category-specific glyph on a tinted disc rather than a
 * generic up/down arrow, which is what makes a list scannable at a glance.
 *
 * Keys are the real vocabulary from `TransactionOptions`, matched case-insensitively so a
 * parser-produced or hand-typed value still lands somewhere sensible. Tones are assigned by hand
 * from the brand accents — deterministic, so a category never changes colour between screens.
 */
data class CategoryVisual(val icon: ImageVector, val tint: Color)

private val expenseVisuals: Map<String, CategoryVisual> = mapOf(
    "housing" to CategoryVisual(Icons.Default.Home, Charcoal.primaryFixedDim),
    "food" to CategoryVisual(Icons.Default.Restaurant, Charcoal.tertiaryFixed),
    "transport" to CategoryVisual(Icons.Default.DirectionsCar, Charcoal.tertiaryFixedDim),
    "utilities" to CategoryVisual(Icons.Default.Bolt, Charcoal.primary),
    "dependents" to CategoryVisual(Icons.Default.Groups, Charcoal.secondary),
    "entertainment" to CategoryVisual(Icons.Default.Movie, Color(0xFFB9A5D6)),
    "health" to CategoryVisual(Icons.Default.MedicalServices, Charcoal.error),
    "finance" to CategoryVisual(Icons.Default.AccountBalance, Color(0xFF9BB8CE)),
    "shopping" to CategoryVisual(Icons.Default.ShoppingBag, Charcoal.secondary)
)

private val incomeVisuals: Map<String, CategoryVisual> = mapOf(
    "salary" to CategoryVisual(Icons.Default.Payments, Charcoal.primary),
    "freelance" to CategoryVisual(Icons.Default.Work, Charcoal.primaryFixedDim),
    "gift" to CategoryVisual(Icons.Default.CardGiftcard, Charcoal.secondary),
    "interest" to CategoryVisual(Icons.AutoMirrored.Filled.ShowChart, Charcoal.tertiaryFixed),
    "refund" to CategoryVisual(Icons.Default.Replay, Color(0xFF8FC5A9)),
    "other" to CategoryVisual(Icons.Default.Savings, Charcoal.primaryFixedDim)
)

/**
 * Icon and tone for a transaction.
 *
 * Anything unrecognised falls back to the money colour for its direction, so an unmapped category
 * still reads as expense or income rather than as a random hue.
 */
@Composable
fun categoryVisual(label: String, isExpense: Boolean): CategoryVisual {
    val key = label.trim().lowercase()
    val table = if (isExpense) expenseVisuals else incomeVisuals
    return table[key] ?: CategoryVisual(
        icon = if (isExpense) Icons.AutoMirrored.Filled.ReceiptLong else Icons.Default.Payments,
        tint = if (isExpense) moneyColors.expense else moneyColors.income
    )
}
