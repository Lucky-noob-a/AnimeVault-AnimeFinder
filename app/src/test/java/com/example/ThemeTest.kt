package com.example

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.AmoledColorScheme
import com.example.ui.theme.AmoledTrueBlackColorScheme
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.amoledColorScheme
import com.example.ui.theme.amoledTrueBlackColorScheme
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeTest {

    @Test
    fun testAmoledColorSchemeEnforcesPureBlackBackground() {
        val scheme = amoledColorScheme()

        // Background must be pure true black (#000000)
        assertEquals(AmoledBlack, scheme.background)
        assertEquals(Color(0xFF000000), scheme.background)

        // Surface must be pure true black (#000000)
        assertEquals(AmoledBlack, scheme.surface)
        assertEquals(Color(0xFF000000), scheme.surface)

        // Lowest container, surfaceDim, and scrim must also be pure true black
        assertEquals(AmoledBlack, scheme.surfaceContainerLowest)
        assertEquals(AmoledBlack, scheme.surfaceDim)
        assertEquals(AmoledBlack, scheme.scrim)

        // surfaceTint must be transparent so elevation does not wash out OLED black
        assertEquals(Color.Transparent, scheme.surfaceTint)
    }

    @Test
    fun testAmoledColorSchemeHighContrastAccents() {
        val scheme = amoledColorScheme()

        // High contrast primary accent (Crimson) with true black on-primary
        assertEquals(CrimsonAccent, scheme.primary)
        assertEquals(AmoledBlack, scheme.onPrimary)

        // High contrast secondary accent (Neon Cyber Cyan) with true black on-secondary
        assertEquals(NeonCyan, scheme.secondary)
        assertEquals(AmoledBlack, scheme.onSecondary)

        // High contrast tertiary accent (Neon Emerald Green) with true black on-tertiary
        assertEquals(NeonEmerald, scheme.tertiary)
        assertEquals(AmoledBlack, scheme.onTertiary)

        // High contrast text on dark background and surfaces
        assertEquals(TextPrimary, scheme.onBackground)
        assertEquals(TextPrimary, scheme.onSurface)
    }

    @Test
    fun testSchemeDirectPropertyAccess() {
        assertEquals(Color(0xFF000000), AmoledColorScheme.background)
        assertEquals(Color(0xFF000000), AmoledColorScheme.surface)
        assertEquals(Color(0xFF000000), AmoledTrueBlackColorScheme.background)
        assertEquals(Color(0xFF000000), AmoledTrueBlackColorScheme.surface)
        assertEquals(amoledColorScheme(), amoledTrueBlackColorScheme())
    }
}

