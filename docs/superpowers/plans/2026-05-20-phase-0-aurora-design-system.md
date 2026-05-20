# Phase 0 — Aurora Design System Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace SleepWhisper's design tokens (color, typography, motion) with the Aurora system and evolve/add the shared components every later phase will consume. Layouts of existing screens stay unchanged; only their visual treatment shifts as tokens propagate.

**Architecture:** Modify `core/visualkit/` in place. Public API of `SWColor` / `SWFont` / `SWMotion` is preserved (signatures unchanged) so screen files don't need rewrites. New decorations (`Hairline`, `SectionLabel`, etc.) and a `HeroBackdropController` CompositionLocal are added but not yet wired to screens — that happens in Phases 1-5.

**Tech Stack:** Kotlin 2.0.21, Jetpack Compose BOM 2024.12.01, Android minSdk 30 / targetSdk 35, Hilt, Source Serif 4 + DM Serif Display (bundled), Noto Serif CJK SC (downloadable via `androidx.compose.ui.text.googlefonts`).

---

## File Map

**Modified:**
- `app/build.gradle.kts` — add `ui-text-google-fonts` dependency
- `gradle/libs.versions.toml` — add font library entry
- `app/src/main/res/values/strings.xml` — add `R.string.com_google_android_gms_fonts_certs` & `R.array.com_google_android_gms_fonts_certs_dev`/`_prod` (Compose Google Fonts cert table)
- `app/src/main/res/xml/font_certs.xml` (new) — cert reference resource
- `app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/SWTokens.kt` — refresh `SWMotion`, add `SWSpring`
- `app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/SWColor.kt` — Aurora palette, add `accentSecondary`
- `app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/SWGradient.kt` — add `auroraBackdrop`, `auroraGlow`, `moonHalo`
- `app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/SWFont.kt` — rebuild around DM Serif Display + Source Serif 4 + CJK fallback
- `app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/GlassCard.kt` — moonlit-glass treatment, `hero` parameter, dual-layer shadow
- `app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/BreathingBackground.kt` — rename function to `AuroraBackdrop`, 3 drifting ribbons + film grain
- `app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/PulseRing.kt` — 3-ring offset pulse, aurora hues
- `app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/AudioWaveform.kt` — sine-perturbed bars + along-bar gradient + spring tween
- `app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/RollingNumber.kt` — serif tabular + vertical motion blur on roll
- `app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/SoftButton.kt` — `hero` style variant
- `app/src/main/java/com/lizhi1026/sleepwhisper/features/home/HomeScreen.kt` — rename `BreathingBackground()` → `AuroraBackdrop()`
- `app/src/main/java/com/lizhi1026/sleepwhisper/features/trends/TrendsScreen.kt` — same rename
- `app/src/main/java/com/lizhi1026/sleepwhisper/features/player/PlayerScreen.kt` — same rename
- `app/src/main/java/com/lizhi1026/sleepwhisper/features/onboarding/OnboardingScreen.kt` — same rename if present
- `app/src/main/java/com/lizhi1026/sleepwhisper/features/onboarding/WelcomeRitualScreen.kt` — same rename if present
- `app/src/main/java/com/lizhi1026/sleepwhisper/features/home/EventEditSheet.kt` — same rename if present

**Created (new files):**
- `app/src/main/res/font/dm_serif_display_regular.ttf`
- `app/src/main/res/font/source_serif_4_regular.ttf`
- `app/src/main/res/font/source_serif_4_italic.ttf`
- `app/src/main/res/font/source_serif_4_semibold.ttf`
- `app/src/main/res/font/source_serif_4_bold.ttf`
- `app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/HeroBackdropController.kt`
- `app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/Hairline.kt`
- `app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/InnerHighlight.kt` (Modifier extension)
- `app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/SectionLabel.kt`
- `app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/ChevronTrail.kt`
- `app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/SerifMetricRow.kt`
- `app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/AuroraTextHero.kt`
- `app/src/test/java/com/lizhi1026/sleepwhisper/core/visualkit/SWColorTest.kt`
- `app/src/test/java/com/lizhi1026/sleepwhisper/core/visualkit/HeroBackdropControllerTest.kt`

---

## Conventions

- Every component file goes in `core/visualkit/components/`; tokens go in `core/visualkit/`.
- Every public composable has a `@Preview` function in DAY + DARK + NIGHT (use a single multi-preview function for brevity).
- Every animation respects `LocalReduceMotion.current` — when true, skip the animation and render the resting state.
- After each task, run `./gradlew :app:compileDebugKotlin` to verify the project still compiles before committing.
- Commit message style follows existing repo convention (`Area: short imperative summary`). Examples: `visualkit: refresh SWMotion to cinematic-breath rhythm`.

---

## Task 1 — Add Latin font assets

**Files:**
- Create: `app/src/main/res/font/dm_serif_display_regular.ttf`
- Create: `app/src/main/res/font/source_serif_4_regular.ttf`
- Create: `app/src/main/res/font/source_serif_4_italic.ttf`
- Create: `app/src/main/res/font/source_serif_4_semibold.ttf`
- Create: `app/src/main/res/font/source_serif_4_bold.ttf`

- [ ] **Step 1: Ensure the font directory exists**

```bash
mkdir -p app/src/main/res/font
```

- [ ] **Step 2: Download DM Serif Display Regular**

```bash
curl -L -o app/src/main/res/font/dm_serif_display_regular.ttf \
  https://raw.githubusercontent.com/google/fonts/main/ofl/dmserifdisplay/DMSerifDisplay-Regular.ttf
```

Expected: ~110KB file at the target path. Verify with `file app/src/main/res/font/dm_serif_display_regular.ttf` returning `TrueType Font data`.

- [ ] **Step 3: Download Source Serif 4 family (4 weights)**

```bash
curl -L -o app/src/main/res/font/source_serif_4_regular.ttf \
  https://raw.githubusercontent.com/adobe-fonts/source-serif/release/TTF/SourceSerif4-Regular.ttf
curl -L -o app/src/main/res/font/source_serif_4_italic.ttf \
  https://raw.githubusercontent.com/adobe-fonts/source-serif/release/TTF/SourceSerif4-It.ttf
curl -L -o app/src/main/res/font/source_serif_4_semibold.ttf \
  https://raw.githubusercontent.com/adobe-fonts/source-serif/release/TTF/SourceSerif4-Semibold.ttf
curl -L -o app/src/main/res/font/source_serif_4_bold.ttf \
  https://raw.githubusercontent.com/adobe-fonts/source-serif/release/TTF/SourceSerif4-Bold.ttf
```

Expected: 4 .ttf files, each ~80-180KB.

- [ ] **Step 4: Verify the project still builds**

```bash
./gradlew :app:assembleDebug
```

