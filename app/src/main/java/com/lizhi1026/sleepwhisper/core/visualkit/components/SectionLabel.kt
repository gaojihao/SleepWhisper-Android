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
