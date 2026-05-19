package com.lizhi1026.sleepwhisper.features.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lizhi1026.sleepwhisper.core.audio.PlayerState
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor
import com.lizhi1026.sleepwhisper.core.visualkit.SWFont
import com.lizhi1026.sleepwhisper.core.visualkit.SWSpacing
import com.lizhi1026.sleepwhisper.core.visualkit.components.BreathingBackground
import com.lizhi1026.sleepwhisper.core.visualkit.components.GlassCard
import com.lizhi1026.sleepwhisper.core.visualkit.components.SWSegmentedPicker
import com.lizhi1026.sleepwhisper.core.visualkit.components.SegmentedOption
import com.lizhi1026.sleepwhisper.model.AudioPreset

@Composable
fun PlayerScreen(vm: PlayerViewModel = hiltViewModel()) {
    val scheme = LocalSWScheme.current
    val baby by vm.baby.observeAsState(null)
    val settings by vm.settings.observeAsState(null)
    val playerState by vm.playerState.observeAsState(PlayerState.Idle)
    val currentId = (playerState as? PlayerState.Playing)?.presetId

    val minutesNow = settings?.defaultTimerMinutes ?: 30

    val ageMonths = remember(baby) { baby?.ageInMonths() ?: 0 }
    val rec = vm.recommended(ageMonths)
    val rest = vm.allPresets - rec.toSet()

    Box(modifier = Modifier.fillMaxSize()) {
        BreathingBackground()
        Column(modifier = Modifier.fillMaxSize().padding(SWSpacing.lg), verticalArrangement = Arrangement.spacedBy(SWSpacing.lg)) {
            BasicText("Player", style = SWFont.titleXL().copy(color = SWColor.textPrimary(scheme)))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    BasicText("Duration", style = SWFont.labelMD().copy(color = SWColor.textSecondary(scheme)))
                    SWSegmentedPicker(
                        options = listOf(15, 30, 45, 60, 90).map { SegmentedOption(it, "$it min") },
                        selected = minutesNow,
                        onSelect = { m -> vm.saveDefaultTimer(m) }
                    )
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize().weight(1f),
                verticalArrangement = Arrangement.spacedBy(SWSpacing.sm)
            ) {
                if (rec.isNotEmpty()) {
                    item {
                        BasicText(
                            "Recommended for $ageMonths mo",
                            style = SWFont.titleMD().copy(color = SWColor.textPrimary(scheme))
                        )
                    }
                    items(rec, key = { it.id }) { p -> PresetRow(p, currentId == p.id) { onTap(p, minutesNow, vm) } }
                }
                item {
                    BasicText("All presets", style = SWFont.titleMD().copy(color = SWColor.textPrimary(scheme)))
                }
                items(rest, key = { it.id }) { p -> PresetRow(p, currentId == p.id) { onTap(p, minutesNow, vm) } }
            }
        }
    }
}

@Composable
private fun PresetRow(preset: AudioPreset, isPlaying: Boolean, onTap: () -> Unit) {
    val scheme = LocalSWScheme.current
    GlassCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onTap)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Column(modifier = Modifier.padding(end = SWSpacing.md)) {
                BasicText(
                    preset.nameKey.removePrefix("audio."),
                    style = SWFont.titleMD().copy(color = SWColor.textPrimary(scheme))
                )
                BasicText(
                    "${preset.recommendedAgeMinMonths}–${preset.recommendedAgeMaxMonths} mo",
                    style = SWFont.labelMD().copy(color = SWColor.textSecondary(scheme))
                )
            }
            if (isPlaying) {
                BasicText("● Playing", style = SWFont.labelMD().copy(color = SWColor.accent(scheme)))
            }
        }
    }
}

private fun onTap(preset: AudioPreset, minutes: Int, vm: PlayerViewModel) {
    if ((vm.playerState.value as? PlayerState.Playing)?.presetId == preset.id) {
        vm.stop()
    } else {
        vm.playPreset(preset.id, minutes)
    }
}
