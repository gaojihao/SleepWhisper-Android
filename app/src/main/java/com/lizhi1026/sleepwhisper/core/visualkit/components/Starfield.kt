package com.lizhi1026.sleepwhisper.core.visualkit.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.lizhi1026.sleepwhisper.core.visualkit.LocalReduceMotion
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWScheme
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

private data class Star(
    val nx: Float,      // normalized x in [0..1]
    val ny: Float,      // normalized y in [0..1]
    val radius: Float,  // px-independent, in dp at draw time
    val phase: Float,
    val periodMs: Float
)

/**
 * Decorative star field — port of iOS Starfield.swift.
 * Renders only in DARK / NIGHT schemes; DAY returns an empty Box.
 */
@Composable
fun Starfield(
    modifier: Modifier = Modifier,
    density: Int = 60
) {
    val scheme = LocalSWScheme.current
    if (scheme == SWScheme.DAY) {
        Box(modifier = modifier)
        return
    }
    val stars = remember(density) {
        List(density) {
            Star(
                nx = Random.nextFloat(),
                ny = Random.nextFloat(),
                radius = 0.4f + Random.nextFloat() * 1.6f,
                phase = Random.nextFloat() * (Math.PI * 2).toFloat(),
                periodMs = 4000f + Random.nextFloat() * 2000f
            )
        }
    }

    val t = rememberInfiniteTransition(label = "starfield")
    val tick by t.animateFloat(
        initialValue = 0f,
        targetValue = if (LocalReduceMotion.current) 0f else 6000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "tick"
    )

    Canvas(modifier = modifier.fillMaxSize().clearAndSetSemantics { }) {
        val w = size.width
        val h = size.height
        stars.forEach { star ->
            val twoPi = (Math.PI * 2).toFloat()
            val sinVal = sin(tick / star.periodMs * twoPi + star.phase)
            val alpha = 0.25f + 0.65f * abs(sinVal)
            drawCircle(
                color = Color.White.copy(alpha = alpha),
                radius = star.radius,
                center = Offset(star.nx * w, star.ny * h)
            )
        }
    }
}
