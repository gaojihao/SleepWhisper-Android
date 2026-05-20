# Aurora Redesign — Master Design Spec

**Date:** 2026-05-20
**Status:** Approved (pending user review)
**Scope:** Visual & interaction redesign of all six screens of SleepWhisper, executed in six sequential phases. This document establishes the unified design language; each phase gets its own follow-up brainstorming → plan cycle.

---

## 1. North Star

SleepWhisper today is a sophisticated baby-sleep tracker with a custom design system (`core/visualkit/`) ported from iOS — three color schemes (DAY / DARK / NIGHT including a red-light scheme for 3am feedings), bespoke components (GlassCard, BreathingBackground, Starfield, PulseRing, LiquidProgressRing, AudioWaveform, RollingNumber, SoftButton), and a well-conceived 4-based spacing scale.

The bones are strong. The job is to push it from "polished" to "an app a designer would screenshot and share." We do this by committing to one cohesive design DNA and concentrating ambition on three specific hero moments.

**Design DNA: Aurora / Cinematic Calm.** Generous whitespace, super-large serif headlines, multi-layer gradient backdrops, glass and mist effects, slow breathing motion. The emotional register of a bedtime storybook rather than a productivity dashboard.

**Three hero moments:**
1. **Sleep CTA → Sleeping morph** — tapping the Sleep button doesn't transition screens, the world *becomes* night.
2. **Sleeping immersive night sky** — the sky behind the timer is the real night progressing in real time.
3. **Player audio ↔ color synchrony** — choosing a sound changes the room's color, and that color persists app-wide.

---

## 2. Phasing

The master spec produces the unified language. Implementation splits into six sequential sub-specs:

| Phase | Title | Output |
|---|---|---|
| **0** | Aurora Design System | Tokens, fonts, evolved components, new core components. All screens migrate to new tokens but layouts unchanged. Prerequisite for every other phase. |
| **1** | Home + Sleep CTA hero | Home redesign + Hero #1 morph + `MorphingHeroSurface` + `HeroBackdropController`. |
| **2** | Sleeping immersive sky | Sleeping redesign + Hero #2 + `NightSkyCanvas` with parallax/meteors. |
| **3** | Player audio↔color | Player redesign + Hero #3 + `PresetAuraOrb` + per-preset aura table + CompositionLocal wiring. |
| **4** | Trends + Settings polish | Apply tokens to data screens, aurora-tinted chart bars, hairline list rows. |
| **5** | Onboarding + Welcome | Reapply tokens to welcome ritual + onboarding, big DM Serif name moment. |

Evolution strategy: modify existing `core/visualkit/` files **in place**, not a v2 sidecar. Deleted components are removed cleanly, not left as deprecated stubs.

---

## 3. Design Tokens

### 3.1 Color (`SWColor.kt`)

The public API stays the same — `SWColor.primary(scheme)` / `surface(scheme)` / etc. Only the returned values change. This means screens don't have to be rewritten for color migration; Phase 0 changes propagate automatically.

| Token | DAY (Aurora dawn) | DARK (Aurora night) | NIGHT (red-light, unchanged) |
|---|---|---|---|
| `surface` | `#F4F2EE` (paper warm) | `#07101F` (deep night) | `#0A0202` (unchanged) |
| `surfaceElevated` | `#FFFFFF` | `#0F1A2D` | `#1A0707` (unchanged) |
| `surfaceSunken` | `#ECEAE5` | `#050912` | `#050101` (unchanged) |
| `primary` | `#FF8A5E` (dusk orange) | `#FFB088` (dusk orange) | `#B36B6B` (unchanged) |
| `accentSecondary` *(NEW)* | `#9B85FF` (starlight purple) | `#B8A4FF` (starlight purple) | unused |
| `textPrimary` | `#0A1428` | `#ECF2F8` (moonlit silver) | `#FFBFBF` (unchanged) |
| `textSecondary` | `#4A5573` | `#8A93A8` | `#D98F8F` (unchanged) |
| `textTertiary` | `#7A859C` | `#5C667A` | `#B36B6B` (unchanged) |
| `border` | `#E2DDD2` | `#1A2438` | `#260C0C` (unchanged) |

**NIGHT scheme is not modified except via structural tokens (spacing, motion).** Its color values were tuned for ≥ 4.5:1 contrast at 3am and that work is preserved.

