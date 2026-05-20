package com.lizhi1026.sleepwhisper.core.visualkit

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import java.time.LocalTime

/**
 * Sky palette pair — bottom of the gradient toward top, plus a film grain density that
 * picks up at dead-night for extra texture.
 */
data class SkyPalette(
    val bottom: Color,
    val top: Color,
    val grainDensity: Float = 0.08f
)

/**
 * Maps wall-clock time to a sky palette. Real 1:1 with the system clock — 30-minute
 * naps appear visually static, 8-hour overnights show the full dusk → night → dawn
 * progression.
 *
 * Stops are expressed in "extended hours": 19.0 = 19:00 today, 24.0 = 00:00 next day,
 * 29.0 = 05:00 next day, so linear interpolation between consecutive stops never has
 * to cross a 24h boundary.
 */
object TimeOfDayPalette {

    private data class Stop(val hour: Float, val palette: SkyPalette)

    private val STOPS = listOf(
        Stop(6.5f,  SkyPalette(Color(0xFF1F2540), Color(0xFFFFA98E))),                          // morning hand-back
        Stop(19.0f, SkyPalette(Color(0xFF1A2540), Color(0xFFFFB088))),                          // dusk
        Stop(21.0f, SkyPalette(Color(0xFF0F1B30), Color(0xFFB8A4FF))),                          // evening
        Stop(24.0f, SkyPalette(Color(0xFF050912), Color(0xFF1F2848))),                          // night (00:00)
        Stop(27.0f, SkyPalette(Color(0xFF050912), Color(0xFF1F2848), grainDensity = 0.12f)),    // dead-night (03:00)
        Stop(29.0f, SkyPalette(Color(0xFF1F2540), Color(0xFFFFA98E)))                           // dawn (05:00)
    )

    fun forTime(t: LocalTime): SkyPalette {
        val rawHour = t.hour + t.minute / 60f
        val effective = if (rawHour < 6.5f) rawHour + 24f else rawHour

        val upperIdx = STOPS.indexOfFirst { it.hour >= effective }
        if (upperIdx == -1) {
            return STOPS.last().palette
        }
        if (upperIdx == 0) {
            return STOPS.first().palette
        }
        val upper = STOPS[upperIdx]
        val lower = STOPS[upperIdx - 1]
        val fraction = (effective - lower.hour) / (upper.hour - lower.hour)

        return SkyPalette(
            bottom = lerp(lower.palette.bottom, upper.palette.bottom, fraction),
            top = lerp(lower.palette.top, upper.palette.top, fraction),
            grainDensity = lower.palette.grainDensity +
                (upper.palette.grainDensity - lower.palette.grainDensity) * fraction
        )
    }
}
