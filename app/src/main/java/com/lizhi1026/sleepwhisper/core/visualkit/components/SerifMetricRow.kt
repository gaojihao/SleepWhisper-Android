package com.lizhi1026.sleepwhisper.core.visualkit.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor
import com.lizhi1026.sleepwhisper.core.visualkit.SWFont
import com.lizhi1026.sleepwhisper.core.visualkit.SWSpacing

/**
 * Horizontal "label · value" row used by Trends and Settings. Label is the
 * bodyLG style; value is bodyLG serif in secondary color. Optional chevron
 * affordance via [ChevronTrail].
 */
@Composable
fun SerifMetricRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    showChevron: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val scheme = LocalSWScheme.current
    val rowMod = if (onClick != null) modifier.fillMaxWidth().clickable(onClick = onClick)
                 else modifier.fillMaxWidth()
    Row(
        modifier = rowMod.padding(vertical = SWSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        BasicText(
            text = label,
            style = SWFont.bodyLG().copy(color = SWColor.textPrimary(scheme))
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SWSpacing.xs)
        ) {
            BasicText(
                text = value,
                style = SWFont.bodyLG().copy(color = SWColor.textSecondary(scheme))
            )
            if (showChevron) ChevronTrail()
        }
    }
}
