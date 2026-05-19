package com.lizhi1026.sleepwhisper.core.visualkit

import androidx.compose.ui.graphics.Color

/**
 * Color tokens — direct port of iOS Core/Theme/ColorTokens.swift.
 * Three schemes: DAY (cool white + warm orange accent), DARK (misty blue dark), NIGHT (red-light to reduce blue exposure).
 */
object SWColor {

    fun primary(s: SWScheme): Color = when (s) {
        SWScheme.DAY   -> Color(1.00f, 0.54f, 0.40f)
        SWScheme.DARK  -> Color(0.56f, 0.66f, 0.85f)
        SWScheme.NIGHT -> Color(0.70f, 0.42f, 0.42f)
    }

    fun primaryHover(s: SWScheme): Color = when (s) {
        SWScheme.DAY   -> Color(0.96f, 0.47f, 0.33f)
        SWScheme.DARK  -> Color(0.64f, 0.72f, 0.88f)
        SWScheme.NIGHT -> Color(0.78f, 0.50f, 0.50f)
    }

    fun primaryActive(s: SWScheme): Color = when (s) {
        SWScheme.DAY   -> Color(0.88f, 0.40f, 0.26f)
        SWScheme.DARK  -> Color(0.73f, 0.78f, 0.91f)
        SWScheme.NIGHT -> Color(0.85f, 0.55f, 0.55f)
    }

    fun accent(s: SWScheme): Color = when (s) {
        SWScheme.DAY   -> Color(1.00f, 0.54f, 0.40f)
        SWScheme.DARK  -> Color(0.88f, 0.72f, 0.63f)
        SWScheme.NIGHT -> Color(0.80f, 0.45f, 0.45f)
    }

    fun surface(s: SWScheme): Color = when (s) {
        SWScheme.DAY   -> Color(0.984f, 0.984f, 0.988f)
        SWScheme.DARK  -> Color(0.06f, 0.07f, 0.08f)
        SWScheme.NIGHT -> Color(0.04f, 0.02f, 0.02f)
    }

    fun surfaceElevated(s: SWScheme): Color = when (s) {
        SWScheme.DAY   -> Color.White
        SWScheme.DARK  -> Color(0.10f, 0.11f, 0.14f)
        SWScheme.NIGHT -> Color(0.10f, 0.04f, 0.04f)
    }

    fun surfaceSunken(s: SWScheme): Color = when (s) {
        SWScheme.DAY   -> Color(0.949f, 0.953f, 0.961f)
        SWScheme.DARK  -> Color(0.03f, 0.03f, 0.04f)
        SWScheme.NIGHT -> Color(0.02f, 0.01f, 0.01f)
    }

    fun border(s: SWScheme): Color = when (s) {
        SWScheme.DAY   -> Color(0.910f, 0.918f, 0.929f)
        SWScheme.DARK  -> Color(0.16f, 0.18f, 0.23f)
        SWScheme.NIGHT -> Color(0.16f, 0.07f, 0.07f)
    }

    fun textPrimary(s: SWScheme): Color = when (s) {
        SWScheme.DAY   -> Color(0.055f, 0.067f, 0.086f)
        SWScheme.DARK  -> Color(0.96f, 0.96f, 0.94f)
        SWScheme.NIGHT -> Color(0.79f, 0.56f, 0.56f)
    }

    fun textSecondary(s: SWScheme): Color = when (s) {
        SWScheme.DAY   -> Color(0.373f, 0.392f, 0.439f)
        SWScheme.DARK  -> Color(0.74f, 0.75f, 0.78f)
        SWScheme.NIGHT -> Color(0.60f, 0.40f, 0.40f)
    }

    fun textTertiary(s: SWScheme): Color = when (s) {
        SWScheme.DAY   -> Color(0.612f, 0.639f, 0.686f)
        SWScheme.DARK  -> Color(0.56f, 0.57f, 0.60f)
        SWScheme.NIGHT -> Color(0.45f, 0.28f, 0.28f)
    }

    fun textInverse(s: SWScheme): Color = when (s) {
        SWScheme.DAY   -> Color.White
        SWScheme.DARK  -> Color(0.10f, 0.11f, 0.14f)
        SWScheme.NIGHT -> Color(0.10f, 0.04f, 0.04f)
    }

    fun success(s: SWScheme): Color = when (s) {
        SWScheme.DAY   -> Color(0.20f, 0.71f, 0.51f)
        SWScheme.DARK  -> Color(0.58f, 0.76f, 0.55f)
        SWScheme.NIGHT -> Color(0.55f, 0.32f, 0.32f)
    }

    fun warning(s: SWScheme): Color = when (s) {
        SWScheme.DAY   -> Color(0.96f, 0.62f, 0.07f)
        SWScheme.DARK  -> Color(0.89f, 0.72f, 0.45f)
        SWScheme.NIGHT -> Color(0.75f, 0.42f, 0.42f)
    }

    fun danger(s: SWScheme): Color = when (s) {
        SWScheme.DAY   -> Color(0.94f, 0.27f, 0.27f)
        SWScheme.DARK  -> Color(0.85f, 0.54f, 0.54f)
        SWScheme.NIGHT -> Color(0.95f, 0.42f, 0.42f)
    }

    fun softPeach(s: SWScheme): Color = when (s) {
        SWScheme.DAY   -> Color(1.000f, 0.898f, 0.851f)
        SWScheme.DARK  -> Color(0.40f, 0.28f, 0.24f, alpha = 0.5f)
        SWScheme.NIGHT -> Color(0.40f, 0.16f, 0.16f, alpha = 0.5f)
    }

    fun softMint(s: SWScheme): Color = when (s) {
        SWScheme.DAY   -> Color(0.859f, 0.961f, 0.898f)
        SWScheme.DARK  -> Color(0.24f, 0.40f, 0.32f, alpha = 0.5f)
        SWScheme.NIGHT -> Color(0.40f, 0.20f, 0.20f, alpha = 0.5f)
    }

    fun softLilac(s: SWScheme): Color = when (s) {
        SWScheme.DAY   -> Color(0.918f, 0.890f, 1.000f)
        SWScheme.DARK  -> Color(0.32f, 0.28f, 0.48f, alpha = 0.5f)
        SWScheme.NIGHT -> Color(0.40f, 0.16f, 0.16f, alpha = 0.5f)
    }
}
