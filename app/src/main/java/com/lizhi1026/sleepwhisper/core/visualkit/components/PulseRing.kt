package com.lizhi1026.sleepwhisper.core.visualkit.components

import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lizhi1026.sleepwhisper.core.visualkit.LocalReduceMotion
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor

enum class PulseIntensity { SOFT, STRONG }

/**
 * Aurora 3-ring pulse. Each ring is a slightly different hue (orange → orange/purple → purple)
 * with offset phases, giving CTAs a *halo* rather than a single ring.
 *
 * Public signature is unchanged from the prior single-ring version so existing callers
 * (HomeScreen Sleep CTA) work without edits.
 */
@Composable
fun PulseRing(
    modifier: Modifier = Modifier,
    color: Color,
    radius: Dp = 100.dp,
    intensity: PulseIntensity = PulseIntensity.SOFT
) {
    val reduce = LocalReduceMotion.current
    val scheme = LocalSWScheme.current
    val secondary = SWColor.accentSecondary(scheme)

    if (reduce) {
        // Static halo at resting state — render the three rings without animation.
        Canvas(modifier = modifier.size(radius * 2)) {
            val maxAlpha = if (intensity == PulseIntensity.STRONG) 0.18f else 0.10f
            val center = Offset(size.width / 2, size.height / 2)
            drawCircle(color.copy(alpha = maxAlpha * 0.8f), radius = size.minDimension * 0.45f, center = center, style = Stroke(width = 2f))
            drawCircle(lerp(color, secondary, 0.5f).copy(alpha = maxAlpha * 0.6f), radius = size.minDimension * 0.55f, center = center, style = Stroke(width = 2f))
            drawCircle(secondary.copy(alpha = maxAlpha * 0.4f), radius = size.minDimension * 0.65f, center = center, style = Stroke(width = 2f))
        }
        return
    }

    val t = rememberInfiniteTransition(label = "pulse")
    val period = 2800
    val phase by t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(period, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )
    Canvas(modifier = modifier.size(radius * 2)) {
        val maxAlpha = if (intensity == PulseIntensity.STRONG) 0.32f else 0.18f
        val center = Offset(size.width / 2, size.height / 2)
        // Three rings, phase-offset by 1/3 each, growing outward and fading.
        for (i in 0 until 3) {
            val ringPhase = ((phase + i / 3f) % 1f)
            val ringColor = when (i) {
                0 -> color
                1 -> lerp(color, secondary, 0.5f)
                else -> secondary
            }
            val ringRadius = size.minDimension * (0.35f + 0.30f * ringPhase)
            val ringAlpha = maxAlpha * (1f - ringPhase)
            drawCircle(
                color = ringColor.copy(alpha = ringAlpha),
                radius = ringRadius,
                center = center,
                style = Stroke(width = 2f)
            )
        }
    }
}