Expected: BUILD SUCCESSFUL. Fonts are referenced once `SWFont` is updated in Task 6; at this point they're just present in resources.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/res/font/
git commit -m "visualkit: bundle DM Serif Display and Source Serif 4 fonts"
```

---

## Task 2 — Wire Google Fonts (Compose) for CJK fallback

The `androidx.compose.ui.text.googlefonts` package fetches fonts at first use via Google Play Services; nothing is bundled. We use this for the large CJK family (Noto Serif CJK SC).

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Create: `app/src/main/res/values/font_certs.xml`

- [ ] **Step 1: Add the library entry to the version catalog**

Open `gradle/libs.versions.toml` and add to the `[libraries]` section, alphabetically with other compose entries:

```toml
compose-ui-text-google-fonts   = { group = "androidx.compose.ui", name = "ui-text-google-fonts" }
```

- [ ] **Step 2: Reference it from the app build**

Open `app/build.gradle.kts` and add inside the Compose dependency group:

```kotlin
implementation(libs.compose.ui.text.google.fonts)
```

- [ ] **Step 3: Add the Google Fonts cert table resource**

Create `app/src/main/res/values/font_certs.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <array name="com_google_android_gms_fonts_certs">
        <item>@array/com_google_android_gms_fonts_certs_dev</item>
        <item>@array/com_google_android_gms_fonts_certs_prod</item>
    </array>
    <string-array name="com_google_android_gms_fonts_certs_dev">
        <item>
            MIIEqDCCA5CgAwIBAgIJANWFuGx90071MA0GCSqGSIb3DQEBBAUAMIGUMQswCQYDVQQGEwJVUzETMBEGA1UECBMKQ2FsaWZvcm5pYTEWMBQGA1UEBxMNTW91bnRhaW4gVmlldzEQMA4GA1UEChMHQW5kcm9pZDEQMA4GA1UECxMHQW5kcm9pZDEQMA4GA1UEAxMHQW5kcm9pZDEiMCAGCSqGSIb3DQEJARYTYW5kcm9pZEBhbmRyb2lkLmNvbTAeFw0wODA0MTUyMzM2NTZaFw0zNTA5MDEyMzM2NTZaMIGUMQswCQYDVQQGEwJVUzETMBEGA1UECBMKQ2FsaWZvcm5pYTEWMBQGA1UEBxMNTW91bnRhaW4gVmlldzEQMA4GA1UEChMHQW5kcm9pZDEQMA4GA1UECxMHQW5kcm9pZDEQMA4GA1UEAxMHQW5kcm9pZDEiMCAGCSqGSIb3DQEJARYTYW5kcm9pZEBhbmRyb2lkLmNvbTCCASAwDQYJKoZIhvcNAQEBBQADggENADCCAQgCggEBANbOLggKv+IxTdGNs8/TGFy0PTP6DHThvbbR24kT9ixcOd9W+EaBPWW+wPPKQmsHxajtWjmQwWfna8mZuSeJS48LIgAZlKkpFeVyxW0qMBujb8X8ETrWy550NaFtI6t9+u7hZeTfHwqNvacKhp1RbE6dBRGWynwMVX8XW8N1+UjFaq6GCJukT4qmpN2afb8sCjUigq0GuMwYXrFVee74bQgLHWGJwPmvmLHC69EH6kWr22ijx4OKXlSIx2xT1AsSHee70w5iDBiK4aph27yH3TxkXy9V89TDdexAcKk/cVHYNnDBapcavl7y0RiQ4biu8ymM8Ga/nmzhRKjme5cuwIBA6OB/DCB+TAdBgNVHQ4EFgQUH1QIlPKLp2OQ/AoLOjKvBW4zK3AwgckGA1UdIwSBwTCBvoAUH1QIlPKLp2OQ/AoLOjKvBW4zK3ChgZqkgZcwgZQxCzAJBgNVBAYTAlVTMRMwEQYDVQQIEwpDYWxpZm9ybmlhMRYwFAYDVQQHEw1Nb3VudGFpbiBWaWV3MRAwDgYDVQQKEwdBbmRyb2lkMRAwDgYDVQQLEwdBbmRyb2lkMRAwDgYDVQQDEwdBbmRyb2lkMSIwIAYJKoZIhvcNAQkBFhNhbmRyb2lkQGFuZHJvaWQuY29tggkA1YW4bH3TTvUwDAYDVR0TBAUwAwEB/zANBgkqhkiG9w0BAQQFAAOCAQEAj4KOOXC52Mb19zPi2+rmtcCEEt1OcUVPXq8FfgrEAYIZbBKpXkN8AuHHTUJ+JzaiwOuB1ZF8GsetIxnHe0/ZG3O+iCAR+OBZWFqWPxNHkX2/9rcjzwI3HzGRXjL0J3HVfQyJ4lULnGM7Lh1RrumkuVbF5oA3oZG8jfO3K5JcMlPKn6Tk8/8GdgWZCH4i+5flAhYC2vV0+CSUF1iA8VmqgK0jrSrjKxvYrFAm7+sR7CKnPL5h7m4uPNXBhDuRyHsiNGsq8wzkGRYBLM43kV6gn4qBHaaCv00OoZSGZThwTPg5UAhTOL3IDDsAJ/jZJUyRsLcXHIQGNYrr3KU/T2u4KdHzn8nQOPVgi2u/k+1NRyrtjL3WJfV1AIM0RnxF/cmYJUF+RPDg9d2VMlMd5VR5jcDdQGzWNFb1njFQ7c0gK+P9wW7Ek6f7N6QtJWj4hX0qsTuRpKD6oVKgs6FoiQB2bvHHj4HQ4XscZxAEnK7gFnVXz9JL3vXVZ7H44LU7
        </item>
    </string-array>
    <string-array name="com_google_android_gms_fonts_certs_prod">
        <item>
            MIIEqDCCA5CgAwIBAgIJAJNurL4H8gHfMA0GCSqGSIb3DQEBBQUAMIGUMQswCQYDVQQGEwJVUzETMBEGA1UECBMKQ2FsaWZvcm5pYTEWMBQGA1UEBxMNTW91bnRhaW4gVmlldzEUMBIGA1UEChMLR29vZ2xlIEluYy4xEDAOBgNVBAsTB0FuZHJvaWQxEDAOBgNVBAMTB0FuZHJvaWQxIjAgBgkqhkiG9w0BCQEWE2FuZHJvaWRAYW5kcm9pZC5jb20wHhcNMDgwODIxMjMxMzM0WhcNMzYwMTA3MjMxMzM0WjCBlDELMAkGA1UEBhMCVVMxEzARBgNVBAgTCkNhbGlmb3JuaWExFjAUBgNVBAcTDU1vdW50YWluIFZpZXcxFDASBgNVBAoTC0dvb2dsZSBJbmMuMRAwDgYDVQQLEwdBbmRyb2lkMRAwDgYDVQQDEwdBbmRyb2lkMSIwIAYJKoZIhvcNAQkBFhNhbmRyb2lkQGFuZHJvaWQuY29tMIIBIDANBgkqhkiG9w0BAQEFAAOCAQ0AMIIBCAKCAQEAghyhmEsj+nVxh4Q+YHi7VKsBlsHwElmm9hu5OoBJTl5wuxBC2mTOdYrFm+Yz4VVcAGOlfBkP6/JTI6mE5+7zEd6QzL/RmTOTQGYWZuhc3SDxgaaQbeR5HFGFCgM8nNNk3hAo+r3JLDcRYxJVQK0CDXqK2eVwJqkj5oSEmFLkpJrLE+vM/k2gN9G7lZmO9JE9voZ8YpyhMpkVMTBA+1bJrIp3vmzVKLkXpUMa6gE6ucwI1lzdpQwGiF/JU/PsBp5jGFnLI3uG4MYTVH+ye3pdR5oxXqLU+VkPL3eDDeVZbi6SDjVQTd2RHvDD7+qkfqj6/MhpgFy2P2NLFwHJYZ5OdwIBA6OCAQYwggECMB0GA1UdDgQWBBSEJ4b0SiL2x2/8H/3iWPVe7kQOyzCB0gYDVR0jBIHKMIHHgBSEJ4b0SiL2x2/8H/3iWPVe7kQOy6GBmqSBlzCBlDELMAkGA1UEBhMCVVMxEzARBgNVBAgTCkNhbGlmb3JuaWExFjAUBgNVBAcTDU1vdW50YWluIFZpZXcxFDASBgNVBAoTC0dvb2dsZSBJbmMuMRAwDgYDVQQLEwdBbmRyb2lkMRAwDgYDVQQDEwdBbmRyb2lkMSIwIAYJKoZIhvcNAQkBFhNhbmRyb2lkQGFuZHJvaWQuY29tggkAk26svgfyAd8wDAYDVR0TBAUwAwEB/zANBgkqhkiG9w0BAQUFAAOCAQEAgYpvayy0RxdQTV/Gh4MrInTrwSTeFTcvKKJYRpcFqUWyfDhKD8VC23PsZP8AUEZUk7Gmtt9KumYNihGAvQp3EW8slmlIc2RFw4eFC+EvCQPDA7mzwbgnh3VfsuPiCN8U0CSEqL/JCH9FZmAYwBHnf0Lf4t9N/JJyRG5SH+VYDpW7L4LhJZKMm7B+gflYcXrgIVRcyHJq5kBu07Z7XBoqLLJgPmL4M2dWdZN7Vh2voV9wY7M68YIYr+9TQzKxSEY8ZIuvyHEZHfFOPELO0ka2Pjg9TG/G2H7gJ7mD+x77IGY+VnmTRGSE3HSrtCAlGoIWoCJwY7CK1Vx0HtwYYWcWUM
        </item>
    </string-array>
</resources>
```

(These are the standard Google Play Services font certs — required by `GoogleFont.Provider`.)

- [ ] **Step 4: Sync and build**

```bash
./gradlew :app:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts app/src/main/res/values/font_certs.xml
git commit -m "visualkit: enable Compose Google Fonts for CJK fallback"
```

---

## Task 3 — Refresh `SWMotion` and add `SWSpring`

**Files:**
- Modify: `app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/SWTokens.kt`

- [ ] **Step 1: Replace the `SWMotion` object and add `SWSpring`**

Replace the `SWMotion` object and append `SWSpring` below it (keep `SWSpacing` and `SWRadius` exactly as they are):

```kotlin
package com.lizhi1026.sleepwhisper.core.visualkit

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.ui.unit.dp

/** Spacing scale (4-based) — direct port of iOS SWSpacing. */
object SWSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 20.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 40.dp
    val huge = 56.dp
    val giant = 72.dp
    val mammoth = 96.dp
}

/** Corner radius scale — direct port of iOS SWRadius. `pill` for capsule shapes. */
object SWRadius {
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val pill = 999.dp
}

/**
 * Motion / duration tokens. Cinematic-breath rhythm: snappy inhale on press,
 * longer exhale on release, generous transitions. Hero/backdrop durations are new
 * for the Aurora redesign (Phase 0+).
 */
object SWMotion {
    const val pressInMs = 80         // was 100
    const val pressOutMs = 260       // was 150
    const val screenInMs = 420       // was 250
    const val screenOutMs = 320      // was 200
    const val heroMorphMs = 1200     // NEW — Hero #1 transition
    const val backdropDriftMs = 18000 // NEW — Aurora ribbon drift cycle
    const val breathCycleMs = 4200   // NEW — card breathing period
    const val toastMs = 200          // unchanged
    const val bannerMs = 200         // unchanged
    const val numberSwitchMs = 100   // unchanged
    const val longPressMs = 600L     // unchanged
    const val fadeInMs = 1500L       // unchanged
    const val defaultFadeOutMs = 5000L // unchanged
}

/**
 * Shared spring specs. Use `SWSpring.gentle` for every interactive element so the
 * whole app speaks one tactile language.
 */
object SWSpring {
    val gentle: SpringSpec<Float> = spring(
        dampingRatio = 0.85f,
        stiffness = 180f,
        visibilityThreshold = Spring.DefaultDisplacementThreshold
    )
}
```

- [ ] **Step 2: Compile**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL. Any caller of the changed motion constants compiles unchanged (they are constants of the same type).

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/SWTokens.kt
git commit -m "visualkit: refresh SWMotion to cinematic-breath rhythm and add SWSpring.gentle"
```

---

## Task 4 — Replace `SWColor` palette with Aurora values

**Files:**
- Modify: `app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/SWColor.kt`
- Create: `app/src/test/java/com/lizhi1026/sleepwhisper/core/visualkit/SWColorTest.kt`

- [ ] **Step 1: Write the failing test first (verify NIGHT is preserved + new Aurora values are present)**

Create `app/src/test/java/com/lizhi1026/sleepwhisper/core/visualkit/SWColorTest.kt`:

