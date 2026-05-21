package com.lizhi1026.sleepwhisper.core.visualkit.components

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.lizhi1026.sleepwhisper.core.visualkit.LocalReduceMotion
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private data class AuraMote(
    val nx: Float,
    val ny: Float,
    val baseRadiusDp: Float,
    val phaseSec: Float,
    val periodSec: Float
)

private const val AURA_MOTE_COUNT = 24
private const val AURA_MOTE_SEED = 0xBE5F12L
private const val AURA_FRAME_INTERVAL_MS = 33L  // 30fps

/**
 * Full-screen overlay of 24 small motes tinted with [tint]. Used on the Player
 * screen to give the room an active "occupied" feel when a preset is playing.
 *
 * The composable is fully transparent to pointer input — does not consume taps.
 *
 * Reduce-motion: motes static at their seeded positions.
 * Low-RAM: composable returns nothing (caller still draws normally).
 */
@Composable
fun AuraParticles(
    tint: Color,
    modifier: Modifier = Modifier
) {
    val reduce = LocalReduceMotion.current
    val context = LocalContext.current
    val isLowRam = remember {
        val am = context.getSystemService(android.content.Context.ACTIVITY_SERVICE)
            as? android.app.ActivityManager
        am?.isLowRamDevice ?: false
    }
    if (isLowRam) return

    val motes = remember {
        val r = Random(AURA_MOTE_SEED)
        List(AURA_MOTE_COUNT) {
            AuraMote(
                nx = r.nextFloat(),
                ny = r.nextFloat(),
                baseRadiusDp = 1.5f + r.nextFloat() * 2.0f,
                phaseSec = r.nextFloat() * (2f * PI.toFloat()),
                periodSec = 8f + r.nextFloat() * 8f
            )
        }
    }

    var clockMs by remember { mutableFloatStateOf(0f) }
    if (!reduce) {
        LaunchedEffect(Unit) {
            var lastTick = 0L
            while (true) {
                withInfiniteAnimationFrameMillis { frameMs ->
                    if (frameMs - lastTick >= AURA_FRAME_INTERVAL_MS) {
                        clockMs = frameMs.toFloat()
                        lastTick = frameMs
                    }
                }
            }
        }
    }

    Canvas(modifier = modifier.clearAndSetSemantics { }) {
        val twoPi = 2f * PI.toFloat()
        val tSec = clockMs / 1000f
        motes.forEach { m ->
            val dx = if (reduce) 0f else sin(tSec / m.periodSec * twoPi + m.phaseSec) * 0.04f
            val dy = if (reduce) 0f else sin(tSec / (m.periodSec * 0.9f) * twoPi + m.phaseSec + 1f) * 0.04f
            drawCircle(
                color = tint.copy(alpha = 0.30f),
                radius = m.baseRadiusDp.dp.toPx(),
                center = Offset(
                    (m.nx + dx) * size.width,
                    (m.ny + dy) * size.height
                )
            )
        }
    }
}
