package com.lizhi1026.sleepwhisper.core.visualkit

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Gradient tokens — direct port of iOS Core/VisualKit/Gradients.swift.
 * Returns Brush.linearGradient with explicit start/end points (Float.POSITIVE_INFINITY = full extent).
 */
object SWGradient {

    private fun topToBottom(colors: List<Color>): Brush =
        Brush.linearGradient(colors = colors, start = Offset(0f, 0f), end = Offset(0f, Float.POSITIVE_INFINITY))

    private fun topToBottomStops(colorStops: Array<Pair<Float, Color>>): Brush =
        Brush.linearGradient(colorStops = colorStops, start = Offset(0f, 0f), end = Offset(0f, Float.POSITIVE_INFINITY))

    private fun topLeftToBottomRight(colors: List<Color>): Brush =
        Brush.linearGradient(colors = colors, start = Offset(0f, 0f), end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY))

    private fun topLeftToBottomRightStops(colorStops: Array<Pair<Float, Color>>): Brush =
        Brush.linearGradient(colorStops = colorStops, start = Offset(0f, 0f), end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY))

    fun surface(scheme: SWScheme): Brush = when (scheme) {
        SWScheme.DAY -> topToBottom(listOf(
            Color(0.984f, 0.984f, 0.988f),
            Color(0.972f, 0.976f, 0.984f)
        ))
        SWScheme.DARK -> topLeftToBottomRightStops(arrayOf(
            0.0f to Color(0.04f, 0.07f, 0.16f),
            0.5f to Color(0.11f, 0.11f, 0.23f),
            1.0f to Color(0.18f, 0.12f, 0.30f)
        ))
        SWScheme.NIGHT -> topLeftToBottomRightStops(arrayOf(
            0.0f to Color(0.10f, 0.02f, 0.02f),
            0.5f to Color(0.16f, 0.03f, 0.03f),
            1.0f to Color(0.24f, 0.06f, 0.06f)
        ))
    }

    fun primary(scheme: SWScheme): Brush = when (scheme) {
        SWScheme.DAY -> topToBottom(listOf(
            Color(1.00f, 0.60f, 0.46f), Color(0.96f, 0.47f, 0.33f)
        ))
        SWScheme.DARK -> topToBottom(listOf(
            Color(0.64f, 0.72f, 0.88f), Color(0.48f, 0.58f, 0.79f)
        ))
        SWScheme.NIGHT -> topToBottom(listOf(
            Color(0.78f, 0.45f, 0.45f), Color(0.55f, 0.28f, 0.28f)
        ))
    }

    fun accent(scheme: SWScheme): Brush = when (scheme) {
        SWScheme.DAY -> topLeftToBottomRight(listOf(
            Color(1.00f, 0.60f, 0.46f), Color(0.96f, 0.47f, 0.33f)
        ))
        SWScheme.DARK -> topLeftToBottomRight(listOf(
            Color(0.96f, 0.79f, 0.69f), Color(0.82f, 0.62f, 0.49f)
        ))
        SWScheme.NIGHT -> topLeftToBottomRight(listOf(
            Color(0.85f, 0.55f, 0.42f), Color(0.62f, 0.32f, 0.22f)
        ))
    }

    fun cardOverlay(scheme: SWScheme): Brush = when (scheme) {
        SWScheme.DAY -> topToBottom(listOf(Color.Transparent, Color.Transparent))
        SWScheme.DARK, SWScheme.NIGHT -> topToBottom(listOf(
            Color.White.copy(alpha = 0.18f), Color.White.copy(alpha = 0.04f)
        ))
    }

    /**
     * Aurora backdrop — multi-stop diagonal ribbon. The base layer used by
     * AuroraBackdrop. Three of these are stacked at different phases.
     */
    fun auroraBackdrop(scheme: SWScheme): Brush = when (scheme) {
        SWScheme.DAY -> topLeftToBottomRightStops(arrayOf(
            0.0f to Color(0xFFF4F2EE),
            0.5f to Color(0xFFFAE9DC),  // dawn whisper
            1.0f to Color(0xFFEFE4F2)   // lilac whisper
        ))
        SWScheme.DARK -> topLeftToBottomRightStops(arrayOf(
            0.0f to Color(0xFF07101F),
            0.4f to Color(0xFF0F1A2D),
            0.7f to Color(0xFF1A2240),
            1.0f to Color(0xFF2A1F3A)
        ))
        SWScheme.NIGHT -> topLeftToBottomRightStops(arrayOf(
            0.0f to Color(0.10f, 0.02f, 0.02f),
            0.5f to Color(0.16f, 0.03f, 0.03f),
            1.0f to Color(0.24f, 0.06f, 0.06f)
        ))
    }

    /**
     * Aurora glow — radial purple→orange. For hero CTAs and now-playing accents.
     * Returns a radial brush from the natural drawing center.
     */
    fun auroraGlow(scheme: SWScheme): Brush = when (scheme) {
        SWScheme.DAY -> Brush.radialGradient(
            colors = listOf(Color(0xFFFFB088), Color(0xFFB8A4FF).copy(alpha = 0f))
        )
        SWScheme.DARK -> Brush.radialGradient(
            colors = listOf(Color(0xFFB8A4FF), Color(0xFFFFB088).copy(alpha = 0f))
        )
        SWScheme.NIGHT -> Brush.radialGradient(
            colors = listOf(Color(0.78f, 0.45f, 0.45f), Color(0.55f, 0.28f, 0.28f, alpha = 0f))
        )
    }

    /**
     * Moon halo — soft white-silver radial used as an inner highlight on cards
     * on dark schemes. On DAY, returns near-transparent so callers can ignore.
     */
    fun moonHalo(scheme: SWScheme): Brush = when (scheme) {
        SWScheme.DAY -> Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.06f), Color.Transparent)
        )
        SWScheme.DARK -> Brush.radialGradient(
            colors = listOf(Color(0xFFECF2F8).copy(alpha = 0.18f), Color.Transparent)
        )
        SWScheme.NIGHT -> Brush.radialGradient(
            colors = listOf(Color(1.00f, 0.75f, 0.75f).copy(alpha = 0.08f), Color.Transparent)
        )
    }
}