```kotlin
package com.lizhi1026.sleepwhisper.core.visualkit

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class SWColorTest {

    @Test fun `NIGHT textPrimary preserved`() {
        // Hard-won accessibility tuning — must remain at the exact value.
        assertEquals(Color(1.00f, 0.75f, 0.75f), SWColor.textPrimary(SWScheme.NIGHT))
    }

    @Test fun `NIGHT surface preserved`() {
        assertEquals(Color(0.04f, 0.02f, 0.02f), SWColor.surface(SWScheme.NIGHT))
    }

    @Test fun `DAY surface is paper warm`() {
        // #F4F2EE → (0.957, 0.949, 0.933)
        val c = SWColor.surface(SWScheme.DAY)
        assertEquals(0.957f, c.red, 0.005f)
        assertEquals(0.949f, c.green, 0.005f)
        assertEquals(0.933f, c.blue, 0.005f)
    }

    @Test fun `DARK surface is deep night`() {
        // #07101F → (0.027, 0.063, 0.122)
        val c = SWColor.surface(SWScheme.DARK)
        assertEquals(0.027f, c.red, 0.005f)
        assertEquals(0.063f, c.green, 0.005f)
        assertEquals(0.122f, c.blue, 0.005f)
    }

    @Test fun `accentSecondary exists for DAY and DARK`() {
        // Starlight purple — DAY #9B85FF, DARK #B8A4FF
        val day = SWColor.accentSecondary(SWScheme.DAY)
        assertEquals(0.608f, day.red, 0.005f)
        val dark = SWColor.accentSecondary(SWScheme.DARK)
        assertEquals(0.722f, dark.red, 0.005f)
    }
}
```

- [ ] **Step 2: Run the test — expect it to fail**

```bash
./gradlew :app:testDebugUnitTest --tests 'com.lizhi1026.sleepwhisper.core.visualkit.SWColorTest'
```

