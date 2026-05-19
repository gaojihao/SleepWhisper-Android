package com.lizhi1026.sleepwhisper.core.visualkit.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lizhi1026.sleepwhisper.core.visualkit.LocalReduceMotion

/** Visual intensity for [PulseRing] — port of iOS PulseRing.Intensity. */
enum class PulseIntensity(
    val topAlpha: Float,
    val midAlpha: Float,
    val lineWidthDp: Float
) {
    /** Used on dark / night backgrounds; brighter rings. */
    STRONG(topAlpha = 0.55f, midAlpha = 0.40f, lineWidthDp = 1.5f),
    /** Used on light backgrounds; subdued so they don't clash with accent CTA. */
    SOFT(topAlpha = 0.20f, midAlpha = 0.14f, lineWidthDp = 1.0f)
}

/**
 * Two concentric expanding-fading rings — port of iOS PulseRing.swift.
 * `radius` is the **inner** ring radius; each ring scales from 1.0 → 1.8 while alpha fades to 0,
 * over 3 seconds, infinite repeat. The second ring is phase-offset by 1.5s.
 */
@Composable
fun PulseRing(
    modifier: Modifier = Modifier,
    color: Color,
    radius: Dp = 80.dp,
    intensity: PulseIntensity = PulseIntensity.STRONG
) {
    val reduceMotion = LocalReduceMotion.current
    if (reduceMotion) {
        // Static fallback: render the inner ring at full alpha, no animation.
        Canvas(modifier = modifier.size(radius * 2).clearAndSetSemantics { }) {
            val r = radius.toPx()
            val stroke = Stroke(width = intensity.lineWidthDp.dp.toPx())
            drawCircle(color = color.copy(alpha = intensity.topAlpha), radius = r, style = stroke)
        }
        return
    }
    val t = rememberInfiniteTransition(label = "pulse")
    val cycleMs = 3000

    val scale1 by t.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = cycleMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring1-scale"
    )
    val scale2 by t.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = cycleMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(cycleMs / 2)
        ),
        label = "ring2-scale"
    )

    Canvas(modifier = modifier.size(radius * 2).clearAndSetSemantics { }) {
        val r = radius.toPx()
        val stroke = Stroke(width = intensity.lineWidthDp.dp.toPx())
        val a1 = ((1.8f - scale1) / 0.8f).coerceIn(0f, 1f) * intensity.topAlpha
        val a2 = ((1.8f - scale2) / 0.8f).coerceIn(0f, 1f) * intensity.midAlpha
        drawCircle(color = color.copy(alpha = a1), radius = r * scale1, style = stroke)
        drawCircle(color = color.copy(alpha = a2), radius = r * scale2, style = stroke)
    }
}
