package com.lizhi1026.sleepwhisper.core.visualkit.components

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Three concentric expanding-fading rings — port of iOS PulseRing.swift.
 * Used as a halo behind the sleep CTA on Home and behind the wake button on Sleeping.
 */
@Composable
fun PulseRing(
    modifier: Modifier = Modifier,
    color: Color,
    radius: Dp = 80.dp
) {
    val t = rememberInfiniteTransition(label = "pulse")
    val cycle = 2400
    val phases = listOf(0, 800, 1600)
    val animatedFractions = phases.map { offset ->
        t.animateFloat(
            initialValue = 0.6f,
            targetValue = 1.3f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = cycle, easing = LinearOutSlowInEasing),
                repeatMode = RepeatMode.Restart,
                initialStartOffset = androidx.compose.animation.core.StartOffset(offset)
            ),
            label = "ring-$offset"
        )
    }
    Canvas(modifier = modifier.size(radius * 2)) {
        val baseR = radius.toPx()
        animatedFractions.forEach { state ->
            val scale = state.value
            val alpha = ((1.3f - scale) / (1.3f - 0.6f)).coerceIn(0f, 1f) * 0.45f
            drawCircle(
                color = color.copy(alpha = alpha),
                radius = baseR * scale,
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}
