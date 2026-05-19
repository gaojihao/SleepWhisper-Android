package com.lizhi1026.sleepwhisper.core.visualkit.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor
import com.lizhi1026.sleepwhisper.core.visualkit.SWFont
import com.lizhi1026.sleepwhisper.core.visualkit.SWGradient
import com.lizhi1026.sleepwhisper.core.visualkit.SWMotion
import com.lizhi1026.sleepwhisper.core.visualkit.SWRadius

data class SegmentedOption<T>(val value: T, val label: String)

/**
 * Generic segmented picker — port of iOS SWSegmentedPicker.swift.
 * Indicator slides horizontally on change with [SWMotion.screenInMs].
 */
@Composable
fun <T> SWSegmentedPicker(
    options: List<SegmentedOption<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    if (options.isEmpty()) return
    val scheme = LocalSWScheme.current
    val pill = RoundedCornerShape(SWRadius.pill)
    val selectedIndex = options.indexOfFirst { it.value == selected }.coerceAtLeast(0)

    BoxWithConstraints(
        modifier = modifier
            .height(44.dp)
            .fillMaxWidth()
            .clip(pill)
            .background(SWColor.surfaceSunken(scheme))
    ) {
        val density = LocalDensity.current
        val segmentWidth = maxWidth / options.size
        val targetOffset by animateDpAsState(
            targetValue = segmentWidth * selectedIndex,
            animationSpec = tween(durationMillis = SWMotion.screenInMs),
            label = "seg-offset"
        )

        Box(
            modifier = Modifier
                .offset(x = targetOffset)
                .height(44.dp)
                .padding(3.dp)
                .background(SWGradient.primary(scheme), pill)
                .clip(pill),
        ) {
            // indicator
            Box(modifier = Modifier.width(segmentWidth - 6.dp).height(38.dp))
        }
        Row(modifier = Modifier.fillMaxWidth().height(44.dp)) {
            options.forEachIndexed { i, opt ->
                val isSelected = i == selectedIndex
                Box(
                    modifier = Modifier
                        .width(segmentWidth)
                        .height(44.dp)
                        .clickable { onSelect(opt.value) },
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        text = opt.label,
                        style = SWFont.labelMD().copy(
                            color = if (isSelected) Color.White else SWColor.textSecondary(scheme),
                            textAlign = TextAlign.Center
                        )
                    )
                }
            }
        }
    }
}
