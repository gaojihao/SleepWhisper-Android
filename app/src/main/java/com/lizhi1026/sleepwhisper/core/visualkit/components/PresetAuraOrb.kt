package com.lizhi1026.sleepwhisper.core.visualkit.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lizhi1026.sleepwhisper.core.visualkit.LocalReduceMotion
import com.lizhi1026.sleepwhisper.core.visualkit.SWMotion
import com.lizhi1026.sleepwhisper.model.AuraColors
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private data class OrbParticle(
    val nx: Float,
    val ny: Float,
    val baseRadiusDp: Float,
    val phaseSec: Float,
    val periodSec: Float
)

private const val ORB_PARTICLE_COUNT = 4
private const val ORB_PARTICLE_SEED = 0xA42C0BL
private const val ORB_FRAME_INTERVAL_MS = 33L  // 30fps

/**
 * Glassy orb tinted with a preset's 3-color aura. Used in PlayerScreen rows
 * to give each preset a visually distinct, watchable identity.
 *
 * Composition (bottom to top):
 *   1. Radial-gradient disc (aura.bottom outer → aura.mid → aura.top center)
 *   2. innerHighlight overlay — white-to-transparent crescent at the top
 *   3. 4 small drifting particles in aura.top color at 60% alpha
 *   4. (active only) Outer breathing shadow in aura.mid, pulsing alpha
 *      between 30%–60% over SWMotion.breathCycleMs
 *
 * Reduce-motion: particles frozen, breathing shadow at 45% (midpoint).
 * Low-RAM: particle layer skipped entirely.
 */
@Composable
fun PresetAuraOrb(
    aura: AuraColors,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 64.dp
) {
    val reduce = LocalReduceMotion.current
    val context = LocalContext.current
    val isLowRam = remember {
        val am = context.getSystemService(android.content.Context.ACTIVITY_SERVICE)
            as? android.app.ActivityManager
        am?.isLowRamDevice ?: false
    }
    val density = LocalDensity.current

    val shadowAlpha: Float = if (isActive && !reduce) {
        val t = rememberInfiniteTransition(label = "orb-breath")
        val v by t.animateFloat(
            initialValue = 0.30f,
            targetValue = 0.60f,
            animationSpec = infiniteRepeatable(
                animation = tween(SWMotion.breathCycleMs, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "shadow-alpha"
        )
        v
    } else if (isActive && reduce) 0.45f else 0f

    val particles = remember {
        val r = Random(ORB_PARTICLE_SEED)
        List(ORB_PARTICLE_COUNT) {
            OrbParticle(
                nx = 0.20f + r.nextFloat() * 0.60f,
                ny = 0.20f + r.nextFloat() * 0.60f,
                baseRadiusDp = 0.8f + r.nextFloat() * 0.6f,
                phaseSec = r.nextFloat() * (2f * PI.toFloat()),
                periodSec = 4f + r.nextFloat() * 3f
            )
        }
    }
    var particleClockMs by remember { mutableFloatStateOf(0f) }
    if (!reduce && !isLowRam) {
        LaunchedEffect(Unit) {
            var lastTick = 0L
            while (true) {
                withInfiniteAnimationFrameMillis { frameMs ->
                    if (frameMs - lastTick >= ORB_FRAME_INTERVAL_MS) {
                        particleClockMs = frameMs.toFloat()
                        lastTick = frameMs
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .size(sizeDp)
            .then(
                if (isActive) Modifier.shadow(
                    elevation = 24.dp,
                    shape = CircleShape,
                    clip = false,
                    ambientColor = aura.mid.copy(alpha = shadowAlpha),
                    spotColor = aura.mid.copy(alpha = shadowAlpha)
                ) else Modifier
            )
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(aura.top, aura.mid, aura.bottom),
                    radius = with(density) { sizeDp.toPx() * 0.7f }
                )
            )
            .innerHighlight(cornerRadius = sizeDp / 2)
    ) {
        if (!isLowRam) {
            Canvas(modifier = Modifier.size(sizeDp)) {
                val twoPi = 2f * PI.toFloat()
                val tSec = particleClockMs / 1000f
                particles.forEach { p ->
                    val dx = if (reduce) 0f else sin(tSec / p.periodSec * twoPi + p.phaseSec) * 0.05f
                    val dy = if (reduce) 0f else sin(tSec / (p.periodSec * 0.8f) * twoPi + p.phaseSec + 1f) * 0.05f
                    drawCircle(
                        color = aura.top.copy(alpha = 0.60f),
                        radius = p.baseRadiusDp.dp.toPx(),
                        center = Offset(
                            (p.nx + dx) * size.width,
                            (p.ny + dy) * size.height
                        )
                    )
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    showBackground = true,
    backgroundColor = 0xFF07101F,
    widthDp = 80,
    heightDp = 80
)
@Composable
private fun PreviewPresetAuraOrbActive() {
    PresetAuraOrb(
        aura = AuraColors(
            top = Color(0xFFC46B8C),
            mid = Color(0xFF5B4880),
            bottom = Color(0xFF1A0F1E)
        ),
        isActive = true,
        sizeDp = 64.dp
    )
}

@androidx.compose.ui.tooling.preview.Preview(
    showBackground = true,
    backgroundColor = 0xFF07101F,
    widthDp = 80,
    heightDp = 80
)
@Composable
private fun PreviewPresetAuraOrbIdle() {
    PresetAuraOrb(
        aura = AuraColors(
            top = Color(0xFFB7C3CC),
            mid = Color(0xFF4A5A6E),
            bottom = Color(0xFF0F1620)
        ),
        isActive = false,
        sizeDp = 64.dp
    )
}
