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
}
