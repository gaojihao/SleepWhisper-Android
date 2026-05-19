package com.lizhi1026.sleepwhisper.core.visualkit.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/**
 * N-bar audio waveform — port of iOS AudioWaveform.swift.
 * Bars sin-oscillate between 30% and 90% height while [isPlaying] is true, else collapse to 15%.
 */
@Composable
fun AudioWaveform(
    modifier: Modifier = Modifier,
    isPlaying: Boolean,
    color: Color,
    barCount: Int = 14
) {
    val phases = remember(barCount) { List(barCount) { Random.nextFloat() * 2f } }
    val periods = remember(barCount) { List(barCount) { 700 + Random.nextInt(400) } }
    val animatables = remember(barCount) { List(barCount) { Animatable(0.15f) } }

    LaunchedEffect(isPlaying) {
        if (!isPlaying) {
            animatables.forEach { a -> launch { a.animateTo(0.15f, tween(400)) } }
            return@LaunchedEffect
        }
        val startTime = System.currentTimeMillis()
        while (true) {
            val now = System.currentTimeMillis() - startTime
            animatables.forEachIndexed { i, anim ->
                val s = sin(now.toFloat() / periods[i] * Math.PI.toFloat() * 2 + phases[i])
                val target = 0.3f + 0.6f * ((s + 1f) / 2f)
                launch { anim.snapTo(target) }
            }
            delay(50)
        }
    }

    Canvas(modifier = modifier.fillMaxWidth().height(36.dp)) {
        val gap = 2.dp.toPx()
        val totalGap = gap * (barCount - 1)
        val barWidth = (size.width - totalGap) / barCount
        animatables.forEachIndexed { i, anim ->
            val h = abs(anim.value) * size.height
            val x = i * (barWidth + gap)
            val y = (size.height - h) / 2f
            drawRoundRect(
                color = color,
                topLeft = Offset(x, y),
                size = Size(barWidth, h),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
        }
    }
}
