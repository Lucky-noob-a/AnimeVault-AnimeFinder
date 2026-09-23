package com.example

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.NeonAccent
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class NeonProgressIndicatorTest {

    @Test
    fun testNeonAccentColorIsHighContrastForAmoled() {
        // NeonCyan must be vivid cyber cyan (#00F0FF)
        assertEquals(Color(0xFF00F0FF), NeonCyan)
        assertEquals(NeonCyan, NeonAccent)

        // Must contrast significantly with AmoledBlack (#000000)
        assertNotEquals(AmoledBlack, NeonCyan)
        assertNotEquals(AmoledBlack, NeonEmerald)

        // NeonEmerald must be #00FF87
        assertEquals(Color(0xFF00FF87), NeonEmerald)
    }
}