**New gradient tokens** added to `SWGradient.kt`:

```
auroraBackdrop(scheme)   Multi-stop diagonal: deep-night → indigo → dusk-orange whisper
auroraGlow(scheme)       Radial purple → orange, for hero CTAs and now-playing accents
moonHalo(scheme)         Soft white-silver radial for cards on dark scheme
```

### 3.2 Typography (`SWFont.kt`)

Replace `FontFamily.SansSerif` fallbacks with bundled fonts. Body uses Source Serif 4 (designed for screens); headlines use DM Serif Display.

```kotlin
private val DisplaySerif = FontFamily(Font(R.font.dm_serif_display_regular))
private val BodySerif = FontFamily(
    Font(R.font.source_serif_4_regular,  FontWeight.Normal),
    Font(R.font.source_serif_4_italic,   FontWeight.Normal, FontStyle.Italic),
    Font(R.font.source_serif_4_semibold, FontWeight.SemiBold),
    Font(R.font.source_serif_4_bold,     FontWeight.Bold)
)
private val CjkSerifFallback = FontFamily(Font(R.font.noto_serif_cjk_sc_regular))
```

Type ramp (existing names refreshed):

| Token | Size | Family | Weight | Tracking | Line-height |
|---|---|---|---|---|---|
| `displayXL` | 96sp | DM Serif Display | Regular | -2sp | 1.0 |
| `displayLG` | 64sp | DM Serif Display | Regular | -1sp | 1.05 |
| `displayMD` | 48sp | DM Serif Display | Regular | -0.5sp | 1.1 |
| `titleXL` | 34sp | Source Serif 4 | SemiBold | -0.5sp | 1.1 |
| `titleLG` | 24sp | Source Serif 4 | SemiBold | 0 | 1.2 |
| `titleMD` | 18sp | Source Serif 4 | SemiBold | 0 | 1.3 |
| `bodyLG` | 17sp | Source Serif 4 | Regular | 0 | **1.55** |
| `bodyMD` | 15sp | Source Serif 4 | Regular | 0 | **1.55** |
| `labelMD` | 12sp | Source Serif 4 | SemiBold | 1.6sp + UPPERCASE | 1.6 |
| `labelSM` | 10sp | Source Serif 4 | SemiBold | 1.4sp + UPPERCASE | 1.5 |
| `displayLGTabular` | 64sp | Source Serif 4 | SemiBold + `tnum` | 0 | 1.0 |
| `displayXLTabular` | 96sp | Source Serif 4 | SemiBold + `tnum` | -2sp | 1.0 |
| `serifItalic(size)` | as-arg | Source Serif 4 Italic | Regular | 0 | 1.4 |

Body line-height is raised from ~1.3 to **1.55** — serif at small sizes needs breathing room to stay legible. List screens are explicitly tested for fatigue.

**CJK fallback strategy.** Source Serif 4 has no CJK glyphs. We add a helper `SWFont.cjkAware(style: TextStyle): TextStyle` that composes `BodySerif + CjkSerifFallback` so any text containing Chinese characters resolves to Noto Serif CJK SC. Required usage sites: greeting (`R.string.home_greeting_*`), baby names, preset names that may be localized, all settings labels in `values-zh-rCN/`.

**Font bundle budget.** DM Serif Display Regular (~120KB) + Source Serif 4 Regular/Italic/SemiBold/Bold Latin-subset (~160KB total) + Noto Serif CJK SC Regular body-subsetted to ~4000 chars + extended digits (~600KB). Total ≈ 880KB. Documented in build files.

### 3.3 Motion (`SWTokens.kt → SWMotion`)

Replace conservative durations with a cinematic breath rhythm:

| Token | Value | Note |
|---|---|---|
| `pressInMs` | 80 | Was 100 — slightly snappier inhale |
| `pressOutMs` | 260 | Was 150 — exhale longer than inhale |
| `screenInMs` | 420 | Was 250 — give crossfades gravity |
| `screenOutMs` | 320 | Was 200 |
| `heroMorphMs` | 1200 | *New* — Hero #1 transition |
| `backdropDriftMs` | 18000 | *New* — Aurora gradient drift cycle |
| `breathCycleMs` | 4200 | *New* — Card breathing period |
| `toastMs` | 200 | Unchanged — must stay snappy |
| `bannerMs` | 200 | Unchanged |
| `numberSwitchMs` | 100 | Unchanged |
| `longPressMs` | 600L | Unchanged |
| `fadeInMs` | 1500L | Unchanged |
| `defaultFadeOutMs` | 5000L | Unchanged |

