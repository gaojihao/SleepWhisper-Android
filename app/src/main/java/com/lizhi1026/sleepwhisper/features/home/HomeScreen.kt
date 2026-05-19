package com.lizhi1026.sleepwhisper.features.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lizhi1026.sleepwhisper.core.audio.PlayerState
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor
import com.lizhi1026.sleepwhisper.core.visualkit.SWFont
import com.lizhi1026.sleepwhisper.core.visualkit.SWSpacing
import com.lizhi1026.sleepwhisper.core.visualkit.components.AudioWaveform
import com.lizhi1026.sleepwhisper.core.visualkit.components.BreathingBackground
import com.lizhi1026.sleepwhisper.core.visualkit.components.GlassCard
import com.lizhi1026.sleepwhisper.core.visualkit.components.PulseRing
import com.lizhi1026.sleepwhisper.core.visualkit.components.SoftButton
import com.lizhi1026.sleepwhisper.core.visualkit.components.SoftButtonStyle
import com.lizhi1026.sleepwhisper.model.DiaperEvent
import com.lizhi1026.sleepwhisper.model.FeedingEvent.FeedingMethod

@Composable
fun HomeScreen(
    onOpenPlayer: () -> Unit,
    vm: HomeViewModel = hiltViewModel()
) {
    val scheme = LocalSWScheme.current
    val baby by vm.baby.observeAsState(null)
    val wakeWindow by vm.cachedWakeWindow.observeAsState(null)
    val playerState by vm.playerState.observeAsState(PlayerState.Idle)
    val currentPreset by vm.currentPreset.observeAsState(null)

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
                text = "Hi, ${baby?.name ?: "friend"}",
                style = SWFont.titleXL().copy(color = SWColor.textPrimary(scheme))
            )

            wakeWindow?.let { (remaining, total) ->
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        BasicText(
                            text = "Wake window",
                            style = SWFont.labelMD().copy(color = SWColor.textSecondary(scheme))
                        )
                        BasicText(
                            text = "$remaining / $total min",
                            style = SWFont.displayMD().copy(color = SWColor.textPrimary(scheme))
                        )
                    }
                }
            }

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.padding(end = SWSpacing.md)) {
                        BasicText(
                            text = currentPreset?.id ?: "Tap to play",
                            style = SWFont.titleMD().copy(color = SWColor.textPrimary(scheme))
                        )
                        BasicText(
                            text = if (playerState is PlayerState.Playing) "Playing" else "—",
                            style = SWFont.labelMD().copy(color = SWColor.textSecondary(scheme))
                        )
                    }
                    AudioWaveform(
                        modifier = Modifier.weight(1f),
                        isPlaying = playerState is PlayerState.Playing,
                        color = SWColor.accent(scheme)
                    )
                }
            }
            SoftButton(text = "Open player", onClick = onOpenPlayer, style = SoftButtonStyle.GHOST)

            QuickActionsRow(vm)

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                PulseRing(color = SWColor.accent(scheme), radius = 80.dp)
                SoftButton(
                    text = "Sleep",
                    onClick = vm::onTapSleep,
                    style = SoftButtonStyle.ACCENT
                )
            }
        }
    }
}

@Composable
private fun QuickActionsRow(vm: HomeViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(SWSpacing.sm)
    ) {
        QuickAction("Breast L") { vm.onRecordFeeding(FeedingMethod.BREAST_LEFT) }
        QuickAction("Breast R") { vm.onRecordFeeding(FeedingMethod.BREAST_RIGHT) }
        QuickAction("Bottle") { vm.onRecordFeeding(FeedingMethod.BOTTLE, ml = 120) }
        QuickAction("Diaper") { vm.onRecordDiaper(DiaperEvent.DiaperType.WET) }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.QuickAction(label: String, onTap: () -> Unit) {
    SoftButton(
        text = label,
        onClick = onTap,
        style = SoftButtonStyle.GHOST,
        modifier = Modifier.weight(1f)
    )
}
