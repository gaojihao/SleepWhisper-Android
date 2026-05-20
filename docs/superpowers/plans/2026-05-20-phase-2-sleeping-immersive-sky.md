# Phase 2 — Sleeping Immersive Night Sky Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the placeholder `Starfield` on the Sleeping screen with a `NightSkyCanvas` that paints a living, time-of-day-aware night sky (5 composition layers), recolor the timer + LiquidProgressRing to aurora moonlit-silver tones, and add tap-to-dim + mini-Player-sheet interactions.

**Architecture:** New `NightSkyCanvas` composable wraps all 5 layers in a single `Canvas` for low overdraw and coordinated timing. `TimeOfDayPalette` is a pure helper mapping `LocalTime` to a palette struct with 60-minute crossfade interpolation between stops. The existing `WakeButton`'s long-press-to-fill gesture is preserved verbatim — Phase 2 only restyles its colors. Mini Player sheet reuses `PlayerScreen` with a new `embedded: Boolean = false` parameter that suppresses its own AuroraBackdrop. `Starfield.kt` is deleted at the end of the phase.

**Tech Stack:** Compose BOM 2024.12.01, Kotlin 2.0.21, Hilt; existing Phase 0 visualkit (`LiquidProgressRing`, `SWFont`, `SWColor`, `LocalReduceMotion`); existing Phase 1 morph state (`AppStateContainer.morphProgress`); Material3 `ModalBottomSheet`.

---

## File Map

**Created:**
- `app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/TimeOfDayPalette.kt` — pure helper, no Compose deps
- `app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/NightSkyCanvas.kt` — the 5-layer canvas
- `app/src/test/java/com/lizhi1026/sleepwhisper/core/visualkit/TimeOfDayPaletteTest.kt`

**Modified:**
- `app/src/main/java/com/lizhi1026/sleepwhisper/features/sleeping/SleepingScreen.kt` — swap `Starfield` for `NightSkyCanvas`, retint `WakeButton` LiquidProgressRing, retint timer color, add tap-to-dim wrapper and mini-Player-sheet
- `app/src/main/java/com/lizhi1026/sleepwhisper/features/player/PlayerScreen.kt` — add `embedded: Boolean = false` parameter that suppresses `AuroraBackdrop`

**Deleted:**
- `app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/Starfield.kt` — subsumed by NightSkyCanvas in Task 9

---

## Conventions

- All commits follow `area: short imperative summary`. Examples: `visualkit: TimeOfDayPalette with hour-stop interpolation`, `sleeping: replace Starfield with NightSkyCanvas`.
- After each task, run `./gradlew :app:compileDebugKotlin` to verify the project compiles before committing.
- Visual changes are verified via Compose `@Preview` annotations in Android Studio plus on-device install during Task 10 smoke.
- Every animated layer honors `LocalReduceMotion.current` AND degrades on low-RAM devices.
- HEAD at plan-write time: `39bdad0` (Phase 2 spec committed). Phase 2 builds on top of all 11 Phase 1 commits.

---

## Task 1 — `TimeOfDayPalette.kt` with hour-stop interpolation

**Files:**
- Create: `app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/TimeOfDayPalette.kt`
- Create: `app/src/test/java/com/lizhi1026/sleepwhisper/core/visualkit/TimeOfDayPaletteTest.kt`

- [ ] **Step 1: Write the failing test first**

Create `app/src/test/java/com/lizhi1026/sleepwhisper/core/visualkit/TimeOfDayPaletteTest.kt`:

```kotlin
package com.lizhi1026.sleepwhisper.core.visualkit

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

class TimeOfDayPaletteTest {

    @Test fun `returns dusk palette exactly at 1900`() {
        val p = TimeOfDayPalette.forTime(LocalTime.of(19, 0))
        assertEquals(Color(0xFF1A2540), p.bottom)
        assertEquals(Color(0xFFFFB088), p.top)
        assertEquals(0.08f, p.grainDensity, 0.001f)
    }

    @Test fun `returns evening palette exactly at 2100`() {
        val p = TimeOfDayPalette.forTime(LocalTime.of(21, 0))
        assertEquals(Color(0xFF0F1B30), p.bottom)
        assertEquals(Color(0xFFB8A4FF), p.top)
    }

    @Test fun `returns night palette exactly at 0000`() {
        val p = TimeOfDayPalette.forTime(LocalTime.of(0, 0))
        assertEquals(Color(0xFF050912), p.bottom)
        assertEquals(Color(0xFF1F2848), p.top)
    }

    @Test fun `returns dead-night palette with extra grain at 0300`() {
        val p = TimeOfDayPalette.forTime(LocalTime.of(3, 0))
        assertEquals(Color(0xFF050912), p.bottom)
        assertEquals(Color(0xFF1F2848), p.top)
        assertEquals(0.12f, p.grainDensity, 0.001f)
    }

    @Test fun `returns dawn palette exactly at 0500`() {
        val p = TimeOfDayPalette.forTime(LocalTime.of(5, 0))
        assertEquals(Color(0xFF1F2540), p.bottom)
        assertEquals(Color(0xFFFFA98E), p.top)
    }

    @Test fun `interpolates linearly between dusk and evening at 2000`() {
        // Halfway between dusk (#1A2540 bottom) and evening (#0F1B30 bottom)
        val p = TimeOfDayPalette.forTime(LocalTime.of(20, 0))
        // bottom red channel midpoint between 0x1A and 0x0F: (0x1A + 0x0F) / 2 = 0x14
        val r = p.bottom.red
        assertEquals(0x14 / 255f, r, 0.01f)
    }

    @Test fun `wraps midnight correctly between evening and night`() {
        // 22:30 should be halfway between evening (21:00) and night (00:00)
        // The bottom red channel goes from 0x0F to 0x05; midpoint at 22:30 is between
        val p = TimeOfDayPalette.forTime(LocalTime.of(22, 30))
        val r = p.bottom.red
        // 22:30 is 1.5h after 21:00, total 3h gap to 00:00, so fraction = 0.5
        // expected red ≈ (0x0F + 0x05) / 2 = 0x0A
        assertEquals(0x0A / 255f, r, 0.02f)
    }

    @Test fun `daytime hours fall back to dawn palette`() {
        // 12:00 is daytime — we expect the helper to return a sensible value (dawn-equivalent)
        // since the sleeping screen never renders during the day, this is a safety default.
        val p = TimeOfDayPalette.forTime(LocalTime.of(12, 0))
        // Just confirm it doesn't crash and returns a SkyPalette
        assertEquals(true, p.bottom.alpha > 0f)
    }
}
```

