package com.lizhi1026.sleepwhisper.core.visualkit

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class SWColorTest {

    @Test fun `NIGHT textPrimary preserved`() {
        // Hard-won accessibility tuning — must remain at the exact value.
        assertEquals(Color(1.00f, 0.75f, 0.75f), SWColor.textPrimary(SWScheme.NIGHT))
    }

    @Test fun `NIGHT surface preserved`() {
        assertEquals(Color(0.04f, 0.02f, 0.02f), SWColor.surface(SWScheme.NIGHT))
    }

    @Test fun `DAY surface is paper warm`() {
        // #F4F2EE → (0.957, 0.949, 0.933)
        val c = SWColor.surface(SWScheme.DAY)
        assertEquals(0.957f, c.red, 0.005f)
        assertEquals(0.949f, c.green, 0.005f)
        assertEquals(0.933f, c.blue, 0.005f)
    }

    @Test fun `DARK surface is deep night`() {
        // #07101F → (0.027, 0.063, 0.122)
        val c = SWColor.surface(SWScheme.DARK)
        assertEquals(0.027f, c.red, 0.005f)
        assertEquals(0.063f, c.green, 0.005f)
        assertEquals(0.122f, c.blue, 0.005f)
    }

    @Test fun `accentSecondary exists for DAY and DARK`() {
        // Starlight purple — DAY #9B85FF, DARK #B8A4FF
        val day = SWColor.accentSecondary(SWScheme.DAY)
        assertEquals(0.608f, day.red, 0.005f)
        val dark = SWColor.accentSecondary(SWScheme.DARK)
        assertEquals(0.722f, dark.red, 0.005f)
    }
}
