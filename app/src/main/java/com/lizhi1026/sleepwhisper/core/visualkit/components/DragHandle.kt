package com.lizhi1026.sleepwhisper.core.visualkit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor
import com.lizhi1026.sleepwhisper.core.visualkit.SWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWSpacing

/**
 * Pill drag handle at the top of bottom sheets — matches iOS 36×4 capsule.
 * Auto-tints for current scheme (white at 0.2 alpha in dark, text-tertiary on light).
 */
@Composable
fun DragHandle(modifier: Modifier = Modifier) {
    val scheme = LocalSWScheme.current
    val color = if (scheme == SWScheme.DAY)
        SWColor.textTertiary(scheme).copy(alpha = 0.5f)
    else
        Color.White.copy(alpha = 0.2f)
    Box(
        modifier = modifier
            .padding(top = 8.dp)
            .size(width = 36.dp, height = 4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(color)
    )
}