Expected: FAIL (5 tests failing; `accentSecondary` unresolved, DAY/DARK surface values don't match yet).

- [ ] **Step 3: Replace `SWColor` with the Aurora palette**

Replace the body of `app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/SWColor.kt`:

```kotlin
package com.lizhi1026.sleepwhisper.core.visualkit

import androidx.compose.ui.graphics.Color

/**
 * Aurora color tokens. Public API unchanged from the iOS port; values refreshed
 * for the Aurora redesign (Phase 0). NIGHT scheme values are preserved — their
 * contrast against the NIGHT surface was tuned to ≥ 4.5:1 and is not negotiable.
 */
object SWColor {

    fun primary(s: SWScheme): Color = when (s) {
        SWScheme.DAY   -> Color(0xFFFF8A5E)  // dusk orange
        SWScheme.DARK  -> Color(0xFFFFB088)  // dusk orange
        SWScheme.NIGHT -> Color(0.70f, 0.42f, 0.42f) // preserved
    }

    fun primaryHover(s: SWScheme): Color = when (s) {
        SWScheme.DAY   -> Color(0xFFFF7A4A)
        SWScheme.DARK  -> Color(0xFFFFC5A4)
        SWScheme.NIGHT -> Color(0.78f, 0.50f, 0.50f)
    }

    fun primaryActive(s: SWScheme): Color = when (s) {
        SWScheme.DAY   -> Color(0xFFE87646)
        SWScheme.DARK  -> Color(0xFFFFA378)
        SWScheme.NIGHT -> Color(0.85f, 0.55f, 0.55f)
    }

    fun accent(s: SWScheme): Color = when (s) {
        SWScheme.DAY   -> Color(0xFFFF8A5E)
        SWScheme.DARK  -> Color(0xFFFFB088)
        SWScheme.NIGHT -> Color(0.80f, 0.45f, 0.45f)
    }

    /** Starlight purple — NEW. The second hero accent. NIGHT scheme uses primary instead. */
    fun accentSecondary(s: SWScheme): Color = when (s) {
        SWScheme.DAY   -> Color(0xFF9B85FF)
        SWScheme.DARK  -> Color(0xFFB8A4FF)
        SWScheme.NIGHT -> Color(0.70f, 0.42f, 0.42f) // falls back to primary tone
    }

    fun surface(s: SWScheme): Color = when (s) {
        SWScheme.DAY   -> Color(0xFFF4F2EE) // paper warm
        SWScheme.DARK  -> Color(0xFF07101F) // deep night
        SWScheme.NIGHT -> Color(0.04f, 0.02f, 0.02f)
    }

    fun surfaceElevated(s: SWScheme): Color = when (s) {
        SWScheme.DAY   -> Color.White
        SWScheme.DARK  -> Color(0xFF0F1A2D)
        SWScheme.NIGHT -> Color(0.10f, 0.04f, 0.04f)
    }

    fun surfaceSunken(s: SWScheme): Color = when (s) {
        SWScheme.DAY   -> Color(0xFFECEAE5)
        SWScheme.DARK  -> Color(0xFF050912)
        SWScheme.NIGHT -> Color(0.02f, 0.01f, 0.01f)
    }

    fun border(s: SWScheme): Color = when (s) {
        SWScheme.DAY   -> Color(0xFFE2DDD2)
        SWScheme.DARK  -> Color(0xFF1A2438)
        SWScheme.NIGHT -> Color(0.16f, 0.07f, 0.07f)
    }

    fun textPrimary(s: SWScheme): Color = when (s) {
        SWScheme.DAY   -> Color(0xFF0A1428)
        SWScheme.DARK  -> Color(0xFFECF2F8) // moonlit silver
        SWScheme.NIGHT -> Color(1.00f, 0.75f, 0.75f) // ≥ 4.5:1 — DO NOT CHANGE
    }

    fun textSecondary(s: SWScheme): Color = when (s) {
        SWScheme.DAY   -> Color(0xFF4A5573)
        SWScheme.DARK  -> Color(0xFF8A93A8)
        SWScheme.NIGHT -> Color(0.85f, 0.55f, 0.55f)
    }

    fun textTertiary(s: SWScheme): Color = when (s) {
        SWScheme.DAY   -> Color(0xFF7A859C)
        SWScheme.DARK  -> Color(0xFF5C667A)
        SWScheme.NIGHT -> Color(0.70f, 0.42f, 0.42f)
    }

    fun textInverse(s: SWScheme): Color = when (s) {
        SWScheme.DAY   -> Color.White
        SWScheme.DARK  -> Color(0xFF0F1A2D)
        SWScheme.NIGHT -> Color(0.10f, 0.04f, 0.04f)
    }

    fun success(s: SWScheme): Color = when (s) {
        SWScheme.DAY   -> Color(0.20f, 0.71f, 0.51f)
        SWScheme.DARK  -> Color(0.58f, 0.76f, 0.55f)
        SWScheme.NIGHT -> Color(0.55f, 0.32f, 0.32f)
    }

    fun warning(s: SWScheme): Color = when (s) {
        SWScheme.DAY   -> Color(0.96f, 0.62f, 0.07f)
        SWScheme.DARK  -> Color(0.89f, 0.72f, 0.45f)
        SWScheme.NIGHT -> Color(0.75f, 0.42f, 0.42f)
    }

    fun danger(s: SWScheme): Color = when (s) {
        SWScheme.DAY   -> Color(0.94f, 0.27f, 0.27f)
        SWScheme.DARK  -> Color(0.85f, 0.54f, 0.54f)
        SWScheme.NIGHT -> Color(0.95f, 0.42f, 0.42f)
    }

    fun softPeach(s: SWScheme): Color = when (s) {
        SWScheme.DAY   -> Color(1.000f, 0.898f, 0.851f)
        SWScheme.DARK  -> Color(0.40f, 0.28f, 0.24f, alpha = 0.5f)
        SWScheme.NIGHT -> Color(0.40f, 0.16f, 0.16f, alpha = 0.5f)
    }

    fun softMint(s: SWScheme): Color = when (s) {
        SWScheme.DAY   -> Color(0.859f, 0.961f, 0.898f)
        SWScheme.DARK  -> Color(0.24f, 0.40f, 0.32f, alpha = 0.5f)
        SWScheme.NIGHT -> Color(0.40f, 0.20f, 0.20f, alpha = 0.5f)
    }

    fun softLilac(s: SWScheme): Color = when (s) {
        SWScheme.DAY   -> Color(0.918f, 0.890f, 1.000f)
        SWScheme.DARK  -> Color(0.32f, 0.28f, 0.48f, alpha = 0.5f)
        SWScheme.NIGHT -> Color(0.40f, 0.16f, 0.16f, alpha = 0.5f)
    }
}
```

- [ ] **Step 4: Run the test — expect it to pass**

```bash
./gradlew :app:testDebugUnitTest --tests 'com.lizhi1026.sleepwhisper.core.visualkit.SWColorTest'
```

Expected: PASS (5/5).

- [ ] **Step 5: Compile the whole app**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL. Screens compile unchanged because the API didn't change.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/SWColor.kt \
        app/src/test/java/com/lizhi1026/sleepwhisper/core/visualkit/SWColorTest.kt
git commit -m "visualkit: Aurora palette for SWColor with starlight purple accent"
```

---

## Task 5 — Add three Aurora gradients to `SWGradient`

**Files:**
- Modify: `app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/SWGradient.kt`

- [ ] **Step 1: Append the new gradient functions to `SWGradient`**

Add inside the `object SWGradient { ... }` block in `SWGradient.kt` (after `cardOverlay`):

```kotlin
    /**
     * Aurora backdrop — multi-stop diagonal ribbon. The base layer used by
     * AuroraBackdrop. Three of these are stacked at different phases.
     */
    fun auroraBackdrop(scheme: SWScheme): Brush = when (scheme) {
        SWScheme.DAY -> topLeftToBottomRightStops(arrayOf(
            0.0f to Color(0xFFF4F2EE),
            0.5f to Color(0xFFFAE9DC),  // dawn whisper
            1.0f to Color(0xFFEFE4F2)   // lilac whisper
        ))
        SWScheme.DARK -> topLeftToBottomRightStops(arrayOf(
            0.0f to Color(0xFF07101F),
            0.4f to Color(0xFF0F1A2D),
            0.7f to Color(0xFF1A2240),
            1.0f to Color(0xFF2A1F3A)
        ))
        SWScheme.NIGHT -> topLeftToBottomRightStops(arrayOf(
            0.0f to Color(0.10f, 0.02f, 0.02f),
            0.5f to Color(0.16f, 0.03f, 0.03f),
            1.0f to Color(0.24f, 0.06f, 0.06f)
        ))
    }

    /**
     * Aurora glow — radial purple→orange. For hero CTAs and now-playing accents.
     * Returns a radial brush from the natural drawing center.
     */
    fun auroraGlow(scheme: SWScheme): Brush = when (scheme) {
        SWScheme.DAY -> Brush.radialGradient(
            colors = listOf(Color(0xFFFFB088), Color(0xFFB8A4FF).copy(alpha = 0f))
        )
        SWScheme.DARK -> Brush.radialGradient(
            colors = listOf(Color(0xFFB8A4FF), Color(0xFFFFB088).copy(alpha = 0f))
        )
        SWScheme.NIGHT -> Brush.radialGradient(
            colors = listOf(Color(0.78f, 0.45f, 0.45f), Color(0.55f, 0.28f, 0.28f, alpha = 0f))
        )
    }

    /**
     * Moon halo — soft white-silver radial used as an inner highlight on cards
     * on dark schemes. On DAY, returns near-transparent so callers can ignore.
     */
    fun moonHalo(scheme: SWScheme): Brush = when (scheme) {
        SWScheme.DAY -> Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.06f), Color.Transparent)
        )
        SWScheme.DARK -> Brush.radialGradient(
            colors = listOf(Color(0xFFECF2F8).copy(alpha = 0.18f), Color.Transparent)
        )
        SWScheme.NIGHT -> Brush.radialGradient(
            colors = listOf(Color(1.00f, 0.75f, 0.75f).copy(alpha = 0.08f), Color.Transparent)
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
git add app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/SWGradient.kt
git commit -m "visualkit: add auroraBackdrop, auroraGlow, and moonHalo gradients"
```

---

## Task 6 — Rebuild `SWFont` around bundled fonts + CJK fallback

**Files:**
- Modify: `app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/SWFont.kt`

- [ ] **Step 1: Replace the `SWFont` body**

Replace the entire body of `app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/SWFont.kt`:

```kotlin
package com.lizhi1026.sleepwhisper.core.visualkit

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font as GoogleFontVariant
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.lizhi1026.sleepwhisper.R

/**
 * Aurora typography tokens. Public function names unchanged from earlier; the
 * underlying font families and metrics have been rebuilt:
 *
 *   - DM Serif Display drives all `display*()` styles (large headlines only).
 *   - Source Serif 4 drives `title*` / `body*` / `label*` and tabular numerals.
 *   - Source Serif 4 Italic is the emotional accent (`serifItalic`).
 *   - Noto Serif CJK SC (downloadable via Google Fonts Compose) is the fallback
 *     for any text containing Chinese characters — see [cjkAware].
 *
 * Tabular variants use OpenType "tnum" so digit-width jitter doesn't shift the
 * surrounding text on each tick.
 */
object SWFont {

    private const val TNUM = "tnum"

    private val DisplaySerif = FontFamily(
        Font(R.font.dm_serif_display_regular, FontWeight.Normal)
    )

    private val BodySerif = FontFamily(
        Font(R.font.source_serif_4_regular,  FontWeight.Normal),
        Font(R.font.source_serif_4_italic,   FontWeight.Normal, FontStyle.Italic),
        Font(R.font.source_serif_4_semibold, FontWeight.SemiBold),
        Font(R.font.source_serif_4_bold,     FontWeight.Bold)
    )

    private val GoogleProvider = GoogleFont.Provider(
        providerAuthority = "com.google.android.gms.fonts",
        providerPackage = "com.google.android.gms",
        certificates = R.array.com_google_android_gms_fonts_certs
    )

    private val NotoSerifCjkSc = GoogleFont("Noto Serif SC")

    /** Single-weight CJK fallback family. Loaded once via Google Play Services. */
    val CjkFallback: FontFamily = FontFamily(
        GoogleFontVariant(googleFont = NotoSerifCjkSc, fontProvider = GoogleProvider, weight = FontWeight.Normal),
        GoogleFontVariant(googleFont = NotoSerifCjkSc, fontProvider = GoogleProvider, weight = FontWeight.SemiBold)
    )

    /**
     * Wraps a TextStyle so that any unresolved glyph in the Latin family falls
     * through to the CJK serif. Use for any text that may render baby names,
     * Chinese greetings, localized preset names, etc.
     *
     * Compose's font resolution walks the list of families in order until a glyph
     * is found, so the Latin family is tried first (faster, no network) and CJK
     * resolves anything Latin doesn't have.
     */
    fun cjkAware(style: TextStyle): TextStyle =
        style.copy(fontFamily = when (val f = style.fontFamily) {
            null         -> FontFamily(BodySerif, CjkFallback)
            BodySerif    -> FontFamily(BodySerif, CjkFallback)
            DisplaySerif -> FontFamily(DisplaySerif, BodySerif, CjkFallback) // Display has no CJK
            else         -> FontFamily(f, CjkFallback)
        })

    // ─── Display (DM Serif Display) ──────────────────────────────────────────
    fun displayXL(): TextStyle = TextStyle(
        fontSize = 96.sp, fontFamily = DisplaySerif, fontWeight = FontWeight.Normal,
        letterSpacing = (-2).sp, lineHeight = 96.sp
    )
    fun displayLG(): TextStyle = TextStyle(
        fontSize = 64.sp, fontFamily = DisplaySerif, fontWeight = FontWeight.Normal,
        letterSpacing = (-1).sp, lineHeight = 67.sp
    )
    fun displayMD(): TextStyle = TextStyle(
        fontSize = 48.sp, fontFamily = DisplaySerif, fontWeight = FontWeight.Normal,
        letterSpacing = (-0.5).sp, lineHeight = 53.sp
    )

    // ─── Title (Source Serif 4 SemiBold) ─────────────────────────────────────
    fun titleXL(): TextStyle = TextStyle(
        fontSize = 34.sp, fontFamily = BodySerif, fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.5).sp, lineHeight = 37.sp
    )
    fun titleLG(): TextStyle = TextStyle(
        fontSize = 24.sp, fontFamily = BodySerif, fontWeight = FontWeight.SemiBold,
        lineHeight = 29.sp
    )
    fun titleMD(): TextStyle = TextStyle(
        fontSize = 18.sp, fontFamily = BodySerif, fontWeight = FontWeight.SemiBold,
        lineHeight = 23.sp
    )

    // ─── Body (Source Serif 4 Regular) ───────────────────────────────────────
    fun bodyLG(): TextStyle = TextStyle(
        fontSize = 17.sp, fontFamily = BodySerif, fontWeight = FontWeight.Normal,
        lineHeight = 26.sp  // 1.55
    )
    fun bodyMD(): TextStyle = TextStyle(
        fontSize = 15.sp, fontFamily = BodySerif, fontWeight = FontWeight.Normal,
        lineHeight = 23.sp  // 1.55
    )

    // ─── Label (Source Serif 4 SemiBold, expanded tracking) ──────────────────
    fun labelMD(): TextStyle = TextStyle(
        fontSize = 12.sp, fontFamily = BodySerif, fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.6.sp, lineHeight = 19.sp
    )
    fun labelSM(): TextStyle = TextStyle(
        fontSize = 10.sp, fontFamily = BodySerif, fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.4.sp, lineHeight = 15.sp
    )

    // ─── Tabular (timers / numbers in cards) ─────────────────────────────────
    fun displayLGTabular(): TextStyle = TextStyle(
        fontSize = 64.sp, fontFamily = BodySerif, fontWeight = FontWeight.SemiBold,
        fontFeatureSettings = TNUM, lineHeight = 64.sp
    )
    fun displayLargeTabular(): TextStyle = TextStyle(
        fontSize = 72.sp, fontFamily = BodySerif, fontWeight = FontWeight.SemiBold,
        fontFeatureSettings = TNUM, lineHeight = 72.sp
    )
    fun displayXLTabular(): TextStyle = TextStyle(
        fontSize = 96.sp, fontFamily = BodySerif, fontWeight = FontWeight.SemiBold,
        fontFeatureSettings = TNUM, letterSpacing = (-2).sp, lineHeight = 96.sp
    )

    /** Emotional italic accent — used on greetings, sleeping captions, hero subtitles. */
    fun serifItalic(size: Int): TextStyle = TextStyle(
        fontSize = size.sp, fontFamily = BodySerif, fontWeight = FontWeight.Normal,
        fontStyle = FontStyle.Italic, lineHeight = (size * 1.4f).sp
    )
}
```

- [ ] **Step 2: Compile**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL. (No screen file changes needed — the function signatures haven't changed; only the returned `TextStyle` content has.)

- [ ] **Step 3: Install and visually verify on an emulator**

```bash
./gradlew :app:installDebug
adb shell am start -n com.lizhi1026.sleepwhisper/.app.MainActivity
```

Open the app. Confirm:
- Greeting on Home reads in italic serif (Source Serif 4 Italic)
- Wake-window number is serif tabular
- Tab labels are serif

If any text appears in the default sans-serif system font, the font resource is mis-named or the resource ID was unresolved — re-check Task 1.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/SWFont.kt
git commit -m "visualkit: rebuild SWFont on DM Serif Display + Source Serif 4 with CJK fallback"
```

---

## Task 7 — Add `HeroBackdropController` CompositionLocal

**Files:**
- Create: `app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/HeroBackdropController.kt`
- Create: `app/src/test/java/com/lizhi1026/sleepwhisper/core/visualkit/HeroBackdropControllerTest.kt`

This controller exposes the "current ambient color" (audio preset aura, or time-of-day) so `AuroraBackdrop` can shift its palette in response. In Phase 0 we only define and wire the plumbing; full consumers come in Phases 1-3.

- [ ] **Step 1: Write the failing test for the ambient color blend math**

Create `app/src/test/java/com/lizhi1026/sleepwhisper/core/visualkit/HeroBackdropControllerTest.kt`:

```kotlin
package com.lizhi1026.sleepwhisper.core.visualkit

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class HeroBackdropControllerTest {

    @Test fun `blendAmbient with null returns base unchanged`() {
        val base = Color(0xFF07101F)
        assertEquals(base, blendAmbient(base, ambient = null, fraction = 0.5f))
    }

    @Test fun `blendAmbient at fraction 0 returns base`() {
        val base = Color(0xFF07101F)
        val ambient = Color(0xFFFFB088)
        assertEquals(base, blendAmbient(base, ambient, fraction = 0f))
    }

    @Test fun `blendAmbient at fraction 1 returns ambient`() {
        val base = Color(0xFF07101F)
        val ambient = Color(0xFFFFB088)
        assertEquals(ambient, blendAmbient(base, ambient, fraction = 1f))
    }

    @Test fun `blendAmbient at fraction 0_5 averages channels`() {
        val a = Color(0xFF000000)
        val b = Color(0xFFFFFFFF)
        val out = blendAmbient(a, b, fraction = 0.5f)
        assertEquals(0.5f, out.red, 0.005f)
        assertEquals(0.5f, out.green, 0.005f)
        assertEquals(0.5f, out.blue, 0.005f)
    }
}
```

- [ ] **Step 2: Run — expect failure**

```bash
./gradlew :app:testDebugUnitTest --tests 'com.lizhi1026.sleepwhisper.core.visualkit.HeroBackdropControllerTest'
```

Expected: FAIL — `blendAmbient` unresolved.

- [ ] **Step 3: Create `HeroBackdropController.kt`**

```kotlin
package com.lizhi1026.sleepwhisper.core.visualkit

import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * Holds the current "ambient color" the app should tint backdrops with.
 * Sources, in priority order (set by future phases):
 *   1. Active audio preset's mid-aura (Player Hero #3)
 *   2. Time-of-day hint (Home / Sleeping)
 *   3. null — backdrops use their default Aurora palette
 *
 * `setAmbient(null)` clears, allowing the default to surface again.
 */
@Stable
class HeroBackdropController {
    private val _ambient = mutableStateOf<Color?>(null)
    val ambient: Color? get() = _ambient.value

    fun setAmbient(color: Color?) { _ambient.value = color }
}

val LocalHeroBackdropController = compositionLocalOf { HeroBackdropController() }

/**
 * Blend [base] toward [ambient] by [fraction] in linear RGB.
 * `ambient = null` returns base unchanged regardless of fraction.
 * `fraction` is clamped to [0, 1].
 */
fun blendAmbient(base: Color, ambient: Color?, fraction: Float): Color {
    if (ambient == null) return base
    val f = fraction.coerceIn(0f, 1f)
    return lerp(base, ambient, f)
}
```

- [ ] **Step 4: Run — expect pass**

```bash
./gradlew :app:testDebugUnitTest --tests 'com.lizhi1026.sleepwhisper.core.visualkit.HeroBackdropControllerTest'
```

Expected: PASS (4/4).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/HeroBackdropController.kt \
        app/src/test/java/com/lizhi1026/sleepwhisper/core/visualkit/HeroBackdropControllerTest.kt
git commit -m "visualkit: add HeroBackdropController CompositionLocal and blendAmbient util"
```

---

## Task 8 — Evolve `BreathingBackground` → `AuroraBackdrop`

**Files:**
- Modify: `app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/BreathingBackground.kt`
- Modify (callsite rename): `app/src/main/java/com/lizhi1026/sleepwhisper/features/home/HomeScreen.kt`
- Modify (callsite rename): `app/src/main/java/com/lizhi1026/sleepwhisper/features/trends/TrendsScreen.kt`
- Modify (callsite rename): `app/src/main/java/com/lizhi1026/sleepwhisper/features/player/PlayerScreen.kt`
- Modify (callsite rename, if reference exists): the onboarding files and `EventEditSheet`

- [ ] **Step 1: Find all current callers**

```bash
grep -rn "BreathingBackground" app/src/main/java/
```

Expected: a handful of call sites in Home / Trends / Player / Onboarding / EventEditSheet. Note them — every one of them will be renamed to `AuroraBackdrop` in step 4.

- [ ] **Step 2: Rewrite `BreathingBackground.kt`**

Replace the file with the new `AuroraBackdrop` composable. Note the file name stays `BreathingBackground.kt` so the iOS-port symmetry remains greppable; only the function inside is renamed.

```kotlin
package com.lizhi1026.sleepwhisper.core.visualkit.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.lizhi1026.sleepwhisper.core.visualkit.LocalHeroBackdropController
import com.lizhi1026.sleepwhisper.core.visualkit.LocalReduceMotion
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor
import com.lizhi1026.sleepwhisper.core.visualkit.SWGradient
import com.lizhi1026.sleepwhisper.core.visualkit.SWMotion
import com.lizhi1026.sleepwhisper.core.visualkit.SWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.blendAmbient
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Aurora Backdrop — full-screen always-present surface. Replaces the previous
 * `BreathingBackground` composable. Three diagonal aurora ribbons drift at
 * offset phases over `SWMotion.backdropDriftMs`; a subtle film grain overlay
 * lifts it above generic gradients.
 *
 * On DAY scheme it's a quiet paper-warm surface with the lightest possible
 * ribbon shimmer; on DARK it's the cinematic Aurora layered with starlight
 * purple and dusk orange. On NIGHT it stays static (red-light scheme is for
 * 3am readability, not for animation).
 *
 * Animations pause when `LocalReduceMotion.current = true`.
 *
 * Consumes `LocalHeroBackdropController.ambient` — if set, ribbons tint 20%
 * toward the ambient color so audio-preset / time-of-day signals can shift
 * the room's mood app-wide.
 */
@Composable
fun AuroraBackdrop(modifier: Modifier = Modifier) {
    val scheme = LocalSWScheme.current
    val reduceMotion = LocalReduceMotion.current
    val ambient = LocalHeroBackdropController.current.ambient

    Box(
        modifier = modifier
            .fillMaxSize()
            .clearAndSetSemantics { }
            .background(SWColor.surface(scheme))
    ) {
        // Base ribbon layer — the Aurora backdrop gradient.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SWGradient.auroraBackdrop(scheme))
        )

        // Three drifting glow ribbons — animated unless reduce-motion or NIGHT.
        val animate = !reduceMotion && scheme != SWScheme.NIGHT
        if (animate) {
            val t = rememberInfiniteTransition(label = "aurora-drift")
            val phase by t.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(SWMotion.backdropDriftMs, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "phase"
            )
            DriftingRibbon(phaseOffset = 0f,           color = blendAmbient(rib1(scheme), ambient, 0.2f), phase = phase)
            DriftingRibbon(phaseOffset = (2 * PI / 3).toFloat(), color = blendAmbient(rib2(scheme), ambient, 0.2f), phase = phase)
            DriftingRibbon(phaseOffset = (4 * PI / 3).toFloat(), color = blendAmbient(rib3(scheme), ambient, 0.2f), phase = phase)
        }

        // Static film grain — adds the cinematic detail.
        FilmGrain(intensity = if (scheme == SWScheme.DAY) 0.025f else 0.08f)
    }
}

@Composable
private fun DriftingRibbon(phaseOffset: Float, color: Color, phase: Float) {
    val angle = phase * 2 * PI.toFloat() + phaseOffset
    val cx = (0.5f + 0.25f * cos(angle))
    val cy = (0.5f + 0.25f * sin(angle))
    Canvas(modifier = Modifier.fillMaxSize()) {
        val r = size.maxDimension * 0.6f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = 0.35f), color.copy(alpha = 0f)),
                center = Offset(size.width * cx, size.height * cy),
                radius = r
            ),
            radius = r,
            center = Offset(size.width * cx, size.height * cy)
        )
    }
}

@Composable
private fun FilmGrain(intensity: Float) {
    // Pre-generated grain seeded for stability — recomputing per-frame would
    // burn battery and create visible shimmer that fights the calm aesthetic.
    val grain = remember {
        val random = Random(0xA110A4)
        IntArray(400) { random.nextInt(0, 256) }
    }
    Canvas(modifier = Modifier.fillMaxSize()) {
        val cell = size.width / 20f
        var i = 0
        for (y in 0 until 20) for (x in 0 until 20) {
            val v = grain[i++] / 255f
            drawCircle(
                color = Color.White.copy(alpha = intensity * v),
                radius = 0.7f,
                center = Offset(x * cell + cell / 2, y * (size.height / 20f) + (size.height / 20f) / 2)
            )
        }
    }
}

private fun rib1(s: SWScheme): Color = when (s) {
    SWScheme.DAY   -> Color(0xFFFFB088)
    SWScheme.DARK  -> Color(0xFFB8A4FF)
    SWScheme.NIGHT -> Color(0.78f, 0.45f, 0.45f)
}
private fun rib2(s: SWScheme): Color = when (s) {
    SWScheme.DAY   -> Color(0xFFB8A4FF)
    SWScheme.DARK  -> Color(0xFFFFB088)
    SWScheme.NIGHT -> Color(0.55f, 0.28f, 0.28f)
}
private fun rib3(s: SWScheme): Color = when (s) {
    SWScheme.DAY   -> Color(0xFFFAE9DC)
    SWScheme.DARK  -> Color(0xFF1F3A8C)
    SWScheme.NIGHT -> Color(0.24f, 0.06f, 0.06f)
}

/**
 * Backwards-compatible alias so any forgotten caller still works while we
 * complete the rename in step 4. Removed at the end of this task.
 */
