# Phase 2 — Sleeping Immersive Night Sky

**Date:** 2026-05-20
**Status:** Approved (pending user review)
**Parent spec:** [Aurora Redesign Master](2026-05-20-aurora-redesign-master-design.md)

Phase 2 of the Aurora redesign. Implements **Hero #2**: the Sleeping screen as a living window onto the night, with a time-of-day-aware multi-layer canvas that progresses through the actual night as the user watches.

---

## 1. Scope

### In scope

- `NightSkyCanvas` — new composable, the 5-layer composition (aurora gradient mesh, breath-twinkle stars, drifting stars, meteor showers, cloud wisps)
- `TimeOfDayPalette` — pure helper mapping `LocalTime` → palette struct, with 60-minute crossfade interpolation between stops
- Sleeping screen layout redesign — apply Aurora tokens, hero timer at 64sp `displayLGTabular`, aurora-recolored `LiquidProgressRing` (280dp), repositioned caption + metadata, bottom-anchored now-playing chip
- Replace the Phase 1 `Starfield` placeholder in Sleeping with `NightSkyCanvas` (the canvas itself fades in during the morph from Home, driven by `morphProgress`)
- New interactions: tap-to-dim timer (300ms tween to 24% opacity), long-press 600ms center to reveal stop control, tap now-playing chip → mini Player bottom sheet
- Delete `Starfield.kt` (subsumed by NightSkyCanvas) — final task in Phase 2

### Out of scope (explicit phase boundaries)

| Item | Phase |
|---|---|
| `PresetAuraOrb` (audio-color sync on Player) | Phase 3 — NowPlaying chip stays at Phase 1 treatment |
| Sleep-stage-aware sky tinting (e.g. "approaching wake" warms the sky) | Future enhancement, not Phase 2 |
| Constellation overlays, wish-on-meteor, audio-reactive elements | Out of scope |
| Changes to `SleepingViewModel` business logic | View layer only |
| Trends, Settings, Onboarding visual changes | Phases 4 / 5 |

### Constraints inherited

- Sleeping screen forces `LocalSWScheme = DARK` (existing behavior, preserved)
- Hero #1 morph from Phase 1 stays intact — `morphProgress` drives NightSkyCanvas opacity during entry
- Reverse morph from Phase 1 stays intact — `endSleepMorph` drives the opacity fade out

---

## 2. Time-of-Day Palette

Real wall-clock 1:1 progression. The night you see IS the night outside. Tradeoff acknowledged: 30-minute naps appear visually static; 8-hour overnights show the full progression. For a sleep app, this is the right call.

### Palette stops

```kotlin
data class SkyPalette(val bottom: Color, val top: Color, val grainDensity: Float = 0.08f)

object TimeOfDayPalette {
    private val STOPS = listOf(
        06.5f to SkyPalette(Color(0xFF1F2540), Color(0xFFFFA98E)),  // morning hand-back
        19.0f to SkyPalette(Color(0xFF1A2540), Color(0xFFFFB088)),  // dusk
        21.0f to SkyPalette(Color(0xFF0F1B30), Color(0xFFB8A4FF)),  // evening
        24.0f to SkyPalette(Color(0xFF050912), Color(0xFF1F2848)),  // night (mod 24 → 0)
        27.0f to SkyPalette(Color(0xFF050912), Color(0xFF1F2848), grainDensity = 0.12f), // 03:00 dead-night
        29.0f to SkyPalette(Color(0xFF1F2540), Color(0xFFFFA98E))   // 05:00 dawn
    )

    fun forTime(t: LocalTime): SkyPalette {
        val h = t.hour + t.minute / 60f
        // Wrap to the 06.5..29 (i.e. 06:30 to 05:00 next day) range
        val effective = if (h < 6.5f) h + 24f else h
        // Find surrounding stops; lerp linearly between them
        return interpolateBetweenStops(effective)
    }
}
```

Linear interpolation between adjacent stops handles every wall-clock time. Tested at sample times to ensure no jarring hue between any two adjacent stops.

### Interpolation samples (verified by unit test)

