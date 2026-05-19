package com.lizhi1026.sleepwhisper.features.trends

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor
import com.lizhi1026.sleepwhisper.core.visualkit.SWFont
import com.lizhi1026.sleepwhisper.model.FeedingEvent
import com.lizhi1026.sleepwhisper.model.SleepSession
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * 24-hour horizontal timeline for today — port of iOS TodayTimelineCard.
 * Renders a track + sleep capsules (filled) + feeding dots (small circles).
 * Long sleeps that started before today are clipped to 00:00.
 */
@Composable
fun TodayTimeline(
    sleeps: List<SleepSession>,
    feedings: List<FeedingEvent>,
    modifier: Modifier = Modifier
) {
    val scheme = LocalSWScheme.current
    val todayStartMs = remember24hStart()
    val dayMs = 24 * 3600_000L
    val now = System.currentTimeMillis()

    Box(modifier = modifier.fillMaxWidth()) {
        Canvas(modifier = Modifier.fillMaxWidth().height(64.dp)) {
            // Track background
            val trackHeight = size.height * 0.6f
            val trackY = (size.height - trackHeight) / 2f
            drawRoundRect(
                color = SWColor.surfaceSunken(scheme),
                topLeft = Offset(0f, trackY),
                size = Size(size.width, trackHeight),
                cornerRadius = CornerRadius(trackHeight / 2f)
            )

            // Sleep capsules
            sleeps.forEach { s ->
                val rawStart = s.startAt.coerceAtLeast(todayStartMs)
                val rawEnd = (s.endAt ?: now).coerceAtMost(todayStartMs + dayMs)
                if (rawEnd <= rawStart) return@forEach
                val xStart = (rawStart - todayStartMs).toFloat() / dayMs * size.width
                val xEnd = (rawEnd - todayStartMs).toFloat() / dayMs * size.width
                drawRoundRect(
                    color = SWColor.primary(scheme).copy(alpha = 0.85f),
                    topLeft = Offset(xStart, trackY),
                    size = Size((xEnd - xStart).coerceAtLeast(2.dp.toPx()), trackHeight),
                    cornerRadius = CornerRadius(trackHeight / 2f)
                )
            }

            // Feeding dots
            feedings.forEach { f ->
                val x = (f.startedAt - todayStartMs).toFloat() / dayMs * size.width
                drawCircle(
                    color = SWColor.accent(scheme),
                    radius = 4.dp.toPx(),
                    center = Offset(x, size.height / 2f)
                )
            }
        }
        HourLegend(modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun HourLegend(modifier: Modifier = Modifier) {
    val scheme = LocalSWScheme.current
    Row(modifier = modifier.padding(top = 70.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
        listOf("0", "6", "12", "18", "24").forEach { h ->
            BasicText(
                text = h,
                style = SWFont.labelSM().copy(
                    color = SWColor.textTertiary(scheme),
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}

@Composable
private fun remember24hStart(): Long = remember {
    ZonedDateTime.now(ZoneId.systemDefault())
        .with(LocalTime.MIDNIGHT)
        .toInstant().toEpochMilli()
}

@Suppress("unused")
private fun nowFormatted(ms: Long): String =
    java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(ms))

@Suppress("unused")
private fun instantHour(ms: Long): Int =
    Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).hour