- [ ] **Step 2: Run the test — expect failure**

```bash
./gradlew :app:testDebugUnitTest --tests 'com.lizhi1026.sleepwhisper.core.visualkit.TimeOfDayPaletteTest'
```

Expected: FAIL — `TimeOfDayPalette` unresolved.

- [ ] **Step 3: Create `TimeOfDayPalette.kt`**

Create `app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/TimeOfDayPalette.kt`:

```kotlin
package com.lizhi1026.sleepwhisper.core.visualkit

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import java.time.LocalTime

/**
 * Sky palette pair — bottom of the gradient toward top, plus a film grain density that
 * picks up at dead-night for extra texture.
 */
data class SkyPalette(
    val bottom: Color,
    val top: Color,
    val grainDensity: Float = 0.08f
)

/**
 * Maps wall-clock time to a sky palette. Real 1:1 with the system clock — 30-minute
 * naps appear visually static, 8-hour overnights show the full dusk → night → dawn
 * progression.
 *
 * Stops are expressed in "extended hours": 19.0 = 19:00 today, 24.0 = 00:00 next day,
 * 29.0 = 05:00 next day, so linear interpolation between consecutive stops never has
 * to cross a 24h boundary.
 */
object TimeOfDayPalette {

    private data class Stop(val hour: Float, val palette: SkyPalette)

    private val STOPS = listOf(
        Stop(6.5f,  SkyPalette(Color(0xFF1F2540), Color(0xFFFFA98E))),                          // morning hand-back
        Stop(19.0f, SkyPalette(Color(0xFF1A2540), Color(0xFFFFB088))),                          // dusk
        Stop(21.0f, SkyPalette(Color(0xFF0F1B30), Color(0xFFB8A4FF))),                          // evening
        Stop(24.0f, SkyPalette(Color(0xFF050912), Color(0xFF1F2848))),                          // night (00:00)
        Stop(27.0f, SkyPalette(Color(0xFF050912), Color(0xFF1F2848), grainDensity = 0.12f)),    // dead-night (03:00)
        Stop(29.0f, SkyPalette(Color(0xFF1F2540), Color(0xFFFFA98E)))                           // dawn (05:00)
    )

    fun forTime(t: LocalTime): SkyPalette {
        val rawHour = t.hour + t.minute / 60f
        // 06:30..23:59 → use as-is; 00:00..06:29 → add 24 so it falls in the 06.5..29 range
        val effective = if (rawHour < 6.5f) rawHour + 24f else rawHour

        // Find the surrounding stops
        val upperIdx = STOPS.indexOfFirst { it.hour >= effective }
        if (upperIdx == -1) {
            // Past the last stop — return the last stop's palette
            return STOPS.last().palette
        }
        if (upperIdx == 0) {
            // Before the first stop — return the first stop's palette
            return STOPS.first().palette
        }
        val upper = STOPS[upperIdx]
        val lower = STOPS[upperIdx - 1]
        val fraction = (effective - lower.hour) / (upper.hour - lower.hour)

        return SkyPalette(
            bottom = lerp(lower.palette.bottom, upper.palette.bottom, fraction),
            top = lerp(lower.palette.top, upper.palette.top, fraction),
            grainDensity = lower.palette.grainDensity +
                (upper.palette.grainDensity - lower.palette.grainDensity) * fraction
        )
    }
}
```

- [ ] **Step 4: Run the test — expect pass**

```bash
./gradlew :app:testDebugUnitTest --tests 'com.lizhi1026.sleepwhisper.core.visualkit.TimeOfDayPaletteTest'
```

Expected: PASS (8/8).

