package com.pkoder.finest.presentation.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * "Charcoal & Mint Premium" — the token set from the Stitch reference designs
 * (`D:\Projects\Finest\Design\*\DESIGN.md`).
 *
 * The palette only exists in dark: a near-black canvas with tonal charcoal layers, mint as the one
 * loud accent, and soft pink / muted yellow used sparingly. Names mirror the Material 3 roles so
 * [FinEstTheme]'s scheme is a straight mapping and nothing has to be reinterpreted here.
 */
object Charcoal {
    // Canvas and tonal layers. Depth comes from these, not from elevation shadows.
    val surface = Color(0xFF131313)
    val surfaceDim = Color(0xFF131313)
    val surfaceBright = Color(0xFF393939)
    val surfaceContainerLowest = Color(0xFF0E0E0E)
    val surfaceContainerLow = Color(0xFF1C1B1B)
    val surfaceContainer = Color(0xFF201F1F)
    val surfaceContainerHigh = Color(0xFF2A2A2A)
    val surfaceContainerHighest = Color(0xFF353534)
    val surfaceVariant = Color(0xFF353534)

    val onSurface = Color(0xFFE5E2E1)
    val onSurfaceVariant = Color(0xFFC0C8C8)
    val inverseSurface = Color(0xFFE5E2E1)
    val inverseOnSurface = Color(0xFF313030)

    val outline = Color(0xFF8A9292)
    val outlineVariant = Color(0xFF404848)
    val surfaceTint = Color(0xFF9ECFD1)

    // Mint — primary actions, brand marks, income.
    val primary = Color(0xFFC4F7F9)
    val onPrimary = Color(0xFF003739)
    val primaryContainer = Color(0xFFA8DADC)
    val onPrimaryContainer = Color(0xFF306163)
    val inversePrimary = Color(0xFF356668)
    val primaryFixed = Color(0xFFB9ECEE)
    val primaryFixedDim = Color(0xFF9ECFD1)
    val onPrimaryFixed = Color(0xFF002021)
    val onPrimaryFixedVariant = Color(0xFF1A4E50)

    // Soft pink — expenses and negative trends.
    val secondary = Color(0xFFE7BBC5)
    val onSecondary = Color(0xFF442830)
    val secondaryContainer = Color(0xFF62434B)
    val onSecondaryContainer = Color(0xFFDBB1BB)
    val secondaryFixed = Color(0xFFFFD9E1)
    val secondaryFixedDim = Color(0xFFE7BBC5)
    val onSecondaryFixed = Color(0xFF2D141B)
    val onSecondaryFixedVariant = Color(0xFF5D3E46)

    // Muted yellow — informational chips and the third chart tone.
    val tertiary = Color(0xFFFFEBC0)
    val onTertiary = Color(0xFF3D2E00)
    val tertiaryContainer = Color(0xFFEDCE7E)
    val onTertiaryContainer = Color(0xFF6D5712)
    val tertiaryFixed = Color(0xFFFFDF91)
    val tertiaryFixedDim = Color(0xFFE2C375)
    val onTertiaryFixed = Color(0xFF241A00)
    val onTertiaryFixedVariant = Color(0xFF584400)

    val error = Color(0xFFFFB4AB)
    val onError = Color(0xFF690005)
    val errorContainer = Color(0xFF93000A)
    val onErrorContainer = Color(0xFFFFDAD6)
}
