package com.lizhi1026.sleepwhisper.core.visualkit.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor
import com.lizhi1026.sleepwhisper.core.visualkit.SWFont
import com.lizhi1026.sleepwhisper.core.visualkit.SWSpacing

/**
 * Empty-state card — port of iOS EmptyStateCard.swift.
 * Title + subtitle text only (no icon yet; substitute drawable when available).
 */
@Composable
fun EmptyStateCard(
    titleRes: Int,
    subtitleRes: Int,
    modifier: Modifier = Modifier
) {
    val scheme = LocalSWScheme.current
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(SWSpacing.xl)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SWSpacing.sm)
        ) {
            BasicText(
                text = stringResource(titleRes),
                style = SWFont.titleLG().copy(
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
