package com.pkoder.finest.presentation.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * The Stitch reference designs ship a single dark token set, so the app is dark-only and no longer
 * takes Material 3's dynamic colour.
 *
 * Dynamic colour would repaint mint with whatever the device wallpaper suggests, which is the one
 * thing "Charcoal & Mint Premium" is built around — the accent *is* the brand here.
 */
private val CharcoalMintScheme = darkColorScheme(
    primary = Charcoal.primary,
    onPrimary = Charcoal.onPrimary,
    primaryContainer = Charcoal.primaryContainer,
    onPrimaryContainer = Charcoal.onPrimaryContainer,
    inversePrimary = Charcoal.inversePrimary,

    secondary = Charcoal.secondary,
    onSecondary = Charcoal.onSecondary,
    secondaryContainer = Charcoal.secondaryContainer,
    onSecondaryContainer = Charcoal.onSecondaryContainer,

    tertiary = Charcoal.tertiary,
    onTertiary = Charcoal.onTertiary,
    tertiaryContainer = Charcoal.tertiaryContainer,
    onTertiaryContainer = Charcoal.onTertiaryContainer,

    background = Charcoal.surface,
    onBackground = Charcoal.onSurface,
    surface = Charcoal.surface,
    onSurface = Charcoal.onSurface,
    surfaceVariant = Charcoal.surfaceVariant,
    onSurfaceVariant = Charcoal.onSurfaceVariant,
    surfaceTint = Charcoal.surfaceTint,
    inverseSurface = Charcoal.inverseSurface,
    inverseOnSurface = Charcoal.inverseOnSurface,

    surfaceDim = Charcoal.surfaceDim,
    surfaceBright = Charcoal.surfaceBright,
    surfaceContainerLowest = Charcoal.surfaceContainerLowest,
    surfaceContainerLow = Charcoal.surfaceContainerLow,
    surfaceContainer = Charcoal.surfaceContainer,
    surfaceContainerHigh = Charcoal.surfaceContainerHigh,
    surfaceContainerHighest = Charcoal.surfaceContainerHighest,

    outline = Charcoal.outline,
    outlineVariant = Charcoal.outlineVariant,

    error = Charcoal.error,
    onError = Charcoal.onError,
    errorContainer = Charcoal.errorContainer,
    onErrorContainer = Charcoal.onErrorContainer,

    scrim = Charcoal.surfaceContainerLowest
)

@Composable
fun FinEstTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalMoneyColors provides CharcoalMoneyColors) {
        MaterialTheme(
            colorScheme = CharcoalMintScheme,
            typography = Typography,
            shapes = FinEstShapes,
            content = content
        )
    }
}
