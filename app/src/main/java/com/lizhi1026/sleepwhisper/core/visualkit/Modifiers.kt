package com.lizhi1026.sleepwhisper.core.visualkit

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity

/**
 * Slow vertical floating animation — port of iOS `.floatingY(amplitude:duration:)` modifier
 * used on Home wake-window card and Sleeping countdown.
 *
 * Skipped when the system has [LocalReduceMotion] = true.
 */
fun Modifier.floatingY(
    amplitude: Dp = 3.dp,
    durationMillis: Int = 8000
): Modifier = composed {
    if (LocalReduceMotion.current) return@composed this
    val transition = rememberInfiniteTransition(label = "floating-y")
    val phase by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "phase"
    )
    val density = LocalDensity.current
    val offsetPx = with(density) { amplitude.toPx() } * phase
    this.graphicsLayer { translationY = offsetPx }
}
