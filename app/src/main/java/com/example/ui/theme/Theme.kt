package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Custom Material 3 ColorScheme that strictly enforces a true black (#000000)
 * background and base surfaces to complement the AMOLED interface design.
 *
 * Setting [surfaceTint] to [Color.Transparent] prevents Material 3 tonal elevation
 * overlays from lightening true black backgrounds into tinted dark grays.
 */
val AmoledTrueBlackColorScheme: ColorScheme = darkColorScheme(
    // Primary Accents
    primary = CrimsonAccent,
    onPrimary = AmoledBlack,
    primaryContainer = CrimsonDark,
    onPrimaryContainer = TextPrimary,
    inversePrimary = CrimsonLight,

    // Secondary Accents
    secondary = CrimsonLight,
    onSecondary = AmoledBlack,
    secondaryContainer = Color(0xFF2E0010),
    onSecondaryContainer = Color(0xFFFFD9E2),

    // Tertiary Accents
    tertiary = VerifiedGreen,
    onTertiary = AmoledBlack,
    tertiaryContainer = Color(0xFF003915),
    onTertiaryContainer = Color(0xFF86EFAC),

    // Enforced True Black (#000000) Background & Surfaces
    background = AmoledBlack,
    onBackground = TextPrimary,
    surface = AmoledBlack,
    onSurface = TextPrimary,
    surfaceVariant = AmoledSurfaceVariant,
    onSurfaceVariant = TextSecondary,

    // M3 Surface Container Hierarchy for AMOLED
    surfaceContainerLowest = AmoledBlack,
    surfaceContainerLow = AmoledSurface,
    surfaceContainer = AmoledContainer,
    surfaceContainerHigh = AmoledCard,
    surfaceContainerHighest = Color(0xFF1C1C22),
    surfaceDim = AmoledBlack,
    surfaceBright = AmoledSurfaceBright,

    // Transparent Surface Tint prevents elevation from graying out true black
    surfaceTint = Color.Transparent,

    // High-Contrast AMOLED Outlines & Borders
    outline = AmoledBorder,
    outlineVariant = AmoledOutlineVariant,

    // Inverted Surfaces
    inverseSurface = TextPrimary,
    inverseOnSurface = AmoledBlack,

    // Error States
    error = Color(0xFFFF4D4D),
    onError = AmoledBlack,
    errorContainer = Color(0xFF4A000A),
    onErrorContainer = Color(0xFFFFD8D8),

    scrim = AmoledBlack
)

/**
 * Convenience accessor for the true black AMOLED ColorScheme.
 */
fun amoledTrueBlackColorScheme(): ColorScheme = AmoledTrueBlackColorScheme

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Pure AMOLED aesthetic: always enforce the true black color scheme
    MaterialTheme(
        colorScheme = AmoledTrueBlackColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun AnimeVaultAmoledTheme(
    content: @Composable () -> Unit
) {
    MyApplicationTheme(
        darkTheme = true,
        dynamicColor = false,
        content = content
    )
}
