package com.lizhi1026.sleepwhisper.features.trends

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import com.lizhi1026.sleepwhisper.core.strings.displayKey
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor
import com.lizhi1026.sleepwhisper.core.visualkit.SWFont
import com.lizhi1026.sleepwhisper.core.visualkit.SWSpacing
import com.lizhi1026.sleepwhisper.core.visualkit.components.GlassCard
import com.lizhi1026.sleepwhisper.model.FeedingEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Recent events list (24h window, capped 8) — port of iOS recentEventsList.
 * Long-press on any row triggers [onLongPress] which the caller wires to
 * AppStateContainer.requestEdit().
 */
@Composable
fun RecentEventsList(
    events: List<RecentEvent>,
    onLongPress: (RecentEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalSWScheme.current
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    GlassCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(SWSpacing.sm)) {
            BasicText(
                text = stringResource(com.lizhi1026.sleepwhisper.R.string.trends_recentevents_title),
                style = SWFont.titleMD().copy(color = SWColor.textPrimary(scheme))
            )
            BasicText(
                text = stringResource(com.lizhi1026.sleepwhisper.R.string.trends_recentevents_hint),
                style = SWFont.labelSM().copy(color = SWColor.textTertiary(scheme))
            )
            if (events.isEmpty()) {
                BasicText(
                    text = "—",
                    style = SWFont.bodyMD().copy(color = SWColor.textSecondary(scheme))
                )
            } else {
                events.forEach { item ->
                    EventRow(item, timeFormat, onLongPress)
                }
            }
        }
    }
}

@Composable
private fun EventRow(
    event: RecentEvent,
    timeFormat: SimpleDateFormat,
    onLongPress: (RecentEvent) -> Unit
) {
    val scheme = LocalSWScheme.current
    val (label, detail) = when (event) {
        is RecentEvent.OfFeeding -> stringResource(event.event.method.displayKey()) to feedingDetail(event.event)
        is RecentEvent.OfDiaper -> stringResource(event.event.type.displayKey()) to "—"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(event) {
                detectTapGestures(onLongPress = { onLongPress(event) })
            }
            .padding(vertical = SWSpacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                text = label,
                style = SWFont.bodyMD().copy(color = SWColor.textPrimary(scheme))
            )
            BasicText(
                text = detail,
                style = SWFont.labelSM().copy(color = SWColor.textSecondary(scheme))
            )
        }
        BasicText(
            text = timeFormat.format(Date(event.timestamp)),
            style = SWFont.labelMD().copy(color = SWColor.textSecondary(scheme))
        )
    }
}

@Composable
private fun feedingDetail(f: FeedingEvent): String {
    val ml = f.amountMl ?: return "—"
    return "$ml ml"
}
