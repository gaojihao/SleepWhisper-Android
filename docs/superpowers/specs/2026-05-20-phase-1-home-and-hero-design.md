# Phase 1 — Home + Sleep CTA Hero Morph

**Date:** 2026-05-20
**Status:** Approved (pending user review)
**Parent spec:** [Aurora Redesign Master](2026-05-20-aurora-redesign-master-design.md)

Phase 1 of the Aurora redesign. Applies the design system foundation (Phase 0) to the Home screen and implements **Hero #1**: the Sleep CTA → Sleeping screen cinematic morph.

---

## 1. Scope

### In scope

- Home screen visual redesign — apply Aurora tokens, refreshed typography, evolved components (`GlassCard hero=true`, `SoftButton HERO`, refined `QuickActionTile` internals, evolved `AudioWaveform`)
- **Hero #1 morph** — Sleep CTA → Sleeping cinematic transition, forward and reverse, ~1200ms each direction
- `MorphingHeroSurface.kt` — new file containing `Modifier.sleepHeroOrigin(...)` and `Modifier.sleepHeroDestination(...)` extensions
- Restructure `RootRoute` from `Crossfade` to `SharedTransitionLayout { AnimatedContent { ... } }`
- `AppStateContainer` additions: `morphProgress: Animatable<Float>`, `heroBackdrop: HeroBackdropController`, `beginSleepMorph()` / `endSleepMorph()` suspend APIs
- Wire time-of-day ambient hint through `LocalHeroBackdropController` so AuroraBackdrop tints warmer in evening hours

### Deferred (explicit phase boundaries)

| Item | Phase |
|---|---|
| `PresetAuraOrb` (audio-color sync) | Phase 3 — NowPlayingCard keeps icon-in-circle treatment with new tokens applied |
| `NightSkyCanvas` (full time-of-day sky) | Phase 2 — Phase 1 uses a placeholder Starfield-equivalent render at the destination end of the morph |
| Sleeping screen layout redesign | Phase 2 — Phase 1 only adds the halo `Box` around the timer for the morph destination |
| Long-press → SleepTypePicker copy/UX | Out of scope — preserve existing behavior |
| Tab bar / bottom navigation visual | Out of scope |

---

## 2. Home Layout

### Structural diagram

```
┌─────────────────────────────────────────┐
│                                         │ ← status bar inset
│   ⋅ aurora drift  ⋅                     │
│                                         │
│   good evening,                         │ ← serifItalic(22) · textSecondary
│   Emma                                  │ ← titleXL (34sp) · textPrimary
│   ────────                              │ ← Hairline (60dp wide)
│   142 DAYS WITH US                      │ ← labelMD · textTertiary · tracked
│                                         │
│   ─ SWSpacing.xl gap ─                  │
│                                         │
│   ┌─────────────────────────────────┐   │ ← GlassCard hero=true, ElevationLevel.HERO
│   │ WAKE WINDOW                      │  │   SectionLabel inside (12sp tracked)
│   │ 47 min                           │  │ ← displayLGTabular (64sp tabular)
│   │ ●─────────○─                     │  │ ← 6dp gradient bar, auroraGlow
│   └─────────────────────────────────┘   │
│                                         │
│   ┌─────────────────────────────────┐   │ ← GlassCard ElevationLevel.SOFT
│   │ ◉  Womb                  ≋≋≋  ⋯ │  │   clickable → Player tab
│   └─────────────────────────────────┘   │
│                                         │
│   ─ SWSpacing.xl gap ─                  │
│                                         │
│   ┌──────────┐  ┌──────────┐            │ ← 2×2 QuickActionTile grid
│   │ 🤱       │  │ 🤱       │            │   peach / peach tints preserved
│   │ Left     │  │ Right    │            │
│   └──────────┘  └──────────┘            │
│   ┌──────────┐  ┌──────────┐            │
│   │ 🍼       │  │ 🧷       │            │   lilac / mint
│   │ Bottle   │  │ Diaper   │            │
│   └──────────┘  └──────────┘            │
│                                         │
│   ─ SWSpacing.huge gap (56dp) ─         │
│                                         │
│                ⋅   ⋅                    │
│           ⋅   ╭───────╮    ⋅            │ ← 3-ring PulseRing (140dp radius)
│         ⋅   ╱           ╲   ⋅           │   STRONG intensity
│            │   ☾ Sleep   │              │ ← SoftButton HERO, titleMD serif
│         ⋅   ╲           ╱   ⋅           │   leadingIcon ic_moon_zzz
│           ⋅   ╰───────╯    ⋅            │
│                ⋅   ⋅                    │
│                                         │
└─────────────────────────────────────────┘
```