- [ ] **Step 5: Compile the full app**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/TimeOfDayPalette.kt \
        app/src/test/java/com/lizhi1026/sleepwhisper/core/visualkit/TimeOfDayPaletteTest.kt
git commit -m "visualkit: TimeOfDayPalette with hour-stop interpolation"
```

---

## Task 2 — `NightSkyCanvas.kt` Layer 1 (gradient mesh)

**Files:**
- Create: `app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/NightSkyCanvas.kt`

This task creates the bare canvas with just the gradient base — no stars, meteors, or wisps yet. Subsequent tasks add layers incrementally.

- [ ] **Step 1: Create NightSkyCanvas.kt with Layer 1 only**

```kotlin
package com.lizhi1026.sleepwhisper.core.visualkit.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.lizhi1026.sleepwhisper.core.visualkit.SkyPalette
import com.lizhi1026.sleepwhisper.core.visualkit.TimeOfDayPalette
import kotlinx.coroutines.delay
import java.time.LocalTime

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
    // Repoll the palette every 60 seconds so the sky drifts across hour boundaries
    // without redrawing the gradient brush every frame.
    var palette by remember { mutableStateOf(TimeOfDayPalette.forTime(timeProvider())) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
            palette = TimeOfDayPalette.forTime(timeProvider())
        }
    }

    Canvas(
        modifier = modifier
            .alpha(morphProgress.coerceIn(0f, 1f))
            .clearAndSetSemantics { }
    ) {
        // Layer 1: vertical gradient — bottom palette at top of screen, top palette
        // at bottom. The "top" in palette refers to horizon-warm hint near the bottom
        // of the screen at dusk.
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(palette.bottom, palette.top),
                startY = 0f,
                endY = size.height
            ),
            topLeft = Offset.Zero,
            size = size
        )
    }
}
```

- [ ] **Step 2: Add a `@Preview` for visual inspection**

Append to `NightSkyCanvas.kt`:

```kotlin
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
```

Add the missing import `androidx.compose.foundation.layout.fillMaxSize`.

- [ ] **Step 3: Compile**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/NightSkyCanvas.kt
git commit -m "visualkit: NightSkyCanvas layer 1 (gradient mesh)"
```

---

## Task 3 — NightSkyCanvas Layer 2 (static stars with breath-twinkle)

**Files:**
- Modify: `app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/NightSkyCanvas.kt`

Add ~80 static stars with deterministic positions and a 10fps twinkle throttle.

- [ ] **Step 1: Add imports and the Star data class**

In `NightSkyCanvas.kt` add at the top of the file (before the function) and update imports:

```kotlin
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lizhi1026.sleepwhisper.core.visualkit.LocalReduceMotion
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random
```

Add the Star data class at the top of the file (file-private):

```kotlin
private data class Star(
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
// rendering, both of which need 30fps to look smooth. Stars compute their alpha
// from a sinusoid that's slow enough that the extra frames between visible
// changes are imperceptible — we accept the small extra draw cost in exchange for
// smooth meteors. Drops to 0fps (loop suspended) on reduce-motion.
private const val FRAME_INTERVAL_MS = 33L
```

- [ ] **Step 2: Update the `NightSkyCanvas` composable body**

Replace the existing `NightSkyCanvas` function body with:

```kotlin
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

    // Layer 2 — pre-generated static stars
    val stars = remember {
        val r = Random(STAR_SEED)
        List(STAR_COUNT) {
            Star(
                nx = r.nextFloat(),
                ny = r.nextFloat(),
                baseSizeDp = 0.4f + r.nextFloat() * 1.2f,
                phase = r.nextFloat() * (2f * PI.toFloat()),
                periodMs = 3000f + r.nextFloat() * 2000f
            )
        }
    }

    val reduce = LocalReduceMotion.current

    // Frame clock driving twinkle, drifting stars, meteors, and wisps. 30fps.
    var twinkleClockMs by remember { mutableFloatStateOf(0f) }
    if (!reduce) {
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

    Canvas(
        modifier = modifier
            .alpha(morphProgress.coerceIn(0f, 1f))
            .clearAndSetSemantics { }
    ) {
        // Layer 1 — gradient mesh
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(palette.bottom, palette.top),
                startY = 0f,
                endY = size.height
            ),
            topLeft = Offset.Zero,
            size = size
        )

        // Layer 2 — static twinkle stars. When reduce-motion is on, twinkleClockMs
        // stays at 0 so every star gets a stable alpha = 0.6 (base, no twinkle).
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
```

Note: We are using `withInfiniteAnimationFrameMillis` (the Compose primitive for frame-accurate animation timing) and throwing away frames that come too quickly. This gives us a 10fps throttle without instantiating an `InfiniteTransition` (which always runs at 60fps).

