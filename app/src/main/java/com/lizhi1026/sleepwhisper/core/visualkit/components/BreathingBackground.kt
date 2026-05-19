package com.lizhi1026.sleepwhisper.core.visualkit.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor
import com.lizhi1026.sleepwhisper.core.visualkit.SWGradient
import com.lizhi1026.sleepwhisper.core.visualkit.SWScheme

/**
 * Always-present full-screen surface — port of iOS BreathingBackground.swift.
 * DAY: static surface gradient; DARK/NIGHT: surface + slow radial glow that breathes
 * at a 10-second period (0..0.18 alpha).
 */
@Composable
fun BreathingBackground(modifier: Modifier = Modifier) {
    val scheme = LocalSWScheme.current
    Box(modifier = modifier.fillMaxSize().background(SWGradient.surface(scheme))) {
        if (scheme != SWScheme.DAY) {
            val t = rememberInfiniteTransition(label = "breath")
            val phase by t.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 10_000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "breath-phase"
            )
            val glow = SWColor.accent(scheme)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.18f * phase)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(glow, glow.copy(alpha = 0f))
                        )
                    )
            )
        }
    }
}
