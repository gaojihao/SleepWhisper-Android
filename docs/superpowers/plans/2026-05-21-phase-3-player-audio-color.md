# Phase 3 — Player Audio↔Color Synchrony Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Attach a 3-color aura to each of the 12 bundled audio presets, replace the Player screen's flat icon circles with `PresetAuraOrb` (glassy disc + drifting particles + active breathing shadow), and push the selected preset's mid color into `HeroBackdropController` so the AuroraBackdrop drifts to match — persisting across Home, Trends, Settings until playback stops.

**Architecture:** A new `AuraColors` data class lives on `AudioPreset` with `@Transient` so kotlinx-serialization skips it. A new `PresetAuraOrb` composable consumes those colors directly. A new `AmbientSync.syncToPlayer(currentPreset, fallback)` extension on `HeroBackdropController` is called from `LaunchedEffect(currentPreset)` on PlayerScreen and from the existing `LaunchedEffect` on HomeScreen. `GlassCard` gains a backward-compatible `surfaceTintOverride: Color? = null` parameter so PresetRows can mix 12% of the current ambient into their elevated surface for a "reflected light" feel. A new `AuraParticles` composable overlays the Player screen with 24 drifting motes when a preset is active.

**Tech Stack:** Compose BOM 2024.12.01, Kotlin 2.0.21, Hilt, existing Phase 0 visualkit (`SWGradient.auroraGlow`, `Modifier.innerHighlight`, `SWMotion.breathCycleMs`, `ChevronTrail`, `SectionLabel`, `withInfiniteAnimationFrameMillis` pattern from Phase 2), existing Phase 1 morph state (`LocalHeroBackdropController`).

---

## File Map

**Created:**
- `app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/PresetAuraOrb.kt` — glassy disc + inner particles + active breathing shadow
- `app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/AuraParticles.kt` — full-screen 24-mote overlay for Player screen
- `app/src/main/java/com/lizhi1026/sleepwhisper/features/player/AmbientSync.kt` — `HeroBackdropController.syncToPlayer(...)` extension
- `app/src/test/java/com/lizhi1026/sleepwhisper/model/AudioPresetAuraTest.kt` — catalog-completeness assertions

**Modified:**
- `app/src/main/java/com/lizhi1026/sleepwhisper/model/AudioPreset.kt` — add `AuraColors` data class, `auraColors` field with `@Transient`, populate all 12 bundled presets
- `app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/GlassCard.kt` — add optional `surfaceTintOverride: Color? = null` parameter
- `app/src/main/java/com/lizhi1026/sleepwhisper/features/player/PlayerScreen.kt` — `PresetRow` uses `PresetAuraOrb` + `surfaceTintOverride`, AudioWaveform uses preset.auraColors.top, "›" → `ChevronTrail`, DurationChip active uses `auroraGlow`, drop local `SectionLabel` in favor of shared, add `LaunchedEffect(currentPreset)` ambient sync, overlay `AuraParticles`
- `app/src/main/java/com/lizhi1026/sleepwhisper/features/home/HomeScreen.kt` — extend `LaunchedEffect(hour)` to `LaunchedEffect(hour, currentPreset)` and call `syncToPlayer(currentPreset, eveningHint)`

**Deleted:**
- (none)

---

## Conventions

- Commit messages: `area: short imperative summary` (`model:`, `visualkit:`, `player:`, `home:`)
- After each task, run `./gradlew :app:compileDebugKotlin` to verify before committing
- Visual changes verified via `@Preview` in Android Studio; final smoke on real device in Task 9
- Every animated component honors `LocalReduceMotion.current` AND degrades on low-RAM devices via `ActivityManager.isLowRamDevice()`
- HEAD at plan-write time: `661ac3e` (Phase 3 spec committed). Phase 3 builds on top of all Phase 0/1/2 commits.

---

## Task 1 — `AuraColors` data class + `AudioPreset.auraColors` field + catalog-completeness test