### Section-by-section changes

| Element | Today (post-Phase-0 tokens already apply) | Phase 1 layout change |
|---|---|---|
| Greeting "good evening" | serifItalic(17) | serifItalic(22), wrapped in `cjkAware` for localized strings |
| Baby name | titleXL (28sp SansSerif SemiBold) — already migrated by Phase 0 | titleXL stays (now 34sp Source Serif SemiBold via Phase 0 SWFont rebuild). **Not 64sp DM Serif** — save that scale for Sleeping screen timer |
| Day count line | "142 days old" bodyMD style | "142 DAYS WITH US" — labelMD uppercase tracked, with a 60dp wide Hairline above |
| WakeWindowCard | GlassCard ElevationLevel.HERO | Same elevation + new `hero=true` (continuous breath scale 0.998↔1.002 over `breathCycleMs`); section label uses new `SectionLabel` shared component |
| NowPlayingCard | 52dp icon circle + name + waveform + chevron | Same structure. Icon circle gets `Modifier.innerHighlight`. AudioWaveform inherits the evolved aurora-gradient bars. Chevron → `ChevronTrail` |
| QuickActionTile (4 tiles) | 2×2 grid, soft tinted fills, icon + bodyMD label | Same 2×2 grid, same tints. Internal label switches to bodyMD Source Serif. New Hairline (60% alpha border color) above the label inside each tile. `Modifier.innerHighlight` on the tile shape |
| Sleep CTA | SoftButton ACCENT + PulseRing 110dp STRONG | SoftButton **HERO** + PulseRing **140dp** STRONG. Wrapped in a Box that carries `Modifier.sleepHeroOrigin(...)` from `MorphingHeroSurface` |

### Decisions

1. **Baby name 34sp, not 64sp DM Serif.** The master spec mockup showed 64sp; at that scale on a baby tracker home screen the name dominates and pushes the WakeWindow card too far down. 34sp Source Serif SemiBold keeps it special. The 64sp+ DM Serif treatment is saved for the Sleeping screen timer (Phase 2) where it earns the real estate.
2. **Hairline under greeting is 60dp wide, not full-width.** Reads as a typographic flourish (a book chapter rule) rather than a divider.
3. **PulseRing 140dp, up from 110dp.** Sleep CTA is now the only element below the QuickActions grid and has the room to breathe larger.
4. **No greeting copy changes.** `greetingForHour()` already exists and is correct.
5. **NowPlayingCard keeps icon-in-circle.** The Aurora-Orb upgrade is Phase 3. We don't rebuild it twice.
6. **Time-of-day ambient hint.** When `LocalTime.now().hour` is in 19–23 or 0–5, set `heroBackdrop.ambient` to dusk-orange whisper so AuroraBackdrop tints warmer in evening hours. Cleared during 6–18. A small touch that makes Home feel alive without code complexity.

---

## 3. Hero #1 Morph — Forward (Home → Sleeping)

### Storyboard