@Deprecated("Use AuroraBackdrop", ReplaceWith("AuroraBackdrop(modifier)"))
@Composable
fun BreathingBackground(modifier: Modifier = Modifier) = AuroraBackdrop(modifier)
```

- [ ] **Step 3: Compile to ensure existing callers still work via the alias**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL with deprecation warnings on callers (good — that's how we find them all).

- [ ] **Step 4: Rename every `BreathingBackground` call site to `AuroraBackdrop`**

For each callsite (use the list from Step 1), replace `BreathingBackground` with `AuroraBackdrop` and update the import. Example for `HomeScreen.kt`:

In `app/src/main/java/com/lizhi1026/sleepwhisper/features/home/HomeScreen.kt`:

Replace the import:
```kotlin
import com.lizhi1026.sleepwhisper.core.visualkit.components.BreathingBackground
```
with:
```kotlin
import com.lizhi1026.sleepwhisper.core.visualkit.components.AuroraBackdrop
```

Replace the call site `BreathingBackground()` with `AuroraBackdrop()`.

Apply the same edit to `TrendsScreen.kt`, `PlayerScreen.kt`, and any other files turned up by the grep in Step 1.

- [ ] **Step 5: Remove the deprecation alias**

In `BreathingBackground.kt`, delete the trailing `@Deprecated` `BreathingBackground` function (the alias added in Step 2).

- [ ] **Step 6: Re-grep to make sure nothing references the old name**

```bash
grep -rn "BreathingBackground" app/src/main/java/ || echo "Clean rename"
```

Expected: only matches in the file name `BreathingBackground.kt` (no source references).

- [ ] **Step 7: Compile + install + visual smoke**

```bash
./gradlew :app:installDebug
adb shell am start -n com.lizhi1026.sleepwhisper/.app.MainActivity
```

Confirm Home / Trends / Player render with the new drifting Aurora backdrop. On DARK, observe the slow purple↔orange ribbon drift over ~18s.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/BreathingBackground.kt \
        app/src/main/java/com/lizhi1026/sleepwhisper/features/
git commit -m "visualkit: replace BreathingBackground with AuroraBackdrop (3 ribbons + film grain)"
```

---

## Task 9 — Evolve `GlassCard` to moonlit-glass treatment

**Files:**
- Modify: `app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/GlassCard.kt`

- [ ] **Step 1: Replace the file body**

```kotlin
package com.lizhi1026.sleepwhisper.core.visualkit.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lizhi1026.sleepwhisper.core.visualkit.LocalReduceMotion
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor
import com.lizhi1026.sleepwhisper.core.visualkit.SWGradient
import com.lizhi1026.sleepwhisper.core.visualkit.SWMotion
import com.lizhi1026.sleepwhisper.core.visualkit.SWRadius
import com.lizhi1026.sleepwhisper.core.visualkit.SWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWShadow
import com.lizhi1026.sleepwhisper.core.visualkit.SWSpacing

enum class ElevationLevel { SOFT, MEDIUM, STRONG, HERO }

/**
 * Moonlit-glass card. Aurora evolution of the prior GlassCard:
 *   - DARK / NIGHT: surface + moon-halo radial top-left + aurora-glow whisper bottom-right
 *   - DAY: surface + 1dp border + subtle inner highlight gradient (top 30% slightly brighter)
 *   - All schemes: dual-layer shadow (close tight + distant wide) for cinematic depth
 *   - `hero = true` adds a continuous subtle breath scale (0.998 ↔ 1.002) at SWMotion.breathCycleMs
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = SWRadius.lg,
    contentPadding: PaddingValues = PaddingValues(SWSpacing.md),
    elevation: ElevationLevel = ElevationLevel.SOFT,
    hero: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val scheme = LocalSWScheme.current
    val reduce = LocalReduceMotion.current
    val shape = RoundedCornerShape(cornerRadius)

    val close = SWShadow.soft(scheme)
    val distant = when (elevation) {
        ElevationLevel.SOFT -> SWShadow.soft(scheme)
        ElevationLevel.MEDIUM -> SWShadow.medium(scheme)
        ElevationLevel.STRONG -> SWShadow.strong(scheme)
        ElevationLevel.HERO -> SWShadow.Spec(
            color = SWShadow.strong(scheme).color,
            radius = SWShadow.strong(scheme).radius + 16.dp,
            y = SWShadow.strong(scheme).y + 4.dp
        )
    }

    val breathScale = if (hero && !reduce) {
        val t = rememberInfiniteTransition(label = "card-breath")
        val v by t.animateFloat(
            initialValue = 0.998f,
            targetValue = 1.002f,
            animationSpec = infiniteRepeatable(
                animation = tween(SWMotion.breathCycleMs, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )
        v
    } else 1f

    val borderMod = if (scheme == SWScheme.DAY) {
        Modifier.border(1.dp, SWColor.border(scheme), shape)
    } else Modifier

    Box(
        modifier = modifier
            .scale(breathScale)
            // dual-layer shadow — close tight + distant wide for cinematic depth
            .shadow(close.radius, shape, clip = false, ambientColor = close.color, spotColor = close.color)
            .shadow(distant.radius, shape, clip = false, ambientColor = distant.color, spotColor = distant.color)
            .clip(shape)
            .background(SWColor.surfaceElevated(scheme))
            .then(borderMod)
    ) {
        if (scheme != SWScheme.DAY) {
            // Moon halo top-left + aurora glow whisper bottom-right.
            // The glow is dimmed via Modifier.alpha because Compose's Brush has no
            // built-in alpha multiplier — the wrapping Box scales the gradient down
            // to a whisper without changing the gradient's stop colors.
            Box(Modifier.fillMaxSize().background(SWGradient.moonHalo(scheme)))
            Box(
                Modifier
                    .fillMaxSize()
                    .alpha(0.06f)
                    .background(SWGradient.auroraGlow(scheme))
            )
        } else {
            // DAY: inner highlight — top 30% slightly brighter
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0f to Color.White.copy(alpha = 0.5f),
                        0.3f to Color.Transparent,
                        1f to Color.Transparent
                    )
                )
            )
        }
        Box(Modifier.padding(contentPadding), content = content)
    }
}
```

- [ ] **Step 2: Compile**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Visual smoke (real device)**

```bash
./gradlew :app:installDebug
```

Launch the app and confirm:
- WakeWindow card on Home shows moon-halo glow in top-left + warm whisper in bottom-right when in DARK scheme
- On DAY scheme, the same card has a 1dp border and a subtle top-third lightness gradient

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/GlassCard.kt
git commit -m "visualkit: GlassCard moonlit-glass treatment with dual-shadow and hero breath"
```

---

## Task 10 — Evolve `PulseRing` to 3-ring offset

**Files:**
- Modify: `app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/PulseRing.kt`

- [ ] **Step 1: Read the current signature**

Read the file to capture the existing public composable signature (parameters, defaults). Note any callers in `HomeScreen.kt` and elsewhere — the public surface must stay the same.

```bash
grep -rn "PulseRing" app/src/main/java/ | head -20
```

- [ ] **Step 2: Replace the file body**

Open `app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/PulseRing.kt` and replace its body with:

```kotlin
package com.lizhi1026.sleepwhisper.core.visualkit.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lizhi1026.sleepwhisper.core.visualkit.LocalReduceMotion
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor

enum class PulseIntensity { SOFT, STRONG }

/**
 * Aurora 3-ring pulse. Each ring is a slightly different hue (orange → orange/purple → purple)
 * with offset phases, giving CTAs a *halo* rather than a single ring.
 *
 * Public signature is unchanged from the prior single-ring version so existing callers
 * (HomeScreen Sleep CTA) work without edits.
 */
@Composable
fun PulseRing(
    color: Color,
    radius: Dp = 100.dp,
    intensity: PulseIntensity = PulseIntensity.SOFT
) {
    val reduce = LocalReduceMotion.current
    val scheme = LocalSWScheme.current
    val secondary = SWColor.accentSecondary(scheme)

    if (reduce) {
        // Static halo at resting state — render the three rings without animation.
        Canvas(modifier = Modifier.size(radius * 2)) {
            val maxAlpha = if (intensity == PulseIntensity.STRONG) 0.18f else 0.10f
            val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
            drawCircle(color.copy(alpha = maxAlpha * 0.8f), radius = size.minDimension * 0.45f, center = center, style = Stroke(width = 2f))
            drawCircle(lerpColor(color, secondary, 0.5f).copy(alpha = maxAlpha * 0.6f), radius = size.minDimension * 0.55f, center = center, style = Stroke(width = 2f))
            drawCircle(secondary.copy(alpha = maxAlpha * 0.4f), radius = size.minDimension * 0.65f, center = center, style = Stroke(width = 2f))
        }
        return
    }

    val t = rememberInfiniteTransition(label = "pulse")
    val period = 2800
    val phase by t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(period, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )
    Canvas(modifier = Modifier.size(radius * 2)) {
        val maxAlpha = if (intensity == PulseIntensity.STRONG) 0.32f else 0.18f
        val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
        // Three rings, phase-offset by 1/3 each, growing outward and fading.
        for (i in 0 until 3) {
            val ringPhase = ((phase + i / 3f) % 1f)
            val ringColor = when (i) {
                0 -> color
                1 -> lerpColor(color, secondary, 0.5f)
                else -> secondary
            }
            val ringRadius = size.minDimension * (0.35f + 0.30f * ringPhase)
            val ringAlpha = maxAlpha * (1f - ringPhase)
            drawCircle(
                color = ringColor.copy(alpha = ringAlpha),
                radius = ringRadius,
                center = center,
                style = Stroke(width = 2f)
            )
        }
    }
}

private fun lerpColor(a: Color, b: Color, f: Float): Color =
    androidx.compose.ui.graphics.lerp(a, b, f.coerceIn(0f, 1f))
```

If the existing file declared `PulseIntensity` already, the enum above is identical — leave it, or remove the new copy and keep the existing one. Verify with `grep -n "enum class PulseIntensity" app/src/main/java/`.

- [ ] **Step 3: Compile + visual smoke**

```bash
./gradlew :app:installDebug
```

On Home, observe the Sleep CTA: instead of a single pulsing ring there are now three offset rings drifting outward in dusk-orange → blend → starlight-purple.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/PulseRing.kt
git commit -m "visualkit: PulseRing as 3-ring offset aurora halo"
```

---

## Task 11 — Evolve `AudioWaveform` (sine-perturbed bars + gradient + spring)

**Files:**
- Modify: `app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/AudioWaveform.kt`

- [ ] **Step 1: Read current signature**

```bash
grep -n "fun AudioWaveform" app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/AudioWaveform.kt
```

Note the parameter list (`modifier`, `isPlaying`, `color`, `barCount`) — preserve it.

- [ ] **Step 2: Replace the file body**