| Sample time | Bottom | Top | Notes |
|---|---|---|---|
| 19:00 dusk (start) | `#1A2540` | `#FFB088` | exact stop |
| 20:00 mid-dusk | `#152030` | `#DCAFA0` | 50% between dusk and evening |
| 21:00 evening | `#0F1B30` | `#B8A4FF` | exact stop |
| 22:30 evening→night | `#0A1421` | `#6EA0CB` | 50% between |
| 00:00 night | `#050912` | `#1F2848` | exact stop |
| 03:00 dead-night | `#050912` | `#1F2848` | exact, grain density 1.5× |
| 05:00 dawn | `#1F2540` | `#FFA98E` | exact stop |
| 06:00 dawn→morning | `#1F2540` | `#FFA98E` | held at dawn (still morning-prep) |

---

## 3. NightSkyCanvas Composition

Single `Canvas` composable drawing 5 layers in z-order. The composable signature:

```kotlin
@Composable
fun NightSkyCanvas(
    modifier: Modifier = Modifier,
    morphProgress: Float = 1f,           // fade-in driver from Phase 1 hero morph
    timeProvider: () -> LocalTime = { LocalTime.now() }  // injectable for tests
)
```

The function is wrapped in `Modifier.alpha(morphProgress)` so during the Hero #1 morph the entire canvas fades in. Inside the canvas, all layers respect `LocalReduceMotion.current`.

### Layer 1 — Aurora gradient mesh (the sky)

```
- Render: vertical linear gradient between palette.bottom and palette.top
- Palette: TimeOfDayPalette.forTime(now). Polled every 60 seconds via produceState.
- Extra: two diagonal aurora ribbons drift across the gradient (reuse the
  AuroraBackdrop drifting logic from Phase 0 but scoped to night palettes)
- Reduce-motion: ribbons frozen at phase 0, gradient still selected by current time
```

### Layer 2 — Static stars with breath-twinkle (~80 stars)

```
- Star positions: pre-generated once via Random(seed = 0xA110A4) into a remember { ... } list
- Each star: (x, y) in 0..1 normalized, baseSize 0.4..1.6 dp, twinkle phase offset, cycle 3..5s
- Per-frame alpha: 0.3 + 0.6 * (sin(t/cycleDur + phase) * 0.5 + 0.5)
- THROTTLED to ~10 fps via withFrameNanos and conditional invalidation:
    `if (frameTimeNanos - lastFrameNanos > 100_000_000L) { update; lastFrameNanos = ... }`
- Reduce-motion: render once at base alpha, no animation
```

### Layer 3 — Drifting stars (~6 stars)

```
- Same model as Layer 2 but with horizontal velocity = 0.5 dp/sec
- Brighter: +0.2 to base alpha
- When x > 1.05 wraps to x = -0.05 with new random y, alpha 0 → base over 800ms (no pop)
- Reduce-motion: frozen at last position, no drift, no wrap
```

### Layer 4 — Meteor showers (rare)

```
- Spawn cadence: every Random.nextLong(90_000..180_000) ms (real time)
- Max 1 meteor visible at any moment
- Each meteor: 600ms total life
    - Tapered streak: 16dp head → 2dp tail over 60dp length
    - Random angle in [-30°, +30°] from horizontal
    - Spawned at random y in 0.1..0.6, x = -0.05 (off-screen left)
    - Travels to x = 1.1 with full visibility for 400ms, fade to 0 over last 200ms
- Reduce-motion: disabled entirely
- Low-RAM device: disabled
```

### Layer 5 — Atmosphere / cloud wisps

```
- Visible only when current hour is in 22..03 (deep dark hours)
- Max 2 wisps visible at once
- Each wisp: 2% opacity blurred ellipse (40dp x 12dp), horizontal drift 0.1 dp/sec
- Each lives ~120 seconds, fades over last 8 seconds, respawns at new random y
- Reduce-motion: disabled
- Low-RAM device: disabled
```

### Reduce-motion behavior summary

