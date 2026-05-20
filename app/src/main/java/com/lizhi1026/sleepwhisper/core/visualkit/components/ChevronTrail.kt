package com.lizhi1026.sleepwhisper.core.visualkit.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor

/**
 * Subtle 3-dot trail used in place of `ic_chevron_right` for list rows and
 * settings rows. Reads as forward-affordance without a heavy arrow shape.
 */
@Composable
fun ChevronTrail(modifier: Modifier = Modifier, tint: Color = Color.Unspecified) {
    val scheme = LocalSWScheme.current
    val color = if (tint == Color.Unspecified) SWColor.textTertiary(scheme) else tint
    Canvas(modifier = modifier.size(width = 18.dp, height = 6.dp)) {
        val r = size.height / 3f
        val gap = (size.width - r * 6f) / 2f
        for (i in 0 until 3) {
            drawCircle(
                color = color.copy(alpha = 0.4f + 0.2f * i),
                radius = r,
                center = Offset(r + i * (r * 2 + gap), size.height / 2)
            )
        }
    }
}
