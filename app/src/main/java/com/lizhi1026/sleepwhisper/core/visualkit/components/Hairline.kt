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
