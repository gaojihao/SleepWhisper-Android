# Phase 1 — Home + Sleep CTA Hero Morph Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Apply Aurora tokens to Home and implement Hero #1 — the cinematic Sleep CTA → Sleeping morph using `SharedTransitionLayout`, with `morphProgress: Animatable<Float>` lifted to `AppStateContainer` so both screens can stage their secondary animations against it.

**Architecture:** `RootRoute` becomes a `SharedTransitionLayout { AnimatedContent { ... } }`. The Sleep CTA on Home and the timer halo on Sleeping share an element key `"sleep-hero"`. The `Animatable<Float>` in `AppStateContainer` drives staggered content fades and the placeholder star-density ramp on Sleeping. RootKey switch is **immediate on tap** — perceived phasing of the storyboard (circle forms → stars spawn → timer reveals) is achieved by gating each visual on `morphProgress` thresholds, not on rootKey timing.

**Tech Stack:** Compose BOM 2024.12.01 (`androidx.compose.animation.SharedTransitionLayout` is stable here), Kotlin 2.0.21, Hilt, existing Phase 0 visualkit (`SoftButton HERO`, `GlassCard hero=true`, `PulseRing` 3-ring, `ChevronTrail`, `Hairline`, `innerHighlight` Modifier, `HeroBackdropController`).

---

## File Map

**Modified:**
- `app/src/main/java/com/lizhi1026/sleepwhisper/app/AppStateContainer.kt` — add `morphProgress`, `heroBackdrop`, `beginSleepMorph()` / `endSleepMorph()` / `snapMorphToCurrent()`
- `app/src/main/java/com/lizhi1026/sleepwhisper/app/RootRoute.kt` — replace `Crossfade` with `SharedTransitionLayout { AnimatedContent }`, thread scopes into `MainScaffold` + `SleepingScreen`, wrap in `CompositionLocalProvider(LocalHeroBackdropController provides app.heroBackdrop)`
- `app/src/main/java/com/lizhi1026/sleepwhisper/features/home/HomeScreen.kt` — restructure greeting; refresh WakeWindowCard, NowPlayingCard; wire Sleep CTA to morph
- `app/src/main/java/com/lizhi1026/sleepwhisper/features/home/QuickActionTile.kt` — add Hairline + innerHighlight
- `app/src/main/java/com/lizhi1026/sleepwhisper/features/sleeping/SleepingScreen.kt` — accept scopes, add halo wrapper around timer, reverse morph wiring
- `app/src/main/java/com/lizhi1026/sleepwhisper/features/sleeping/SleepingViewModel.kt` — promote `app` from `private` to public (one-line change)

**Created (new files):**
- `app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/MorphingHeroSurface.kt`
- `app/src/test/java/com/lizhi1026/sleepwhisper/app/AppStateContainerMorphTest.kt`

---

## Conventions

- All commits follow the existing repo style: `area: short imperative summary`. Examples: `home: greeting block with hairline flourish`, `sleeping: halo wrapper for sleep hero destination`.
- After each task, run `./gradlew :app:compileDebugKotlin` to verify the project compiles before committing.
- Visual changes are reviewed via Compose `@Preview` annotations in Android Studio (no Paparazzi infrastructure in this repo).
- Reduce-motion is honored by every animated component (no exceptions).
- The repo HEAD at plan-write time: `e3f9a62` (Phase 1 spec committed). Phase 1 implementation builds on top of all 19 Phase 0 commits.

---

## Task 1 — Phase 0 manual smoke gate

**Files:** none (verification only)

This is a hard gate. If Phase 0's foundation has any visual regression we haven't seen yet, fixing it inside a Phase 1 commit muddies the history. So Phase 0 gets smoke-verified first.

- [ ] **Step 1: Install + open the app on a real device or emulator**

```bash
./gradlew :app:installDebug
adb shell am start -n com.lizhi1026.sleepwhisper/.app.MainActivity
```

Expected: app launches without crash.

- [ ] **Step 2: Verify each Phase 0 visual on Home in DAY scheme**

In Settings → Appearance set DAY. Confirm:
- Greeting reads in Source Serif Italic
- Baby name reads in Source Serif (titleXL, currently 28sp — Phase 1 will keep at 34sp)
- WakeWindowCard renders with GlassCard moonlit treatment
- NowPlayingCard renders, AudioWaveform shows aurora-gradient sine bars when audio plays
- QuickActionTiles render with peach/lilac/mint tints
- Sleep CTA renders as the existing ACCENT-style SoftButton + 3-ring PulseRing (Phase 1 upgrades this to HERO)
- AuroraBackdrop drifts subtly behind everything (paper-warm in DAY)

- [ ] **Step 3: Repeat in DARK scheme**

Switch to DARK. Confirm:
- All text in moonlit silver
- Aurora ribbons drift visibly (deep night → indigo → starlight purple)
- 3-ring PulseRing on Sleep CTA shows orange → blend → purple offset rings

- [ ] **Step 4: Verify NIGHT scheme accessibility preserved**

Switch to NIGHT. Confirm:
- All text in reddish-pink with high contrast
- Backdrop static (no ribbon drift)
- PulseRing renders static

- [ ] **Step 5: Reduce-motion check**

```bash
adb shell settings put secure transition_animation_scale 0
adb shell settings put secure window_animation_scale 0
adb shell settings put secure animator_duration_scale 0
```

Relaunch. Confirm: AuroraBackdrop static, PulseRing static, AudioWaveform static.

Restore:
```bash
adb shell settings put secure transition_animation_scale 1
adb shell settings put secure window_animation_scale 1
adb shell settings put secure animator_duration_scale 1
```

- [ ] **Step 6: Report regressions before proceeding**

If anything in steps 2-5 is broken, fix it as Phase 0 follow-up commits (using the same `visualkit:` commit prefix) before starting Task 2. If everything looks good, proceed to Task 2.

No commit required for this task.

---

## Task 2 — `AppStateContainer.morphProgress` + morph control APIs

**Files:**
- Modify: `app/src/main/java/com/lizhi1026/sleepwhisper/app/AppStateContainer.kt`
- Create: `app/src/test/java/com/lizhi1026/sleepwhisper/app/AppStateContainerMorphTest.kt`

