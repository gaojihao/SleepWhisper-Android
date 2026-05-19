package com.lizhi1026.sleepwhisper.features.trends

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor
import com.lizhi1026.sleepwhisper.core.visualkit.SWFont
import com.lizhi1026.sleepwhisper.core.visualkit.SWSpacing
import com.lizhi1026.sleepwhisper.core.visualkit.components.BreathingBackground
import com.lizhi1026.sleepwhisper.core.visualkit.components.GlassCard

@Composable
fun TrendsScreen(vm: TrendsViewModel = hiltViewModel()) {
    val scheme = LocalSWScheme.current
    val buckets by vm.weeklyBuckets.observeAsState(emptyList())
    val rec by vm.currentRecommendation.observeAsState(null)

    Box(modifier = Modifier.fillMaxSize()) {
        BreathingBackground()
        Column(modifier = Modifier.fillMaxSize().padding(SWSpacing.lg), verticalArrangement = Arrangement.spacedBy(SWSpacing.lg)) {
            BasicText("Trends", style = SWFont.titleXL().copy(color = SWColor.textPrimary(scheme)))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    BasicText(
                        "Last 7 days · total sleep",
                        style = SWFont.titleMD().copy(color = SWColor.textPrimary(scheme))
                    )
                    if (buckets.isEmpty()) {
                        BasicText(
                            "No data yet — log a sleep session to see trends.",
                            style = SWFont.bodyMD().copy(color = SWColor.textSecondary(scheme))
                        )
                    } else {
                        WeeklyBars(buckets)
                    }
                }
            }
            rec?.let {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    BasicText(
                        "Next window: ${it.wakeWindowMinutes} min (confidence ${(it.confidence * 100).toInt()}%)",
                        style = SWFont.bodyMD().copy(color = SWColor.textPrimary(scheme))
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklyBars(buckets: List<Pair<String, Long>>) {
    val scheme = LocalSWScheme.current
    val maxSec = buckets.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
    Row(
        modifier = Modifier.fillMaxWidth().height(140.dp),
        horizontalArrangement = Arrangement.spacedBy(SWSpacing.xs)
    ) {
        buckets.forEach { (label, secs) ->
            Column(modifier = Modifier.weight(1f).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(SWSpacing.xs)) {
                Canvas(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    val barH = (secs.toFloat() / maxSec) * size.height
                    drawRoundRect(
                        color = SWColor.accent(scheme),
                        topLeft = Offset(0f, size.height - barH),
                        size = Size(size.width, barH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
                    )
                }
                BasicText(
                    label,
                    style = SWFont.labelSM().copy(color = SWColor.textSecondary(scheme))
                )
            }
        }
    }
}
