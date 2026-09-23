package com.example

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.AmoledTrueBlackColorScheme
import com.example.ui.theme.amoledTrueBlackColorScheme
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeTest {

    @Test
    fun testAmoledTrueBlackColorSchemeEnforcesPureBlackBackground() {
        val scheme = amoledTrueBlackColorScheme()

        // Background must be pure true black (#000000)
        assertEquals(AmoledBlack, scheme.background)
        assertEquals(Color(0xFF000000), scheme.background)

        // Surface must be pure true black (#000000)
        assertEquals(AmoledBlack, scheme.surface)
        assertEquals(Color(0xFF000000), scheme.surface)

        // Lowest container and dim surfaces must also be pure true black
        assertEquals(AmoledBlack, scheme.surfaceContainerLowest)
        assertEquals(AmoledBlack, scheme.surfaceDim)

        // surfaceTint must be transparent so elevation does not wash out OLED black
        assertEquals(Color.Transparent, scheme.surfaceTint)
    }

    @Test
    fun testSchemeDirectPropertyAccess() {
        assertEquals(Color(0xFF000000), AmoledTrueBlackColorScheme.background)
        assertEquals(Color(0xFF000000), AmoledTrueBlackColorScheme.surface)
    }
}