**Files:**
- Modify: `app/src/main/java/com/lizhi1026/sleepwhisper/model/AudioPreset.kt`
- Create: `app/src/test/java/com/lizhi1026/sleepwhisper/model/AudioPresetAuraTest.kt`

- [ ] **Step 1: Write the failing test first**

Create `app/src/test/java/com/lizhi1026/sleepwhisper/model/AudioPresetAuraTest.kt`:

```kotlin
package com.lizhi1026.sleepwhisper.model

import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioPresetAuraTest {

    @Test fun `every bundled preset has a non-default AuraColors`() {
        AudioPreset.bundled.forEach { preset ->
            assertNotEquals(
                "Preset ${preset.id} is still using the default AuraColors — populate its auraColors field.",
                AuraColors.DEFAULT,
                preset.auraColors
            )
        }
    }

    @Test fun `all bundled preset mid colors are unique`() {
        val mids = AudioPreset.bundled.map { it.auraColors.mid }
        val uniqueMids = mids.toSet()
        assertTrue(
            "Found duplicate mid colors in AudioPreset.bundled: ${mids.size} total but only ${uniqueMids.size} unique. " +
                "Each preset's mid color drives the cross-screen ambient — they must be visually distinct.",
            mids.size == uniqueMids.size
        )
    }

    @Test fun `every preset's top color is brighter than its bottom color`() {
        AudioPreset.bundled.forEach { preset ->
            val top = preset.auraColors.top
            val bottom = preset.auraColors.bottom
            val topLuma = top.red + top.green + top.blue
            val bottomLuma = bottom.red + bottom.green + bottom.blue
            assertTrue(
                "Preset ${preset.id}: top color (luma=$topLuma) is not brighter than bottom (luma=$bottomLuma). " +
                    "auraColors swap detected — top should be the brightest highlight, bottom the darkest room color.",
                topLuma > bottomLuma
            )
        }
    }
}
```

- [ ] **Step 2: Run the test — expect failure**

```bash
./gradlew :app:testDebugUnitTest --tests 'com.lizhi1026.sleepwhisper.model.AudioPresetAuraTest'
```

Expected: FAIL — `AuraColors` unresolved.

- [ ] **Step 3: Add `AuraColors` and `auraColors` field to AudioPreset.kt**

Open `app/src/main/java/com/lizhi1026/sleepwhisper/model/AudioPreset.kt`. Add the imports near the top:

```kotlin
import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Transient
```

Add the `AuraColors` data class outside the `AudioPreset` class (e.g. above its declaration, in the same file). The `Color` type carries no `@Serializable` annotation, so we mark every field `@Transient` to keep kotlinx-serialization happy:

```kotlin
/**
 * 3-color aura for an audio preset:
 *  - [top]    brightest highlight; used as AudioWaveform color and orb core
 *  - [mid]    primary tone; the one pushed to HeroBackdropController.ambient
 *             so the AuroraBackdrop drifts toward this color across screens
 *  - [bottom] deepest "room" color; the orb's outer ring
 *
 * Marked @Transient because Compose `Color` is not @Serializable. The
 * bundled catalog is a hardcoded list and is never serialized at runtime,
 * but @Transient defends against future regressions if a feature tries
 * to write a whole AudioPreset to DataStore.
 */
