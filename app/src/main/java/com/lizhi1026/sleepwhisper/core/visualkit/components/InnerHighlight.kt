package com.lizhi1026.sleepwhisper.core.visualkit.components

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWScheme

/**
 * Draws a 1dp gradient stroke at the top of any elevated surface — the
 * "Liquid Glass" cue. White → transparent on dark schemes; on DAY it falls
 * back to a near-invisible warm-paper accent so the same call works
 * cross-scheme without conditionals at the call site.
 */
fun Modifier.innerHighlight(cornerRadius: Dp = 16.dp): Modifier = composed {
    val scheme = LocalSWScheme.current
    val brush = when (scheme) {
        SWScheme.DAY -> Brush.verticalGradient(
            0f to Color.White.copy(alpha = 0.45f),
            0.45f to Color.Transparent
        )
        SWScheme.DARK -> Brush.verticalGradient(
            0f to Color.White.copy(alpha = 0.18f),
            0.45f to Color.Transparent
        )
        SWScheme.NIGHT -> Brush.verticalGradient(
            0f to Color(1.00f, 0.75f, 0.75f).copy(alpha = 0.10f),
            0.45f to Color.Transparent
        )
    }
    border(1.dp, brush, RoundedCornerShape(cornerRadius))
}
