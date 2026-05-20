package com.lizhi1026.sleepwhisper.core.visualkit.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lizhi1026.sleepwhisper.R
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor
import com.lizhi1026.sleepwhisper.core.visualkit.SWFont
import com.lizhi1026.sleepwhisper.core.visualkit.SWSpacing
import com.lizhi1026.sleepwhisper.core.visualkit.floatingY

/**
 * Empty-state card — port of iOS EmptyStateCard.swift.
 * 64dp accent-glow circle + 28dp icon + title + subtitle, all centered.
 * Icon floats with [floatingY] (amplitude 4dp, 5s) for a subtle "alive" feel.
 */
@Composable
fun EmptyStateCard(
    titleRes: Int,
    subtitleRes: Int,
    modifier: Modifier = Modifier,
    @DrawableRes iconRes: Int = R.drawable.ic_empty_sparkles
) {
    val scheme = LocalSWScheme.current
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        contentPadding = PaddingValues(SWSpacing.xl)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SWSpacing.sm)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(SWColor.accent(scheme).copy(alpha = 0.18f))
                    .floatingY(amplitude = 4.dp, durationMillis = 5000),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(SWColor.accent(scheme)),
                    modifier = Modifier.size(28.dp)
                )
            }
            BasicText(
                text = stringResource(titleRes),
                style = SWFont.titleMD().copy(
                    color = SWColor.textPrimary(scheme),
                    textAlign = TextAlign.Center
                )
            )
            BasicText(
                text = stringResource(subtitleRes),
                style = SWFont.bodyMD().copy(
                    color = SWColor.textSecondary(scheme),
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}
