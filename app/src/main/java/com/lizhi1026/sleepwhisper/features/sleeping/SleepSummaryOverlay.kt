package com.lizhi1026.sleepwhisper.features.sleeping

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.lizhi1026.sleepwhisper.R
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor
import com.lizhi1026.sleepwhisper.core.visualkit.SWFont
import com.lizhi1026.sleepwhisper.core.visualkit.SWSpacing
import com.lizhi1026.sleepwhisper.core.visualkit.components.ElevationLevel
import com.lizhi1026.sleepwhisper.core.visualkit.components.GlassCard
import com.lizhi1026.sleepwhisper.core.visualkit.components.SoftButton
import com.lizhi1026.sleepwhisper.model.SleepSummary

/**
 * Overlay card shown after a sleep session ends. Mirrors iOS SleepSummaryCard.swift —
 * displays just-finished duration plus a comparison sentence vs. yesterday.
 */
@Composable
fun SleepSummaryOverlay(
    summary: SleepSummary,
    babyName: String?,
    onDismiss: () -> Unit
) {
    val scheme = LocalSWScheme.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        GlassCard(
            modifier = Modifier.fillMaxWidth().padding(SWSpacing.xl),
            elevation = ElevationLevel.STRONG
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(SWSpacing.md)) {
                BasicText(
                    text = stringResource(R.string.summary_babyjust, babyName ?: ""),
                    style = SWFont.titleMD().copy(color = SWColor.textSecondary(scheme))
                )
                BasicText(
                    text = formatDurationDisplay(summary.justFinishedDurationSec),
                    style = SWFont.displayMD().copy(color = SWColor.primary(scheme))
                )
                BasicText(
                    text = comparisonSentence(summary),
                    style = SWFont.bodyMD().copy(color = SWColor.textPrimary(scheme))
                )
                SoftButton(
                    text = stringResource(R.string.common_ok),
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun formatDurationDisplay(seconds: Long): String {
    val h = (seconds / 3600).toInt()
    val m = ((seconds % 3600) / 60).toInt()
    return if (h > 0) stringResource(R.string.summary_duration_hm, h, m)
    else stringResource(R.string.summary_duration_m, m)
}

@Composable
private fun comparisonSentence(s: SleepSummary): String {
    val todayTotal = formatDurationDisplay(s.todayTotalSec)
    if (!s.hasYesterdayData) return stringResource(R.string.summary_todayonly, todayTotal)
    val absMin = kotlin.math.abs(s.diffVsYesterdayMin)
    return when {
        absMin <= 5 -> stringResource(R.string.summary_todaysame, todayTotal)
        s.trendIsPositive -> stringResource(R.string.summary_todaymore, todayTotal, absMin)
        else -> stringResource(R.string.summary_todayless, todayTotal, absMin)
    }
}