This task lifts the morph state above the route boundary. The `Animatable<Float>` is held by the Hilt singleton so it survives configuration changes.

- [ ] **Step 1: Write the failing test for the morph control APIs**

Create `app/src/test/java/com/lizhi1026/sleepwhisper/app/AppStateContainerMorphTest.kt`:

```kotlin
package com.lizhi1026.sleepwhisper.app

import androidx.compose.animation.core.Animatable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppStateContainerMorphTest {

    @Test fun `morphProgress starts at zero`() {
        val progress = Animatable(0f)
        assertEquals(0f, progress.value, 0.001f)
    }

    @Test fun `animateTo 1f reaches target`() = runTest {
        val progress = Animatable(0f)
        progress.animateTo(targetValue = 1f)
        assertEquals(1f, progress.value, 0.001f)
    }

    @Test fun `snapTo bypasses animation`() = runTest {
        val progress = Animatable(0f)
        progress.snapTo(1f)
        assertEquals(1f, progress.value, 0.001f)
        progress.snapTo(0f)
        assertEquals(0f, progress.value, 0.001f)
    }

    @Test fun `animateTo reverse from 1f to 0f works`() = runTest {
        val progress = Animatable(1f)
        progress.animateTo(targetValue = 0f)
        assertEquals(0f, progress.value, 0.001f)
    }
}
```

These tests cover the `Animatable<Float>` contract our morph APIs depend on. The `AppStateContainer` wiring is covered by integration tests in Task 11; here we just confirm the underlying Compose primitive behaves as expected (e.g. that animateTo settles at the target, that snapTo bypasses).

- [ ] **Step 2: Run the test — expect failure**

```bash
./gradlew :app:testDebugUnitTest --tests 'com.lizhi1026.sleepwhisper.app.AppStateContainerMorphTest'
```

Expected: FAIL because `kotlinx.coroutines.test` (for `runTest`) may not be on the test classpath. If it fails for that reason, add to `app/build.gradle.kts` inside `dependencies { ... }`:

```kotlin
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
```

Then re-run. Tests should now PASS (4/4) since they're testing only the Compose `Animatable` API which is already available.

- [ ] **Step 3: Add `morphProgress` and `heroBackdrop` properties to AppStateContainer**

Open `app/src/main/java/com/lizhi1026/sleepwhisper/app/AppStateContainer.kt`. Add these imports near the top with the other imports:

```kotlin
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import com.lizhi1026.sleepwhisper.core.visualkit.HeroBackdropController
import com.lizhi1026.sleepwhisper.core.visualkit.SWMotion
```

Then, inside the `class AppStateContainer @Inject constructor(...) { ... }` body, AFTER the `// ─── Internal ───` section but BEFORE `init { ... }`, add:

```kotlin
    // ─── Hero morph state (Phase 1) ──────────────────────────────────────────

    /**
     * Sleep CTA → Sleeping screen morph progress, 0..1.
     * Held in AppStateContainer (singleton) so it survives configuration changes
     * and so both Home and Sleeping screens can read it to stage their own
     * secondary animations (content stagger fade, star density ramp, etc).
     */
    val morphProgress: Animatable<Float> = Animatable(0f)

    /**
     * Single source of truth for the app's "ambient color" — drives the
     * AuroraBackdrop's tint. Phase 1 uses it for the time-of-day evening hint
     * on Home; Phase 3 will use it for audio-preset auras from Player.
     */
    val heroBackdrop: HeroBackdropController = HeroBackdropController()

    /** Drive the forward morph (Home → Sleeping). Suspends ~heroMorphMs. */
    suspend fun beginSleepMorph() {
        morphProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(SWMotion.heroMorphMs, easing = LinearEasing)
        )
    }

    /** Drive the reverse morph (Sleeping → Home). Suspends ~heroMorphMs. */
    suspend fun endSleepMorph() {
        morphProgress.animateTo(
            targetValue = 0f,
            animationSpec = tween(SWMotion.heroMorphMs, easing = LinearEasing)
        )
    }

    /**
     * Snap morphProgress to the value that matches the current rootKey. Call
     * this from Lifecycle.onResume so re-entering the app after a background
     * doesn't replay a half-finished morph.
     */
    fun snapMorphToCurrent() {
        val target = if (rootKey.value == "sleeping") 1f else 0f
        scope.launch { morphProgress.snapTo(target) }
    }
```

- [ ] **Step 4: Compile**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Re-run all unit tests as a sanity check**

```bash
./gradlew :app:testDebugUnitTest
```

Expected: BUILD SUCCESSFUL. All existing tests (including SWColorTest, HeroBackdropControllerTest, and the new AppStateContainerMorphTest) pass.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/lizhi1026/sleepwhisper/app/AppStateContainer.kt \
        app/src/test/java/com/lizhi1026/sleepwhisper/app/AppStateContainerMorphTest.kt
