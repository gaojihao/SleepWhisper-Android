# Phase 4 — Trends + Settings Visual Polish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Apply Phase 0 design tokens to Trends and Settings — `SectionLabel` replacing inline uppercase BasicText, `Hairline` replacing ad-hoc dividers, `SerifMetricRow` replacing label/value patterns, and the WeeklyBars chart fill switching to a vertical aurora gradient.

**Architecture:** Pure visual polish, no business logic or string changes. Settings has a private `LabelRow(label, value)` used 4 times and a private `Divider()` used 7 times — both get replaced. Trends has inline `BasicText` for section titles in WeeklyCard and TodayCard — both switch to the shared `SectionLabel`. WeeklyBars' `drawRoundRect` call gains a `Brush.verticalGradient(accent → accentSecondary)` for today's column.

**Tech Stack:** Compose BOM 2024.12.01, existing Phase 0 shared components (`SectionLabel`, `Hairline`, `SerifMetricRow`, `ChevronTrail`, `Modifier.innerHighlight`).

---

## File Map

**Modified:**
- `app/src/main/java/com/lizhi1026/sleepwhisper/features/trends/TrendsScreen.kt`
- `app/src/main/java/com/lizhi1026/sleepwhisper/features/trends/TodayTimeline.kt` (audit only — likely no changes needed)
- `app/src/main/java/com/lizhi1026/sleepwhisper/features/trends/RecentEventsList.kt` (audit only)
- `app/src/main/java/com/lizhi1026/sleepwhisper/features/settings/SettingsScreen.kt`

**Created:** none

