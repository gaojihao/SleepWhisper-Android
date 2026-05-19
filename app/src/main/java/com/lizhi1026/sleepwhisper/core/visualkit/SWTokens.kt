package com.lizhi1026.sleepwhisper.core.visualkit

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

/** Motion / duration tokens — direct port of iOS SWMotion. Durations are in milliseconds. */
object SWMotion {
    const val pressInMs = 100
    const val pressOutMs = 150
    const val screenInMs = 250
    const val screenOutMs = 200
    const val toastMs = 200
    const val bannerMs = 200
    const val numberSwitchMs = 100
    const val longPressMs = 600L
    const val fadeInMs = 1500L
    const val defaultFadeOutMs = 5000L
}
