package com.lizhi1026.sleepwhisper.features.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.lizhi1026.sleepwhisper.R
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor
import com.lizhi1026.sleepwhisper.core.visualkit.SWFont
import com.lizhi1026.sleepwhisper.core.visualkit.SWSpacing
import com.lizhi1026.sleepwhisper.core.visualkit.components.GlassCard
import com.lizhi1026.sleepwhisper.core.visualkit.components.SoftButton
import com.lizhi1026.sleepwhisper.core.visualkit.components.SoftButtonStyle

private data class Hint(val titleRes: Int, val bodyRes: Int)

private val HINTS = listOf(
    Hint(R.string.hint_startsleep_title, R.string.hint_startsleep_body),
    Hint(R.string.hint_window_title, R.string.hint_window_body),
    Hint(R.string.hint_ai_title, R.string.hint_ai_body)
)

/**
 * Inline 3-page hints card shown on Home — port of iOS OnboardingHintsCard.swift.
 * User taps Next to advance pages, "I'm ready" on the final page to dismiss.
 */
@Composable
fun OnboardingHintsCard(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalSWScheme.current
    var page by remember { mutableIntStateOf(0) }
    val hint = HINTS[page]
    val isLast = page == HINTS.lastIndex

    GlassCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(SWSpacing.sm)) {
            BasicText(
                text = stringResource(hint.titleRes),
                style = SWFont.titleMD().copy(color = SWColor.textPrimary(scheme))
            )
            BasicText(
                text = stringResource(hint.bodyRes),
                style = SWFont.bodyMD().copy(color = SWColor.textSecondary(scheme))
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicText(
                    text = "${page + 1} / ${HINTS.size}",
                    style = SWFont.labelMD().copy(color = SWColor.textTertiary(scheme))
                )
                Box(modifier = Modifier.weight(1f))
                SoftButton(
                    text = if (isLast) stringResource(R.string.common_imready)
                           else stringResource(R.string.common_next),
                    onClick = { if (isLast) onDismiss() else page += 1 },
                    style = if (isLast) SoftButtonStyle.PRIMARY else SoftButtonStyle.GHOST
                )
            }
        }
    }
}
