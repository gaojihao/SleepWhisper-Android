package com.lizhi1026.sleepwhisper.core.visualkit.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.lizhi1026.sleepwhisper.core.visualkit.LocalHeroBackdropController
import com.lizhi1026.sleepwhisper.core.visualkit.LocalReduceMotion
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor
import com.lizhi1026.sleepwhisper.core.visualkit.SWGradient
import com.lizhi1026.sleepwhisper.core.visualkit.SWMotion
import com.lizhi1026.sleepwhisper.core.visualkit.SWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.blendAmbient
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Aurora Backdrop — full-screen always-present surface. Replaces the previous
 * `BreathingBackground` composable. Three diagonal aurora ribbons drift at
 * offset phases over `SWMotion.backdropDriftMs`; a subtle film grain overlay
 * lifts it above generic gradients.
 *
 * On DAY scheme it's a quiet paper-warm surface with the lightest possible
 * ribbon shimmer; on DARK it's the cinematic Aurora layered with starlight
 * purple and dusk orange. On NIGHT it stays static (red-light scheme is for
 * 3am readability, not for animation).
 *
 * Animations pause when `LocalReduceMotion.current = true`.
 *
 * Consumes `LocalHeroBackdropController.ambient` — if set, ribbons tint 20%
 * toward the ambient color so audio-preset / time-of-day signals can shift
 * the room's mood app-wide.
 */
@Composable
fun AuroraBackdrop(modifier: Modifier = Modifier) {
    val scheme = LocalSWScheme.current
    val reduceMotion = LocalReduceMotion.current
    val ambient = LocalHeroBackdropController.current.ambient

    Box(
        modifier = modifier
            .fillMaxSize()
            .clearAndSetSemantics { }
            .background(SWColor.surface(scheme))
    ) {
        // Base ribbon layer — the Aurora backdrop gradient.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SWGradient.auroraBackdrop(scheme))
        )

        // Three drifting glow ribbons — animated unless reduce-motion or NIGHT.
        val animate = !reduceMotion && scheme != SWScheme.NIGHT
        if (animate) {
            val t = rememberInfiniteTransition(label = "aurora-drift")
            val phase by t.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(SWMotion.backdropDriftMs, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "phase"
            )
            DriftingRibbon(phaseOffset = 0f,                           color = blendAmbient(rib1(scheme), ambient, 0.2f), phase = phase)
            DriftingRibbon(phaseOffset = (2 * PI / 3).toFloat(),       color = blendAmbient(rib2(scheme), ambient, 0.2f), phase = phase)
            DriftingRibbon(phaseOffset = (4 * PI / 3).toFloat(),       color = blendAmbient(rib3(scheme), ambient, 0.2f), phase = phase)
        }

        // Static film grain — adds the cinematic detail.
        FilmGrain(intensity = if (scheme == SWScheme.DAY) 0.025f else 0.08f)
    }
}

@Composable
private fun DriftingRibbon(phaseOffset: Float, color: Color, phase: Float) {
    val angle = phase * 2 * PI.toFloat() + phaseOffset
    val cx = (0.5f + 0.25f * cos(angle))
    val cy = (0.5f + 0.25f * sin(angle))
    Canvas(modifier = Modifier.fillMaxSize()) {
        val r = size.maxDimension * 0.6f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = 0.35f), color.copy(alpha = 0f)),
                center = Offset(size.width * cx, size.height * cy),
                radius = r
            ),
            radius = r,
            center = Offset(size.width * cx, size.height * cy)
        )
    }
}

@Composable
private fun FilmGrain(intensity: Float) {
    // Pre-generated grain seeded for stability — recomputing per-frame would
    // burn battery and create visible shimmer that fights the calm aesthetic.
    val grain = remember {
        val random = Random(0xA110A4)
        IntArray(400) { random.nextInt(0, 256) }
    }
    Canvas(modifier = Modifier.fillMaxSize()) {
        val cell = size.width / 20f
        var i = 0
        for (y in 0 until 20) for (x in 0 until 20) {
            val v = grain[i++] / 255f
            drawCircle(
                color = Color.White.copy(alpha = intensity * v),
                radius = 0.7f,
                center = Offset(x * cell + cell / 2, y * (size.height / 20f) + (size.height / 20f) / 2)
            )
        }
    }
}

private fun rib1(s: SWScheme): Color = when (s) {
    SWScheme.DAY   -> Color(0xFFFFB088)
    SWScheme.DARK  -> Color(0xFFB8A4FF)
    SWScheme.NIGHT -> Color(0.78f, 0.45f, 0.45f)
}
private fun rib2(s: SWScheme): Color = when (s) {
    SWScheme.DAY   -> Color(0xFFB8A4FF)
    SWScheme.DARK  -> Color(0xFFFFB088)
    SWScheme.NIGHT -> Color(0.55f, 0.28f, 0.28f)
}
private fun rib3(s: SWScheme): Color = when (s) {
    SWScheme.DAY   -> Color(0xFFFAE9DC)
    SWScheme.DARK  -> Color(0xFF1F3A8C)
    SWScheme.NIGHT -> Color(0.24f, 0.06f, 0.06f)
}