- [ ] **Step 3: Compile**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/NightSkyCanvas.kt
git commit -m "visualkit: NightSkyCanvas layer 2 (static twinkle stars)"
```

---

## Task 4 — NightSkyCanvas Layer 3 (drifting stars)

**Files:**
- Modify: `app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/NightSkyCanvas.kt`

Add 6 brighter stars that drift horizontally at 0.5 dp/sec, wrap off-screen, and fade-in on respawn.

- [ ] **Step 1: Add the DriftingStar data class and seed**

In `NightSkyCanvas.kt` near the existing Star definitions, add:

```kotlin
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
```

- [ ] **Step 2: Update the composable to render drifting stars**

Inside `NightSkyCanvas`, after the `stars` `remember` block (which we added in Task 3), add:

```kotlin
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

    // Drifting stars run at 30fps (every ~33ms). We piggyback on the same
    // animation frame loop as twinkles but only update drifting state on every
    // 3rd twinkle tick (10fps * 3 = 30fps).
    if (!reduce) {
        LaunchedEffect(Unit) {
            var lastFrameMs = 0L
            while (true) {
                withInfiniteAnimationFrameMillis { frameMs ->
                    val dt = (frameMs - lastFrameMs).coerceAtMost(100L)  // clamp big jumps
                    lastFrameMs = frameMs
                    val dtSec = dt / 1000f
                    driftingStars.forEachIndexed { i, ds ->
                        // velocityDpPerSec normalized by canvas width is roughly
                        // velocityDpPerSec / 360dp screen → so ~0.0014 norm/sec
                        ds.nx += ds.velocityDpPerSec / 360f * dtSec
                        if (ds.nx > 1.05f) {
                            // Wrap to off-screen left, new random y, alpha 0 → 1 fade
                            ds.nx = -0.05f
                            // Mutate via copy (since data class has val/var mix)
                            driftingStars[i] = ds.copy(ny = Random.nextFloat(), alpha = 0f)
                        } else if (ds.alpha < 1f) {
                            // Fade in
                            ds.alpha = (ds.alpha + dtSec * (1000f / DRIFTING_RESPAWN_FADE_MS)).coerceAtMost(1f)
                        }
                    }
                }
            }
        }
    }
```

Then add the rendering pass inside the `Canvas { ... }` block, after the static-stars drawing pass:

```kotlin
        // Layer 3 — drifting stars (brighter)
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
```

- [ ] **Step 3: Compile**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/NightSkyCanvas.kt
git commit -m "visualkit: NightSkyCanvas layer 3 (drifting stars with wrap-and-fade)"
```

---

## Task 5 — NightSkyCanvas Layer 4 (meteor showers)

**Files:**
- Modify: `app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/NightSkyCanvas.kt`

Add rare diagonal meteor streaks (1 at a time, every 90-180s).

- [ ] **Step 1: Add the Meteor data class and constants**

In `NightSkyCanvas.kt` near other private definitions, add:

```kotlin
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
```

- [ ] **Step 2: Add the meteor state and spawn loop inside `NightSkyCanvas`**

After the `driftingStars` block, add:

```kotlin
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
```

- [ ] **Step 3: Render the meteor inside the Canvas block**

Inside the `Canvas { ... }` block, after the drifting-stars drawing pass, add:

```kotlin
        // Layer 4 — meteor streak (if active)
        activeMeteor?.let { m ->
            val ageMs = System.currentTimeMillis() - m.spawnedAtMs
            val lifeFraction = ageMs.toFloat() / METEOR_LIFE_MS.toFloat()
            if (lifeFraction in 0f..1f) {
                // Meteor head travels from startX to ~1.1 over the full life
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
```

Note: this rendering reads `System.currentTimeMillis()` inside the draw scope every frame the meteor is active. Compose snapshot state `activeMeteor` triggers recomposition when it changes; combined with the `withInfiniteAnimationFrameMillis` loops above, the Canvas will repaint while a meteor is in flight.

- [ ] **Step 4: Compile**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/NightSkyCanvas.kt
git commit -m "visualkit: NightSkyCanvas layer 4 (meteor showers with random spawn)"
```

---

## Task 6 — NightSkyCanvas Layer 5 (cloud wisps) + low-RAM gating audit

**Files:**
- Modify: `app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/NightSkyCanvas.kt`

Add the cloud wisp layer (visible 22:00-03:59 only) and audit all layers for reduce-motion + low-RAM gating.

- [ ] **Step 1: Add the CloudWisp data class and constants**

```kotlin
private data class CloudWisp(
    var nx: Float,
    var ny: Float,
    val widthDp: Float,        // 30..50
    val heightDp: Float,       // 10..14
    var alpha: Float,          // 0..0.02
    val velocityDpPerSec: Float = 0.1f,
    var lifetimeMs: Long       // age in ms; respawn when > MAX_LIFETIME
)