```
t=0      User taps Sleep CTA.
         HapticFeedbackType.LongPress (heavy thunk).
         SoftButton press scale 1.0 → 0.96 (existing feedback).

t=80     PulseRing rings freeze their drift phase.
         heroBackdrop.ambient palette begins shifting:
           dusk-orange whisper → starlight-purple → deep-night
         (Animatable<Color>, tween easing matched to overall morph).
         AnimatedContent fade-in for "sleeping" begins (heroMorphMs duration).

t=200    Sleep CTA composite begins shape morph (driven by SharedTransitionLayout):
           • width   320dp → 600dp
           • height  56dp → 600dp
           • corner  pill (999dp) → 50% (circle)
         Home secondary content fades with top-down stagger (each 200ms):
           • Greeting block       starts t=140
           • WakeWindowCard       starts t=180
           • NowPlayingCard       starts t=220
           • QuickActions row 1   starts t=260
           • QuickActions row 2   starts t=300
         (40ms stagger steps, controlled by morphProgress.value)

t=400    Sleep CTA is now a 600dp circle centered on screen.
         Aurora ribbons palette settles at deep-night.
         Stars begin sparking from the circle perimeter outward
         (placeholder Starfield density ramping 0 → 80 over 400ms;
          Phase 2 replaces this with NightSkyCanvas).

t=700    Background fully transitioned to night-sky surface.
         Circle dissolves: alpha 1.0 → 0 over 200ms.
         The transparent circle's interior reveals the timer position
         (timer is at exactly the circle's center → visual continuity preserved).

t=900    Sleeping timer fades in (00:00:00 → live tick).
         "sleeping since 9:47" italic caption settles below.
         Aurora ribbons resume drift on Sleeping screen with NIGHT-default backdrop.

t=1200   Settled. Sleeping screen owns the canvas.
         Real-time clock ticking.
         morphProgress.value = 1.0.
```

### Reverse (Sleeping → Home)

Triggered by long-press stop on Sleeping (existing gesture). Symmetric duration (1200ms):

```
t=0      Long-press detected, haptic.
t=80     Timer fades out (200ms).
t=200    Halo expands from invisible → 600dp circle, opacity 0 → 1.
         Aurora ribbons palette shifts: deep-night → dawn.
         Starfield density 80 → 0 over 400ms.
t=400    AnimatedContent crosses to MainScaffold/Home.
         Circle morphs: 600dp → 320dp wide, height 56dp, corner pill.
         Aurora backdrop dawn palette settles.
t=700    Home secondary content fades IN with reverse stagger (bottom-up):
           QuickActions row 2 first → row 1 → NowPlayingCard
           → WakeWindowCard → greeting block (40ms stagger).
t=900    Sleep CTA back at its bottom-anchored position.
         PulseRing resumes its 3-ring drift.
t=1200   Settled. morphProgress.value = 0.
         SleepSummaryOverlay fades in over the Home screen (existing behavior).
```

### Reduce-motion fallback

When `LocalReduceMotion.current = true`:
- No shape morph, no stagger, no halo expand
- 250ms crossfade only (matches existing screenInMs behavior)
- `morphProgress` snaps to 0 or 1 directly, no animation
- Backdrop palette change happens but uses `snap()` not `tween()`
- All other downstream consumers (star density, content fade) read morphProgress and respond to the snap, so they too go directly to end state

### Edge cases

| Case | Behavior |
|---|---|
| Tap Sleep CTA while ongoing sleep session | `vm.onTapSleep()` toggles correctly (existing logic). Morph plays normally. |
| Double-tap on Sleep CTA | SoftButton `enabled = !isMorphing` where `isMorphing = 0 < morphProgress.value < 1`. Second tap is rejected. |
| Backgrounded mid-morph | Compose pauses animation. On `Lifecycle.onResume`, `morphProgress` snaps to 0 or 1 based on current `rootKey` (sleeping → 1, else → 0). No re-play. |
| Process death mid-sleep | Re-entry routes directly to Sleeping (persisted rootKey). No morph played — direct render. |
| Stop sleep from outside (notification action) | Direct route swap back to Home, no morph. |

---

## 4. Technical Architecture

### AppStateContainer additions

