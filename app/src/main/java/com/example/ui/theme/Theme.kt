package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Custom Material 3 ColorScheme configured specifically for pure black (#000000)
 * AMOLED displays with high-contrast accent colors.
 *
 * Key Design Principles:
 * 1. Pure Black Background & Surface (#000000): Pixels are completely powered off
 *    on OLED/AMOLED screens, maximizing battery conservation and contrast ratio.
 * 2. High-Contrast Accents: Primary CrimsonAccent (#FF2A5F), Secondary NeonCyan (#00F0FF),
 *    and Tertiary NeonEmerald (#00FF87) provide razor-sharp clarity against #000000.
 * 3. Transparent Surface Tint: Setting [surfaceTint] to [Color.Transparent] completely
 *    disables Material 3 tonal elevation overlays, preventing dark surfaces from
 *    becoming washed-out gray.
 * 4. Micro-Tiered Container Hierarchy: Surface containers scale subtly from #000000
 *    up to #1C1C22 to maintain depth while preserving the deep ink-black aesthetic.
 */
val AmoledColorScheme: ColorScheme = darkColorScheme(
    // High-Contrast Primary Accents (Crimson Accent)
    primary = CrimsonAccent,
    onPrimary = AmoledBlack,
    primaryContainer = CrimsonDark,
    onPrimaryContainer = Color(0xFFFFD9E2),
    inversePrimary = CrimsonLight,

    // High-Contrast Secondary Accents (Neon Cyber Cyan)
    secondary = NeonCyan,
    onSecondary = AmoledBlack,
    secondaryContainer = Color(0xFF00363D),
    onSecondaryContainer = Color(0xFF97F0FF),

    // High-Contrast Tertiary Accents (Neon Emerald Green)
    tertiary = NeonEmerald,
    onTertiary = AmoledBlack,
    tertiaryContainer = Color(0xFF003915),
    onTertiaryContainer = Color(0xFF86EFAC),

    // Enforced Pure True Black (#000000) Background & Surfaces
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
 * Backward-compatible alias for the AMOLED color scheme.
 */
val AmoledTrueBlackColorScheme: ColorScheme = AmoledColorScheme

/**
 * Convenience accessors for the AMOLED ColorScheme.
 */
fun amoledColorScheme(): ColorScheme = AmoledColorScheme
fun amoledTrueBlackColorScheme(): ColorScheme = AmoledColorScheme

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Pure AMOLED aesthetic: always enforce the true black color scheme
    MaterialTheme(
        colorScheme = AmoledColorScheme,
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