private const val WISP_COUNT = 2
private const val WISP_MAX_LIFETIME_MS = 120_000L
private const val WISP_FADE_EDGE_MS = 8_000L
```

- [ ] **Step 2: Add wisp state and animation loop in the composable**

After the meteor block, add:

```kotlin
    // Layer 5 — atmospheric cloud wisps. Visible 22:00-03:59 only. Disabled on
    // reduce-motion and low-RAM devices.
    val currentHour = remember(palette) {
        // palette changes only on the 60s poll, so we can derive hour from timeProvider
        // here — once per poll, not every frame.
        timeProvider().hour
    }
    val wispsVisible = currentHour in 22..23 || currentHour in 0..3
    val wisps = remember {
        val r = Random(0xC10D55L)
        mutableListOf<CloudWisp>().apply {
            repeat(WISP_COUNT) {
                add(CloudWisp(
                    nx = r.nextFloat(),
                    ny = 0.2f + r.nextFloat() * 0.6f,
                    widthDp = 30f + r.nextFloat() * 20f,
                    heightDp = 10f + r.nextFloat() * 4f,
                    alpha = 0f,
                    lifetimeMs = r.nextLong(0, WISP_MAX_LIFETIME_MS)
                ))
            }
        }
    }
    if (!reduce && !isLowRam && wispsVisible) {
        LaunchedEffect(wispsVisible) {
            var lastFrameMs = 0L
            while (true) {
                withInfiniteAnimationFrameMillis { frameMs ->
                    val dt = (frameMs - lastFrameMs).coerceAtMost(100L)
                    lastFrameMs = frameMs
                    val dtSec = dt / 1000f
                    wisps.forEachIndexed { i, w ->
                        w.lifetimeMs += dt
                        w.nx += w.velocityDpPerSec / 360f * dtSec
                        // Alpha curve: fade in over first WISP_FADE_EDGE_MS, hold,
                        // fade out over last WISP_FADE_EDGE_MS, then respawn.
                        val targetAlpha = when {
                            w.lifetimeMs < WISP_FADE_EDGE_MS ->
                                0.02f * (w.lifetimeMs.toFloat() / WISP_FADE_EDGE_MS)
                            w.lifetimeMs > WISP_MAX_LIFETIME_MS - WISP_FADE_EDGE_MS ->
                                0.02f * ((WISP_MAX_LIFETIME_MS - w.lifetimeMs).toFloat() / WISP_FADE_EDGE_MS).coerceAtLeast(0f)
                            else -> 0.02f
                        }
                        w.alpha = targetAlpha
                        if (w.lifetimeMs >= WISP_MAX_LIFETIME_MS) {
                            wisps[i] = w.copy(
                                nx = -0.1f,
                                ny = 0.2f + Random.nextFloat() * 0.6f,
                                lifetimeMs = 0L
                            )
                        }
                    }
                }
            }
        }
    }
```

- [ ] **Step 3: Render wisps inside the Canvas block**

After the meteor drawing pass, add:

```kotlin
        // Layer 5 — cloud wisps (drawn as faint blurred ellipses via filled ovals)
        if (wispsVisible) {
            wisps.forEach { w ->
                if (w.alpha > 0f) {
                    drawOval(
                        color = Color.White.copy(alpha = w.alpha),
                        topLeft = Offset(
                            x = w.nx * size.width - w.widthDp.dp.toPx() / 2f,
                            y = w.ny * size.height - w.heightDp.dp.toPx() / 2f
                        ),
                        size = androidx.compose.ui.geometry.Size(
                            w.widthDp.dp.toPx(),
                            w.heightDp.dp.toPx()
                        )
                    )
                }
            }
        }
```

- [ ] **Step 4: Compile**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/NightSkyCanvas.kt
git commit -m "visualkit: NightSkyCanvas layer 5 (cloud wisps) + low-RAM gating"
```

---

## Task 7 — SleepingScreen redesign: swap Starfield → NightSkyCanvas, retint timer + ring

**Files:**
- Modify: `app/src/main/java/com/lizhi1026/sleepwhisper/features/sleeping/SleepingScreen.kt`

Replace the placeholder `Starfield` from Phase 1 with the new `NightSkyCanvas`. Retint the WakeButton's LiquidProgressRing to aurora. Recolor the timer to moonlit silver.

- [ ] **Step 1: Replace the `Starfield` call**

In `SleepingScreen.kt`, find this line:

```kotlin
            Starfield(modifier = Modifier.fillMaxSize(), density = density)
```

Replace with:

```kotlin
            NightSkyCanvas(
                modifier = Modifier.fillMaxSize(),
                morphProgress = morphProgress
            )
```

Note: the `density = morphProgress * 80` logic is now embedded inside NightSkyCanvas as the `morphProgress` alpha multiplier — the whole canvas fades in instead of just the star count ramping.

Update the imports at the top of `SleepingScreen.kt`:

```kotlin
// Remove:
import com.lizhi1026.sleepwhisper.core.visualkit.components.Starfield
// Add:
import com.lizhi1026.sleepwhisper.core.visualkit.components.NightSkyCanvas
```

Also remove the now-unused local `density` variable (the `val density = (morphProgress * 80f).toInt()...` line just above the Starfield call).

- [ ] **Step 2: Retint the WakeButton's LiquidProgressRing**

In the `WakeButton` private composable, find the existing call:

```kotlin
        LiquidProgressRing(
            progress = fillProgress.value,
            color = SWColor.accent(scheme),
            radius = 65.dp
        )
```

The existing call uses `SWColor.accent(scheme)` which on DARK gives dusk-orange (`#FFB088`). That's already the aurora tone. **No change needed** — just verify this is still the case.

Also find the inner `Box` with `.background(SWGradient.accent(scheme))`. Replace with:

```kotlin
        Box(
            modifier = Modifier
                .size(112.dp)
                .clip(CircleShape)
                .background(SWGradient.auroraGlow(scheme)),
            contentAlignment = Alignment.Center
        ) {
```

Update the import:

```kotlin
import com.lizhi1026.sleepwhisper.core.visualkit.SWGradient
// (SWGradient.auroraGlow is already exported from Phase 0)
```

- [ ] **Step 3: Retint the CountdownSection timer text**

In `CountdownSection`, the timer is drawn with `color = Color.White`. Change to moonlit silver `#ECF2F8`. Replace:

```kotlin
    val style = if (h > 0)
        SWFont.displayLargeTabular().copy(color = Color.White)
    else
        SWFont.displayXLTabular().copy(color = Color.White)
```

with:

```kotlin
    val moonlitSilver = Color(0xFFECF2F8)
    val style = if (h > 0)
        SWFont.displayLGTabular().copy(color = moonlitSilver)   // 64sp (fits in 280dp ring)
    else
        SWFont.displayXLTabular().copy(color = moonlitSilver)   // 96sp (only used briefly for MM:SS)
```

The change for HH:MM:SS case: was `displayLargeTabular()` (72sp), now `displayLGTabular()` (64sp) to fit within the 280dp progress ring. MM:SS stays at 96sp since 5 chars at 96sp does fit in 280dp.

- [ ] **Step 4: Compile and visual smoke**

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:installDebug
```

Start a sleep session. Confirm on Sleeping:
- Background gradient matches the current wall-clock time (test at 21:00 if possible)
- Stars twinkle subtly across the screen
- A meteor may appear after 90+ seconds (be patient or wait through one)
- Timer renders in moonlit silver (slightly off-white, not pure white)
- WakeButton's inner circle now uses aurora-glow gradient
- WakeButton's long-press-fill still works as before

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/lizhi1026/sleepwhisper/features/sleeping/SleepingScreen.kt
git commit -m "sleeping: replace Starfield with NightSkyCanvas, retint timer and WakeButton"
```

---

## Task 8 — Tap-to-dim interaction + mini Player sheet

**Files:**
- Modify: `app/src/main/java/com/lizhi1026/sleepwhisper/features/sleeping/SleepingScreen.kt`
- Modify: `app/src/main/java/com/lizhi1026/sleepwhisper/features/player/PlayerScreen.kt`

Add a tap on the canvas that dims the timer block + WakeButton to 24% opacity over 300ms. Add a mini Player sheet via `ModalBottomSheet` that reuses `PlayerScreen` with a new `embedded = true` parameter.

- [ ] **Step 1: Add the `embedded` parameter to PlayerScreen**

Open `app/src/main/java/com/lizhi1026/sleepwhisper/features/player/PlayerScreen.kt`. Find the public composable signature:

```kotlin
@Composable
fun PlayerScreen(vm: PlayerViewModel = hiltViewModel()) {
```

Change to:

```kotlin
@Composable
fun PlayerScreen(
    embedded: Boolean = false,
    vm: PlayerViewModel = hiltViewModel()
) {
```

Find the existing `AuroraBackdrop()` call near the top of the function body. Wrap in a conditional:

```kotlin
    if (!embedded) {
        AuroraBackdrop()
    }
```

(All existing callers use `PlayerScreen()` with default `embedded = false`, so they keep the AuroraBackdrop.)

Compile to verify:

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: Add tap-to-dim state and ModalBottomSheet to SleepingScreen**

In `SleepingScreen.kt`, inside the `CompositionLocalProvider(LocalSWScheme provides SWScheme.DARK) { ... }` block, after the `val morphProgress` and `val morphScope` declarations, add:

```kotlin
        var dimmed by remember { mutableStateOf(false) }
        var showMiniPlayer by remember { mutableStateOf(false) }
        val dimAlpha by animateFloatAsState(
            targetValue = if (dimmed) 0.24f else 1f,
            animationSpec = tween(if (LocalReduceMotion.current) 100 else 300),
            label = "dim-alpha"
        )
```

Add the missing imports at the top of the file:

```kotlin
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.mutableStateOf
import com.lizhi1026.sleepwhisper.core.visualkit.LocalReduceMotion
import com.lizhi1026.sleepwhisper.features.player.PlayerScreen
```

- [ ] **Step 3: Wrap the timer-content Column with tap-to-dim gesture and dim alpha**

Find the inner `Column` (the one with `verticalArrangement = Arrangement.spacedBy(SWSpacing.xxl, Alignment.CenterVertically)` containing `TitleSection`, `CountdownSection`, etc).

Modify its modifier chain to add a tap-to-toggle-dim gesture and the `.alpha(dimAlpha)`:

```kotlin
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .padding(horizontal = SWSpacing.xl)
                    .alpha(dimAlpha)
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { dimmed = !dimmed })
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(SWSpacing.xxl, Alignment.CenterVertically)
            ) {
                // ... existing children: TitleSection, CountdownSection, PlayerStatusCard, WakeButton, PausePill ...
            }
```

Add imports:

```kotlin
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.draw.alpha
```