```kotlin
@Singleton
class AppStateContainer @Inject constructor(...) {

    // ... existing state (rootKey, baby, toast, etc.) ...

    /**
     * Morph progress lifted across the route boundary. Both Home and Sleeping read
     * this to time their secondary animations (content stagger, star density).
     */
    val morphProgress: Animatable<Float> = Animatable(0f)

    /** Hero backdrop controller — single source of truth for ambient color. */
    val heroBackdrop: HeroBackdropController = HeroBackdropController()

    /** Drive forward morph. Suspends ~heroMorphMs. */
    suspend fun beginSleepMorph() {
        morphProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(SWMotion.heroMorphMs, easing = LinearEasing)
        )
    }

    /** Drive reverse morph. Suspends ~heroMorphMs. */
    suspend fun endSleepMorph() {
        morphProgress.animateTo(
            targetValue = 0f,
            animationSpec = tween(SWMotion.heroMorphMs, easing = LinearEasing)
        )
    }

    /** Called from onResume to recover state after backgrounding. */
    fun snapMorphToCurrent() {
        val target = if (rootKey.value == "sleeping") 1f else 0f
        // We're outside a coroutine here; use the snap() spec which completes instantly.
        // Compose handles this via Animatable.snapTo() inside a launched coroutine.
        // Concrete invocation pattern in implementation plan.
    }
}
```

### RootRoute restructuring

Replace `Crossfade(rootKey)` with `SharedTransitionLayout { AnimatedContent(rootKey) { ... } }`. The `transitionSpec` lambda uses the heroMorphMs duration for Home↔Sleeping crossings and falls back to screenInMs for all other transitions (onboarding → main, welcome → main).

Both `MainScaffold` and `SleepingScreen` accept new `sharedScope: SharedTransitionScope` and `animScope: AnimatedVisibilityScope` parameters threaded down from RootRoute. These are required by `Modifier.sharedBounds(...)` at the Sleep CTA and timer-halo callsites.

Wrap everything in `CompositionLocalProvider(LocalHeroBackdropController provides app.heroBackdrop)` so all descendants of RootRoute (including AuroraBackdrop in every screen) see the single source-of-truth controller.

### MorphingHeroSurface.kt (new file)

```kotlin
package com.lizhi1026.sleepwhisper.core.visualkit.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import com.lizhi1026.sleepwhisper.core.visualkit.SWMotion

/**
 * Tag this composable as the Home end of the Sleep hero morph.
 * Bounds animate to the Sleeping halo's position/size during route change.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
fun Modifier.sleepHeroOrigin(
    sharedScope: SharedTransitionScope,
    animScope: AnimatedVisibilityScope
): Modifier = with(sharedScope) {
    this@sleepHeroOrigin.sharedBounds(
        sharedContentState = rememberSharedContentState(key = "sleep-hero"),
        animatedVisibilityScope = animScope,
        boundsTransform = { _, _ -> tween(SWMotion.heroMorphMs, easing = LinearEasing) },
        resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds
    )
}

/**
 * Tag this composable as the Sleeping end of the Sleep hero morph.
 * Same key as `sleepHeroOrigin` — Compose matches them during route change.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
fun Modifier.sleepHeroDestination(
    sharedScope: SharedTransitionScope,
    animScope: AnimatedVisibilityScope
): Modifier = with(sharedScope) {
    this@sleepHeroDestination.sharedBounds(
        sharedContentState = rememberSharedContentState(key = "sleep-hero"),
        animatedVisibilityScope = animScope,
        boundsTransform = { _, _ -> tween(SWMotion.heroMorphMs, easing = LinearEasing) },
        resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds
    )
}
```

The named-pair-extensions pattern (vs a single `sleepHero(role)`) is intentional for call-site readability.

### Sleep CTA wiring (HomeScreen.kt SleepCTA composable)

```kotlin
@Composable
private fun SleepCTA(
    sharedScope: SharedTransitionScope,
    animScope: AnimatedVisibilityScope,
    morphProgress: Float,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    val isMorphing = morphProgress > 0f && morphProgress < 1f
    Box(
        modifier = Modifier.fillMaxWidth().padding(top = SWSpacing.huge),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .sleepHeroOrigin(sharedScope, animScope)
                .pointerInput(onTap, onLongPress) {
                    detectTapGestures(onLongPress = { onLongPress() }, onTap = { onTap() })
                }
        ) {
            PulseRing(color = SWColor.accent(LocalSWScheme.current), radius = 140.dp, intensity = PulseIntensity.STRONG)
            SoftButton(
                text = stringResource(R.string.home_sleepcta),
                onClick = onTap,
                style = SoftButtonStyle.HERO,
                leadingIconRes = R.drawable.ic_moon_zzz,
                enabled = !isMorphing
            )
        }
    }
}
```