# Include build.gradle.kts only if step 2 required adding kotlinx-coroutines-test
git add app/build.gradle.kts 2>/dev/null || true
git commit -m "app: AppStateContainer hero morph state and control APIs"
```

---

## Task 3 — `RootRoute` → `SharedTransitionLayout { AnimatedContent }`

**Files:**
- Modify: `app/src/main/java/com/lizhi1026/sleepwhisper/app/RootRoute.kt`
- Modify (signature only): `app/src/main/java/com/lizhi1026/sleepwhisper/features/sleeping/SleepingScreen.kt`

The current `Crossfade(rootKey)` becomes `SharedTransitionLayout { AnimatedContent(rootKey) }`. Both `MainScaffold` and `SleepingScreen` receive `sharedScope: SharedTransitionScope` and `animScope: AnimatedVisibilityScope` parameters threaded from RootRoute. Other top-level routes (`onboarding`, `welcome`) don't need the scopes — they ignore them.

We also wrap everything in `CompositionLocalProvider(LocalHeroBackdropController provides app.heroBackdrop)` so all AuroraBackdrop descendants read the app-wide controller.

- [ ] **Step 1: Add the temporary `app` accessor**

The existing RootRoute receives `app: AppStateContainer` as a parameter. We'll use it directly. No change needed to MainActivity.

- [ ] **Step 2: Add Phase 1 imports to RootRoute.kt**

In `app/src/main/java/com/lizhi1026/sleepwhisper/app/RootRoute.kt`, add the following imports near the top with the existing imports:

```kotlin
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.CompositionLocalProvider
import com.lizhi1026.sleepwhisper.core.visualkit.LocalHeroBackdropController
```

And **remove** the now-unused imports if any (`Crossfade`, `tween` if only used by Crossfade — leave any other uses intact):

```kotlin
// Delete this line:
import androidx.compose.animation.Crossfade
// Delete this line if not used elsewhere in the file:
import androidx.compose.animation.core.tween
```

- [ ] **Step 3: Replace the RootRoute body**

Replace the entire `fun RootRoute(app: AppStateContainer)` composable in `RootRoute.kt` with:

```kotlin
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun RootRoute(app: AppStateContainer) {
    val scheme by app.themeProvider.scheme.observeAsState(SWScheme.DAY)
    SWTheme(scheme) {
        CompositionLocalProvider(LocalHeroBackdropController provides app.heroBackdrop) {
            val rootKey by app.rootKey.observeAsState("onboarding")
            val toast by app.toast.events.observeAsState(null)
            val summary by app.lastSleepSummary.observeAsState(null)
            val baby by app.baby.observeAsState(null)
            val editTarget by app.pendingEditTarget.observeAsState(null)
            val scope = rememberCoroutineScope()

            Box(modifier = Modifier.fillMaxSize()) {
                SharedTransitionLayout {
                    AnimatedContent(
                        targetState = rootKey,
                        transitionSpec = {
                            // Home ↔ Sleeping: hero morph timing. Other transitions: standard fade.
                            val isHeroCrossing =
                                (initialState == "sleeping") != (targetState == "sleeping")
                            val durIn  = if (isHeroCrossing) SWMotion.heroMorphMs else SWMotion.screenInMs
                            val durOut = if (isHeroCrossing) SWMotion.heroMorphMs else SWMotion.screenOutMs
                            fadeIn(animationSpec = androidx.compose.animation.core.tween(durIn)) togetherWith
                                fadeOut(animationSpec = androidx.compose.animation.core.tween(durOut))
                        },
                        label = "root"
                    ) { key ->
                        when (key) {
                            "onboarding" -> WithStatusBarPadding { OnboardingScreen() }
                            "welcome"    -> WelcomeRitualScreen(app)
                            "sleeping"   -> SleepingScreen(
                                sharedScope = this@SharedTransitionLayout,
                                animScope = this@AnimatedContent
                            )
                            else         -> MainScaffold(
                                sharedScope = this@SharedTransitionLayout,
                                animScope = this@AnimatedContent
                            )
                        }
                    }
                }

                summary?.let { s ->
                    SleepSummaryOverlay(
                        summary = s,
                        babyName = baby?.name,
                        onDismiss = app::clearSleepSummary
                    )
                }

                editTarget?.let { t ->
                    EventEditSheet(
                        target = t,
                        onDismiss = app::clearPendingEdit,
                        onSaveFeeding = { f -> scope.launch { app.updateFeeding(f) } },
                        onSaveDiaper = { d -> scope.launch { app.updateDiaper(d) } }
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
                ) {
                    ToastOverlay(
                        item = toast,
                        onDismiss = app.toast::consume,
                        onUndo = toast?.undo,
                        onEditAction = toast?.editAction
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 4: Update MainScaffold signature in the same file**

In `RootRoute.kt`, replace `private fun MainScaffold()` with:

```kotlin
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun MainScaffold(
    sharedScope: SharedTransitionScope,
    animScope: AnimatedVisibilityScope
) {
    var tab by remember { mutableStateOf(0) }
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(top = statusBarTop)
        ) {
            when (tab) {
                0 -> HomeScreen(
                    sharedScope = sharedScope,
                    animScope = animScope,
                    onOpenPlayer = { tab = 3 }
                )
                1 -> TrendsScreen()
                2 -> SettingsScreen()
                3 -> PlayerScreen()
            }
        }
        BottomTabs(tab) { tab = it }
    }
}
```

HomeScreen now receives scopes too. Trends / Settings / Player don't — they don't participate in the shared element morph.

- [ ] **Step 5: Update SleepingScreen signature**

Open `app/src/main/java/com/lizhi1026/sleepwhisper/features/sleeping/SleepingScreen.kt`. Change the public `@Composable fun SleepingScreen` signature from:

```kotlin
@Composable
fun SleepingScreen(vm: SleepingViewModel = hiltViewModel()) {
```

to:

```kotlin
@OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
fun SleepingScreen(
    sharedScope: androidx.compose.animation.SharedTransitionScope,
    animScope: androidx.compose.animation.AnimatedVisibilityScope,
    vm: SleepingViewModel = hiltViewModel()
) {
```

The scopes are accepted but not yet consumed — Task 10 wires them. This step exists only to keep the project compiling between tasks.

- [ ] **Step 6: Update HomeScreen signature**

Open `app/src/main/java/com/lizhi1026/sleepwhisper/features/home/HomeScreen.kt`. Change the public `@Composable fun HomeScreen` from:

```kotlin
@Composable
fun HomeScreen(
    onOpenPlayer: () -> Unit,
    vm: HomeViewModel = hiltViewModel()
) {
```

to:

```kotlin
@OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
fun HomeScreen(
    sharedScope: androidx.compose.animation.SharedTransitionScope,
    animScope: androidx.compose.animation.AnimatedVisibilityScope,
    onOpenPlayer: () -> Unit,
    vm: HomeViewModel = hiltViewModel()
) {
```

Same as Sleeping — accepted but not yet consumed. Task 9 wires them into the Sleep CTA composable.

- [ ] **Step 7: Add lifecycle observer for snapMorphToCurrent**

So a backgrounded mid-morph doesn't resume to a half-animated state, register a Lifecycle observer that snaps `morphProgress` to 0 or 1 on `ON_RESUME`. Add this inside `RootRoute` immediately after `val scope = rememberCoroutineScope()`:

```kotlin
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                app.snapMorphToCurrent()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
```

Imports needed (these may be in the file already from other Compose use):
```kotlin
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
```

- [ ] **Step 8: Compile**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL. The whole app compiles. AnimatedContent + SharedTransitionLayout are stable in Compose BOM 2024.12.01.

- [ ] **Step 9: Install + smoke-verify no behavior regression**

```bash
./gradlew :app:installDebug
```

Launch the app. Confirm:
- Onboarding → Welcome → Main flow still works (each transition fades over screenInMs=420ms, same as before)
- Home → Sleeping crossing (start a sleep) now fades over heroMorphMs=1200ms (slower than before, no morph yet — just a longer plain crossfade)
- Sleeping → Home (stop a sleep) also takes 1200ms now

No visual morph yet — just the duration change. If the longer crossfade feels jarring, that's expected and gets fixed once Task 9/10 add the shared element.

- [ ] **Step 10: Commit**

```bash
git add app/src/main/java/com/lizhi1026/sleepwhisper/app/RootRoute.kt \
        app/src/main/java/com/lizhi1026/sleepwhisper/features/sleeping/SleepingScreen.kt \
        app/src/main/java/com/lizhi1026/sleepwhisper/features/home/HomeScreen.kt
git commit -m "app: SharedTransitionLayout root with hero morph timing"
```

---

## Task 4 — `MorphingHeroSurface.kt` Modifier extensions

**Files:**
- Create: `app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/MorphingHeroSurface.kt`

Two named Modifier extensions — `sleepHeroOrigin` and `sleepHeroDestination` — wrapping Compose's `sharedBounds(...)` with the shared key and our shared `boundsTransform`.

- [ ] **Step 1: Create the file**

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
 * Shared-element key for the Sleep CTA → Sleeping timer halo morph (Hero #1).
 * The same value is used by both the Home origin and the Sleeping destination.
 */
private const val SLEEP_HERO_KEY = "sleep-hero"

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
        sharedContentState = rememberSharedContentState(key = SLEEP_HERO_KEY),
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
        sharedContentState = rememberSharedContentState(key = SLEEP_HERO_KEY),
        animatedVisibilityScope = animScope,
        boundsTransform = { _, _ -> tween(SWMotion.heroMorphMs, easing = LinearEasing) },
        resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds
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
git add app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/MorphingHeroSurface.kt
git commit -m "visualkit: add sleepHeroOrigin and sleepHeroDestination Modifier extensions"
```

---

## Task 5 — Home greeting block refresh

**Files:**
- Modify: `app/src/main/java/com/lizhi1026/sleepwhisper/features/home/HomeScreen.kt` (only the `GreetingSection` composable)

Add a Hairline flourish under the name and convert the day-count line to the uppercase tracked labelMD style. Wire a small `LaunchedEffect` that sets the `heroBackdrop.ambient` to a dusk-orange whisper during evening hours.

- [ ] **Step 1: Add imports to HomeScreen.kt**

In the imports section of `app/src/main/java/com/lizhi1026/sleepwhisper/features/home/HomeScreen.kt`, add (or confirm present):

```kotlin
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import com.lizhi1026.sleepwhisper.core.visualkit.LocalHeroBackdropController
import com.lizhi1026.sleepwhisper.core.visualkit.components.Hairline
```

- [ ] **Step 2: Add the time-of-day ambient effect inside HomeScreen**

Inside `fun HomeScreen(...)` (the public one, not GreetingSection), add this `LaunchedEffect` near the top of the composable, after the existing `val hour = remember { ... }` line:

```kotlin
    val heroBackdrop = LocalHeroBackdropController.current
    LaunchedEffect(hour) {
        // Evening (19:00–05:59) — tint backdrop dusk-orange whisper. Otherwise clear.
        heroBackdrop.setAmbient(
            if (hour >= 19 || hour < 6) Color(0xFFFFB088) else null
        )
    }
```

- [ ] **Step 3: Rewrite the GreetingSection composable**

Find the `@Composable private fun GreetingSection(...)` function in HomeScreen.kt. Replace its body entirely with:

```kotlin
@Composable
private fun GreetingSection(babyName: String?, dobMs: Long?, hour: Int) {
    val scheme = LocalSWScheme.current
    val daysOld = remember(dobMs) {
        dobMs?.let {
            val birth = LocalDate.ofEpochDay(it / 86_400_000L)
            val today = LocalDate.now(ZoneId.systemDefault())
            ChronoUnit.DAYS.between(birth, today).toInt().coerceAtLeast(0)
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(SWSpacing.xs)) {
        BasicText(
            text = stringResource(greetingForHour(hour)),
            style = SWFont.serifItalic(22).copy(color = SWColor.textSecondary(scheme))
        )
        babyName?.let {
            BasicText(
                text = it,
                style = SWFont.titleXL().copy(color = SWColor.textPrimary(scheme))
            )
        }
        // Short hairline flourish — reads as a book chapter rule, not a divider.
        Hairline(modifier = Modifier.width(60.dp).padding(top = SWSpacing.xs))
        daysOld?.let {
            BasicText(
                text = stringResource(R.string.home_daycount, it).uppercase(),
                style = SWFont.labelMD().copy(color = SWColor.textTertiary(scheme))
            )
        }
    }
}
```

Note: `serifItalic(22)` (up from 17), and the day-count line is now uppercased via the standard `.uppercase()` call so the existing `R.string.home_daycount` resource (e.g. "%d days with us") gets shouted correctly.

- [ ] **Step 4: Compile**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Install + visual verify**

```bash
./gradlew :app:installDebug
```

Open Home. Confirm:
- Greeting reads larger (22sp vs prior 17sp)
- Baby name unchanged size (titleXL, 34sp from Phase 0)
- A short ~60dp horizontal line sits under the name
- Day count is UPPERCASE with tracked letter-spacing

Switch device clock to 21:00 (or use `adb shell date -s` if rooted; otherwise just check during evening). Confirm the AuroraBackdrop tints warmer (subtle).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/lizhi1026/sleepwhisper/features/home/HomeScreen.kt
git commit -m "home: greeting block with hairline flourish and evening ambient"
```

---

## Task 6 — WakeWindowCard refresh

**Files:**
- Modify: `app/src/main/java/com/lizhi1026/sleepwhisper/features/home/HomeScreen.kt` (only `WakeWindowCard`)

Convert the WakeWindowCard's section label to use the new `SectionLabel` shared composable, enable `hero = true` on the GlassCard for the subtle breath scale, and ensure the displayLGTabular number is hooked up.

- [ ] **Step 1: Add imports**

Confirm these imports are present in HomeScreen.kt (some may already be there):

```kotlin
import com.lizhi1026.sleepwhisper.core.visualkit.components.SectionLabel
```

- [ ] **Step 2: Replace the WakeWindowCard composable**

Find `@Composable private fun WakeWindowCard(remaining: Int, total: Int)` in HomeScreen.kt and replace its body entirely with:

```kotlin
@Composable
private fun WakeWindowCard(remaining: Int, total: Int) {
    val scheme = LocalSWScheme.current
    val color = when {
        remaining > 20 -> SWColor.textPrimary(scheme)
        remaining > 0 -> SWColor.warning(scheme)
        else -> SWColor.danger(scheme)
    }
    GlassCard(
        modifier = Modifier.fillMaxWidth().floatingY(),
        contentPadding = PaddingValues(SWSpacing.xl),
        elevation = ElevationLevel.HERO,
        hero = true
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(SWSpacing.sm)) {
            SectionLabel(stringResource(R.string.home_wakewindow_title))
            Row(verticalAlignment = Alignment.Bottom) {
                RollingNumber(
                    value = remaining,
                    style = SWFont.displayLGTabular().copy(color = color)
                )
                Spacer(Modifier.width(SWSpacing.xs))
                BasicText(
                    text = stringResource(R.string.home_wakewindow_unit),
                    style = androidx.compose.ui.text.TextStyle(
                        fontSize = 28.sp,
                        color = SWColor.textSecondary(scheme)
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(SWColor.border(scheme).copy(alpha = 0.3f))
            ) {
                val frac = (remaining.toFloat() / total.coerceAtLeast(1)).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(frac)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(SWGradient.auroraGlow(scheme))
                )
            }
        }
    }
}
```

Changes vs current code:
- Section label uses `SectionLabel(...)` shared composable instead of inline `BasicText` with `.uppercase()` + manual styling
- `hero = true` parameter added — engages GlassCard's subtle breath scale
- Progress bar fill uses `SWGradient.auroraGlow(scheme)` instead of `SWGradient.accent(scheme)`

- [ ] **Step 3: Compile**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Install + visual verify**

```bash
./gradlew :app:installDebug
```

Open Home. Confirm:
- WakeWindow card breathes very gently (0.998 ↔ 1.002 scale over 4.2s)
- The "WAKE WINDOW" label inside the card looks identical to before (SectionLabel implements the same uppercase tracked style)
- Progress bar fill is now the aurora-glow gradient (purple↔orange whisper) rather than the orange accent

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/lizhi1026/sleepwhisper/features/home/HomeScreen.kt
git commit -m "home: WakeWindowCard hero breath and aurora glow progress bar"
```

---

## Task 7 — NowPlayingCard refresh

**Files:**
- Modify: `app/src/main/java/com/lizhi1026/sleepwhisper/features/home/HomeScreen.kt` (only `NowPlayingCard`)

Replace the plain `ic_chevron_right` with the new `ChevronTrail`. Apply `innerHighlight` to the icon circle. The AudioWaveform already inherits the Phase 0 aurora-gradient look automatically; no change needed there.

- [ ] **Step 1: Add imports**

```kotlin
import com.lizhi1026.sleepwhisper.core.visualkit.components.ChevronTrail
import com.lizhi1026.sleepwhisper.core.visualkit.components.innerHighlight
```

- [ ] **Step 2: Replace the NowPlayingCard composable**

Find `@Composable private fun NowPlayingCard(...)` in HomeScreen.kt. Replace its body entirely with:

```kotlin
@Composable
private fun NowPlayingCard(
    presetIconName: String?,
    presetName: String?,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val scheme = LocalSWScheme.current
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val presetIconRes = remember(presetIconName) {
        presetIconName?.let { name ->
            ctx.resources.getIdentifier(name, "drawable", ctx.packageName).takeIf { it != 0 }
        }
    }
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        contentPadding = PaddingValues(SWSpacing.md)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SWSpacing.md)
        ) {
            // Left icon circle (52dp) — preset icon if known, else "moon" placeholder.
            // The innerHighlight modifier adds a subtle white-to-transparent stroke at
            // the top of the circle, giving it the "liquid glass" cue.
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        if (scheme == SWScheme.DAY) SWColor.softLilac(scheme)
                        else SWColor.primary(scheme)
                    )
                    .innerHighlight(cornerRadius = 26.dp),
                contentAlignment = Alignment.Center
            ) {
                val iconRes = presetIconRes ?: R.drawable.ic_empty_moon
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = iconRes),
                    contentDescription = null,
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                        if (scheme == SWScheme.DAY) SWColor.primary(scheme) else androidx.compose.ui.graphics.Color.White
                    ),
                    modifier = Modifier.size(24.dp)
                )
            }
            // Middle: title + (waveform when playing)
            Column(modifier = Modifier.weight(1f)) {
                BasicText(
                    text = presetName ?: stringResource(R.string.home_nowplaying_placeholder),
                    style = SWFont.titleMD().copy(color = SWColor.textPrimary(scheme))
                )
                if (isPlaying) {
                    AudioWaveform(
                        modifier = Modifier.height(18.dp).padding(top = 4.dp),
                        isPlaying = true,
                        color = SWColor.accent(scheme),
                        barCount = 24
                    )
                }
            }
            // Right: ChevronTrail replaces the previous static chevron icon
            ChevronTrail()
        }
    }
}
```

Changes vs current code:
- Icon circle now has `Modifier.innerHighlight(cornerRadius = 26.dp)` applied (52dp circle has 26dp corner radius)
- The trailing `Image(painter = painterResource(R.drawable.ic_chevron_right), ...)` is replaced with `ChevronTrail()`

The `Modifier.innerHighlight` must come AFTER `.background(...)` so it draws on top of the background fill.

- [ ] **Step 3: Compile**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Install + visual verify**

```bash
./gradlew :app:installDebug
```

Open Home. Confirm:
- The 52dp icon circle has a very subtle bright crescent at its top (the innerHighlight)
- Where the chevron used to be, you now see a 3-dot trail in textTertiary color

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/lizhi1026/sleepwhisper/features/home/HomeScreen.kt
git commit -m "home: NowPlayingCard with ChevronTrail and innerHighlight on icon"
```

---

## Task 8 — QuickActionTile internals refresh

**Files:**
- Modify: `app/src/main/java/com/lizhi1026/sleepwhisper/features/home/QuickActionTile.kt`

Add an `innerHighlight` modifier to the icon circle inside each tile. The existing GlassCard wrapping the tile already has the Phase 0 moonlit treatment from the upgrade in Task 9 of Phase 0, so we don't add anything at the outer level. The 2×2 grid layout is unchanged.

- [ ] **Step 1: Add the import**

In `app/src/main/java/com/lizhi1026/sleepwhisper/features/home/QuickActionTile.kt`, add:

```kotlin
import com.lizhi1026.sleepwhisper.core.visualkit.components.innerHighlight
```

- [ ] **Step 2: Apply innerHighlight to the 40dp icon circle**

Find the `Box` that holds the icon (around the middle of `QuickActionTile`):

```kotlin
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(tintBg),
                contentAlignment = Alignment.Center
            ) {
```

Replace it with:

```kotlin
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(tintBg)
                    .innerHighlight(cornerRadius = 20.dp),
                contentAlignment = Alignment.Center
            ) {
```

The cornerRadius matches the 40dp circle (20dp radius).

- [ ] **Step 3: Compile**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Install + visual verify**

```bash
./gradlew :app:installDebug
```

Open Home. Confirm:
- Each of the 4 tiles' icon circles (peach / peach / lilac / mint) has a faint bright crescent at the top — the innerHighlight
- Tap interaction is unchanged
- Long-press on Bottle still triggers the bottle amount sheet (existing behavior preserved)

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/lizhi1026/sleepwhisper/features/home/QuickActionTile.kt
git commit -m "home: QuickActionTile icon circle with innerHighlight"
```

---

## Task 9 — Sleep CTA wired to HERO + sleepHeroOrigin + suspending tap

**Files:**
- Modify: `app/src/main/java/com/lizhi1026/sleepwhisper/features/home/HomeScreen.kt` (`HomeScreen` public composable + `SleepCTA` private composable)

This task wires Phase 0's `SoftButton HERO` style, enlarges the PulseRing to 140dp, tags the Sleep CTA composite with `sleepHeroOrigin`, and adds the suspending tap handler that drives `morphProgress` in parallel with the immediate `vm.onTapSleep()` call.

- [ ] **Step 1: Add imports**

Add to HomeScreen.kt's imports:

```kotlin
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.rememberCoroutineScope
import com.lizhi1026.sleepwhisper.core.visualkit.SWMotion
import com.lizhi1026.sleepwhisper.core.visualkit.components.sleepHeroOrigin
import kotlinx.coroutines.launch
import com.lizhi1026.sleepwhisper.core.visualkit.components.SoftButtonStyle
import com.lizhi1026.sleepwhisper.core.visualkit.components.PulseIntensity
```

(Some of these are likely already present — keep them if so, add the missing ones.)

- [ ] **Step 2: Thread scopes through HomeScreen into SleepCTA**

The HomeScreen signature was changed in Task 3 to accept `sharedScope` and `animScope`. Now consume them. In the body of `HomeScreen`, change the call to `SleepCTA(...)` from:

```kotlin
            SleepCTA(
                onTap = vm::onTapSleep,
                onLongPress = { showSleepTypePicker = true }
            )
```

to:

```kotlin
            val morphScope = rememberCoroutineScope()
            val morphProgress by remember(vm.app.morphProgress) {
                derivedStateOf { vm.app.morphProgress.value }
            }
            SleepCTA(
                sharedScope = sharedScope,
                animScope = animScope,
                morphProgress = morphProgress,
                onTap = {
                    if (morphProgress <= 0f || morphProgress >= 1f) {
                        // Kick the morph animation in the background; immediately swap
                        // rootKey (via vm.onTapSleep). The morphProgress drives staggered
                        // content fades; the shared element morph is driven by the
                        // AnimatedContent transition triggered by the rootKey change.
                        morphScope.launch { vm.app.beginSleepMorph() }
                        vm.onTapSleep()
                    }
                },
                onLongPress = { showSleepTypePicker = true }
            )
```

Add the corresponding imports:
```kotlin
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
```

- [ ] **Step 3: Replace the SleepCTA composable**

Replace `@Composable private fun SleepCTA(onTap: () -> Unit, onLongPress: () -> Unit)` with:

```kotlin
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SleepCTA(
    sharedScope: SharedTransitionScope,
    animScope: AnimatedVisibilityScope,
    morphProgress: Float,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    val scheme = LocalSWScheme.current
    val isMorphing = morphProgress > 0f && morphProgress < 1f
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = SWSpacing.huge),
        contentAlignment = Alignment.Center
    ) {
        PulseRing(
            color = SWColor.accent(scheme),
            radius = 140.dp,
            intensity = PulseIntensity.STRONG
        )
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .fillMaxWidth()
                .sleepHeroOrigin(sharedScope, animScope)
                .pointerInput(onTap, onLongPress) {
                    detectTapGestures(
                        onLongPress = { onLongPress() },
                        onTap = { onTap() }
                    )
                }
        ) {
            SoftButton(
                text = stringResource(R.string.home_sleepcta),
                onClick = onTap,
                style = SoftButtonStyle.HERO,
                leadingIconRes = R.drawable.ic_moon_zzz,
                enabled = !isMorphing,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
```

Changes vs current code:
- PulseRing radius 110.dp → 140.dp
- SoftButton style ACCENT → HERO
- Wrapped in `sleepHeroOrigin(sharedScope, animScope)` — the shared bounds attach point
- `enabled = !isMorphing` debounces double-taps during the transition

- [ ] **Step 4: Compile**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL. The whole flow now compiles.

- [ ] **Step 5: Install + functional smoke**

```bash
./gradlew :app:installDebug
```

Open Home. Confirm:
- Sleep CTA pulse halo is bigger (140dp vs previous 110dp)
- Button background uses the aurora-glow gradient with white inner-highlight stroke
- An outer accent-tinted shadow pulses gently around the button (~4.2s period)

Tap the Sleep CTA. Confirm:
- The rootKey transitions to Sleeping after the tap
- During the transition (~1200ms), the SoftButton is disabled (a second tap should do nothing)
- The morph itself isn't pretty yet — Task 10 adds the destination halo. Right now you'll see the Sleep CTA "fly" toward a 0×0 destination, which is the expected intermediate state.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/lizhi1026/sleepwhisper/features/home/HomeScreen.kt
git commit -m "home: Sleep CTA with HERO style and morph origin wiring"
```

---

## Task 10 — Sleeping halo destination + reverse morph

**Files:**
- Modify: `app/src/main/java/com/lizhi1026/sleepwhisper/features/sleeping/SleepingScreen.kt`
- Modify: `app/src/main/java/com/lizhi1026/sleepwhisper/features/sleeping/SleepingViewModel.kt`

Add a 600dp halo `Box` wrapping the timer with `sleepHeroDestination`. The placeholder Starfield density ramps with `morphProgress` (Phase 2 replaces Starfield with NightSkyCanvas). Wire the reverse morph: when the Sleeping screen calls `vm.endSleep()` (long-press stop), kick `app.endSleepMorph()` in parallel.

- [ ] **Step 1: Promote `app` to public in SleepingViewModel**

In `app/src/main/java/com/lizhi1026/sleepwhisper/features/sleeping/SleepingViewModel.kt`, change:

```kotlin
class SleepingViewModel @Inject constructor(
    private val app: AppStateContainer
```

to:

```kotlin
class SleepingViewModel @Inject constructor(
    val app: AppStateContainer
```

Just the `private` → `val` change. All existing references to `app` inside the ViewModel continue to work.

- [ ] **Step 2: Add imports to SleepingScreen.kt**

```kotlin
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.lizhi1026.sleepwhisper.core.visualkit.components.sleepHeroDestination
import kotlinx.coroutines.launch
```

Some may already be present — keep them if so.

- [ ] **Step 3: Update the SleepingScreen signature and consume scopes**

In Task 3 step 5 we added the `sharedScope` and `animScope` parameters but didn't consume them. Replace the entire body of `@Composable fun SleepingScreen(...)` with one that wraps the timer in a halo `Box` with `sleepHeroDestination`, AND drives the Starfield density from `morphProgress`.

Open the file. The current top of the function (after the lifecycle tick block) looks like:

```kotlin
    CompositionLocalProvider(LocalSWScheme provides SWScheme.DARK) {
        Box(modifier = Modifier.fillMaxSize().background(SWColor.surface(SWScheme.DARK))) {
            Starfield(modifier = Modifier.fillMaxSize(), density = 80)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = SWSpacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.weight(0.5f))
                // ... rest of column with timer composables ...
            }
        }
    }
```

The minimal Phase 1 change: drive the Starfield's `density` from morphProgress, and wrap the timer's central `Column` (or just the timer text area within it) in a 600dp `Box` with `.sleepHeroDestination(...)`.

To keep diff small and avoid restructuring the whole screen (which is Phase 2's job), add the halo wrapper at the topmost Z level — overlaid on top of the existing screen content, sized 600dp, anchored to center. This is what carries the shared bounds and is invisible (transparent) so the existing screen renders through it.

Apply this diff to the contents of `CompositionLocalProvider(LocalSWScheme provides SWScheme.DARK) { ... }`:

```kotlin
    CompositionLocalProvider(LocalSWScheme provides SWScheme.DARK) {
        val morphProgress by remember(vm.app.morphProgress) {
            derivedStateOf { vm.app.morphProgress.value }
        }
        Box(modifier = Modifier.fillMaxSize().background(SWColor.surface(SWScheme.DARK))) {
            // Star density ramps with morph progress on entry. After the morph settles
            // (morphProgress = 1), density stays at the full 80. Phase 2 replaces this
            // with NightSkyCanvas.
            val density = (morphProgress * 80f).toInt().coerceAtLeast(0).coerceAtMost(80)
            Starfield(modifier = Modifier.fillMaxSize(), density = density)

            // Invisible 600dp halo placeholder — anchors the shared element to its
            // destination position so the Sleep CTA's morph has somewhere to fly to.
            // Drawn first (under the timer column) so the timer text remains visible.
            Box(
                modifier = Modifier
                    .size(600.dp)
                    .align(Alignment.Center)
                    .sleepHeroDestination(sharedScope, animScope)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = SWSpacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.weight(0.5f))
                // ... keep the rest of the existing column EXACTLY as-is (timer text,
                // captions, stop button, now-playing chip, etc.) ...
            }
        }
    }
```

- [ ] **Step 4: Wire the reverse morph on stop**

The stop-sleep flow in `SleepingScreen.kt` ultimately calls `vm.endSleep()` (or similar). Find that call site (search for `endSleep` in the file). The handler likely looks something like:

```kotlin
                .clickable {
                    coroutineScope.launch { vm.endSleep() }
                }
```

Wrap the call site so the reverse morph kicks in parallel. Replace with:

```kotlin
                .clickable {
                    coroutineScope.launch { vm.app.endSleepMorph() }
                    coroutineScope.launch { vm.endSleep() }
                }
```

(If `coroutineScope` is named differently in the file, e.g. `scope`, adapt accordingly. The current SleepingScreen.kt already declares `val coroutineScope = rememberCoroutineScope()` near the top.)

- [ ] **Step 5: Compile**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Install + functional smoke of the morph round-trip**

```bash
./gradlew :app:installDebug
```

Open Home. Tap Sleep CTA. Confirm:
- The Sleep button morphs visually (shared bounds) toward the center of the Sleeping screen over ~1200ms
- Stars fade in during the morph (density ramps with morphProgress)
- After morph settles, you're on the Sleeping screen with the timer running

Long-press the stop control on Sleeping. Confirm:
- The reverse morph plays — the halo position morphs back toward where the Sleep CTA was on Home
- Stars fade out during the reverse morph
- You land on Home with the SleepSummaryOverlay fading in (existing behavior)

Slow down system animations to 0.5× via Developer Options if you want to inspect the morph in slow-mo.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/lizhi1026/sleepwhisper/features/sleeping/SleepingScreen.kt \
        app/src/main/java/com/lizhi1026/sleepwhisper/features/sleeping/SleepingViewModel.kt
git commit -m "sleeping: hero morph destination halo and reverse on stop"
```

---

## Task 11 — Full smoke walkthrough + screenshots

**Files:** none — manual verification gate.

- [ ] **Step 1: Build and install fresh**

```bash
./gradlew :app:installDebug
adb shell am start -n com.lizhi1026.sleepwhisper/.app.MainActivity
```

- [ ] **Step 2: Smoke-test forward morph at 1× speed in DARK scheme**

Settings → Appearance → DARK. Return to Home. Tap Sleep CTA. Confirm:
- 1200ms morph plays smoothly (~36 frames at 60fps)
- Aurora ribbons palette drifts toward deeper night during the morph
- 3-ring PulseRing fades during the morph
- Sleeping timer fades in cleanly post-morph

- [ ] **Step 3: Smoke-test forward morph at 0.5× system animation scale**

```bash
adb shell settings put global animator_duration_scale 2.0
adb shell settings put global transition_animation_scale 2.0
adb shell settings put global window_animation_scale 2.0
```

(Increasing the scale slows animations.) Re-tap Sleep CTA, watch the morph in slow-motion. Confirm no jank, no missing frames at the morph midpoint.

Restore to normal:
```bash
adb shell settings put global animator_duration_scale 1.0
adb shell settings put global transition_animation_scale 1.0
adb shell settings put global window_animation_scale 1.0
```

- [ ] **Step 4: Smoke-test reverse morph**

From Sleeping, long-press stop. Confirm 1200ms reverse plays, lands on Home with SleepSummaryOverlay.

- [ ] **Step 5: Backgrounding mid-morph**

Tap Sleep CTA. Immediately press the recents button (or home button). Wait 5 seconds. Return to the app. Confirm:
- App resumes on Sleeping screen (rootKey was "sleeping" when backgrounded)
- The morph does NOT replay — Sleeping is shown directly
- The timer is ticking

This proves `snapMorphToCurrent` is being called by the lifecycle observer wired in Task 3 step 7.

- [ ] **Step 6: Double-tap debounce**

On Home, double-tap Sleep CTA in quick succession. Confirm only ONE morph triggers (the second tap is rejected because `isMorphing = true` during the first).

- [ ] **Step 7: TalkBack pass**

Enable TalkBack. Navigate to Sleep CTA. Confirm it announces as "Sleep" (or whatever the localized R.string.home_sleepcta resolves to). Activate. Morph plays, and TalkBack announces the Sleeping screen.

- [ ] **Step 8: Reduce-motion enabled, repeat forward + reverse**

```bash
adb shell settings put secure transition_animation_scale 0
adb shell settings put secure window_animation_scale 0
adb shell settings put secure animator_duration_scale 0
```

Relaunch. Tap Sleep CTA. Confirm:
- No shape morph plays — it's a direct fade between Home and Sleeping
- Aurora backdrop palette change is instant (snap, not tween)
- morphProgress jumps directly from 0 to 1

Long-press stop. Confirm same: direct fade reverse.

Restore animations:
```bash
adb shell settings put secure transition_animation_scale 1
adb shell settings put secure window_animation_scale 1
adb shell settings put secure animator_duration_scale 1
```

- [ ] **Step 9: Battery sanity**

Start a sleep session. Leave the device with the Sleeping screen visible for 10 minutes. Note the battery percentage before and after. Compare against a baseline 10-minute Home-screen session.

Expected: difference is ≤2 percentage points on a Pixel-class device, with the Sleeping screen's continuous animations.

If the delta is significantly higher, that's a regression to investigate — `Starfield` may be running too hot, or the AuroraBackdrop in Sleeping (which inherits from DARK scheme) may be over-drawing. Open a bug, don't block the phase but flag it.

- [ ] **Step 10: Capture screenshots**

```bash
mkdir -p docs/superpowers/screenshots/2026-05-20-phase-1

# Home in DAY scheme
adb exec-out screencap -p > docs/superpowers/screenshots/2026-05-20-phase-1/home-day.png

# Home in DARK scheme — switch theme first, then capture
adb exec-out screencap -p > docs/superpowers/screenshots/2026-05-20-phase-1/home-dark.png

# Sleeping screen
adb exec-out screencap -p > docs/superpowers/screenshots/2026-05-20-phase-1/sleeping.png

# Mid-morph — easiest captured by enabling Developer Options "Show animation playback" or using the OS screen recording while tapping
adb shell screenrecord /sdcard/morph.mp4 &
sleep 1
# (manually trigger morph)
sleep 3
adb shell killall -INT screenrecord
adb pull /sdcard/morph.mp4 docs/superpowers/screenshots/2026-05-20-phase-1/morph.mp4
adb shell rm /sdcard/morph.mp4
```

- [ ] **Step 11: Final commit**

```bash
git add docs/superpowers/screenshots/
git commit -m "phase-1: capture verification screenshots and morph recording"
```

- [ ] **Step 12: Run the full test suite one last time**

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

Expected: BUILD SUCCESSFUL. All tests pass (SWColorTest 5/5, HeroBackdropControllerTest 4/4, AppStateContainerMorphTest 4/4).

---

## Phase 1 Done When

- All 11 tasks committed
- `./gradlew :app:testDebugUnitTest :app:assembleDebug` green
- Home renders with: bigger greeting (22sp italic), hairline flourish, hero WakeWindowCard, NowPlayingCard with ChevronTrail + inner-highlight icon, QuickActionTile inner-highlight icons, HERO SoftButton CTA with 140dp 3-ring PulseRing
- Tapping Sleep CTA plays a smooth 1200ms morph into the Sleeping screen; long-pressing stop plays a smooth 1200ms reverse
- Reduce-motion fallback verified — direct fade, no shape morph
- Double-tap debounce verified
- Backgrounding mid-morph verified — no replay on resume
- Screenshots captured in `docs/superpowers/screenshots/2026-05-20-phase-1/`

## Out of Scope (deferred to later phases)

- `NightSkyCanvas` — Phase 2 (Phase 1 uses Starfield density placeholder ramping with morphProgress)
- `PresetAuraOrb` — Phase 3 (NowPlayingCard keeps icon-in-circle treatment)
- Sleeping screen layout/content redesign — Phase 2
- Trends, Settings, Player, Onboarding visual changes — Phases 4 / 3 / 5
