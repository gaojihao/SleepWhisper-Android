package com.lizhi1026.sleepwhisper.core.visualkit

import androidx.compose.ui.graphics.Color

/**
 * Aurora color tokens. Public API unchanged from the iOS port; values refreshed
 * for the Aurora redesign (Phase 0). NIGHT scheme values are preserved — their
 * contrast against the NIGHT surface was tuned to >= 4.5:1 and is not negotiable.
 */
object SWColor {

    fun primary(s: SWScheme): Color = when (s) {
        SWScheme.DAY   -> Color(0xFFFF8A5E)  // dusk orange
        SWScheme.DARK  -> Color(0xFFFFB088)  // dusk orange
        SWScheme.NIGHT -> Color(0.70f, 0.42f, 0.42f) // preserved
    }

    fun primaryHover(s: SWScheme): Color = when (s) {
        SWScheme.DAY   -> Color(0xFFFF7A4A)
        SWScheme.DARK  -> Color(0xFFFFC5A4)
        SWScheme.NIGHT -> Color(0.78f, 0.50f, 0.50f)
    }

    fun primaryActive(s: SWScheme): Color = when (s) {
        SWScheme.DAY   -> Color(0xFFE87646)
        SWScheme.DARK  -> Color(0xFFFFA378)
        SWScheme.NIGHT -> Color(0.85f, 0.55f, 0.55f)
    }

    fun accent(s: SWScheme): Color = when (s) {
        SWScheme.DAY   -> Color(0xFFFF8A5E)
        SWScheme.DARK  -> Color(0xFFFFB088)
        SWScheme.NIGHT -> Color(0.80f, 0.45f, 0.45f)
    }

    /** Starlight purple — NEW. The second hero accent. NIGHT scheme uses primary instead. */
    fun accentSecondary(s: SWScheme): Color = when (s) {
        SWScheme.DAY   -> Color(0xFF9B85FF)
        SWScheme.DARK  -> Color(0xFFB8A4FF)
        SWScheme.NIGHT -> Color(0.70f, 0.42f, 0.42f) // falls back to primary tone
    }

    fun surface(s: SWScheme): Color = when (s) {
        SWScheme.DAY   -> Color(0xFFF4F2EE) // paper warm
        SWScheme.DARK  -> Color(0xFF07101F) // deep night
        SWScheme.NIGHT -> Color(0.04f, 0.02f, 0.02f)
    }

    fun surfaceElevated(s: SWScheme): Color = when (s) {
        SWScheme.DAY   -> Color.White
        SWScheme.DARK  -> Color(0xFF0F1A2D)
        SWScheme.NIGHT -> Color(0.10f, 0.04f, 0.04f)
    }

    fun surfaceSunken(s: SWScheme): Color = when (s) {
        SWScheme.DAY   -> Color(0xFFECEAE5)
        SWScheme.DARK  -> Color(0xFF050912)
        SWScheme.NIGHT -> Color(0.02f, 0.01f, 0.01f)
    }

    fun border(s: SWScheme): Color = when (s) {
        SWScheme.DAY   -> Color(0xFFE2DDD2)
        SWScheme.DARK  -> Color(0xFF1A2438)
        SWScheme.NIGHT -> Color(0.16f, 0.07f, 0.07f)
    }

    fun textPrimary(s: SWScheme): Color = when (s) {
        SWScheme.DAY   -> Color(0xFF0A1428)
        SWScheme.DARK  -> Color(0xFFECF2F8) // moonlit silver
        SWScheme.NIGHT -> Color(1.00f, 0.75f, 0.75f) // >= 4.5:1 — DO NOT CHANGE
    }

    fun textSecondary(s: SWScheme): Color = when (s) {
        SWScheme.DAY   -> Color(0xFF4A5573)
        SWScheme.DARK  -> Color(0xFF8A93A8)
        SWScheme.NIGHT -> Color(0.85f, 0.55f, 0.55f)
    }

    fun textTertiary(s: SWScheme): Color = when (s) {
        SWScheme.DAY   -> Color(0xFF7A859C)
        SWScheme.DARK  -> Color(0xFF5C667A)
        SWScheme.NIGHT -> Color(0.70f, 0.42f, 0.42f)
    }

    fun textInverse(s: SWScheme): Color = when (s) {
        SWScheme.DAY   -> Color.White
        SWScheme.DARK  -> Color(0xFF0F1A2D)
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
