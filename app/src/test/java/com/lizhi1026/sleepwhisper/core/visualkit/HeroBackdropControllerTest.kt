package com.lizhi1026.sleepwhisper.core.visualkit

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class HeroBackdropControllerTest {

    @Test fun `blendAmbient with null returns base unchanged`() {
        val base = Color(0xFF07101F)
        assertEquals(base, blendAmbient(base, ambient = null, fraction = 0.5f))
    }

    @Test fun `blendAmbient at fraction 0 returns base`() {
        val base = Color(0xFF07101F)
        val ambient = Color(0xFFFFB088)
        assertEquals(base, blendAmbient(base, ambient, fraction = 0f))
    }

    @Test fun `blendAmbient at fraction 1 returns ambient`() {
        val base = Color(0xFF07101F)
        val ambient = Color(0xFFFFB088)
        assertEquals(ambient, blendAmbient(base, ambient, fraction = 1f))
    }

    @Test fun `blendAmbient at fraction 0_5 averages channels`() {
        val a = Color(0xFF000000)
        val b = Color(0xFFFFFFFF)
        val out = blendAmbient(a, b, fraction = 0.5f)
        assertEquals(0.5f, out.red, 0.005f)
        assertEquals(0.5f, out.green, 0.005f)
        assertEquals(0.5f, out.blue, 0.005f)
    }
}
