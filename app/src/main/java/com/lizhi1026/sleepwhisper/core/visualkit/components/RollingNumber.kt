package com.lizhi1026.sleepwhisper.core.visualkit.components

import androidx.compose.animation.AnimatedContent
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
import com.lizhi1026.sleepwhisper.core.visualkit.SWFont
import com.lizhi1026.sleepwhisper.core.visualkit.SWMotion

/**
 * Rolling number — each digit rolls vertically when its value changes. Callers
 * pass a serif tabular style (e.g. `SWFont.displayLGTabular()`) so digit-width
 * jitter doesn't shift the surrounding text.
 *
 * Aurora upgrade: per-digit animation (not whole-number) with directional roll
 * (rising digits slide in from below, falling digits from above). On reduce-
 * motion, digits crossfade instead of rolling.
 */
@Composable
fun RollingNumber(
    value: Int,
    modifier: Modifier = Modifier,
    style: TextStyle = SWFont.displayLG()
) {
    val reduce = LocalReduceMotion.current
    val digits = value.toString()

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