New shared spring spec: `SWSpring.gentle = spring(dampingRatio = 0.85f, stiffness = 180f)`. Used for every interactive element so the whole app shares one tactile language.

---

## 4. Components

### 4.1 Evolved (existing files, upgraded in place)

**`GlassCard`** — Current implementation is a flat surface with a 1dp border on DAY. Upgrade to "moonlit glass":
- DARK/NIGHT: surface + soft `moonHalo` radial at top-left + `auroraGlow` whisper at bottom-right
- DAY: surface + 1dp border + subtle inner highlight gradient (top 30% slightly brighter)
- All schemes: dual-layer shadow (close tight + distant wide) for cinematic depth
- New parameter `hero: Boolean = false` — when true, card has continuous subtle breath scale `0.998 ↔ 1.002` over `breathCycleMs`

**`BreathingBackground`** → conceptually **Aurora Backdrop** (file stays `BreathingBackground.kt`, class becomes `AuroraBackdrop` for grep continuity):
- Three diagonal aurora ribbons (purple, orange-whisper, deep blue) drift independently with phases offset by `2π/3`
- Subtle film grain overlay (8% opacity RGB noise) — the cinematic detail that lifts above generic gradients
- Auto-pauses when `LocalReduceMotion = true`
- Consumes `LocalHeroBackdropController` to honor the current ambient color (audio preset aura or time-of-day)

**`PulseRing`** — upgrade from single alpha pulse to a 3-ring offset pulse with each ring a slightly different aurora hue (orange → orange/purple → purple), giving CTAs a "halo" rather than a "ring".

**`AudioWaveform`** — upgrade:
- Bar heights drawn from a sine-perturbed FFT-shape (no real audio analysis needed — just convincing)
- Color gradient along the waveform (dusk-orange → starlight-purple)
- Bars smoothly tween between frames with `SWSpring.gentle`

**`RollingNumber`** — keep behavior, upgrade with serif tabular numerals (Source Serif 4 + `tnum`) and a subtle vertical motion blur during the digit roll.

**`SoftButton`** — add a `hero` style that uses the aurora-glow gradient fill, with a 1dp inner highlight stroke and a 24-blur outer glow pulsing at `breathCycleMs`.

**`Starfield`** — already used by Sleeping; subsumed into the new `NightSkyCanvas` (see 4.2). Existing file becomes deprecated and is deleted in Phase 2 once `NightSkyCanvas` is in place.

### 4.2 New components