The actual tap handler at the caller level:

```kotlin
val scope = rememberCoroutineScope()
SleepCTA(
    sharedScope = sharedScope,
    animScope = animScope,
    morphProgress = app.morphProgress.value,
    onTap = {
        scope.launch {
            // Start morph; once it's about 40% through (circle formed), swap rootKey.
            val morphJob = launch { app.beginSleepMorph() }
            delay((SWMotion.heroMorphMs * 0.4f).toLong())  // ~480ms
            vm.onTapSleep()  // sets rootKey = "sleeping"
            morphJob.join()  // wait for morph completion
        }
    },
    onLongPress = { showSleepTypePicker = true }
)
```

### Sleeping screen halo (SleepingScreen.kt)

```kotlin
@Composable
fun SleepingScreen(
    sharedScope: SharedTransitionScope,
    animScope: AnimatedVisibilityScope,
    vm: SleepingViewModel = hiltViewModel()
) {
    // ... existing state observers ...

    CompositionLocalProvider(LocalSWScheme provides SWScheme.DARK) {
        Box(modifier = Modifier.fillMaxSize().background(SWColor.surface(SWScheme.DARK))) {
            // Phase 1 placeholder — Phase 2 replaces with NightSkyCanvas
            Starfield(modifier = Modifier.fillMaxSize(), density = (morphProgress * 80).toInt())

            Box(
                modifier = Modifier
                    .size(600.dp)
                    .align(Alignment.Center)
                    .sleepHeroDestination(sharedScope, animScope),
                contentAlignment = Alignment.Center
            ) {
                // Existing timer composables, recolored to use SWFont.displayLGTabular for now
                // (Phase 2 upgrades to displayXLTabular and adds the time-of-day-aware sky)
            }
        }
    }
}
```

---

## 5. Risks & Mitigations

| ID | Risk | Mitigation |
|---|---|---|
| R1 | `SharedTransitionLayout` API quirks (state reset on rotation, key collisions) | Instrumented test triggering Home → Sleeping → Home with `mainClock.autoAdvance = false`, asserting shared element removed cleanly between transitions |
| R2 | rootKey change happens before morph completes, causing "snap then animate" visual | Tap handler suspends ~480ms (40% of heroMorphMs) before calling `vm.onTapSleep()`. Concrete sequencing locked in Section 4. |
| R3 | morphProgress stuck mid-value if app backgrounded mid-morph | `Lifecycle.onResume` calls `app.snapMorphToCurrent()` which snaps to 0 or 1 based on `rootKey` |
| R4 | Reduce-motion path easy to miss in QA | Two instrumented tests, one per code branch (animated + reduce-motion) |
| R5 | Race condition on double-tap | `SoftButton.enabled = !isMorphing` where `isMorphing = 0 < morphProgress.value < 1` |
| R6 | Sleeping forces DARK scheme; morph crosses scheme boundary | Palette interp during morph uses absolute Animatable<Color> values, not scheme-derived colors. Scheme-derived backdrop resumes only after morphProgress settles. |
| R7 | Phase 0 visual regressions could compound into Phase 1 | Task 1 of implementation plan is a hard gate: manual smoke of Phase 0 on Home before any Phase 1 code lands |

---

## 6. Verification

### Unit tests

- `AppStateContainerMorphTest.kt` — `beginSleepMorph` / `endSleepMorph` drive `morphProgress` over correct duration; `snapMorphToCurrent()` snaps to expected target

`MorphingHeroSurface.kt` is two Modifier extensions wrapping `sharedBounds(...)` — no testable pure logic. Verified via instrumented tests below instead.

### Instrumented Compose tests (`androidTest`)

- `HomeScreenLayoutTest.kt` — assert structural layout renders without crash in DAY / DARK / NIGHT
- `HeroMorphForwardTest.kt` — `mainClock.autoAdvance = false`, step morph, assert sharedBounds change, morphProgress reaches 1.0, rootKey settles at "sleeping"
- `HeroMorphReverseTest.kt` — symmetric reverse test
- `HeroMorphReduceMotionTest.kt` — `LocalReduceMotion = true`, assert no sharedBounds animation, direct crossfade timing

