package com.lizhi1026.sleepwhisper.features.trends

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lizhi1026.sleepwhisper.R
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor
import com.lizhi1026.sleepwhisper.core.visualkit.SWFont
import com.lizhi1026.sleepwhisper.core.visualkit.SWSpacing
import com.lizhi1026.sleepwhisper.core.visualkit.floatingY
import com.lizhi1026.sleepwhisper.core.visualkit.components.AuroraBackdrop
import com.lizhi1026.sleepwhisper.core.visualkit.components.ElevationLevel
import com.lizhi1026.sleepwhisper.core.visualkit.components.EmptyStateCard
import com.lizhi1026.sleepwhisper.core.visualkit.components.GlassCard
import com.lizhi1026.sleepwhisper.core.visualkit.components.Hairline
import com.lizhi1026.sleepwhisper.core.visualkit.components.SectionLabel

@Composable
fun TrendsScreen(vm: TrendsViewModel = hiltViewModel()) {
    val scheme = LocalSWScheme.current
    val buckets by vm.weeklyBuckets.observeAsState(emptyList())
    val rec by vm.currentRecommendation.observeAsState(null)
    val todaySleeps by vm.todaySleeps.observeAsState(emptyList())
    val todayFeedings by vm.todayFeedings.observeAsState(emptyList())
    val recent by vm.recentEvents.observeAsState(emptyList())
    val hasAnyData by vm.hasAnyData.observeAsState(false)
    val baby by vm.app.baby.observeAsState(null)

    Box(modifier = Modifier.fillMaxSize()) {
        AuroraBackdrop()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SWSpacing.lg)
                .padding(top = SWSpacing.md),
            verticalArrangement = Arrangement.spacedBy(SWSpacing.lg)
        ) {
            Header(babyName = baby?.name)

            if (!hasAnyData) {
                EmptyStateCard(
                    titleRes = R.string.trends_empty_title,
                    subtitleRes = R.string.trends_empty_subtitle,
                    iconRes = R.drawable.ic_empty_moon
                )
            } else {
                TodayCard(todaySleeps, todayFeedings)
                if (recent.isNotEmpty()) {
                    RecentEventsList(events = recent, onLongPress = vm::beginEdit)
                }
                WeeklyCard(buckets)
            }

            if (hasAnyData && rec == null) {
                EmptyStateCard(
                    titleRes = R.string.trends_aiasleep_title,
                    subtitleRes = R.string.trends_aiasleep_subtitle
                )
            }
            Spacer(Modifier.height(SWSpacing.huge))
        }
    }
}

@Composable
private fun Header(babyName: String?) {
    val scheme = LocalSWScheme.current
    Column(verticalArrangement = Arrangement.spacedBy(SWSpacing.xs)) {
        BasicText(
            text = stringResource(R.string.trends_title),
            style = SWFont.serifItalic(28).copy(color = SWColor.textPrimary(scheme))
        )
        Hairline(modifier = Modifier.width(60.dp).padding(top = SWSpacing.xs))
        BasicText(
            text = stringResource(R.string.trends_subtitle, babyName ?: "").uppercase(),
            style = SWFont.labelMD().copy(
                color = SWColor.textSecondary(scheme),
                letterSpacing = 1.2.sp
            )
        )
    }
}

@Composable
private fun TodayCard(
    sleeps: List<com.lizhi1026.sleepwhisper.model.SleepSession>,
    feedings: List<com.lizhi1026.sleepwhisper.model.FeedingEvent>
) {
    val scheme = LocalSWScheme.current
    GlassCard(
        modifier = Modifier.fillMaxWidth().floatingY(amplitude = 2.dp, durationMillis = 9000),
        cornerRadius = 22.dp,
        contentPadding = PaddingValues(SWSpacing.lg),
        elevation = ElevationLevel.MEDIUM
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(SWSpacing.sm)) {
            SectionLabel(stringResource(R.string.trends_today))
            TodayTimeline(sleeps = sleeps, feedings = feedings)
        }
    }
}

@Composable
private fun WeeklyCard(buckets: List<Pair<String, Long>>) {
    val scheme = LocalSWScheme.current
    val totalSec = buckets.sumOf { it.second }
    val avgHours = totalSec.toDouble() / 3600.0 / 7.0
    val avgHoursFormatted = "%.1f".format(avgHours)
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        contentPadding = PaddingValues(SWSpacing.lg),
        elevation = ElevationLevel.MEDIUM
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(SWSpacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                SectionLabel(stringResource(R.string.trends_week_title))
                BasicText(
                    text = stringResource(R.string.trends_week_avg, avgHoursFormatted),
                    style = SWFont.titleMD().copy(color = SWColor.textPrimary(scheme))
                )
            }
            Hairline()
            WeeklyBars(buckets)
        }
    }
}

@Composable
private fun WeeklyBars(buckets: List<Pair<String, Long>>) {
    val scheme = LocalSWScheme.current
    val maxSec = buckets.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
    val today = buckets.lastOrNull()
    Row(
        modifier = Modifier.fillMaxWidth().height(200.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        buckets.forEach { (label, secs) ->
            val isToday = (label == today?.first)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(SWSpacing.xs)
            ) {
                Box(modifier = Modifier.height(180.dp), contentAlignment = Alignment.BottomCenter) {
                    val brush = chartBarBrush(isHighlighted = isToday)
                    Canvas(modifier = Modifier.width(22.dp).height(180.dp)) {
                        val h = (secs.toFloat() / maxSec) * size.height
                        val capsuleH = h.coerceAtLeast(8.dp.toPx())
                        drawRoundRect(
                            brush = brush,
                            topLeft = Offset(0f, size.height - capsuleH),
                            size = Size(size.width, capsuleH),
                            cornerRadius = CornerRadius(size.width / 2f)
                        )
                    }
                }
                BasicText(
                    text = label,
                    style = SWFont.labelSM().copy(color = SWColor.textSecondary(scheme))
                )
            }
        }
    }
}

/**
 * Vertical gradient brush for the WeeklyBars chart. Today's column uses a
 * dual-color aurora gradient that visually pops; non-today columns use accent
 * at 70% alpha for a quieter row that doesn't compete.
 */
@Composable
private fun chartBarBrush(isHighlighted: Boolean): Brush {
    val scheme = LocalSWScheme.current
    return if (isHighlighted) {
        Brush.verticalGradient(
            colors = listOf(
                SWColor.accent(scheme),
                SWColor.accentSecondary(scheme)
            )
        )
    } else {
        val base = SWColor.accent(scheme).copy(alpha = 0.7f)
        Brush.verticalGradient(colors = listOf(base, base))
    }
}
