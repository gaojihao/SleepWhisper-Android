package com.lizhi1026.sleepwhisper.core.visualkit.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lizhi1026.sleepwhisper.core.visualkit.LocalReduceMotion
import kotlin.math.PI
import kotlin.math.sin

/**
 * Animated liquid-fill progress ring — port of iOS LiquidProgressRing inside SleepingView.swift.
 * Outer track (Stroke) plus an interior sin-wave fill whose level rises with [progress] (0..1).
 */
@Composable
fun LiquidProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color,
    radius: Dp = 80.dp
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val reduceMotion = LocalReduceMotion.current
    val phaseTransition = rememberInfiniteTransition(label = "liquid-phase")
    val phase by phaseTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (reduceMotion) 0f else (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(
        modifier = modifier
            .size(radius * 2)
            .clip(CircleShape)
    ) {
        val r = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        // Outer track.
        drawCircle(
            color = color.copy(alpha = 0.3f),
            radius = r - 2.dp.toPx(),
            center = center,
            style = Stroke(width = 4.dp.toPx())
        )

        // Liquid fill: a horizontal sin-wave path bounded by the circle.
        val waterLevel = size.height * (1f - clampedProgress)
        val amplitude = 4.dp.toPx()
        val path = Path().apply {
            moveTo(0f, size.height)
            lineTo(0f, waterLevel)
            val steps = 32
            for (i in 0..steps) {
                val x = size.width * i / steps
                val y = waterLevel + amplitude * sin(phase + i * 2f * PI.toFloat() / steps)
                lineTo(x, y)
            }
            lineTo(size.width, size.height)
            close()
        }
        drawPath(path, color = color.copy(alpha = 0.55f))
    }
}