```kotlin
package com.lizhi1026.sleepwhisper.core.visualkit.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.lizhi1026.sleepwhisper.core.visualkit.LocalReduceMotion
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor
import kotlin.math.PI
import kotlin.math.sin

/**
 * Aurora waveform. Bars follow a sine-perturbed envelope (mimics a real FFT
 * shape without actually analyzing audio), tween smoothly between frames,
 * and color along the bar uses a dusk-orange → starlight-purple gradient.
 *
 * Animation pauses (or never starts) when `isPlaying = false` OR when
 * `LocalReduceMotion.current = true`. Public signature preserved.
 */
@Composable
fun AudioWaveform(
    modifier: Modifier = Modifier,
    isPlaying: Boolean,
    color: Color = Color.Unspecified,
    barCount: Int = 24
) {
    val scheme = LocalSWScheme.current
    val reduce = LocalReduceMotion.current
    val tint = if (color == Color.Unspecified) SWColor.accent(scheme) else color
    val secondary = SWColor.accentSecondary(scheme)

    val animate = isPlaying && !reduce
    val phase = if (animate) {
        val t = rememberInfiniteTransition(label = "wave")
        val v by t.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1800, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "phase"
        )
        v
    } else 0.5f

    Canvas(modifier = modifier.fillMaxSize()) {
        val barWidth = size.width / (barCount * 1.7f)
        val gap = (size.width - barWidth * barCount) / (barCount - 1).coerceAtLeast(1)
        val mid = size.height / 2f
        val brush = Brush.verticalGradient(colors = listOf(tint, secondary))
        for (i in 0 until barCount) {
            val t = i.toFloat() / (barCount - 1).coerceAtLeast(1)
            // Two overlapping sines + a phase offset for variety.
            val envelope = (sin(PI * t * 2) * 0.5 + 0.5 +
                            sin((phase + t) * PI * 2) * 0.35).toFloat()
            val barH = (size.height * 0.18f + size.height * 0.6f * envelope).coerceAtLeast(barWidth)
            val x = i * (barWidth + gap)
            drawRoundRect(
                brush = brush,
                topLeft = Offset(x, mid - barH / 2),
                size = Size(barWidth, barH),
                cornerRadius = CornerRadius(barWidth / 2)
            )
        }
    }
}
```

- [ ] **Step 3: Compile + visual smoke**

```bash
./gradlew :app:installDebug
```

Open Home, tap the now-playing card to start playback (or trigger one of the audio-preset tiles). Observe a serif-styled, gradient-bar waveform pulsing softly.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/AudioWaveform.kt
git commit -m "visualkit: AudioWaveform with sine envelope and aurora gradient bars"
```

---

## Task 12 — Evolve `RollingNumber` (serif tabular + roll blur)

**Files:**
- Modify: `app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/RollingNumber.kt`

- [ ] **Step 1: Read current signature**

```bash
grep -n "fun RollingNumber" app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/RollingNumber.kt
```

Preserve the existing param list (likely `value: Int`, `style: TextStyle`, `modifier: Modifier`).

- [ ] **Step 2: Replace the file body**

```kotlin
package com.lizhi1026.sleepwhisper.core.visualkit.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import com.lizhi1026.sleepwhisper.core.visualkit.LocalReduceMotion
import com.lizhi1026.sleepwhisper.core.visualkit.SWMotion

/**
 * Rolling number — each digit rolls vertically when its value changes. Uses
 * serif tabular numerals (callers pass `SWFont.displayLGTabular()` etc.) so
 * no digit-width jitter shifts the surrounding text.
 *
 * On reduce-motion, digits crossfade instead of rolling.
 */
@Composable
fun RollingNumber(
    value: Int,
    style: TextStyle,
    modifier: Modifier = Modifier
) {
    val reduce = LocalReduceMotion.current
    val digits = value.toString().padStart(1)

    Row(modifier = modifier) {
        digits.forEachIndexed { i, ch ->
            AnimatedContent(
                targetState = ch,
                transitionSpec = {
                    if (reduce) {
                        fadeIn(tween(SWMotion.numberSwitchMs)) togetherWith fadeOut(tween(SWMotion.numberSwitchMs))
                    } else {
                        val rising = (initialState.digitToIntOrNull() ?: 0) < (targetState.digitToIntOrNull() ?: 0)
                        val dir = if (rising) 1 else -1
                        (slideInVertically(tween(SWMotion.numberSwitchMs)) { h -> dir * h } + fadeIn(tween(SWMotion.numberSwitchMs))) togetherWith
                            (slideOutVertically(tween(SWMotion.numberSwitchMs)) { h -> -dir * h } + fadeOut(tween(SWMotion.numberSwitchMs)))
                    }
                },
                label = "digit-$i"
            ) { c ->
                BasicText(text = c.toString(), style = style)
            }
        }
    }
}
```

- [ ] **Step 3: Compile + visual smoke**

```bash
./gradlew :app:installDebug
```

Trigger the wake-window countdown (it ticks every minute). The digit changes should roll vertically in serif tabular — direction matches whether the value increased or decreased.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/RollingNumber.kt
git commit -m "visualkit: RollingNumber with directional vertical roll and serif tabular"
```

---

## Task 13 — Evolve `SoftButton` (add `hero` style)

**Files:**
- Modify: `app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/SoftButton.kt`

- [ ] **Step 1: Inspect current signature**

```bash
grep -n "fun SoftButton" app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/SoftButton.kt
grep -n "enum class SoftButtonStyle" app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/SoftButton.kt
```

We're adding a new enum value `HERO` to `SoftButtonStyle` and a `when` branch in the rendering. Existing styles stay untouched.

- [ ] **Step 2: Add `HERO` to the enum**

In `SoftButton.kt`, find the `enum class SoftButtonStyle { ... }` declaration and add `HERO` to it. Example (yours may have slightly different existing values — preserve them):

```kotlin
enum class SoftButtonStyle { PRIMARY, ACCENT, SUBTLE, HERO }
```

- [ ] **Step 3: Add the `HERO` rendering branch**

In the `SoftButton(...)` composable's `when (style) { ... }` block(s), add a branch for `SoftButtonStyle.HERO`. The hero variant fills with `SWGradient.auroraGlow(scheme)` (radial), draws an inner-highlight 1dp gradient stroke at the top, and is wrapped in a `Modifier.shadow(24.dp, ...)` outer glow tinted with the accent — pulsing at `SWMotion.breathCycleMs` when not in reduce-motion.

Snippet to splice into the existing `when` chain (place near the existing `ACCENT` branch — keep existing visuals intact):

```kotlin
            SoftButtonStyle.HERO -> {
                val accent = SWColor.accent(LocalSWScheme.current)
                val reduce = LocalReduceMotion.current
                val glowAlpha = if (reduce) 0.45f else {
                    val t = rememberInfiniteTransition(label = "hero-glow")
                    val v by t.animateFloat(
                        initialValue = 0.35f,
                        targetValue = 0.55f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(SWMotion.breathCycleMs, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "alpha"
                    )
                    v
                }
                Modifier
                    .shadow(24.dp, shape, clip = false, ambientColor = accent.copy(alpha = glowAlpha), spotColor = accent.copy(alpha = glowAlpha))
                    .background(SWGradient.auroraGlow(LocalSWScheme.current))
                    .border(1.dp, Brush.verticalGradient(
                        0f to Color.White.copy(alpha = 0.35f),
                        0.4f to Color.Transparent
                    ), shape)
            }
```

Add the imports at the top:
```kotlin
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lizhi1026.sleepwhisper.core.visualkit.LocalReduceMotion
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor
import com.lizhi1026.sleepwhisper.core.visualkit.SWGradient
import com.lizhi1026.sleepwhisper.core.visualkit.SWMotion
```

(Some may already be present.)

- [ ] **Step 4: Compile**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL. Existing `SoftButton(style = SoftButtonStyle.ACCENT)` callers (on Home Sleep CTA, Onboarding "Begin") are untouched.

- [ ] **Step 5: Add a Preview for the HERO variant**

At the bottom of `SoftButton.kt`, add (or extend) a preview function:

```kotlin
@androidx.compose.ui.tooling.preview.Preview(showBackground = true, backgroundColor = 0xFF07101F)
@androidx.compose.runtime.Composable
private fun PreviewSoftButtonHero() {
    com.lizhi1026.sleepwhisper.core.visualkit.SWTheme(com.lizhi1026.sleepwhisper.core.visualkit.SWScheme.DARK) {
        SoftButton(text = "Sleep", onClick = {}, style = SoftButtonStyle.HERO)
    }
}
```

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/SoftButton.kt
git commit -m "visualkit: SoftButton hero style with aurora glow and pulsing outer shadow"
```

---

## Task 14 — Add `Hairline` decoration

**Files:**
- Create: `app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/Hairline.kt`

- [ ] **Step 1: Create the file**

```kotlin
package com.lizhi1026.sleepwhisper.core.visualkit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor

/**
 * 0.5dp divider in border color at 60% alpha. Sprinkled on card edges and
 * between list rows to give the eye gentle resting points without heavy lines.
 */
