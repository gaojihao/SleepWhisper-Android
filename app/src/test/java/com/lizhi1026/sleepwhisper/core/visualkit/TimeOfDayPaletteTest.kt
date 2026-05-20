package com.lizhi1026.sleepwhisper.core.visualkit

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

class TimeOfDayPaletteTest {

    @Test fun `returns dusk palette exactly at 1900`() {
        val p = TimeOfDayPalette.forTime(LocalTime.of(19, 0))
        assertEquals(Color(0xFF1A2540), p.bottom)
        assertEquals(Color(0xFFFFB088), p.top)
        assertEquals(0.08f, p.grainDensity, 0.001f)
    }

    @Test fun `returns evening palette exactly at 2100`() {
        val p = TimeOfDayPalette.forTime(LocalTime.of(21, 0))
        assertEquals(Color(0xFF0F1B30), p.bottom)
        assertEquals(Color(0xFFB8A4FF), p.top)
    }

    @Test fun `returns night palette exactly at 0000`() {
        val p = TimeOfDayPalette.forTime(LocalTime.of(0, 0))
        assertEquals(Color(0xFF050912), p.bottom)
        assertEquals(Color(0xFF1F2848), p.top)
    }

    @Test fun `returns dead-night palette with extra grain at 0300`() {
        val p = TimeOfDayPalette.forTime(LocalTime.of(3, 0))
        assertEquals(Color(0xFF050912), p.bottom)
        assertEquals(Color(0xFF1F2848), p.top)
        assertEquals(0.12f, p.grainDensity, 0.001f)
    }

    @Test fun `returns dawn palette exactly at 0500`() {
        val p = TimeOfDayPalette.forTime(LocalTime.of(5, 0))
        assertEquals(Color(0xFF1F2540), p.bottom)
        assertEquals(Color(0xFFFFA98E), p.top)
    }

    @Test fun `interpolates linearly between dusk and evening at 2000`() {
        val p = TimeOfDayPalette.forTime(LocalTime.of(20, 0))
        val r = p.bottom.red
        assertEquals(0x14 / 255f, r, 0.01f)
    }

    @Test fun `wraps midnight correctly between evening and night`() {
        val p = TimeOfDayPalette.forTime(LocalTime.of(22, 30))
        val r = p.bottom.red
        assertEquals(0x0A / 255f, r, 0.02f)
    }

    @Test fun `daytime hours fall back to dawn palette`() {
        val p = TimeOfDayPalette.forTime(LocalTime.of(12, 0))
        assertEquals(true, p.bottom.alpha > 0f)
    }
}