@kotlinx.serialization.Serializable
data class AuraColors(
    @Transient val top: Color = Color(0xFFB8A4FF),
    @Transient val mid: Color = Color(0xFF4D3C7A),
    @Transient val bottom: Color = Color(0xFF0F0C1E)
) {
    companion object {
        /** WARNING: returned only if a preset forgot to populate auraColors —
         *  catch via AudioPresetAuraTest before merging. */
        val DEFAULT = AuraColors()
    }
}
```

Add the `auraColors` field to `AudioPreset` (as the last field before the closing brace of the data class declaration):

```kotlin
@Serializable
data class AudioPreset(
    val id: String,
    val nameKey: String,
    val category: AudioCategory,
    val recommendedAgeMinMonths: Int,
    val recommendedAgeMaxMonths: Int,
    val fileBundleName: String,
    val defaultDurationSeconds: Int = 1800,
    val loop: Boolean = true,
    val license: String = "CC0",
    val iconName: String,
    @Transient
    val auraColors: AuraColors = AuraColors.DEFAULT
) {
    // ... existing AudioCategory enum and companion object ...
}
```

- [ ] **Step 4: Populate all 12 bundled presets**

In the `bundled` list, add the `auraColors = AuraColors(...)` argument to each constructor call. Use these exact colors:

```kotlin
// Womb (preset_womb_001)
auraColors = AuraColors(top = Color(0xFFC46B8C), mid = Color(0xFF5B4880), bottom = Color(0xFF1A0F1E))

// Heartbeat (preset_heartbeat_001)
auraColors = AuraColors(top = Color(0xFFC44E5C), mid = Color(0xFF4E1A22), bottom = Color(0xFF1A0A0E))

// Dryer (preset_dryer_001)
auraColors = AuraColors(top = Color(0xFFC99B5A), mid = Color(0xFF6B4A20), bottom = Color(0xFF1A1108))

// Vacuum (preset_vacuum_001)
auraColors = AuraColors(top = Color(0xFF6E6A66), mid = Color(0xFF38362F), bottom = Color(0xFF14130F))

// White noise (preset_white_001)
auraColors = AuraColors(top = Color(0xFFFAFAFA), mid = Color(0xFFECE9E2), bottom = Color(0xFF1F222A))

// Brown noise (preset_brown_001)
auraColors = AuraColors(top = Color(0xFF8A6850), mid = Color(0xFF3E2A1F), bottom = Color(0xFF1A100A))

// Pink noise (preset_pink_001)
auraColors = AuraColors(top = Color(0xFFE6B6B8), mid = Color(0xFFB85F77), bottom = Color(0xFF1A0F12))

// Rain (preset_rain_001)
auraColors = AuraColors(top = Color(0xFFB7C3CC), mid = Color(0xFF4A5A6E), bottom = Color(0xFF0F1620))

// Ocean (preset_ocean_001)
auraColors = AuraColors(top = Color(0xFF2E7A86), mid = Color(0xFF1F4F73), bottom = Color(0xFF051A26))

// Fan (preset_fan_001)
auraColors = AuraColors(top = Color(0xFFB5BFC8), mid = Color(0xFF4F5C68), bottom = Color(0xFF0E1418))

// Lullaby 1 (preset_lullaby_001)
auraColors = AuraColors(top = Color(0xFFB8A4FF), mid = Color(0xFF4D3C7A), bottom = Color(0xFF0F0C1E))

// Lullaby 2 (preset_lullaby_002)
auraColors = AuraColors(top = Color(0xFFFFB088), mid = Color(0xFF8C4A26), bottom = Color(0xFF1A0E08))
```

**Note on Rain and Ocean:** the spec table lists Rain's top as `#4A5A6E` (slate) and mid as `#B7C3CC` (light silver) — but a top that's darker than the mid breaks the "top is highlight" invariant and the third unit test ("top brighter than bottom") would still pass while the orb gradient inverts visually. I'm flipping them here: Rain's top = `#B7C3CC` (light silver), mid = `#4A5A6E` (slate); same fix for Ocean: top = `#2E7A86` (teal), mid = `#1F4F73` (deeper blue). The spec table's "top/mid/bottom" header is the contract; the values get reshuffled to honor brightness ordering.

- [ ] **Step 5: Run the test — expect pass**

```bash
./gradlew :app:testDebugUnitTest --tests 'com.lizhi1026.sleepwhisper.model.AudioPresetAuraTest'
```

Expected: PASS (3/3).

- [ ] **Step 6: Run the full test suite as sanity**

```bash
./gradlew :app:testDebugUnitTest
```