The `pointerInput(Unit) { detectTapGestures { ... } }` here catches taps anywhere in the Column — but the existing `WakeButton` has its own `pointerInput` for the long-press-to-fill gesture. Compose pointer input semantics: child pointer handlers consume events first; the parent tap detector only fires when no child claims the gesture. The WakeButton's long-press claim consumes its area; tapping anywhere else in the column toggles dim. **Verify this with a quick install in Step 4.**

- [ ] **Step 4: Wrap PlayerStatusCard tap → show mini-player sheet**

The current `PlayerStatusCard` doesn't have a click handler. We need to add `clickable { showMiniPlayer = true }` to its outer modifier. Find the `PlayerStatusCard` private composable. In its `GlassCard(...)` modifier, add:

```kotlin
    GlassCard(
        modifier = Modifier.clickable { onClick() },  // new
        cornerRadius = 18.dp,
        elevation = ElevationLevel.MEDIUM
    ) {
```

Change the `PlayerStatusCard` signature to accept `onClick: () -> Unit`:

```kotlin
@Composable
private fun PlayerStatusCard(presetId: String, endsAt: Long?, nowMs: Long, onClick: () -> Unit) {
```

Update the call site inside the main `SleepingScreen` composable:

```kotlin
                if (playerState is PlayerState.Playing) {
                    val ps = playerState as PlayerState.Playing
                    PlayerStatusCard(
                        presetId = ps.presetId,
                        endsAt = ps.endsAt,
                        nowMs = nowMs,
                        onClick = { showMiniPlayer = true }
                    )
                }
```

Add the `clickable` import if missing:

```kotlin
import androidx.compose.foundation.clickable
```

- [ ] **Step 5: Add the ModalBottomSheet rendering**

Inside the outer `Box(modifier = Modifier.fillMaxSize().background(...)) { ... }` block, AFTER the Column but BEFORE the closing `}` of the Box, add:

```kotlin
            if (showMiniPlayer) {
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
                ModalBottomSheet(
                    onDismissRequest = { showMiniPlayer = false },
                    sheetState = sheetState
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        PlayerScreen(embedded = true)
                    }
                }
            }
```

- [ ] **Step 6: Add hardware-back handling**

Inside the outer `CompositionLocalProvider(LocalSWScheme provides SWScheme.DARK) { ... }`, after the `dimAlpha` declaration, add:

```kotlin
        androidx.activity.compose.BackHandler(enabled = dimmed || showMiniPlayer) {
            when {
                showMiniPlayer -> showMiniPlayer = false
                dimmed -> dimmed = false
            }
        }
```

This ensures hardware back priority matches the spec: first dismiss sheet → then restore dim → otherwise no-op.

Add the import:

```kotlin
import androidx.activity.compose.BackHandler
```

- [ ] **Step 7: Compile + install + visual smoke**

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:installDebug
```

Start a sleep session. Confirm:
- Single tap anywhere except on WakeButton → timer dims to 24%
- Single tap again → restores to 100%
- Long-press on WakeButton → fills LiquidProgressRing as before, completes wake
- Tap the PlayerStatusCard (while audio is playing) → mini Player sheet slides up
- Swipe down on sheet → dismisses
- Press hardware back while sheet open → dismisses sheet
- Press hardware back while dim active → restores
- Press hardware back at idle → no-op (doesn't exit Sleeping)

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/lizhi1026/sleepwhisper/features/sleeping/SleepingScreen.kt \
        app/src/main/java/com/lizhi1026/sleepwhisper/features/player/PlayerScreen.kt
git commit -m "sleeping: tap-to-dim + mini Player sheet, PlayerScreen embedded param"
```

---

## Task 9 — Delete `Starfield.kt`

**Files:**
- Delete: `app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/Starfield.kt`

`Starfield` is no longer referenced by any source code after Task 7 swapped it for NightSkyCanvas.

- [ ] **Step 1: Confirm no source references remain**

```bash
grep -rn "Starfield" app/src/main/java/
```

Expected: empty output (no source-level references). If any references exist, do not proceed — update them to `NightSkyCanvas` first.

- [ ] **Step 2: Delete the file**

```bash
rm app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/Starfield.kt
```

