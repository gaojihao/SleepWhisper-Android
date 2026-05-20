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

private data class DriftingStar(
    var nx: Float,           // current normalized x (mutable, drifts)
    val ny: Float,            // fixed y
    val baseSizeDp: Float,
    var alpha: Float,         // current alpha (0..1, fades in on respawn)
    val phase: Float,
    val periodMs: Float,
    val velocityDpPerSec: Float  // ~0.5 dp/sec
)

private const val DRIFTING_STAR_COUNT = 6
private const val DRIFTING_STAR_SEED = 0xD81FA1L
private const val DRIFTING_RESPAWN_FADE_MS = 800L

private data class Meteor(
    val startX: Float,       // normalized x where meteor head starts (off-screen left, e.g. -0.05)
    val startY: Float,       // 0.1..0.6 normalized
    val angleRad: Float,     // [-30°, +30°] from horizontal, in radians
    val spawnedAtMs: Long    // wall-clock spawn time
)

private const val METEOR_LIFE_MS = 600L
private const val METEOR_FADE_START_MS = 400L  // fade begins at 400ms of 600ms life
private const val METEOR_HEAD_DP = 16f
private const val METEOR_TAIL_LENGTH_DP = 60f
private const val METEOR_TAIL_THICKNESS_DP = 2f
private const val METEOR_MIN_INTERVAL_MS = 90_000L   // 90s
private const val METEOR_MAX_INTERVAL_MS = 180_000L  // 180s

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

    val driftingStars = remember {
        val r = Random(DRIFTING_STAR_SEED)
        mutableListOf<DriftingStar>().apply {
            repeat(DRIFTING_STAR_COUNT) {
                add(DriftingStar(
                    nx = r.nextFloat(),
                    ny = r.nextFloat(),
                    baseSizeDp = 0.8f + r.nextFloat() * 1.0f,
                    alpha = 1f,
                    phase = r.nextFloat() * (2f * PI.toFloat()),
                    periodMs = 3000f + r.nextFloat() * 2000f,
                    velocityDpPerSec = 0.5f
                ))
            }
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
    if (!LocalReduceMotion.current) {
        LaunchedEffect(Unit) {
            var lastFrameMs = 0L
            while (true) {
                withInfiniteAnimationFrameMillis { frameMs ->
                    if (lastFrameMs == 0L) {
                        lastFrameMs = frameMs
                        return@withInfiniteAnimationFrameMillis
                    }
                    val dt = (frameMs - lastFrameMs).coerceAtMost(100L)
                    lastFrameMs = frameMs
                    val dtSec = dt / 1000f
                    driftingStars.forEachIndexed { i, ds ->
                        ds.nx += ds.velocityDpPerSec / 360f * dtSec
                        if (ds.nx > 1.05f) {
                            driftingStars[i] = ds.copy(nx = -0.05f, ny = Random.nextFloat(), alpha = 0f)
                        } else if (ds.alpha < 1f) {
                            ds.alpha = (ds.alpha + dtSec * (1000f / DRIFTING_RESPAWN_FADE_MS)).coerceAtMost(1f)
                        }
                    }
                }
            }
        }
    }
    val reduce = LocalReduceMotion.current

    // Layer 4 — meteor showers. Single meteor visible at any moment. Spawns every
    // 90-180s. Disabled on reduce-motion and on low-RAM devices.
    val context = androidx.compose.ui.platform.LocalContext.current
    val isLowRam = remember {
        val am = context.getSystemService(android.content.Context.ACTIVITY_SERVICE)
            as? android.app.ActivityManager
        am?.isLowRamDevice ?: false
    }
    var activeMeteor by remember { mutableStateOf<Meteor?>(null) }
    if (!reduce && !isLowRam) {
        LaunchedEffect(Unit) {
            while (true) {
                val nextDelay = Random.nextLong(METEOR_MIN_INTERVAL_MS, METEOR_MAX_INTERVAL_MS)
                delay(nextDelay)
                activeMeteor = Meteor(
                    startX = -0.05f,
                    startY = 0.1f + Random.nextFloat() * 0.5f,
                    angleRad = ((-30 + Random.nextFloat() * 60) * PI / 180f).toFloat(),
                    spawnedAtMs = System.currentTimeMillis()
                )
                delay(METEOR_LIFE_MS)
                activeMeteor = null
            }
        }
    }

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
        // Layer 3 — drifting stars (brighter, with twinkle)
        driftingStars.forEach { ds ->
            val alphaBase = if (reduce) 0.85f else {
                val sinVal = sin(twinkleClockMs / ds.periodMs * twoPi + ds.phase)
                0.5f + 0.5f * (sinVal * 0.5f + 0.5f)
            }
            drawCircle(
                color = Color.White.copy(alpha = alphaBase * ds.alpha),
                radius = ds.baseSizeDp.dp.toPx(),
                center = Offset(ds.nx * size.width, ds.ny * size.height)
            )
        }
        // Layer 4 — meteor streak (if active). Re-read twinkleClockMs (a state)
        // here just to force this draw pass to recompose when the canvas clock
        // ticks — otherwise the meteor head/tail position would only update
        // when `activeMeteor` itself changes (spawn / unspawn), once per ~120s.
        if (activeMeteor != null) {
            // Touching twinkleClockMs ensures recomposition on each frame.
            @Suppress("UNUSED_EXPRESSION") twinkleClockMs
        }
        activeMeteor?.let { m ->
            val ageMs = System.currentTimeMillis() - m.spawnedAtMs
            val lifeFraction = ageMs.toFloat() / METEOR_LIFE_MS.toFloat()
            if (lifeFraction in 0f..1f) {
                val headX = m.startX + (1.1f - m.startX) * lifeFraction
                val headY = m.startY + sin(m.angleRad) * (lifeFraction * 0.3f)
                val tailDx = -kotlin.math.cos(m.angleRad) * (METEOR_TAIL_LENGTH_DP.dp.toPx())
                val tailDy = -sin(m.angleRad) * (METEOR_TAIL_LENGTH_DP.dp.toPx())
                val alpha = if (ageMs < METEOR_FADE_START_MS) 1f
                            else 1f - (ageMs - METEOR_FADE_START_MS) / (METEOR_LIFE_MS - METEOR_FADE_START_MS).toFloat()

                // Tail: tapered line from head backwards
                drawLine(
                    color = Color.White.copy(alpha = alpha * 0.6f),
                    start = Offset(headX * size.width + tailDx, headY * size.height + tailDy),
                    end = Offset(headX * size.width, headY * size.height),
                    strokeWidth = METEOR_TAIL_THICKNESS_DP.dp.toPx()
                )
                // Head: brighter dot
                drawCircle(
                    color = Color.White.copy(alpha = alpha),
                    radius = METEOR_HEAD_DP.dp.toPx() / 2f,
                    center = Offset(headX * size.width, headY * size.height)
                )
            }
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