Expected: BUILD SUCCESSFUL. All prior phase tests still pass.

- [ ] **Step 7: Compile the full app**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/lizhi1026/sleepwhisper/model/AudioPreset.kt \
        app/src/test/java/com/lizhi1026/sleepwhisper/model/AudioPresetAuraTest.kt
git commit -m "model: AudioPreset gains auraColors for 12 presets with catalog-completeness tests"
```

---

## Task 2 — `PresetAuraOrb.kt`

**Files:**
- Create: `app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/PresetAuraOrb.kt`

A glassy disc with an inner-highlight crescent, 4 internal drifting particles, and an active-state breathing outer shadow.

- [ ] **Step 1: Create the file**

```kotlin
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
    val nx: Float,             // 0..1 within orb
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
 *   1. Radial-gradient disc (aura.bottom → aura.mid → aura.top center)
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

    // Active-state breathing shadow alpha.
    val shadowAlpha = if (isActive && !reduce) {
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

    // Particle state — generated once, position field cached.
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
                    radius = with(density) { (sizeDp.toPx() * 0.7f) }
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
```

- [ ] **Step 2: Compile**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/PresetAuraOrb.kt
git commit -m "visualkit: add PresetAuraOrb (glassy preset-colored disc with breathing shadow)"
```

---

## Task 3 — `AmbientSync.kt` extension

**Files:**
- Create: `app/src/main/java/com/lizhi1026/sleepwhisper/features/player/AmbientSync.kt`

A single extension function on `HeroBackdropController` that resolves the active-preset-or-fallback ambient color.

- [ ] **Step 1: Create the file**

```kotlin
package com.lizhi1026.sleepwhisper.features.player

import androidx.compose.ui.graphics.Color
import com.lizhi1026.sleepwhisper.core.visualkit.HeroBackdropController
import com.lizhi1026.sleepwhisper.model.AudioPreset

/**
 * Push the active preset's mid-aura color to the backdrop. If no preset is
 * active, fall back to the caller's contextual color (e.g. Home's evening
 * time-of-day hint). Pass null for [fallback] when there is no fallback
 * (e.g. on the Player screen itself).
 *
 * Priority (highest first):
 *   1. currentPreset.auraColors.mid
 *   2. fallback
 *   3. null (backdrop uses default Aurora palette)
 */
fun HeroBackdropController.syncToPlayer(
    currentPreset: AudioPreset?,
    fallback: Color?
) {
    setAmbient(currentPreset?.auraColors?.mid ?: fallback)
}
```

- [ ] **Step 2: Compile**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/lizhi1026/sleepwhisper/features/player/AmbientSync.kt
git commit -m "player: add HeroBackdropController.syncToPlayer extension"
```

---

## Task 4 — `GlassCard` gains `surfaceTintOverride`

**Files:**
- Modify: `app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/GlassCard.kt`

Backward-compatible parameter so non-active PresetRows can mix 12% ambient into their elevated surface for a "reflected light" feel.

- [ ] **Step 1: Open GlassCard.kt and locate the signature**

```bash
grep -n "fun GlassCard" app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/GlassCard.kt
```

Current signature ends with `content: @Composable BoxScope.() -> Unit`. We're inserting one parameter immediately before `content`.

- [ ] **Step 2: Add the parameter and use it**

Open `app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/GlassCard.kt`.

Change the signature from:

```kotlin
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = SWRadius.lg,
    contentPadding: PaddingValues = PaddingValues(SWSpacing.md),
    elevation: ElevationLevel = ElevationLevel.SOFT,
    hero: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
```

To:

```kotlin
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = SWRadius.lg,
    contentPadding: PaddingValues = PaddingValues(SWSpacing.md),
    elevation: ElevationLevel = ElevationLevel.SOFT,
    hero: Boolean = false,
    surfaceTintOverride: Color? = null,
    content: @Composable BoxScope.() -> Unit
) {
```

Find the existing line where the elevated surface is filled:

```kotlin
            .background(SWColor.surfaceElevated(scheme))
```

Replace with:

```kotlin
            .background(surfaceTintOverride ?: SWColor.surfaceElevated(scheme))
```

The `Color` import is already present.

- [ ] **Step 3: Compile**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL. All existing GlassCard call sites (Phase 0/1/2) compile unchanged because `surfaceTintOverride` defaults to null.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/GlassCard.kt
git commit -m "visualkit: GlassCard gains optional surfaceTintOverride parameter"
```

---

## Task 5 — `AuraParticles.kt`

**Files:**
- Create: `app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/AuraParticles.kt`

Full-screen overlay of 24 slowly drifting motes tinted by the current ambient color. Used on the Player screen when a preset is active.

- [ ] **Step 1: Create the file**

```kotlin
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
```

- [ ] **Step 2: Compile**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/AuraParticles.kt
git commit -m "visualkit: add AuraParticles full-screen overlay"
```

---

## Task 6 — PlayerScreen: PresetRow uses PresetAuraOrb + ambient surfaceTint + ChevronTrail + waveform color + DurationChip auroraGlow

**Files:**
- Modify: `app/src/main/java/com/lizhi1026/sleepwhisper/features/player/PlayerScreen.kt`

The big visual task. Replace flat icon circles with `PresetAuraOrb`, give non-active rows a 12% ambient tint, swap the trailing "›" for `ChevronTrail`, color the active row's waveform with the preset's top aura color, switch `DurationChip` active background to `auroraGlow`, drop the local `SectionLabel` in favor of the shared one.

- [ ] **Step 1: Add imports**

In `app/src/main/java/com/lizhi1026/sleepwhisper/features/player/PlayerScreen.kt`, add:

```kotlin
import androidx.compose.runtime.derivedStateOf
import com.lizhi1026.sleepwhisper.core.visualkit.LocalHeroBackdropController
import com.lizhi1026.sleepwhisper.core.visualkit.components.ChevronTrail
import com.lizhi1026.sleepwhisper.core.visualkit.components.PresetAuraOrb
import com.lizhi1026.sleepwhisper.core.visualkit.components.SectionLabel as SharedSectionLabel
import com.lizhi1026.sleepwhisper.core.visualkit.components.innerHighlight
import androidx.compose.ui.graphics.lerp
```

- [ ] **Step 2: Remove the local SectionLabel**

Find the existing local private composable:

```kotlin
@Composable
private fun SectionLabel(text: String) {
    val scheme = LocalSWScheme.current
    BasicText(
        text = text,
        style = SWFont.labelMD().copy(
            color = SWColor.textSecondary(scheme),
            letterSpacing = 1.2.sp
        )
    )
}
```

Delete it entirely.

- [ ] **Step 3: Read the current ambient state for surfaceTint computation**

Inside `fun PlayerScreen(...)`, immediately after the existing observers (`val rest = vm.allPresets - rec.toSet()`), add:

```kotlin
    val heroBackdrop = LocalHeroBackdropController.current
    val ambient by remember(heroBackdrop) {
        derivedStateOf { heroBackdrop.ambient }
    }
```

- [ ] **Step 4: Rewrite the two `SectionLabel(...)` call sites and pass `ambient` to PresetRow**

In the `LazyColumn { ... }` block, find the two `SectionLabel(stringResource(...))` calls and change them to `SharedSectionLabel(stringResource(...))`. Also add `ambient` as the new third positional argument to each `PresetRow(...)` call:

```kotlin
                if (rec.isNotEmpty()) {
                    item {
                        SharedSectionLabel(stringResource(R.string.player_section_recommended, ageMonths))
                    }
                    items(rec, key = { it.id }) { p ->
                        PresetRow(p, currentId == p.id, ambient) { onTap(p, minutesNow, vm) }
                    }
                }

                item {
                    SharedSectionLabel(stringResource(R.string.player_section_all))
                }
                items(rest, key = { it.id }) { p ->
                    PresetRow(p, currentId == p.id, ambient) { onTap(p, minutesNow, vm) }
                }
```

- [ ] **Step 5: Update PresetRow signature and body**

Find the existing `PresetRow` private composable. Replace its entire body with:

```kotlin
@Composable
private fun PresetRow(
    preset: AudioPreset,
    isPlaying: Boolean,
    ambient: Color?,
    onTap: () -> Unit
) {
    val scheme = LocalSWScheme.current
    // Non-active rows blend 12% of the current ambient into their elevated
    // surface for a "reflected light" feel. Active row keeps the default
    // elevated surface so its orb is the visual anchor.
    val tint = if (!isPlaying && ambient != null) {
        lerp(SWColor.surfaceElevated(scheme), ambient, 0.12f)
    } else null

    GlassCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onTap),
        cornerRadius = 18.dp,
        contentPadding = PaddingValues(SWSpacing.md),
        surfaceTintOverride = tint
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SWSpacing.md)) {
            PresetAuraOrb(
                aura = preset.auraColors,
                isActive = isPlaying,
                sizeDp = 48.dp
            )
            Column(modifier = Modifier.weight(1f)) {
                BasicText(
                    text = stringResource(audioPresetNameKey(preset.nameKey)),
                    style = SWFont.titleMD().copy(color = SWColor.textPrimary(scheme))
                )
                BasicText(
                    text = stringResource(
                        R.string.player_preset_agerange,
                        preset.recommendedAgeMinMonths,
                        preset.recommendedAgeMaxMonths
                    ),
                    style = SWFont.labelSM().copy(color = SWColor.textSecondary(scheme))
                )
            }
            if (isPlaying) {
                AudioWaveform(
                    modifier = Modifier
                        .width(36.dp)
                        .height(22.dp),
                    isPlaying = true,
                    color = preset.auraColors.top,
                    barCount = 8
                )
            } else {
                ChevronTrail()
            }
        }
    }
}
```

The icon-loading code (`getIdentifier(preset.iconName, ...)`) is gone — the orb has replaced the icon entirely, the preset's identity is now color-coded rather than icon-coded.

- [ ] **Step 6: Update DurationChip active gradient**

Find the `DurationChip` composable. Find the line:

```kotlin
            .background(
                if (active) SWGradient.primary(scheme)
                else androidx.compose.ui.graphics.Brush.linearGradient(
```

Change `SWGradient.primary(scheme)` to `SWGradient.auroraGlow(scheme)`:

```kotlin
            .background(
                if (active) SWGradient.auroraGlow(scheme)
                else androidx.compose.ui.graphics.Brush.linearGradient(
```

- [ ] **Step 7: Compile**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/lizhi1026/sleepwhisper/features/player/PlayerScreen.kt
git commit -m "player: PresetRow uses PresetAuraOrb with 12% ambient reflected light and ChevronTrail"
```

---

## Task 7 — PlayerScreen pushes ambient + overlays AuraParticles

**Files:**
- Modify: `app/src/main/java/com/lizhi1026/sleepwhisper/features/player/PlayerScreen.kt`

Wire the ambient sync: a `LaunchedEffect(currentPreset)` that calls `heroBackdrop.syncToPlayer(currentPreset, fallback = null)`. Overlay `AuraParticles` on top of the Player screen content when a preset is active.

- [ ] **Step 1: Add imports**

```kotlin
import com.lizhi1026.sleepwhisper.features.player.syncToPlayer
import com.lizhi1026.sleepwhisper.core.visualkit.components.AuraParticles
import androidx.compose.runtime.LaunchedEffect
```

(Some may already be present from Task 6.)

- [ ] **Step 2: Add the LaunchedEffect that syncs ambient**

Inside `fun PlayerScreen(...)`, after the existing `val ambient by remember { ... }` line added in Task 6, add:

```kotlin
    LaunchedEffect(current, embedded) {
        // When playing in the standalone Player tab, push the preset's mid color
        // to the backdrop. When embedded as the mini sheet inside the Sleeping
        // screen, ALSO push — the ambient persists after the sheet closes,
        // taking effect on Home/Trends/Settings next time the user visits.
        heroBackdrop.syncToPlayer(currentPreset = current, fallback = null)
    }
```

Note: We key on `current` (the LiveData-observed preset) plus `embedded` so the effect re-fires when the user picks a different preset, when playback stops (`current` becomes null), or when the same composable is used in both standalone and embedded modes.

- [ ] **Step 3: Overlay AuraParticles**

Inside the outer `Box(modifier = Modifier.fillMaxSize()) { ... }` block of `PlayerScreen`, AFTER the existing inner `Column(...)` block (which holds the `LazyColumn`), and BEFORE the closing `}` of the outer Box, add:

```kotlin
        // Aura particles overlay — visible only when a preset is active.
        // Drawn on top of the LazyColumn but transparent to pointer input so
        // taps fall through to the rows.
        if (current != null) {
            AuraParticles(
                tint = current!!.auraColors.top,
                modifier = Modifier.fillMaxSize()
            )
        }
```

- [ ] **Step 4: Compile**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/lizhi1026/sleepwhisper/features/player/PlayerScreen.kt
git commit -m "player: sync preset ambient to backdrop and overlay AuraParticles"
```

---

## Task 8 — HomeScreen merges ambient sync with time-of-day fallback

**Files:**
- Modify: `app/src/main/java/com/lizhi1026/sleepwhisper/features/home/HomeScreen.kt`

Extend the Phase 1 `LaunchedEffect(hour)` to `LaunchedEffect(hour, currentPreset)` and call `syncToPlayer(currentPreset, eveningHint)` so the preset's mid color overrides the time-of-day hint when audio is playing, and the time-of-day hint resumes when playback stops.

- [ ] **Step 1: Add the import**

In `app/src/main/java/com/lizhi1026/sleepwhisper/features/home/HomeScreen.kt`, add:

```kotlin
import com.lizhi1026.sleepwhisper.features.player.syncToPlayer
```

- [ ] **Step 2: Replace the existing LaunchedEffect(hour)**

Find the existing block:

```kotlin
    val heroBackdrop = LocalHeroBackdropController.current
    LaunchedEffect(hour) {
        // Evening (19:00–05:59) — tint backdrop dusk-orange whisper. Otherwise clear.
        heroBackdrop.setAmbient(
            if (hour >= 19 || hour < 6) Color(0xFFFFB088) else null
        )
    }
```

Replace with:

```kotlin
    val heroBackdrop = LocalHeroBackdropController.current
    LaunchedEffect(hour, currentPreset) {
        // Priority: active preset's mid color > evening time-of-day hint > null.
        // When a preset is playing, its aura wins; when it stops, the time-of-day
        // hint takes over again automatically.
        val eveningHint = if (hour >= 19 || hour < 6) Color(0xFFFFB088) else null
        heroBackdrop.syncToPlayer(currentPreset = currentPreset, fallback = eveningHint)
    }
```

`currentPreset` is already observed via `val currentPreset by vm.currentPreset.observeAsState(null)` near the top of `HomeScreen` (existing Phase 0/1 code).

- [ ] **Step 3: Compile**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/lizhi1026/sleepwhisper/features/home/HomeScreen.kt
git commit -m "home: merge preset ambient sync with time-of-day fallback"
```

---

## Task 9 — Full smoke walkthrough + screenshots

**Files:** none — verification only.

- [ ] **Step 1: Build and install**

```bash
./gradlew :app:installDebug
adb shell am start -n com.lizhi1026.sleepwhisper/.app.MainActivity
```

- [ ] **Step 2: Verify the test suite is green**

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

Expected: BUILD SUCCESSFUL. AudioPresetAuraTest 3/3 plus all prior phase tests pass.

- [ ] **Step 3: Manual smoke**

Open the app. Confirm:
- **Open Player tab → tap "Womb"** → orb begins breathing, AuroraBackdrop drifts to a powder-violet wash over ~600ms; 24 motes appear as faint pink dots drifting across the screen; the row's `AudioWaveform` is now pink-tinted
- **Switch to Home tab** → backdrop is still powder-violet (ambient persists)
- **Switch to Trends, then Settings** → backdrop still powder-violet across all three tabs
- **Return to Player → tap "Ocean"** → orb on Ocean row begins breathing, Womb orb stops breathing; backdrop drifts to teal over ~600ms; motes recolor to teal/silver
- **Wait for the playback timer to expire** (or tap stop) → particles fade out, backdrop drifts back to the evening tint (or null if it's daytime)
- **In Sleeping screen** (start a sleep with audio playing): NightSkyCanvas does NOT pick up the ambient — Sleeping's sky stays time-of-day-driven (Phase 2 intentional)
- **In Sleeping screen → tap PlayerStatusCard → mini-Player-sheet opens → tap a different preset** → sheet's PlayerScreen reflects the new active row; on dismissing the sheet and stopping the sleep, return to Home — backdrop already shows the new preset's ambient

- [ ] **Step 4: Reduce-motion smoke**

```bash
adb shell settings put secure transition_animation_scale 0
adb shell settings put secure window_animation_scale 0
adb shell settings put secure animator_duration_scale 0
```

Relaunch app. Tap a preset on Player. Confirm:
- Orb breathing shadow holds at ~45% (not pulsing)
- Internal orb particles static at their seeded positions
- Full-screen AuraParticles static
- Backdrop ambient change is instantaneous (no 600ms drift) — already handled by Phase 0 AuroraBackdrop's reduce-motion path

Restore:
```bash
adb shell settings put secure transition_animation_scale 1
adb shell settings put secure window_animation_scale 1
adb shell settings put secure animator_duration_scale 1
```

- [ ] **Step 5: Capture screenshots**

```bash
mkdir -p docs/superpowers/screenshots/2026-05-21-phase-3
# Player screen with Womb active
adb exec-out screencap -p > docs/superpowers/screenshots/2026-05-21-phase-3/player-womb-active.png
# Home screen with Ocean ambient persisted
adb exec-out screencap -p > docs/superpowers/screenshots/2026-05-21-phase-3/home-ocean-ambient.png
# Trends with Womb ambient
adb exec-out screencap -p > docs/superpowers/screenshots/2026-05-21-phase-3/trends-womb-ambient.png
```

- [ ] **Step 6: Commit screenshots**

```bash
git add docs/superpowers/screenshots/2026-05-21-phase-3/
git commit -m "phase-3: verification screenshots across preset ambient persistence"
```

---

## Phase 3 Done When

- All 9 tasks committed
- `./gradlew :app:testDebugUnitTest :app:assembleDebug` green
- `AudioPresetAuraTest` 3/3 pass
- Selecting a preset on Player drifts the AuroraBackdrop to its mid color over ~600ms
- Ambient persists across Home / Trends / Settings until playback stops
- Sleeping screen's NightSkyCanvas correctly ignores ambient (Phase 2 invariant preserved)
- PresetAuraOrb renders with per-preset colors and breathing-shadow on the active row
- AuraParticles overlay visible only when a preset is active on Player
- Reduce-motion freezes particles and snaps ambient transitions
- Low-RAM device skips AuraParticles but keeps orbs

## Out of Scope (deferred)

- Trends + Settings visual polish → Phase 4
- Onboarding + Welcome → Phase 5
- `Starfield.kt` deletion → Phase 5 (still referenced by Onboarding screens)
- Per-preset descriptor copy ("Pink & deep", "Cool & wide") — placeholder uses existing age-range string; copy decisions left to product owner
