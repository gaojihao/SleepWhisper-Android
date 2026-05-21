# Phase 5 — Onboarding + Welcome Ritual + Starfield Deletion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Apply Aurora design language to Onboarding and WelcomeRitualScreen, ascending the baby-name treatment to 96sp DM Serif Display in WelcomeRitual ("the moment of meeting"), then finally delete `Starfield.kt` now that no source references remain.

**Architecture:** Pure visual polish + a strategic typography move. Both screens currently call `Starfield(density = ...)` — replaced with `NightSkyCanvas(morphProgress = 1f)` for time-of-day-aware backgrounds. Hero circles gain `Modifier.innerHighlight` and `SWGradient.auroraGlow` instead of single-color fills. WelcomeRitual baby-name jumps from inline 44sp Sans-Serif to `SWFont.displayXL()` (96sp DM Serif Display) wrapped in `SWFont.cjkAware(...)` for Chinese fallback. Onboarding's ad-hoc `Field`/`Divider`/`Double.sp` private helpers get cleaned up via Phase 0 shared components.

**Tech Stack:** Compose BOM 2024.12.01, existing Phase 0 / 1 / 2 visualkit (`NightSkyCanvas`, `SectionLabel`, `Hairline`, `SoftButton`, `PulseRing`, `Modifier.innerHighlight`, `SWFont.displayXL`, `SWFont.cjkAware`, `SWGradient.auroraGlow`).

---

## File Map

**Modified:**
- `app/src/main/java/com/lizhi1026/sleepwhisper/features/onboarding/OnboardingScreen.kt`
- `app/src/main/java/com/lizhi1026/sleepwhisper/features/onboarding/WelcomeRitualScreen.kt`

**Deleted:**
- `app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/Starfield.kt`

**Created:** none

---

## Conventions

- Commits: `area: short imperative summary` (`onboarding:`, `welcome:`, `visualkit:` for the deletion)
- After each task, `./gradlew :app:compileDebugKotlin` before committing
- HEAD at plan-write time: `43c7911` (Phase 5 spec committed)

---

## Task 1 — OnboardingScreen: Starfield → NightSkyCanvas + Hero auroraGlow

**Files:**
- Modify: `app/src/main/java/com/lizhi1026/sleepwhisper/features/onboarding/OnboardingScreen.kt`

- [ ] **Step 1: Update imports**

In `OnboardingScreen.kt` swap:

```kotlin
// Remove:
import com.lizhi1026.sleepwhisper.core.visualkit.components.Starfield
// Add:
import com.lizhi1026.sleepwhisper.core.visualkit.components.NightSkyCanvas
import com.lizhi1026.sleepwhisper.core.visualkit.components.innerHighlight
```

- [ ] **Step 2: Replace Starfield call**

Find:

```kotlin
        Starfield(modifier = Modifier.fillMaxSize(), density = if (scheme == SWScheme.DAY) 30 else 70)
```

Replace with:

```kotlin
        NightSkyCanvas(modifier = Modifier.fillMaxSize(), morphProgress = 1f)
```

- [ ] **Step 3: Update Hero solid circle to auroraGlow + innerHighlight**

Find:

```kotlin
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(SWGradient.accent(scheme))
                )
```

Replace with:

```kotlin
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(SWGradient.auroraGlow(scheme))
                        .innerHighlight(cornerRadius = 44.dp)
                )
```

- [ ] **Step 4: Compile**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit (deferred — bundle with Task 2)**

Don't commit yet — Task 2 modifies the same file.

---

## Task 2 — OnboardingScreen: Field uses SectionLabel; delete Divider + Double.sp

**Files:**
- Modify: `app/src/main/java/com/lizhi1026/sleepwhisper/features/onboarding/OnboardingScreen.kt`

- [ ] **Step 1: Add SectionLabel + Hairline imports**

In `OnboardingScreen.kt` add:

```kotlin
import com.lizhi1026.sleepwhisper.core.visualkit.components.SectionLabel
import com.lizhi1026.sleepwhisper.core.visualkit.components.Hairline
```

- [ ] **Step 2: Replace Field's inner BasicText with SectionLabel**

Find the existing `Field` private composable:

```kotlin
@Composable
private fun Field(label: String, content: @Composable () -> Unit) {
    val scheme = LocalSWScheme.current
    Column(verticalArrangement = Arrangement.spacedBy(SWSpacing.xs)) {
        BasicText(
            text = label.uppercase(),
            style = SWFont.labelMD().copy(
                color = SWColor.textSecondary(scheme),
                letterSpacing = 1.4.sp()
            )
        )
        content()
    }
}
```

Replace with:

```kotlin
@Composable
private fun Field(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(SWSpacing.xs)) {
        SectionLabel(label)
        content()
    }
}
```

The `val scheme = ...` line goes away (no longer needed). The inline `BasicText(... .uppercase()...)` becomes `SectionLabel(label)`.

