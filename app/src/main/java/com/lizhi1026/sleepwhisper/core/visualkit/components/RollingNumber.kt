package com.lizhi1026.sleepwhisper.core.visualkit.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import com.lizhi1026.sleepwhisper.core.visualkit.SWFont
import com.lizhi1026.sleepwhisper.core.visualkit.SWMotion

/**
 * Number that animates a vertical slide + fade when its value changes.
 * Port of iOS RollingNumber.swift (whole-number transition, not per-digit).
 */
@Composable
fun RollingNumber(
    value: Int,
    modifier: Modifier = Modifier,
    style: TextStyle = SWFont.displayLG()
) {
    AnimatedContent(
        targetState = value,
        transitionSpec = {
            val dur = SWMotion.fadeInMs.toInt() / 5  // ~300ms feels right for digit changes
            val enter = slideInVertically(animationSpec = tween(dur)) { fullHeight -> fullHeight } + fadeIn(tween(dur))
            val exit = slideOutVertically(animationSpec = tween(dur)) { fullHeight -> -fullHeight } + fadeOut(tween(dur))
            enter togetherWith exit
        },
        modifier = modifier,
        label = "rolling-number"
    ) { v ->
        BasicText(text = v.toString(), style = style)
    }
}