| Layer | Reduce-motion |
|---|---|
| 1 Gradient mesh | Static palette, ribbons frozen |
| 2 Static stars | Static at base alpha |
| 3 Drifting stars | Static at last position |
| 4 Meteor showers | Disabled |
| 5 Cloud wisps | Disabled |

Even fully reduced, the sky still LOOKS like a night sky — just doesn't move.

### Performance budget

| Metric | Target |
|---|---|
| Animation frame rate | 10 fps twinkles, 30 fps drifting/meteors |
| Total draw calls per frame | ≤ 120 |
| Persistent canvas memory | ≤ 5 KB |
| Battery delta over 10h Sleeping vs current screen | ≤ 3 percentage points |

---

## 4. Sleeping Screen Layout

```
┌─────────────────────────────────────────┐
│  · ·   ·       · ✦         ·            │
│      ·   · ✦       ·   ·                │ ← NightSkyCanvas full-screen
│   ·         ⟶☄          ·    ·          │
│           ·       ·                     │
│                                         │
│      sleeping since 9:47                │ ← serifItalic(17) @ 88% alpha textPrimary
│                                         │
│      ╭─ ─ ─ ─ ─ ─ ─ ─ ─ ╮               │
│     ╱                     ╲             │ ← 280dp LiquidProgressRing aurora-recolored
│    │                       │            │   fills toward recommended wake window
│    │   0 2 : 3 4 : 1 8    │            │ ← displayLGTabular 64sp moonlit-silver
│    │   ─────              │            │   tabular tnum, aurora-glow brush overlay 12%
│    │   Emma · 142 days     │            │ ← labelMD textTertiary
│     ╲                     ╱             │
│      ╰─ ─ ─ ─ ─ ─ ─ ─ ─ ╯               │
│                                         │
│            ◉ Womb  ≋≋≋                  │ ← bottom-anchored chip 44% opacity
│                                         │   tap → mini Player sheet
│   · ·     ·         ·   ·    ·          │
└─────────────────────────────────────────┘
```

### Timer

- **Size: 64sp `displayLGTabular`, not 96sp.** 96sp overflows the 280dp progress ring on Pixel-class devices. The hero moment is the LIVE NIGHT SKY behind the timer — not the timer's size. 64sp fits cleanly with breathing room.
- Per-digit rendering via existing `RollingNumber`, which gives 100ms directional vertical roll on the seconds tick (with the motion-blur effect from Phase 0)
- Color: `#ECF2F8` moonlit silver
- Aurora-glow brush mask (linear gradient from accent-orange to accent-purple) overlaid at 12% opacity via `Modifier.drawWithCache` — a faint shimmer over the silver

### LiquidProgressRing (280dp)

- Existing component from Phase 0 visualkit, recolored to use aurora palette
- Stroke gradient: starlight-purple → dusk-orange (top to bottom)
- Fill alpha: 18%
- Fills toward the recommended wake window remaining: as wake approaches, the ring drains
- Glanceable answer to "how close to wake?"

### Caption + metadata

- Above timer: `"sleeping since {hh:mm}"` in `serifItalic(17)`, textPrimary @ 88% alpha
- Below timer with hairline divider: `"{babyName} · {ageInDays} days"` in `labelMD` textTertiary

### Now-playing chip (bottom-anchored)

- Bottom-center, 44% opacity until interacted
- Compact pill: preset icon (24dp) + name (titleMD) + tiny `AudioWaveform` when playing
- Tap → opens mini Player as bottom sheet (see Interactions)
- Tap also raises opacity to 100% during sheet open, restores to 44% on sheet close

---

## 5. Interactions

