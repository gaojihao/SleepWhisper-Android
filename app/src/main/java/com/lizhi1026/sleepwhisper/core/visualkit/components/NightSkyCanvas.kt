package com.lizhi1026.sleepwhisper.core.visualkit.components

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.lizhi1026.sleepwhisper.core.visualkit.LocalReduceMotion
import com.lizhi1026.sleepwhisper.core.visualkit.TimeOfDayPalette
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.delay
import java.time.LocalTime

private data class TwinkleStar(
    val nx: Float,        // 0..1 normalized
    val ny: Float,        // 0..1 normalized
    val baseSizeDp: Float, // 0.4..1.6
    val phase: Float,     // 0..2π
    val periodMs: Float   // 3000..5000
)

private const val STAR_COUNT = 80
private const val STAR_SEED = 0xA110A4L
// Canvas frame interval. 33ms ≈ 30fps. Higher than the master spec's "10fps for
// twinkles" target because the same animation loop drives meteor and drifting-star
// rendering in subsequent tasks, both of which need 30fps to look smooth. Stars
// compute their alpha from a sinusoid that's slow enough that the extra frames
// between visible changes are imperceptible. Drops to 0fps (loop suspended) on
// reduce-motion.
private const val FRAME_INTERVAL_MS = 33L

/**
 * Living night sky for the Sleeping screen.
 *
 * 5 layers composited in a single Canvas:
 *   1. Aurora gradient mesh — sky color from current wall-clock time
 *   2. Static breath-twinkle stars (~80)        — added in Task 3
 *   3. Drifting brighter stars (~6)             — added in Task 4
 *   4. Rare meteor showers                       — added in Task 5
 *   5. Atmospheric cloud wisps (deep dark hours) — added in Task 6
 *
 * @param morphProgress fade-in driver from Phase 1's hero morph (0..1).
 * @param timeProvider injectable for tests; defaults to wall-clock.
 */
@Composable
fun NightSkyCanvas(
    modifier: Modifier = Modifier,
    morphProgress: Float = 1f,
    timeProvider: () -> LocalTime = { LocalTime.now() }
) {
    var palette by remember { mutableStateOf(TimeOfDayPalette.forTime(timeProvider())) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
            palette = TimeOfDayPalette.forTime(timeProvider())
        }
    }

    val stars = remember {
        val r = Random(STAR_SEED)
        List(STAR_COUNT) {
            TwinkleStar(
                nx = r.nextFloat(),
                ny = r.nextFloat(),
                baseSizeDp = 0.4f + r.nextFloat() * 1.2f,
                phase = r.nextFloat() * (2f * PI.toFloat()),
                periodMs = 3000f + r.nextFloat() * 2000f
            )
        }
    }

    var twinkleClockMs by remember { mutableFloatStateOf(0f) }
    if (!LocalReduceMotion.current) {
        LaunchedEffect(Unit) {
            var lastTick = 0L
            while (true) {
                withInfiniteAnimationFrameMillis { frameMs ->
                    if (frameMs - lastTick >= FRAME_INTERVAL_MS) {
                        twinkleClockMs = frameMs.toFloat()
                        lastTick = frameMs
                    }
                }
            }
        }
    }
    val reduce = LocalReduceMotion.current

    Canvas(
        modifier = modifier
            .alpha(morphProgress.coerceIn(0f, 1f))
            .clearAndSetSemantics { }
    ) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(palette.bottom, palette.top),
                startY = 0f,
                endY = size.height
            ),
            topLeft = Offset.Zero,
            size = size
        )

        val twoPi = 2f * PI.toFloat()
        stars.forEach { star ->
            val alpha = if (reduce) {
                0.6f
            } else {
                val sinVal = sin(twinkleClockMs / star.periodMs * twoPi + star.phase)
                0.3f + 0.6f * (sinVal * 0.5f + 0.5f)
            }
            drawCircle(
                color = Color.White.copy(alpha = alpha),
                radius = star.baseSizeDp.dp.toPx(),
                center = Offset(star.nx * size.width, star.ny * size.height)
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 800
)
@Composable
private fun PreviewNightSkyCanvasEvening() {
    NightSkyCanvas(
        modifier = Modifier.fillMaxSize(),
        timeProvider = { LocalTime.of(21, 0) }
    )
}

@androidx.compose.ui.tooling.preview.Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 800
)
@Composable
private fun PreviewNightSkyCanvasDeadNight() {
    NightSkyCanvas(
        modifier = Modifier.fillMaxSize(),
        timeProvider = { LocalTime.of(3, 0) }
    )
}
