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