- [ ] **Step 3: Replace 2 `Divider()` call sites with `Hairline()`**

Find both `Divider()` calls inside the form (lines 136 and 171 — there are exactly 2). Replace each `Divider()` with `Hairline()`.

- [ ] **Step 4: Delete the private Divider composable**

Find:

```kotlin
@Composable
private fun Divider() {
    val scheme = LocalSWScheme.current
    Box(
        modifier = Modifier
            .height(1.dp)
            .background(SWColor.border(scheme).copy(alpha = 0.2f))
    )
}
```

Delete it entirely.

- [ ] **Step 5: Delete the private Double.sp extension**

Find:

```kotlin
private fun Double.sp(): androidx.compose.ui.unit.TextUnit =
    androidx.compose.ui.unit.TextUnit(this.toFloat(), androidx.compose.ui.unit.TextUnitType.Sp)
```

Delete it. (After step 2, the only caller `1.4.sp()` is gone, so this extension is dead.)

- [ ] **Step 6: Compile**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit (Tasks 1 + 2 together)**

```bash
git add app/src/main/java/com/lizhi1026/sleepwhisper/features/onboarding/OnboardingScreen.kt
git commit -m "onboarding: NightSkyCanvas background, auroraGlow hero, SectionLabel+Hairline"
```

---

## Task 3 — WelcomeRitualScreen: Starfield → NightSkyCanvas + unified hero auroraGlow + accentSecondary ring

**Files:**
- Modify: `app/src/main/java/com/lizhi1026/sleepwhisper/features/onboarding/WelcomeRitualScreen.kt`

- [ ] **Step 1: Update imports**

In `WelcomeRitualScreen.kt`:

```kotlin
// Remove:
import com.lizhi1026.sleepwhisper.core.visualkit.components.Starfield
// Add:
import com.lizhi1026.sleepwhisper.core.visualkit.components.NightSkyCanvas
import com.lizhi1026.sleepwhisper.core.visualkit.components.innerHighlight
```

- [ ] **Step 2: Remove the conditional purple-hero variables**

Find:

```kotlin
    val isPurpleHero = scheme == SWScheme.DAY
    val ringStrokeColor = if (isPurpleHero) Color(0.45f, 0.36f, 0.85f) else SWColor.accent(scheme)
    val heroSolidColor = if (isPurpleHero) SWColor.softLilac(scheme) else SWColor.accent(scheme)
```

Replace with:

```kotlin
    val ringStrokeColor = SWColor.accentSecondary(scheme)
```

The `heroSolidColor` variable is gone (replaced inline in step 4).

- [ ] **Step 3: Replace Starfield call**

Find:

```kotlin
        Starfield(modifier = Modifier.fillMaxSize(), density = 60)
```

Replace with:

```kotlin
        NightSkyCanvas(modifier = Modifier.fillMaxSize(), morphProgress = 1f)
```

- [ ] **Step 4: Update hero solid circle to auroraGlow + innerHighlight**

Find:

```kotlin
                // hero solid circle (always rendered)
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .background(heroSolidColor, CircleShape)
                )
```

Replace with:

```kotlin
                // hero solid circle (always rendered) — Aurora-glow gradient with inner highlight
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(SWGradient.auroraGlow(scheme))
                        .innerHighlight(cornerRadius = 44.dp)
                )
```

Note: changes from `.background(color, shape)` to `.clip(CircleShape).background(brush).innerHighlight(...)` — explicit clip step is required because background-with-brush doesn't take a shape argument.

- [ ] **Step 5: Compile**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL. The unused `Color` import (was used by `Color(0.45f, 0.36f, 0.85f)`) might trigger a warning — leave it for Task 4 to clean up if still unused.

- [ ] **Step 6: Commit (deferred — bundle with Task 4)**

Don't commit yet — Task 4 modifies the same file.

---

## Task 4 — WelcomeRitualScreen: 96sp baby name + Hairline + greeting 22sp

**Files:**
- Modify: `app/src/main/java/com/lizhi1026/sleepwhisper/features/onboarding/WelcomeRitualScreen.kt`

- [ ] **Step 1: Add Hairline + width imports**

```kotlin
import androidx.compose.foundation.layout.width
import com.lizhi1026.sleepwhisper.core.visualkit.components.Hairline
```

- [ ] **Step 2: Update Stage 2 greeting to serifItalic(22)**

Find inside the Stage 2 `AnimatedVisibility` block:

```kotlin
                    BasicText(
                        text = stringResource(R.string.welcome_greeting),
                        style = SWFont.serifItalic(17).copy(
                            color = SWColor.textPrimary(scheme).copy(alpha = 0.85f)
                        )
                    )
```

Replace `serifItalic(17)` with `serifItalic(22)`:

