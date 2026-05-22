package com.lizhi1026.sleepwhisper.features.sleeping

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lizhi1026.sleepwhisper.R
import com.lizhi1026.sleepwhisper.core.visualkit.LocalReduceMotion
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor
import com.lizhi1026.sleepwhisper.core.visualkit.SWFont
import com.lizhi1026.sleepwhisper.core.visualkit.SWSpacing
import com.lizhi1026.sleepwhisper.core.visualkit.components.ElevationLevel
import com.lizhi1026.sleepwhisper.core.visualkit.components.GlassCard
import com.lizhi1026.sleepwhisper.core.visualkit.components.SoftButton
import com.lizhi1026.sleepwhisper.model.SleepSummary

/**
 * Overlay card shown after a sleep session ends — port of iOS SleepSummaryCard.swift.
 * Spring-pops in (scale 0.85 → 1, opacity 0 → 1) and bounces the sparkles icon.
 */
@Composable
fun SleepSummaryOverlay(
    summary: SleepSummary,
    babyName: String?,
    onDismiss: () -> Unit
) {
    val scheme = LocalSWScheme.current
    val reduceMotion = LocalReduceMotion.current
    val appear = remember { Animatable(if (reduceMotion) 1f else 0f) }
    val iconBounce = remember { Animatable(if (reduceMotion) 1f else 0.6f) }

    LaunchedEffect(Unit) {
        if (!reduceMotion) {
            appear.animateTo(1f, spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow))
        }
    }
    LaunchedEffect(Unit) {
        if (!reduceMotion) {
            iconBounce.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessLow))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f * appear.value))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SWSpacing.xl)
                .scale(0.85f + 0.15f * appear.value),
            elevation = ElevationLevel.STRONG
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(SWSpacing.sm)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(SWColor.accent(scheme).copy(alpha = 0.18f))
                        .scale(iconBounce.value),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(id = R.drawable.ic_empty_sparkles),
                        contentDescription = null,
                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(SWColor.accent(scheme)),
                        modifier = Modifier.size(24.dp)
                    )
                }
                BasicText(
                    text = stringResource(R.string.summary_babyjust, babyName ?: ""),
                    style = SWFont.bodyMD().copy(
                        color = SWColor.textSecondary(scheme),
                        textAlign = TextAlign.Center
                    )
                )
                BasicText(
                    text = formatDurationDisplay(summary.justFinishedDurationSec),
                    style = SWFont.displayMD().copy(color = SWColor.primary(scheme))
                )
                BasicText(
                    text = comparisonSentence(summary),
                    style = SWFont.bodyMD().copy(
                        color = SWColor.textPrimary(scheme),
                        textAlign = TextAlign.Center
                    )
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