### Manual smoke (final task)

- Forward morph at 1× speed on real device
- Forward morph at 0.5× system animation scale (slow-mo review)
- Reverse morph
- Background mid-morph + return
- Double-tap Sleep CTA → verify debounce
- TalkBack on Sleep CTA
- Reduce-motion enabled, repeat forward + reverse
- Battery sanity: 10-minute Sleeping session, check delta

### Visual receipts

Screenshots in `docs/superpowers/screenshots/2026-05-20-phase-1/`:
- Home in DAY, DARK
- Mid-morph frame at progress 0.5
- Sleeping screen (Phase 1 state, not yet redesigned)
- Reduce-motion comparison

---

## 7. Implementation Phasing — 11 Tasks

| Task | Output | Files Touched |
|---|---|---|
| 1 | Phase 0 smoke gate (manual) | none |
| 2 | `AppStateContainer.morphProgress`, `heroBackdrop`, `begin/endSleepMorph`, `snapMorphToCurrent` | `AppStateContainer.kt` |
| 3 | Replace `Crossfade` with `SharedTransitionLayout { AnimatedContent }`; thread scopes | `RootRoute.kt`, signatures of `MainScaffold`, `SleepingScreen` |
| 4 | `MorphingHeroSurface.kt` — extensions + (any) helpers | new file |
| 5 | Home greeting block — Hairline, day-count label, time-of-day ambient hint | `HomeScreen.kt` greeting section |
| 6 | WakeWindowCard — `hero=true`, evolved typography, gradient progress bar | `HomeScreen.kt` WakeWindowCard |
| 7 | NowPlayingCard — new tokens applied, `ChevronTrail`, `innerHighlight` on icon circle | `HomeScreen.kt` NowPlayingCard |
| 8 | QuickActionTile internals refresh | `QuickActionTile.kt` |
| 9 | Sleep CTA — HERO SoftButton + 140dp PulseRing + sleepHeroOrigin + debounce + suspending tap handler | `HomeScreen.kt` SleepCTA |
| 10 | Sleeping halo wrapper + reverse morph wired to long-press stop; placeholder star spawn driven by morphProgress | `SleepingScreen.kt` |
| 11 | Manual smoke walkthrough + screenshots | none |

**Sequencing:**
- 2-4 are infrastructure, must come before 5-10
- 5-8 are independent Home sections, can be done in any order
- 9 depends on 4 (sleepHeroOrigin must exist)
- 10 depends on 3, 4 (sharedScope thread + sleepHeroDestination)
- 11 is the verification gate

---

## 8. Locked Decisions

| Decision | Value |
|---|---|
| Sleep CTA position | Bottom-anchored (preserved) |
| Baby name typography | 34sp Source Serif SemiBold (titleXL), not 64sp |
| Hairline flourish | 60dp wide under greeting |
| PulseRing radius | 140dp (up from 110dp) |
| Sleep CTA style | SoftButton HERO + 3-ring PulseRing |
| Morph duration | 1200ms forward + 1200ms reverse |
| Shared element tech | SharedTransitionLayout (Compose 1.7+, BOM 2024.12.01) |
| Morph state holder | `AppStateContainer.morphProgress: Animatable<Float>` |
| Backdrop ambient driver | `AppStateContainer.heroBackdrop` via `LocalHeroBackdropController` |
| Time-of-day ambient | 19–05: dusk-orange whisper; 06–18: cleared |
| Route swap timing | 40% into morph (~480ms after tap) |
| PresetAuraOrb | Deferred to Phase 3 |
| NightSkyCanvas | Deferred to Phase 2 |
| Reduce-motion fallback | 250ms crossfade; snap morphProgress to 0/1 |
| Double-tap debounce | `SoftButton.enabled = !(0 < morphProgress.value < 1)` |
| Lifecycle recovery | `onResume` snaps morphProgress based on rootKey |
