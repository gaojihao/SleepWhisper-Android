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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lizhi1026.sleepwhisper.R
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor
import com.lizhi1026.sleepwhisper.core.visualkit.SWFont
import com.lizhi1026.sleepwhisper.core.visualkit.SWSpacing
import com.lizhi1026.sleepwhisper.core.visualkit.components.BreathingBackground
import com.lizhi1026.sleepwhisper.core.visualkit.components.EmptyStateCard
import com.lizhi1026.sleepwhisper.core.visualkit.components.GlassCard

@Composable
fun TrendsScreen(vm: TrendsViewModel = hiltViewModel()) {
    val scheme = LocalSWScheme.current
    val buckets by vm.weeklyBuckets.observeAsState(emptyList())
    val rec by vm.currentRecommendation.observeAsState(null)
    val todaySleeps by vm.todaySleeps.observeAsState(emptyList())
    val todayFeedings by vm.todayFeedings.observeAsState(emptyList())
    val recent by vm.recentEvents.observeAsState(emptyList())
    val hasAnyData by vm.hasAnyData.observeAsState(false)

    Box(modifier = Modifier.fillMaxSize()) {
        BreathingBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(SWSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(SWSpacing.lg)
        ) {
            BasicText(
                stringResource(R.string.trends_title),
                style = SWFont.titleXL().copy(color = SWColor.textPrimary(scheme))
            )

            if (!hasAnyData) {
                EmptyStateCard(
                    titleRes = R.string.trends_empty_title,
                    subtitleRes = R.string.trends_empty_subtitle
                )
            } else {
                // Today 24-hour timeline
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        BasicText(
                            stringResource(R.string.trends_today),
                            style = SWFont.titleMD().copy(color = SWColor.textPrimary(scheme))
                        )
                        TodayTimeline(
                            sleeps = todaySleeps,
                            feedings = todayFeedings,
                            modifier = Modifier.padding(top = SWSpacing.sm)
                        )
                    }
                }

                // Recent events with long-press to edit
                RecentEventsList(
                    events = recent,
                    onLongPress = vm::beginEdit
                )

                // Weekly bar chart
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        BasicText(
                            stringResource(R.string.trends_week_title),
                            style = SWFont.titleMD().copy(color = SWColor.textPrimary(scheme))
                        )
                        val avgHours = buckets.sumOf { it.second }.toDouble() / 3600.0 / 7.0
                        BasicText(
                            stringResource(R.string.trends_week_avg, avgHours),
                            style = SWFont.labelMD().copy(color = SWColor.textSecondary(scheme))
                        )
                        WeeklyBars(buckets)
                    }
                }
            }

            if (hasAnyData && rec == null) {
                EmptyStateCard(
                    titleRes = R.string.trends_aiasleep_title,
                    subtitleRes = R.string.trends_aiasleep_subtitle
                )
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