```kotlin
                    BasicText(
                        text = stringResource(R.string.welcome_greeting),
                        style = SWFont.serifItalic(22).copy(
                            color = SWColor.textPrimary(scheme).copy(alpha = 0.85f)
                        )
                    )
```

- [ ] **Step 3: Update baby name to 96sp DM Serif Display + Hairline**

Find:

```kotlin
                    Spacer(Modifier.height(SWSpacing.xs))
                    BasicText(
                        text = baby?.name ?: "",
                        style = androidx.compose.ui.text.TextStyle(
                            fontSize = 44.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SWColor.primary(scheme)
                        )
                    )
```

Replace with:

```kotlin
                    Spacer(Modifier.height(SWSpacing.xs))
                    BasicText(
                        text = baby?.name ?: "",
                        style = SWFont.cjkAware(
                            SWFont.displayXL().copy(color = SWColor.textPrimary(scheme))
                        ),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Visible
                    )
                    Spacer(Modifier.height(SWSpacing.xs))
                    Hairline(modifier = Modifier.width(60.dp))
```

The unused `FontWeight` import can be removed (though leaving it is harmless — only a lint warning).

- [ ] **Step 4: Clean up the now-unused Color import**

If grep `Color\(` in the file returns no remaining call sites that need the explicit Compose Color (besides `SWColor.textPrimary(scheme).copy(...)` which uses Color via .copy method internally — actually `.copy(alpha = 0.85f)` doesn't need a Color literal, it's a method on Color), the explicit `import androidx.compose.ui.graphics.Color` may be unused. Run:

```bash
grep -n "Color(" app/src/main/java/com/lizhi1026/sleepwhisper/features/onboarding/WelcomeRitualScreen.kt
```

If no `Color(` literal use remains, remove the `import androidx.compose.ui.graphics.Color` line. If even one literal remains, keep the import.

- [ ] **Step 5: Compile**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit (Tasks 3 + 4 together)**

```bash
git add app/src/main/java/com/lizhi1026/sleepwhisper/features/onboarding/WelcomeRitualScreen.kt
git commit -m "welcome: NightSkyCanvas background, auroraGlow hero, 96sp DM Serif baby name"
```

---

## Task 5 — Delete Starfield.kt + smoke + screenshots

**Files:**
- Delete: `app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/Starfield.kt`

- [ ] **Step 1: Verify no source references remain**

```bash
grep -rn "Starfield" app/src/main/java/ app/src/test/java/ app/src/androidTest/java/ 2>/dev/null
```

Expected: empty (the only matches should be inside `Starfield.kt` itself, which is the file being deleted).

If any other file still contains `Starfield` references, BLOCKED — fix the references first before deleting.

- [ ] **Step 2: Delete the file**

```bash
rm app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/Starfield.kt
```

- [ ] **Step 3: Verify nothing references Starfield post-deletion**

```bash
grep -rn "Starfield" app/src/ 2>/dev/null || echo "Clean"
```

Expected: "Clean".

- [ ] **Step 4: Compile + run tests**

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:testDebugUnitTest
```

Expected: BUILD SUCCESSFUL on both.

- [ ] **Step 5: Assemble debug APK**

```bash
./gradlew :app:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Install on device + smoke**

```bash
./gradlew :app:installDebug
adb shell pm clear com.lizhi1026.sleepwhisper
adb shell am start -n com.lizhi1026.sleepwhisper/.app.MainActivity
```

(`pm clear` resets app state so we hit Onboarding fresh.)

- [ ] **Step 7: Capture screenshots**

```bash
mkdir -p docs/superpowers/screenshots/2026-05-21-phase-5
sleep 3
adb exec-out screencap -p > docs/superpowers/screenshots/2026-05-21-phase-5/onboarding.png
# Manually fill the form and tap Save to trigger WelcomeRitual...
# After Welcome stage 2 is showing:
adb exec-out screencap -p > docs/superpowers/screenshots/2026-05-21-phase-5/welcome.png
```

(The Welcome screenshot timing is best-effort since the ritual is auto-advancing. If the auto-capture catches stage 1 or 3 instead of 2, that's still useful evidence — the visual upgrade is verifiable on any stage.)

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "visualkit: delete Starfield (subsumed by NightSkyCanvas in Phase 5)"
```

---

## Phase 5 Done When

- All 5 tasks committed
- `./gradlew :app:testDebugUnitTest :app:assembleDebug` green
- `Starfield.kt` no longer exists in the source tree
- `grep -rn "Starfield" app/src/` returns nothing
- Onboarding renders with NightSkyCanvas background and auroraGlow hero
- Welcome ritual renders with NightSkyCanvas background, baby name in 96sp DM Serif, 60dp Hairline below
- Screenshots captured

## Out of Scope

- Application icon / launch screen → potential Phase 6 (currently no scoped phase)
- Onboarding new fields / business logic
- Welcome ritual timing / animation choreography
- Documenting "Aurora redesign complete" in repo README