| Gesture | Behavior |
|---|---|
| Single tap on canvas (not on chip, not on long-press target) | Dim timer + caption + metadata to 24% opacity. Sky stays at full alpha. Tap again to restore. 300ms tween (reduce-motion: 100ms). |
| Long-press 600ms in timer area center | Reveals existing `WakeButton` floating over the dimmed timer. Tap to confirm wake. Deliberate to avoid accidents at 3am. |
| Tap now-playing chip | Opens mini Player as a `ModalBottomSheet` at 65% screen height (default sheet behavior on Android). Sheet contains the existing `PlayerScreen` composable with `embedded = true` to suppress its own AuroraBackdrop. |
| Swipe down on player sheet | Dismisses sheet, returns to bare Sleeping. |
| Hardware back | Priority: 1. If timer dimmed → restore. 2. If player sheet open → dismiss. 3. Otherwise → no-op (don't accidentally exit). |

### Mini Player sheet implementation

- `ModalBottomSheet` from `androidx.compose.material3`
- Sheet height: 65% of screen
- Sheet content: `PlayerScreen(embedded = true)` — same composable used in the Player tab, with one new bool parameter
- `embedded = true` causes PlayerScreen to skip rendering its own AuroraBackdrop (sheet has its own background via Material3 defaults), and to use `transparent` surfaceColor so the sheet's natural surface shows through

PlayerScreen needs `embedded: Boolean = false` parameter added — small modification with default = false so all other call sites continue working unchanged.

---

## 6. Implementation Phasing — 10 Tasks

| Task | Output | Files |
|---|---|---|
| 1 | `TimeOfDayPalette.kt` + unit tests with sample-time interpolation correctness | new + new test |
| 2 | `NightSkyCanvas.kt` — Layer 1 only (gradient mesh + drifting ribbons); accepts morphProgress and timeProvider | new |
| 3 | NightSkyCanvas — Layer 2 (static twinkle stars with 10fps throttle, deterministic seed) | modify |
| 4 | NightSkyCanvas — Layer 3 (drifting stars with wrap-and-fade) | modify |
| 5 | NightSkyCanvas — Layer 4 (meteor showers, state holder, random spawn) | modify |
| 6 | NightSkyCanvas — Layer 5 (cloud wisps, 22-03 hour gate) + reduce-motion gates + low-RAM detection on all layers | modify |
| 7 | Sleeping screen redesign — swap Starfield → NightSkyCanvas; reposition timer + caption + metadata; recolor LiquidProgressRing; aurora-glow brush over timer digits | `SleepingScreen.kt` |
| 8 | Tap-to-dim interaction + long-press stop reveal + mini Player sheet (`PlayerScreen` gains `embedded: Boolean = false`) | `SleepingScreen.kt`, `PlayerScreen.kt` |
| 9 | Delete `Starfield.kt` (no longer used after Task 7) | delete |
| 10 | Manual smoke walkthrough — morph from Home, real-time wall clock observation, tap-dim, long-press stop, mini Player, reduce-motion, battery sanity, screenshots | (verification) |

### Sequencing

- Tasks 1-6 build NightSkyCanvas layer by layer. Each commit is independently reviewable and visually verifiable via Compose `@Preview` annotations.
- Task 7 wires it into Sleeping (visual changes propagate).
- Task 8 adds the new interactions.
- Task 9 cleans up the now-unused `Starfield`.
- Task 10 is the user smoke gate.

---

## 7. Risks & Mitigations

| ID | Risk | Mitigation |
|---|---|---|
| R1 | Battery — 8-hour overnight session with continuous animation | 10fps throttle on stars, meteors disabled most of the time (90-180s spawn), film grain pre-baked. Lifecycle gating already established in Phase 0. Battery sanity test in Task 10. |
| R2 | NightSkyCanvas frame skip / jank on low-end devices | All animation gated through `LocalReduceMotion` AND `ActivityManager.isLowRamDevice()`. On low-RAM devices, meteors and wisps disabled even without reduce-motion. |
| R3 | Time-of-day palette interpolation creates jarring hues mid-transition | Palette colors picked so any linear lerp between adjacent stops produces a sensible color. Unit test verifies interpolation at 7 sample times. |
| R4 | Reverse morph at e.g. 03:00 — backdrop has to switch from "dead night" to "Home dawn palette" instantly | Phase 1 `endSleepMorph` drives the morph animation; Home's AuroraBackdrop reads `LocalSWScheme = DAY` palette regardless of the sky's prior state. Clean handover. |
| R5 | Mini Player sheet reuses PlayerScreen — could double up backdrops | PlayerScreen gains `embedded: Boolean = false` parameter. When `embedded = true` (mini sheet mode), PlayerScreen skips its own AuroraBackdrop. Default unchanged. |
| R6 | Long-press 600ms feels slow OR feels like the gesture is lost | 600ms matches `SWMotion.longPressMs` (existing repo constant). Validated in Task 10 smoke. |
| R7 | Drifting stars wrap creates a visible "pop" at the edge | Wrap when x < -0.05 or > 1.05 (off-screen), respawn at the other edge with new random y, fade in alpha 0→base over 800ms. |
| R8 | Removing Starfield.kt (Task 9) might break a screen we forgot | Grep for `Starfield(` across all source files before deletion. If any reference remains, Task 9 is BLOCKED until that file is updated to use NightSkyCanvas. |

---

## 8. Verification

### Unit tests

- `TimeOfDayPaletteTest.kt`:
  - Interpolation correctness at each stop (returns exact palette)
  - Interpolation correctness mid-stop (returns blended palette)
  - Wrap-around: time 02:00 should be deep night (between 00:00 and 03:00 stops)
  - Boundary: time 06:30 returns morning palette
  - Boundary: time 06:00 returns dawn palette
  - Grain density: returns 0.12 at 03:00, 0.08 otherwise

### Instrumented Compose tests (`androidTest`)

- `NightSkyCanvasTest.kt` — renders without crash with various `timeProvider` inputs
- `NightSkyCanvasReduceMotionTest.kt` — with `LocalReduceMotion = true`, asserts meteor count stays 0, wisps stay 0
- `SleepingScreenLayoutTest.kt` — assert structural layout after redesign (timer + caption + metadata + chip all present)
- `SleepingTapDimTest.kt` — tap on canvas dims timer; tap again restores

### Manual smoke (Task 10)

- Trigger morph from Home, watch NightSkyCanvas fade in
- Sit on Sleeping for 30 minutes; observe sky color hasn't visibly changed (expected, real wall-clock)
- Manually set device time to 03:00 via Settings → Date; relaunch Sleeping; observe dead-night sky with denser grain
- Single-tap timer → dims to 24%, sky stays
- Single-tap again → restores
- Long-press timer center 600ms → wake control reveals
- Tap now-playing chip → mini Player sheet slides up to 65%
- Swipe down on sheet → dismisses
- Reduce-motion: re-test all the above; sky should be static
- Battery: 10-min Sleeping session, verify ≤3pp delta vs Home

### Visual receipts

Screenshots in `docs/superpowers/screenshots/2026-05-20-phase-2/`:
- Sleeping at 19:00 (dusk)
- Sleeping at 21:00 (evening)
- Sleeping at 00:00 (night)
- Sleeping at 03:00 (dead-night)
- Sleeping at 05:00 (dawn)
- Mini Player sheet open
- Reduce-motion variant

---

## 9. Locked Decisions

| Decision | Value |
|---|---|
| Time flow | Real wall-clock 1:1 |
| Star count | ~80 static + ~6 drifting |
| Star animation rate | 10fps throttled |
| Meteor frequency | every 90-180s, max 1 visible |
| Cloud wisps | 22:00-04:00 only, max 2 visible |
| Timer size | 64sp `displayLGTabular` (not 96sp — 280dp ring constraint) |
| Timer color | `#ECF2F8` moonlit silver + 12% aurora-glow brush overlay |
| Progress ring | 280dp, fills toward recommended wake remaining |
| Tap dim | Timer/caption to 24% opacity, sky unchanged, 300ms tween |
| Long-press stop | 600ms in timer center |
| Mini Player | `ModalBottomSheet` at 65% height, reuses `PlayerScreen(embedded = true)` |
| Hardware back priority | 1. Dim restore 2. Sheet dismiss 3. No-op |
| Reduce-motion | Sky static, no meteors/wisps, tap-dim 100ms |
| Low-RAM device | Same as reduce-motion |
| Starfield.kt | Deleted in Task 9 after NightSkyCanvas replaces it |