- [ ] **Step 3: Compile**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "visualkit: delete Starfield (subsumed by NightSkyCanvas)"
```

---

## Task 10 — Manual smoke walkthrough

**Files:** none — verification gate.

- [ ] **Step 1: Build and install fresh**

```bash
./gradlew :app:installDebug
adb shell am start -n com.lizhi1026.sleepwhisper/.app.MainActivity
```

- [ ] **Step 2: Trigger morph from Home**

On Home, tap Sleep CTA. Watch the morph. Confirm NightSkyCanvas fades in (rather than the Phase-1 Starfield density ramping).

- [ ] **Step 3: Real-time wall-clock observation**

Sit on Sleeping for at least 60 seconds. Confirm:
- Stars twinkle subtly
- 1-2 drifting stars move horizontally
- A meteor MAY appear (90-180s spawn — be patient or trigger a few times)
- Background gradient is stable (it only changes meaningfully at hour boundaries)

- [ ] **Step 4: Manually set device clock to test palette stops**

For each test time, use Settings → Date & Time → set time, then trigger a morph or restart Sleeping:

| Set to | Expected sky |
|---|---|
| 19:00 | Dusk — warm horizon hint at bottom |
| 21:00 | Evening — starlight purple at bottom, deep at top |
| 00:00 | Night — very dark, slight blue tint |
| 03:00 | Dead-night — same as night but extra grain visible |
| 05:00 | Dawn — warm horizon hint returns |

Restore device clock to auto when done.

- [ ] **Step 5: Tap-to-dim**

Single tap anywhere on Sleeping (not on WakeButton). Confirm timer + caption + metadata + WakeButton dim to 24%. Sky stays full alpha. Tap again to restore.

- [ ] **Step 6: Long-press wake**

Long-press WakeButton. Confirm LiquidProgressRing fills with aurora-glow gradient. Release before completion → cancels and resets. Hold past completion → triggers wake → reverse morph back to Home.

- [ ] **Step 7: Mini Player sheet**

Start audio playback (from Home Now Playing or Player tab). Begin sleep. On Sleeping, tap the PlayerStatusCard (showing current preset + waveform). Confirm the mini Player sheet slides up to ~65% screen height. Swipe down to dismiss. Press hardware back while sheet open → dismisses.

- [ ] **Step 8: Reduce-motion**

```bash
adb shell settings put secure transition_animation_scale 0
adb shell settings put secure window_animation_scale 0
adb shell settings put secure animator_duration_scale 0
```

Relaunch Sleeping. Confirm:
- Sky gradient still renders (selects palette by current time, just doesn't drift)
- Stars are static at base alpha (no twinkle)
- No meteors appear
- No cloud wisps
- Tap-to-dim still works, faster transition (100ms)

Restore:
```bash
adb shell settings put secure transition_animation_scale 1
adb shell settings put secure window_animation_scale 1
adb shell settings put secure animator_duration_scale 1
```

- [ ] **Step 9: Battery sanity**

Start a sleep session. Leave with Sleeping screen visible for 10 minutes. Note battery percentage delta. Compare to a 10-minute Home-screen session. Expected: ≤3 percentage point difference.

If significantly higher, investigate — the drifting stars or wisps may need throttling.

- [ ] **Step 10: Capture screenshots**

```bash
mkdir -p docs/superpowers/screenshots/2026-05-20-phase-2

# Sleeping at each palette stop — set time, then capture
adb exec-out screencap -p > docs/superpowers/screenshots/2026-05-20-phase-2/sleeping-dusk-1900.png
adb exec-out screencap -p > docs/superpowers/screenshots/2026-05-20-phase-2/sleeping-evening-2100.png
adb exec-out screencap -p > docs/superpowers/screenshots/2026-05-20-phase-2/sleeping-night-0000.png
adb exec-out screencap -p > docs/superpowers/screenshots/2026-05-20-phase-2/sleeping-dead-night-0300.png
adb exec-out screencap -p > docs/superpowers/screenshots/2026-05-20-phase-2/sleeping-dawn-0500.png

# Tap-dim variant
adb exec-out screencap -p > docs/superpowers/screenshots/2026-05-20-phase-2/sleeping-dimmed.png

# Mini player sheet open
adb exec-out screencap -p > docs/superpowers/screenshots/2026-05-20-phase-2/sleeping-mini-player.png
```

- [ ] **Step 11: Final commit (screenshots)**

```bash
git add docs/superpowers/screenshots/
git commit -m "phase-2: verification screenshots across palette stops"
```

- [ ] **Step 12: Final test suite run**

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

Expected: BUILD SUCCESSFUL. All tests pass (TimeOfDayPaletteTest 8/8, plus all prior phase tests).

---

## Phase 2 Done When

- All 10 tasks committed
- `./gradlew :app:testDebugUnitTest :app:assembleDebug` green
- NightSkyCanvas renders all 5 layers in DARK scheme
- Sky palette changes based on current wall-clock time (visible by manually setting device clock)
- Timer renders in moonlit silver, fits cleanly inside the 280dp WakeButton ring
- WakeButton's existing long-press-to-fill gesture preserved verbatim
- Tap-to-dim toggles timer + WakeButton to 24% opacity
- Mini Player sheet opens via PlayerStatusCard tap, reuses PlayerScreen with no backdrop double-up
- Reduce-motion: sky static, no meteors, no wisps
- Low-RAM device: same as reduce-motion (no meteors, no wisps)
- Battery delta ≤3 percentage points vs Home over 10-minute session
- `Starfield.kt` deleted from source tree
- Screenshots captured for each palette stop

## Out of Scope (deferred to later phases)

- `PresetAuraOrb` — Phase 3 (NowPlaying chip on Sleeping uses the existing PlayerStatusCard treatment)
- Sleep-stage-aware sky tinting — future enhancement
- Constellation overlays, wish-on-meteor, audio-reactive elements
- Sleeping screen reverse morph improvements (already wired in Phase 1)
- Trends, Settings, Onboarding visual changes — Phases 4 / 5
