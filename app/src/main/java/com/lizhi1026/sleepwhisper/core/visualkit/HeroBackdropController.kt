package com.lizhi1026.sleepwhisper.core.visualkit

import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color

/**
 * Holds the current "ambient color" the app should tint backdrops with.
 * Sources, in priority order (set by future phases):
 *   1. Active audio preset's mid-aura (Player Hero #3)
 *   2. Time-of-day hint (Home / Sleeping)
 *   3. null — backdrops use their default Aurora palette
 *
 * `setAmbient(null)` clears, allowing the default to surface again.
 */
@Stable
class HeroBackdropController {
    private val _ambient = mutableStateOf<Color?>(null)
    val ambient: Color? get() = _ambient.value

    fun setAmbient(color: Color?) { _ambient.value = color }
}

val LocalHeroBackdropController = compositionLocalOf { HeroBackdropController() }

/**
 * Blend [base] toward [ambient] by [fraction] in linear RGB.
 * `ambient = null` returns base unchanged regardless of fraction.
 * `fraction` is clamped to [0, 1].
 */
fun blendAmbient(base: Color, ambient: Color?, fraction: Float): Color {
    if (ambient == null) return base
    val f = fraction.coerceIn(0f, 1f)
    return base.copy(
        red   = base.red   * (1f - f) + ambient.red   * f,
        green = base.green * (1f - f) + ambient.green * f,
        blue  = base.blue  * (1f - f) + ambient.blue  * f,
        alpha = base.alpha,
    )
}