| Component | Purpose | Used by |
|---|---|---|
| `NightSkyCanvas` | Time-of-day-aware starfield with parallax layers, meteor showers, drifting cloud wisps | Sleeping screen |
| `MorphingHeroSurface` | Drives the CTA → Sleeping transition (Hero #1). Holds a single shared `progress: Float` consumed by Compose Modifier extensions to morph shape/blur/color across the route boundary | Home ↔ Sleeping handoff |
| `AuroraTextHero` | Large-display text with a slowly-drifting gradient mask (performance-bounded to `displayXL`) | Greeting, Sleeping title, hero numbers |
| `SectionLabel` | Already exists inline in screens — promote to shared component. Uppercase, tracking 1.6sp, label color | Every screen |
| `SerifMetricRow` | Standard horizontal "label · value" line with serif tabular value and labelMD label | Trends, Settings |
| `PresetAuraOrb` | 64dp glassy orb tinted with preset's signature color, internal animated noise particles. Drives Hero #3 | Player |
| `HeroBackdropController` | CompositionLocal providing the current "ambient color" derived from active audio preset or time-of-day. Consumed by `AuroraBackdrop` | App-wide |

### 4.3 Decoration tokens

- **`Hairline`** — 0.5dp divider in `border` color at 60% alpha. Sprinkled on card edges and list rows.
- **`InnerHighlight`** — 1dp gradient stroke (white → transparent on dark schemes) at the top of any elevated surface. The "Liquid Glass" cue.
- **`ChevronTrail`** — replaces plain `ic_chevron_right` with a subtle 3-dot trail that fades on tap.

### 4.4 What gets deleted in Phase 0

- `Starfield.kt` — subsumed by `NightSkyCanvas` in Phase 2 (deferred deletion until Phase 2)
- The inline `SectionLabel` patterns scattered across `HomeScreen.kt`, `PlayerScreen.kt`, `TrendsScreen.kt` — replaced by the new shared component

No public API breaks expected at the component level — `SWColor.primary(scheme)` keeps returning a `Color`, just a different one.

---

## 5. Per-Screen Treatment Overview

Detailed per-screen specs are written when each phase is brainstormed. This section establishes the visual statement and structural sketch.

### 5.1 Home (Phase 1)

A warm, breathing dawn-or-dusk surface (palette shifts with hour) holding the parent's day in serif. Greeting reads like a journal entry. Sleep CTA sits like a moon — halo'd and gravitational — the visual center of the screen.

```
┌─────────────────────────────────────┐
│   ⋅ aurora drift  ⋅                 │
│  good evening,         ← serif italic 22sp
│  Emma                  ← DM Serif 64sp
│  ────                  ← Hairline
│  142 DAYS WITH US      ← labelMD spaced
│                                     │
│  ┌─ WAKE WINDOW ──────────────┐    │
│  │ 47 min   ●───────○──        │    │  GlassCard hero
│  └─────────────────────────────┘    │
│                                     │
│  Now playing  ●  Womb   ≋≋≋  →     │  PresetAuraOrb + wave + chevron
│                                     │
│  ┌──┐ ┌──┐    ┌──┐ ┌──┐             │  4 QuickActionTile
│                                     │
│            ╭─────────╮              │
│         ╱  │  Sleep  │  ╲           │  hero SoftButton + 3-ring PulseRing
│        ⊙   │  ☾ Zzz  │   ⊙          │  long-press → SleepTypePicker
│            ╰─────────╯              │
└─────────────────────────────────────┘
```

Header gets the most dramatic upgrade: baby's name was 28sp, now **64sp DM Serif Display** — this is the "极致排版" move.

### 5.2 Sleeping (Phase 2)

Not a timer screen — a window onto the night. The sky behind the timer is the actual current night, advancing in real time.

```
┌─────────────────────────────────────┐
│  · ·   ·       · ✦         ·        │
│      ·   · ✦       ·   ·            │  NightSkyCanvas
│   ·         ⟶☄          ·    ·      │  (meteor occasionally)
│                                     │
│       sleeping since 9:47           │  serif italic 17sp
│                                     │
│        0 2 : 3 4 : 1 8              │  DM Serif 96sp tabular
│        ─────                        │  Hairline
│        Emma, 142 days               │  labelMD
│                                     │
│         ◯ ── ── ── ───              │  LiquidProgressRing
│           Now playing               │
│             Womb  ≋≋                │  44% opacity
└─────────────────────────────────────┘
```

The clock face is the hero. Background advances continuously through the night palette.

### 5.3 Player (Phase 3)

Choosing a sound changes the room. Background, glow color, and particle texture all shift to the chosen preset's aura. The list reads like a poetry index, not a settings menu.

```
┌─────────────────────────────────────┐
│ ── drag handle ──                   │
│  Tonight's room                     │  serif italic 24sp
│  ────                               │
│  RECOMMENDED FOR 4 MONTHS           │  labelMD
│                                     │
│  ┌──────────────────────────────┐   │
│  │ ◉ Womb           ≋≋≋  active │   │  PresetAuraOrb glowing
│  │   Pink & deep · 65 dB        │   │
│  └──────────────────────────────┘   │
│  ┌──────────────────────────────┐   │
│  │ ◯ Ocean                       │   │
│  │   Cool & wide · 60 dB        │   │
│  └──────────────────────────────┘   │
│                                     │
│  ── ALL SOUNDS ──                   │
│  ┌──────────────────────────────┐   │
│  │ ◯ Heartbeat                   │   │
│  └──────────────────────────────┘   │
│                                     │
│   15m    30m   60m   ∞              │  duration chips, serif numerals
└─────────────────────────────────────┘
```

Active orb pulses; rows above/below blend 12% of the active preset's mid-color into their surface tint (RGB lerp), giving a subtle "reflected light" feel without changing their text/icon colors.

### 5.4 Trends (Phase 4)

Data as poetry. Numbers in serif feel like manuscript notations. Weekly chart uses aurora gradient bars, not flat fills.

```
┌─────────────────────────────────────┐
│  How Emma slept                     │  DM Serif Display 34sp
│  ────                               │
│  THIS WEEK                          │
│                                     │
│  ┌──────────────────────────────┐   │
│  │ 11h 42m                       │   │  RollingNumber serif
│  │ average sleep · last 7 days   │   │
│  │ ╭─╮ ╭─╮ ╭─╮ ╭─╮ ╭─╮ ╭─╮ ╭─╮ │   │  gradient bars (auroraGlow)
│  │ M  T  W  T  F  S  S          │   │
│  └──────────────────────────────┘   │
│                                     │
│  ── TODAY'S TIMELINE ──             │
│  ┌──────────────────────────────┐   │
│  │  9:20  ──── nap ──── 10:15   │   │  serif timeline
│  │ 11:45  ┃  feeding             │   │
│  │ 13:00  ──── nap ──── 14:30   │   │
│  └──────────────────────────────┘   │
└─────────────────────────────────────┘
```

### 5.5 Settings (Phase 4)

Settings as a writing desk. Each row a single serif line, generous space, hairline dividers, no noisy icons.

```
┌─────────────────────────────────────┐
│  Settings                           │  DM Serif Display 34sp
│                                     │
│  BABY                               │
│  Name              Emma         ›   │  SerifMetricRow + ChevronTrail
│  ────                               │  Hairline
│  Born          Dec 28, 2025     ›   │
│                                     │
│  THEME                              │
│  Appearance         Auto        ›   │
│  ────                               │
│  Night mode      11pm–6am       ›   │
└─────────────────────────────────────┘
```

### 5.6 Onboarding / Welcome (Phase 5)

The welcome page is already a cinematic ritual — we strengthen its identity. Slow drifting aurora behind a single serif line per step. The moment of meeting (parent typing the name) uses the largest DM Serif treatment in the app.

```
┌─────────────────────────────────────┐
│  ⋅⋅  aurora drift  ⋅⋅               │
│                                     │
│  meet                               │  serif italic 28sp
│  Emma                               │  DM Serif Display 96sp
│  December 28                        │  serif italic 22sp
│                                     │
│            ╭─────────╮              │
│            │ Begin   │              │  hero SoftButton
│            ╰─────────╯              │
└─────────────────────────────────────┘
```

---

## 6. Hero Moments — Choreography

### 6.1 Hero #1 — Sleep CTA → Sleeping morph

The parent taps Sleep on Home. Instead of a screen transition, the world *becomes* night.

```
t=0ms       Tap. Soft haptic. SoftButton scales 0.96, halo bloom.
t=80ms      Button shape morphs pill → circle (320dp → 280dp wide)
            Aurora backdrop palette begins shifting deeper-blue
t=200ms     Button continues morphing toward full-screen circle
            QuickActionTiles, greeting, NowPlaying fade to 0 opacity
            (sequential, 60ms stagger from top)
t=400ms     Button is now a 600dp circle centered
            Stars spark into existence from circle's perimeter outward
            Pulse rings expand and dissolve into the night sky
t=700ms     Background fully transitioned to NightSkyCanvas
            Circle dissolves into the central halo of the timer
t=900ms     Sleeping timer fades in from center
            "sleeping since 9:47" italic label settles in
t=1200ms    Settled. Real-time clock begins ticking.
```

**Technical approach:** Compose `SharedTransitionLayout` (1.7+) with a shared element key between the SoftButton on Home and the timer halo on Sleeping. Both screens render simultaneously during transition with `SharedTransitionScope.sharedElement()`. Background palette interpolates via a single `Animatable<Color>` lifted to `AppStateContainer` level so it crosses route boundaries.

**Reverse (stop):** Inverse storyboard with dawn palette, button reforms into pill, parent receives `SleepSummaryOverlay` afterward. Total reverse 1200ms.

**Reduce-motion fallback:** If `LocalReduceMotion = true`, replace morph with a 250ms crossfade + backdrop dim. No shared elements, no shape morph.

### 6.2 Hero #2 — Sleeping immersive night sky

**Five composition layers**, all drawn on a single `Canvas` with offscreen layer caching:

| Layer | Content | Note |
|---|---|---|
| 1 (deepest) | Aurora gradient mesh — palette interpolates against wall-clock time | See palette table below |
| 2 | ~80 static stars with breath-twinkle (random phase, opacity 0.3↔0.9 over 3–5s) | Throttled to ~10fps |
| 3 | ~6 drifting stars moving 0.5dp/sec horizontally, extra brightness | |
| 4 (rare) | Meteor shower: a streak every 90–180s randomly, diagonal, 600ms total, fading tail | Disabled if reduce-motion |
| 5 (atmosphere) | Subtle cloud-wisps (white noise blobs, 2% opacity, slow drift) | Only visible 22:00–04:00 |

Sky palette progression:

| Wall-clock | Palette |
|---|---|
| 19:00 dusk | `#1A2540 → #FFB088` (horizon hint) |
| 21:00 evening | `#0F1B30 → #B8A4FF` (whisper) |
| 00:00 night | `#050912 → #1F2848` (deep) |
| 03:00 dead-night | same, film-grain density 1.5× |
| 05:00 dawn | `#1F2540 → #FFA98E` (horizon) |
| 06:30 morning | hand back to Home's daytime palette |

**Timer treatment:**
- DM Serif Display 96sp tabular, color `#ECF2F8` (moonlit silver)
- Each digit drawn separately; on tick, the seconds digit does a 100ms vertical roll with motion blur
- Behind the timer, a 280dp `LiquidProgressRing` (existing, recolored) fills toward the recommended wake-window end, not elapsed time — gives parents a glanceable "how close to wake" answer

**Now-playing chip:** Bottom-center, 44% opacity. Tap → expands into a mini Player overlay without leaving Sleeping. Single-finger swipe down dismisses overlay.

**Interactions:**
- Single tap anywhere → dims timer to 24% opacity, keeps sky live (the "just glancing" mode). Tap again to restore.
- Long-press in center → reveals stop/pause controls. Deliberate gesture to avoid accidents.

### 6.3 Hero #3 — Player audio↔color synchrony

Selecting a sound changes the room. Each preset has an aura (signature mesh-gradient palette + particle texture) and selection morphs the whole-screen backdrop over ~600ms.

**Per-preset signature** (extends `AudioPreset` model with `auraColors: List<Color>`):

| Preset | Aura colors |
|---|---|
| Womb | `#C46B8C` → `#5B4880` → `#1A0F1E` |
| Ocean | `#1F4F73` → `#2E7A86` → `#051A26` |
| Rain | `#4A5A6E` → `#B7C3CC` → `#0F1620` |
| White | `#FAFAFA` → `#ECE9E2` → `#1F222A` |
| Brown | `#8A6850` → `#3E2A1F` → `#1A100A` |
| Pink | `#E6B6B8` → `#B85F77` → `#1A0F12` |
| Heartbeat | `#C44E5C` → `#4E1A22` → `#1A0A0E` |
| Vacuum | `#6E6A66` → `#38362F` → `#14130F` |
| Dryer | `#C99B5A` → `#6B4A20` → `#1A1108` |
| Fan | `#B5BFC8` → `#4F5C68` → `#0E1418` |
| Lullaby 1 | `#B8A4FF` → `#4D3C7A` → `#0F0C1E` |
| Lullaby 2 | `#FFB088` → `#8C4A26` → `#1A0E08` |

**On selection:**

```
t=0ms       Soft haptic
            New auraColors lifted to HeroBackdropController CompositionLocal
            AuroraBackdrop palette begins interpolating
            (Animatable<Color> per stop, 600ms)
            PresetAuraOrb on selected row begins glowing
            Particle texture (24 floating motes tinted to aura) fades in
t=180ms     Adjacent rows (±2 from selected) blend 12% of selected aura's mid-color
            into their own surface tint (RGB lerp, not HSL rotation), creating the
            "reflected light" feel without changing their text/icon colors
t=400ms     Active row's metadata line reveals (italic descriptor + dB tag)
t=600ms     Settled
```

**Cross-screen persistence:** The active-preset color is **shared via CompositionLocal back to `AuroraBackdrop`**, so when the player sheet is dismissed, Home and Sleeping inherit the chosen aura. The "room" persists across screens — this is the unifying detail.

**Particle motes:** 24 small (3-5dp) blurred dots tinted with aura's mid-color, drifting at 0.2dp/sec with sine-perturbed paths. Only visible on Player screen. Disabled if reduce-motion.

**Duration chips (15/30/60/∞):** Existing component; restyled as serif numerals in pill-row, selected chip gets aura-tinted glow.

---

## 7. Risks and Mitigations

| ID | Risk | Mitigation |
|---|---|---|
| R1 | Serif on long lists fatigues the eye | bodyMD 15sp with **1.55 line-height** (up from 1.3), Source Serif 4 (screen-optimized), hairline dividers. Phase-0 includes a snapshot test of a many-row list reviewed before lock. |
| R2 | Chinese fallback under serif clashes | Bundle Noto Serif CJK SC Regular (subset, ~600KB) and use `SWFont.cjkAware(...)` for any text that may contain CJK. |
| R3 | Aurora backdrop battery cost during 10h Sleeping | Pause backdrop drift when screen off (`Lifecycle.STARTED` gate). Stars run at ~10fps via choreographer throttling. Meteors rare. Reduce-motion disables drift. |
| R4 | `SharedTransitionLayout` API stability | Stable in Compose 1.7+. Verify BOM ≥ 2024.06.00 in Phase 0; bump if needed. Fallback: build morph manually with `Animatable<Float>` + a single `Box` overlay shared by both routes. |
| R5 | Bundle size growth (~900KB fonts) | Subset Latin weights to drop unused styles. CJK Regular-only, subsetted to top ~4000 chars + extended digits. Budget documented in build files. |
| R6 | NIGHT scheme accessibility tuning is hard-won | NIGHT colors are NOT modified. Only inherits structural tokens. Compass test in Phase 0 verifies ≥ 4.5:1 contrast preserved. |
| R7 | Hero morph laggy on low-end devices | Target 60fps on `!ActivityManager.isLowRamDevice()`. If low-RAM, hero morph auto-degrades to reduce-motion crossfade. |

---

## 8. Verification

Each phase concludes with:

1. **Snapshot tests** — Compose Paparazzi-style tests for each new/evolved component in DAY/DARK/NIGHT, asserting rendered pixels match approved goldens.
2. **Reduce-motion tests** — every animated component has a snapshot/behavior test with `LocalReduceMotion = true` proving the fallback works.
3. **Real-device smoke** — manual smoke session on a real Pixel + a low-end test device. Battery Historian capture during a 30-min Sleeping run for phases that touch backdrop/sky.
4. **Accessibility audit** — TalkBack pass, all interactive elements have `contentDescription`, color contrast ≥ 4.5:1 for body and ≥ 3:1 for large text in every scheme.
5. **Before/after screenshots** — captured per screen, attached to phase PR description.

---

## 9. Out of Scope

Explicitly NOT decided in this master spec, deferred to per-phase brainstorming or future work:

- Exact per-screen layout pixel measurements (laid out per phase)
- Sleep CTA / greeting / button copy variants (no copywriting changes)
- New features (sleep schedule export, AI recommendations UI) — visual polish only
- Backend / persistence changes
- App icon / launch screen redesign (potential follow-up Phase 6)
- New audio presets or preset metadata changes (model gets only the `auraColors` field added)

---

## 10. Acceptance — Locked Decisions

| Decision | Value |
|---|---|
| DNA | Aurora / Cinematic Calm |
| Scope | All 6 screens + 3 hero moments, 6 sequential phases |
| Palette | Deep night + moonlit silver + dusk orange + starlight purple. DAY/DARK refreshed, NIGHT untouched. |
| Typography | DM Serif Display + Source Serif 4 + Noto Serif CJK SC fallback |
| Motion | Cinematic-breath rhythm, shared `SWSpring.gentle`, full reduce-motion support |
| Heroes | (1) Sleep CTA morph (2) Living night sky (3) Player audio↔color synchrony |
| Evolution strategy | Modify `core/visualkit/` in place. No v2 sidecar. Deleted components removed cleanly. |
