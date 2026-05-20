package com.lizhi1026.sleepwhisper.core.visualkit.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.lizhi1026.sleepwhisper.core.visualkit.LocalReduceMotion
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor
import kotlin.math.PI
import kotlin.math.sin

/**
 * Aurora waveform. Bars follow a sine-perturbed envelope (mimics a real FFT
 * shape without actually analyzing audio), tween smoothly between frames,
 * and color along the bar uses a dusk-orange → starlight-purple gradient.
 *
 * Animation pauses (or never starts) when `isPlaying = false` OR when
 * `LocalReduceMotion.current = true`. Public signature preserved.
 */
@Composable
fun AudioWaveform(
    modifier: Modifier = Modifier,
    isPlaying: Boolean,
    color: Color,
    barCount: Int = 14
) {
    val scheme = LocalSWScheme.current
    val reduce = LocalReduceMotion.current
    val tint = if (color == Color.Unspecified) SWColor.accent(scheme) else color
    val secondary = SWColor.accentSecondary(scheme)

    val animate = isPlaying && !reduce
    val phase = if (animate) {
        val t = rememberInfiniteTransition(label = "wave")
        val v by t.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1800, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "phase"
        )
        v
    } else 0.5f

    Canvas(modifier = modifier.fillMaxSize()) {
        val barWidth = size.width / (barCount * 1.7f)
        val gap = (size.width - barWidth * barCount) / (barCount - 1).coerceAtLeast(1)
        val mid = size.height / 2f
        val brush = Brush.verticalGradient(colors = listOf(tint, secondary))
        for (i in 0 until barCount) {
            val t = i.toFloat() / (barCount - 1).coerceAtLeast(1)
            // Two overlapping sines + a phase offset for variety.
            val envelope = (sin(PI * t * 2) * 0.5 + 0.5 +
                            sin((phase + t) * PI * 2) * 0.35).toFloat()
            val barH = (size.height * 0.18f + size.height * 0.6f * envelope).coerceAtLeast(barWidth)
            val x = i * (barWidth + gap)
            drawRoundRect(
                brush = brush,
                topLeft = Offset(x, mid - barH / 2),
                size = Size(barWidth, barH),
                cornerRadius = CornerRadius(barWidth / 2)
            )
        }
    }
}
