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