@Composable
fun Hairline(modifier: Modifier = Modifier, thickness: Dp = 0.5.dp) {
    val scheme = LocalSWScheme.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(thickness)
            .background(SWColor.border(scheme).copy(alpha = 0.6f))
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
git add app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/Hairline.kt
git commit -m "visualkit: add Hairline divider component"
```

---

## Task 15 — Add `InnerHighlight` Modifier extension

**Files:**
- Create: `app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/InnerHighlight.kt`

- [ ] **Step 1: Create the file**

```kotlin
package com.lizhi1026.sleepwhisper.core.visualkit.components

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWScheme

/**
 * Draws a 1dp gradient stroke at the top of any elevated surface — the
 * "Liquid Glass" cue. White → transparent on dark schemes; on DAY it falls
 * back to a near-invisible warm-paper accent so the same call works
 * cross-scheme without conditionals at the call site.
 */
fun Modifier.innerHighlight(cornerRadius: Dp = 16.dp): Modifier = composed {
    val scheme = LocalSWScheme.current
    val brush = when (scheme) {
        SWScheme.DAY -> Brush.verticalGradient(
            0f to Color.White.copy(alpha = 0.45f),
            0.45f to Color.Transparent
        )
        SWScheme.DARK -> Brush.verticalGradient(
            0f to Color.White.copy(alpha = 0.18f),
            0.45f to Color.Transparent
        )
        SWScheme.NIGHT -> Brush.verticalGradient(
            0f to Color(1.00f, 0.75f, 0.75f).copy(alpha = 0.10f),
            0.45f to Color.Transparent
        )
    }
    border(1.dp, brush, RoundedCornerShape(cornerRadius))
}
```

- [ ] **Step 2: Compile and commit**

```bash
./gradlew :app:compileDebugKotlin
git add app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/InnerHighlight.kt
git commit -m "visualkit: add Modifier.innerHighlight extension"
```

---

## Task 16 — Add shared `SectionLabel` component

**Files:**
- Create: `app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/SectionLabel.kt`

- [ ] **Step 1: Create the file**

```kotlin
package com.lizhi1026.sleepwhisper.core.visualkit.components

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor
import com.lizhi1026.sleepwhisper.core.visualkit.SWFont

/**
 * Uppercase tracked label used as a section divider throughout the app —
 * e.g. "WAKE WINDOW", "ALL SOUNDS", "THIS WEEK". Promoted from inline patterns
 * scattered across HomeScreen / PlayerScreen / TrendsScreen.
 */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    val scheme = LocalSWScheme.current
    BasicText(
        text = text.uppercase(),
        style = SWFont.labelMD().copy(color = SWColor.textSecondary(scheme)),
        modifier = modifier
    )
}
```

- [ ] **Step 2: Compile and commit**

```bash
./gradlew :app:compileDebugKotlin
git add app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/SectionLabel.kt
git commit -m "visualkit: add shared SectionLabel component"
```

(Existing screens still use their inline label patterns. Phases 1-5 migrate them to `SectionLabel`.)

---

## Task 17 — Add `ChevronTrail` component

**Files:**
- Create: `app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/ChevronTrail.kt`

- [ ] **Step 1: Create the file**

```kotlin
package com.lizhi1026.sleepwhisper.core.visualkit.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor

/**
 * Subtle 3-dot trail used in place of `ic_chevron_right` for list rows and
 * settings rows. Reads as forward-affordance without a heavy arrow shape.
 */
@Composable
fun ChevronTrail(modifier: Modifier = Modifier, tint: Color = Color.Unspecified) {
    val scheme = LocalSWScheme.current
    val color = if (tint == Color.Unspecified) SWColor.textTertiary(scheme) else tint
    Canvas(modifier = modifier.size(width = 18.dp, height = 6.dp)) {
        val r = size.height / 3f
        val gap = (size.width - r * 6f) / 2f
        for (i in 0 until 3) {
            drawCircle(
                color = color.copy(alpha = 0.4f + 0.2f * i),
                radius = r,
                center = Offset(r + i * (r * 2 + gap), size.height / 2)
            )
        }
    }
}
```

- [ ] **Step 2: Compile and commit**

```bash
./gradlew :app:compileDebugKotlin
git add app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/ChevronTrail.kt
git commit -m "visualkit: add ChevronTrail (3-dot row affordance)"
```

---

## Task 18 — Add `SerifMetricRow` component

**Files:**
- Create: `app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/SerifMetricRow.kt`

- [ ] **Step 1: Create the file**

```kotlin
package com.lizhi1026.sleepwhisper.core.visualkit.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor
import com.lizhi1026.sleepwhisper.core.visualkit.SWFont
import com.lizhi1026.sleepwhisper.core.visualkit.SWSpacing

/**
 * Horizontal "label · value" row used by Trends and Settings. Label is the
 * labelMD style (uppercase, tracked); value is bodyLG serif. Optional chevron
 * affordance via [ChevronTrail].
 */
@Composable
fun SerifMetricRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    showChevron: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val scheme = LocalSWScheme.current
    val rowMod = if (onClick != null) modifier.fillMaxWidth().clickable(onClick = onClick)
                 else modifier.fillMaxWidth()
    Row(
        modifier = rowMod.padding(vertical = SWSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        BasicText(
            text = label,
            style = SWFont.bodyLG().copy(color = SWColor.textPrimary(scheme))
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SWSpacing.xs)) {
            BasicText(
                text = value,
                style = SWFont.bodyLG().copy(color = SWColor.textSecondary(scheme))
            )
            if (showChevron) ChevronTrail()
        }
    }
}
```

- [ ] **Step 2: Compile and commit**

```bash
./gradlew :app:compileDebugKotlin
git add app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/SerifMetricRow.kt
git commit -m "visualkit: add SerifMetricRow for Trends and Settings"
```

---

## Task 19 — Add `AuroraTextHero` component

**Files:**
- Create: `app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/AuroraTextHero.kt`

Large-display text with a slowly-drifting gradient mask. Performance-bounded to display-size text (callers pass `SWFont.displayLG()` or `displayXL()`).

- [ ] **Step 1: Create the file**

```kotlin
package com.lizhi1026.sleepwhisper.core.visualkit.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import com.lizhi1026.sleepwhisper.core.visualkit.LocalReduceMotion
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor
import com.lizhi1026.sleepwhisper.core.visualkit.SWMotion

/**
 * Display text with a drifting aurora gradient applied to the glyph fill.
 * Intended for displayXL/displayLG only — applying this to body text creates
 * visual noise.
 *
 * Pauses to a static gradient when reduce-motion is on.
 */
@Composable
fun AuroraTextHero(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier
) {
    val scheme = LocalSWScheme.current
    val reduce = LocalReduceMotion.current

    val phase = if (reduce) 0f else {
        val t = rememberInfiniteTransition(label = "aurora-text")
        val v by t.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(SWMotion.backdropDriftMs / 2, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "phase"
        )
        v
    }

    val brush = Brush.linearGradient(
        colors = listOf(
            SWColor.textPrimary(scheme),
            SWColor.accentSecondary(scheme),
            SWColor.textPrimary(scheme)
        ),
        start = Offset(0f, 0f),
        end = Offset(1000f * (0.5f + phase), 200f)
    )

    BasicText(
        text = text,
        style = style.copy(brush = brush),
        modifier = modifier
    )
}
```

- [ ] **Step 2: Compile and commit**

```bash
./gradlew :app:compileDebugKotlin
git add app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/AuroraTextHero.kt
git commit -m "visualkit: add AuroraTextHero with drifting gradient mask"
```

---

## Task 20 — Full smoke walkthrough across DAY / DARK / NIGHT

**Files:** none modified — verification only.

- [ ] **Step 1: Build and install**

```bash
./gradlew :app:installDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: Walk through every screen in DAY scheme**

Launch the app. From Settings, set theme to DAY (or system → check device is in light mode).

Verify:
- **Home** — greeting reads in serif italic; baby name in display serif; quick action tiles still functional; Sleep CTA shows 3-ring aurora pulse
- **Trends** — header reads in serif; weekly bars present (still original visual until Phase 4)
- **Settings** — header reads in serif
- **Player** — list scrolls; now-playing waveform shows aurora-gradient bars
- Backdrop shows soft drifting paper-warm aurora; barely-perceptible ribbon motion over ~18s

- [ ] **Step 3: Switch to DARK and repeat**

In Settings → Appearance → DARK. Repeat all screens. Confirm:
- Aurora backdrop now shows distinct deep-night → indigo → starlight-purple ribbon drift
- GlassCard surfaces have moon-halo top-left + aurora-glow whisper bottom-right
- 3-ring PulseRing on Sleep CTA now offsets orange → blend → starlight-purple
- All text is moonlit-silver (textPrimary)

- [ ] **Step 4: Switch to NIGHT and verify accessibility preserved**

In Settings → Appearance → NIGHT (red-light, intended for 3am feedings).

Confirm:
- All text is the pre-existing reddish-pink at ≥ 4.5:1 contrast
- Aurora backdrop is static (no drift) — NIGHT does not animate
- 3-ring pulse runs as a static halo (no animation)

If any text is unreadable, the NIGHT branch in `SWColor` was modified — revert to the values verified by `SWColorTest`.

- [ ] **Step 5: Enable reduce-motion and re-verify**

```bash
adb shell settings put secure transition_animation_scale 0
adb shell settings put secure window_animation_scale 0
adb shell settings put secure animator_duration_scale 0
```

Re-launch the app. Confirm:
- Aurora backdrop is static (no drift)
- PulseRing is static
- AudioWaveform is at its resting envelope (no animation)
- RollingNumber digits crossfade instead of rolling

Restore animations:
```bash
adb shell settings put secure transition_animation_scale 1
adb shell settings put secure window_animation_scale 1
adb shell settings put secure animator_duration_scale 1
```

- [ ] **Step 6: Capture before/after screenshots for the phase summary**

For each screen (Home, Trends, Settings, Player) capture one screenshot in DAY and one in DARK. Store in `docs/superpowers/screenshots/2026-05-20-phase-0/` (create the dir if missing). These attach to the phase PR description as the visual receipt.

```bash
mkdir -p docs/superpowers/screenshots/2026-05-20-phase-0
adb exec-out screencap -p > docs/superpowers/screenshots/2026-05-20-phase-0/home-dark.png
# repeat for each combination
```

- [ ] **Step 7: Final commit (screenshots + any last visual tweaks)**

```bash
git add docs/superpowers/screenshots/
git commit -m "phase-0: capture verification screenshots across all 3 schemes"
```

- [ ] **Step 8: Run all unit tests one last time**

```bash
./gradlew :app:testDebugUnitTest
```

Expected: all tests pass, including the new `SWColorTest` (5 tests) and `HeroBackdropControllerTest` (4 tests).

---

## Phase 0 Done When

- All 20 tasks committed
- `./gradlew :app:testDebugUnitTest` passes
- `./gradlew :app:assembleDebug` produces a working APK
- Manual smoke walkthrough in DAY / DARK / NIGHT shows: serif typography everywhere, Aurora drift on DAY+DARK backdrops, 3-ring pulse on Sleep CTA, moonlit-glass treatment on cards, NIGHT scheme accessibility preserved
- Screenshots captured in `docs/superpowers/screenshots/2026-05-20-phase-0/`

After this phase, every later phase (1-5) can pick up the language and add screen-specific layouts and hero moments without re-litigating tokens.