**Deleted:** none (Settings's private `LabelRow` and `Divider` definitions go away inside SettingsScreen.kt, but the file remains)

---

## Conventions

- Commit messages: `area: short imperative summary` (`trends:`, `settings:`)
- After each task, `./gradlew :app:compileDebugKotlin` before committing
- HEAD at plan-write time: `0039b34` (Phase 4 spec committed)

---

## Task 1 — TrendsScreen: header Hairline + WeeklyCard SectionLabel + Today SectionLabel

**Files:**
- Modify: `app/src/main/java/com/lizhi1026/sleepwhisper/features/trends/TrendsScreen.kt`

- [ ] **Step 1: Add imports**

In `TrendsScreen.kt`, add:

```kotlin
import androidx.compose.foundation.layout.width
import com.lizhi1026.sleepwhisper.core.visualkit.components.Hairline
import com.lizhi1026.sleepwhisper.core.visualkit.components.SectionLabel
```

`width` may already be imported — check.

- [ ] **Step 2: Update Header composable to add Hairline flourish**

Find the existing `@Composable private fun Header(babyName: String?)` function. Replace its body entirely with:

```kotlin
@Composable
private fun Header(babyName: String?) {
    val scheme = LocalSWScheme.current
    Column(verticalArrangement = Arrangement.spacedBy(SWSpacing.xs)) {
        BasicText(
            text = stringResource(R.string.trends_title),
            style = SWFont.serifItalic(28).copy(color = SWColor.textPrimary(scheme))
        )
        Hairline(modifier = Modifier.width(60.dp).padding(top = SWSpacing.xs))
        BasicText(
            text = stringResource(R.string.trends_subtitle, babyName ?: "").uppercase(),
            style = SWFont.labelMD().copy(
                color = SWColor.textSecondary(scheme),
                letterSpacing = 1.2.sp
            )
        )
    }
}
```

Key change: `Column.spacedBy(SWSpacing.xxs)` → `SWSpacing.xs` (gives Hairline breathing room), and the new `Hairline(modifier = ...width(60.dp).padding(top = ...))` line between title and subtitle.

- [ ] **Step 3: Update TodayCard's title to use SectionLabel**

Find `@Composable private fun TodayCard(...)`. Replace the inner Column with:

```kotlin
    GlassCard(
        modifier = Modifier.fillMaxWidth().floatingY(amplitude = 2.dp, durationMillis = 9000),
        cornerRadius = 22.dp,
        contentPadding = PaddingValues(SWSpacing.lg),
        elevation = ElevationLevel.MEDIUM
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(SWSpacing.sm)) {
            SectionLabel(stringResource(R.string.trends_today))
            TodayTimeline(sleeps = sleeps, feedings = feedings)
        }
    }
```

The `BasicText(text = ..., style = SWFont.titleMD()...)` line is replaced with `SectionLabel(stringResource(R.string.trends_today))`. Same call — Phase 0's SectionLabel handles uppercase + tracked + textSecondary styling.

- [ ] **Step 4: Update WeeklyCard's title to use SectionLabel + avg as SerifMetricRow pattern**

Find `@Composable private fun WeeklyCard(buckets: List<Pair<String, Long>>)`. Replace its body:

```kotlin
@Composable
private fun WeeklyCard(buckets: List<Pair<String, Long>>) {
    val scheme = LocalSWScheme.current
    val totalSec = buckets.sumOf { it.second }
    val avgHours = totalSec.toDouble() / 3600.0 / 7.0
    val avgHoursFormatted = "%.1f".format(avgHours)
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        contentPadding = PaddingValues(SWSpacing.lg),
        elevation = ElevationLevel.MEDIUM
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(SWSpacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                SectionLabel(stringResource(R.string.trends_week_title))
                BasicText(
                    text = stringResource(R.string.trends_week_avg, avgHoursFormatted),
                    style = SWFont.titleMD().copy(color = SWColor.textPrimary(scheme))
                )
            }
            Hairline()
            WeeklyBars(buckets)
        }
    }
}
```

Key changes:
- Title `BasicText titleMD` → `SectionLabel`
- avg-hours text upgraded from `bodyMD textSecondary` → `titleMD textPrimary` (the data point earns the larger weight)
- New `Hairline()` between title row and bars

- [ ] **Step 5: Compile**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/lizhi1026/sleepwhisper/features/trends/TrendsScreen.kt
git commit -m "trends: header Hairline + WeeklyCard SectionLabel and avg upgrade"
```

---

## Task 2 — WeeklyBars chart gradient

**Files:**
- Modify: `app/src/main/java/com/lizhi1026/sleepwhisper/features/trends/TrendsScreen.kt`

- [ ] **Step 1: Add imports**

In `TrendsScreen.kt`, add (if missing):

```kotlin
import androidx.compose.ui.graphics.Brush
```

- [ ] **Step 2: Add a private chartBarBrush helper**

Inside `TrendsScreen.kt`, after the `WeeklyBars` function, add:

```kotlin
/**
 * Vertical gradient brush for the WeeklyBars chart. Today's column uses a
 * dual-color gradient that visually pops; non-today columns use accent at
 * 70% alpha for a quieter row that doesn't compete.
 */
@androidx.compose.runtime.Composable
private fun chartBarBrush(isHighlighted: Boolean): Brush {
    val scheme = LocalSWScheme.current
    return if (isHighlighted) {
        Brush.verticalGradient(
            colors = listOf(
                SWColor.accent(scheme),
                SWColor.accentSecondary(scheme)
            )
        )
    } else {
        val base = SWColor.accent(scheme).copy(alpha = 0.7f)
        Brush.verticalGradient(colors = listOf(base, base))
    }
}
```

- [ ] **Step 3: Use the helper in WeeklyBars**

Find the existing `Canvas(modifier = Modifier.width(22.dp).height(180.dp)) { ... }` block inside `WeeklyBars`. The current code reads:

```kotlin
                    Canvas(modifier = Modifier.width(22.dp).height(180.dp)) {
                        val h = (secs.toFloat() / maxSec) * size.height
                        val capsuleH = h.coerceAtLeast(8.dp.toPx())
                        drawRoundRect(
                            color = if (isToday) SWColor.accent(scheme)
                                    else SWColor.primary(scheme).copy(alpha = 0.7f),
                            topLeft = Offset(0f, size.height - capsuleH),
                            size = Size(size.width, capsuleH),
                            cornerRadius = CornerRadius(size.width / 2f)
                        )
                    }
```

The `drawRoundRect(color = ...)` parameter must change to `brush = chartBarBrush(...)`. But `chartBarBrush` is `@Composable` and `Canvas { }` is a DrawScope, not a Composable — so we have to compute the brush OUTSIDE the Canvas lambda.

Replace the Canvas block with:

```kotlin
                    val brush = chartBarBrush(isHighlighted = isToday)
                    Canvas(modifier = Modifier.width(22.dp).height(180.dp)) {
                        val h = (secs.toFloat() / maxSec) * size.height
                        val capsuleH = h.coerceAtLeast(8.dp.toPx())
                        drawRoundRect(
                            brush = brush,
                            topLeft = Offset(0f, size.height - capsuleH),
                            size = Size(size.width, capsuleH),
                            cornerRadius = CornerRadius(size.width / 2f)
                        )
                    }
```

Note `color = ...` is replaced by `brush = brush`. The brush is computed in the parent Composable scope (`Row` / `Column` / `Canvas`'s composable enclosing parent).

- [ ] **Step 4: Compile**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/lizhi1026/sleepwhisper/features/trends/TrendsScreen.kt
git commit -m "trends: WeeklyBars uses vertical aurora gradient for highlighted column"
```

---

## Task 3 — TodayTimeline + RecentEventsList audit

**Files:**
- Audit/modify: `app/src/main/java/com/lizhi1026/sleepwhisper/features/trends/TodayTimeline.kt`
- Audit/modify: `app/src/main/java/com/lizhi1026/sleepwhisper/features/trends/RecentEventsList.kt`

These files were written before the Aurora redesign; they may contain inline uppercase BasicText for section labels, hardcoded `Color.White`, or ad-hoc dividers that should be replaced with shared components.

- [ ] **Step 1: Read both files**

```bash
cat app/src/main/java/com/lizhi1026/sleepwhisper/features/trends/TodayTimeline.kt
cat app/src/main/java/com/lizhi1026/sleepwhisper/features/trends/RecentEventsList.kt
```

- [ ] **Step 2: For TodayTimeline.kt — replace any inline section-label patterns and hardcoded Color.White**

If there is `BasicText(text = "...".uppercase(), style = SWFont.labelMD()...)` patterns:
- Add `import com.lizhi1026.sleepwhisper.core.visualkit.components.SectionLabel`
- Replace with `SectionLabel("...")`

If there is `Color.White` hardcoded:
- Replace with `SWColor.textPrimary(scheme)` (where `scheme = LocalSWScheme.current` is already in scope)

If there are no such patterns, the file is clean — leave it alone and add nothing.

- [ ] **Step 3: For RecentEventsList.kt — same audit**

Same replacement rules as Step 2.

Additionally, if event rows have no visible separator between them but visually need one:
- Add `import com.lizhi1026.sleepwhisper.core.visualkit.components.Hairline`
- Insert `Hairline()` between adjacent event Composables in the LazyColumn `items` block

If event rows are GlassCards with native borders, no Hairline needed.

- [ ] **Step 4: Compile**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit (only if changes were made)**

```bash
git add app/src/main/java/com/lizhi1026/sleepwhisper/features/trends/TodayTimeline.kt \
        app/src/main/java/com/lizhi1026/sleepwhisper/features/trends/RecentEventsList.kt
git commit -m "trends: align TodayTimeline and RecentEventsList with shared visualkit"
```

If no changes were needed (clean audit), skip the commit and proceed to Task 4.

---

## Task 4 — SettingsScreen: header Hairline + SectionCard title uses SectionLabel

**Files:**
- Modify: `app/src/main/java/com/lizhi1026/sleepwhisper/features/settings/SettingsScreen.kt`

- [ ] **Step 1: Add imports**

In `SettingsScreen.kt`, add:

```kotlin
import androidx.compose.foundation.layout.width
import com.lizhi1026.sleepwhisper.core.visualkit.components.Hairline
import com.lizhi1026.sleepwhisper.core.visualkit.components.SectionLabel
```

- [ ] **Step 2: Add Hairline below the Settings header**

Find the existing top `BasicText(text = R.string.settings_title, style = SWFont.serifItalic(28)...)` block inside `SettingsScreen`. Immediately after it (still inside the outer Column), add:

```kotlin
            Hairline(modifier = Modifier.width(60.dp).padding(bottom = SWSpacing.xs))
```

The result: title → 60dp Hairline → first SectionCard. The Column already has `spacedBy(SWSpacing.md)` so the Hairline gets its own ~16dp of space.

- [ ] **Step 3: Update SectionCard's title rendering to use SectionLabel**

Find the existing `@Composable private fun SectionCard(title: String, content: @Composable () -> Unit)` function. Inside its body, find the line that renders the title — likely something like:

```kotlin
        BasicText(
            text = title.uppercase(),
            style = SWFont.labelMD().copy(
                color = SWColor.textSecondary(scheme),
                letterSpacing = 1.2.sp
            )
        )
```

Replace with:

```kotlin
        SectionLabel(title)
```

`SectionLabel` from Phase 0 wraps title in uppercase, tracked, textSecondary automatically.

- [ ] **Step 4: Compile**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/lizhi1026/sleepwhisper/features/settings/SettingsScreen.kt
git commit -m "settings: header Hairline + SectionCard title via SectionLabel"
```

---

## Task 5 — SettingsScreen: LabelRow → SerifMetricRow + Divider → Hairline

**Files:**
- Modify: `app/src/main/java/com/lizhi1026/sleepwhisper/features/settings/SettingsScreen.kt`

There are 4 `LabelRow(label, value)` call sites (lines 87, 92, 183, 196) and 7 `Divider()` call sites (lines 91, 110, 121, 153, 182, 187, 195 — actually 7 total). Plus the private `LabelRow` definition at line 251 and private `Divider` at line 343 — both can be deleted if no other consumers.

- [ ] **Step 1: Add the SerifMetricRow import**

```kotlin
import com.lizhi1026.sleepwhisper.core.visualkit.components.SerifMetricRow
```

(`Hairline` import from Task 4 is already there.)

- [ ] **Step 2: Replace all `LabelRow(...)` call sites**

For each `LabelRow(label = ..., value = ...)` call in the file, replace with `SerifMetricRow(label = ..., value = ..., showChevron = false)`.

Sample replacement:

```kotlin
                    LabelRow(
                        label = stringResource(R.string.settings_baby_name),
                        value = b.name
                    )
```

becomes:

```kotlin
                    SerifMetricRow(
                        label = stringResource(R.string.settings_baby_name),
                        value = b.name,
                        showChevron = false
                    )
```

Apply to all 4 `LabelRow(...)` call sites.

- [ ] **Step 3: Replace all `Divider()` calls with `Hairline()`**

For each bare `Divider()` call, replace with `Hairline()`.

- [ ] **Step 4: Delete the private `LabelRow` and `Divider` definitions**

Find the private definitions (search for `private fun LabelRow` and `private fun Divider`). Delete both functions entirely. They are no longer referenced.

- [ ] **Step 5: Compile**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Run unit tests**

```bash
./gradlew :app:testDebugUnitTest
```

Expected: BUILD SUCCESSFUL — Settings has no specific unit tests but the rest of the suite confirms nothing broke.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/lizhi1026/sleepwhisper/features/settings/SettingsScreen.kt
git commit -m "settings: LabelRow → SerifMetricRow and Divider → Hairline"
```

---

## Task 6 — Manual smoke + screenshots

**Files:** none — verification.

- [ ] **Step 1: Build and install**

```bash
./gradlew :app:installDebug
adb shell am start -n com.lizhi1026.sleepwhisper/.app.MainActivity
```

- [ ] **Step 2: Run full test suite**

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

Expected: BUILD SUCCESSFUL across all tests.

- [ ] **Step 3: Verify Trends visually**

Switch to Trends tab. Confirm:
- Header "Trends" + 60dp Hairline below + uppercase subtitle
- TodayCard "TODAY" section label is uppercase tracked (SectionLabel style)
- WeeklyCard "THIS WEEK" matches the same style; avg hours appears in titleMD on the right
- Hairline between WeeklyCard title row and chart
- WeeklyBars: today's column shows a vertical gradient (orange top → purple bottom); other days show a quieter accent fill

- [ ] **Step 4: Verify Settings visually**

Switch to Settings tab. Confirm:
- Header "Settings" + 60dp Hairline below
- Each SectionCard's title is in SectionLabel style (uppercase, tracked, textSecondary)
- BABY section's "Name / Emma" "Age / 142 days" rows render via SerifMetricRow (left-label, right-value, no chevron)
- Row separators between Name/Age/Theme/etc. are hairline-thin (Hairline)

- [ ] **Step 5: Capture screenshots**

```bash
mkdir -p docs/superpowers/screenshots/2026-05-21-phase-4
adb exec-out screencap -p > docs/superpowers/screenshots/2026-05-21-phase-4/trends.png
# Switch to Settings tab manually, then:
adb exec-out screencap -p > docs/superpowers/screenshots/2026-05-21-phase-4/settings.png
```

- [ ] **Step 6: Commit screenshots**

```bash
git add docs/superpowers/screenshots/2026-05-21-phase-4/
git commit -m "phase-4: verification screenshots for trends and settings polish"
```

---

## Phase 4 Done When

- All 6 tasks committed
- `./gradlew :app:testDebugUnitTest :app:assembleDebug` green
- Trends WeeklyBars today column shows vertical aurora gradient
- Trends + Settings headers have 60dp Hairline flourish
- All SectionCard / Trends card titles use shared `SectionLabel`
- Settings LabelRow patterns replaced by `SerifMetricRow`
- Private `LabelRow` and `Divider` removed from SettingsScreen.kt
- Screenshots captured

## Out of Scope (deferred)

- Onboarding + Welcome visual → Phase 5
- `Starfield.kt` deletion → Phase 5
- New Trends chart types / data dimensions
- Settings permission flow changes
