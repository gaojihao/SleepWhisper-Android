package com.lizhi1026.sleepwhisper.features.home

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lizhi1026.sleepwhisper.R
import com.lizhi1026.sleepwhisper.core.audio.PlayerState
import com.lizhi1026.sleepwhisper.core.strings.audioPresetNameKey
import com.lizhi1026.sleepwhisper.core.strings.greetingForHour
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor
import com.lizhi1026.sleepwhisper.core.visualkit.SWFont
import com.lizhi1026.sleepwhisper.core.visualkit.SWSpacing
import com.lizhi1026.sleepwhisper.core.visualkit.components.AudioWaveform
import com.lizhi1026.sleepwhisper.core.visualkit.components.BreathingBackground
import com.lizhi1026.sleepwhisper.core.visualkit.components.GlassCard
import com.lizhi1026.sleepwhisper.core.visualkit.components.PulseRing
import com.lizhi1026.sleepwhisper.core.visualkit.components.RollingNumber
import com.lizhi1026.sleepwhisper.core.visualkit.components.SoftButton
import com.lizhi1026.sleepwhisper.core.visualkit.components.SoftButtonStyle
import com.lizhi1026.sleepwhisper.model.DiaperEvent
import com.lizhi1026.sleepwhisper.model.FeedingEvent.FeedingMethod
import java.time.LocalTime

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
    val hasSeenHints by vm.hasSeenHints.observeAsState(false)

    val hour = remember { LocalTime.now().hour }

    var showBottleSheet by remember { mutableStateOf(false) }
    var showSleepTypePicker by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        BreathingBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(SWSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(SWSpacing.lg)
        ) {
            Column {
                BasicText(
                    text = stringResource(greetingForHour(hour)),
                    style = SWFont.titleXL().copy(color = SWColor.textPrimary(scheme))
                )
                baby?.let {
                    BasicText(
                        text = it.name,
                        style = SWFont.titleLG().copy(color = SWColor.primary(scheme))
                    )
                }
            }

            if (!hasSeenHints) {
                OnboardingHintsCard(onDismiss = vm::onDismissHints)
            }

            wakeWindow?.let { (remaining, total) ->
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        BasicText(
                            text = stringResource(R.string.home_wakewindow_title),
                            style = SWFont.labelMD().copy(color = SWColor.textSecondary(scheme))
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            RollingNumber(
                                value = remaining,
                                style = SWFont.displayMD().copy(color = SWColor.textPrimary(scheme))
                            )
                            BasicText(
                                text = " / $total ${stringResource(R.string.home_wakewindow_unit)}",
                                style = SWFont.bodyMD().copy(color = SWColor.textSecondary(scheme)),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                    }
                }
            }

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.padding(end = SWSpacing.md)) {
                        BasicText(
                            text = currentPreset
                                ?.let { stringResource(audioPresetNameKey(it.nameKey)) }
                                ?: stringResource(R.string.home_nowplaying_placeholder),
                            style = SWFont.titleMD().copy(color = SWColor.textPrimary(scheme))
                        )
                    }
                    AudioWaveform(
                        modifier = Modifier.weight(1f),
                        isPlaying = playerState is PlayerState.Playing,
                        color = SWColor.accent(scheme)
                    )
                }
            }
            SoftButton(
                text = stringResource(R.string.player_header),
                onClick = onOpenPlayer,
                style = SoftButtonStyle.GHOST
            )

            QuickActionsRow(vm, onBottleTap = { showBottleSheet = true })

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = { showSleepTypePicker = true },
                            onTap = { vm.onTapSleep() }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                PulseRing(color = SWColor.accent(scheme), radius = 80.dp)
                SoftButton(
                    text = stringResource(R.string.home_sleepcta),
                    onClick = vm::onTapSleep,
                    style = SoftButtonStyle.ACCENT
                )
            }
        }

        if (showBottleSheet) {
            BottleAmountSheet(
                onConfirm = { ml -> vm.onRecordFeeding(FeedingMethod.BOTTLE, ml = ml) },
                onSkip = { vm.onRecordFeeding(FeedingMethod.BOTTLE, ml = null) },
                onDismiss = { showBottleSheet = false }
            )
        }
        if (showSleepTypePicker) {
            SleepTypePicker(
                suggestedType = vm.app.defaultSleepType(),
                onPick = { type -> vm.onPickSleepType(type) },
                onDismiss = { showSleepTypePicker = false }
            )
        }
    }
}

@Composable
private fun QuickActionsRow(vm: HomeViewModel, onBottleTap: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(SWSpacing.sm)
    ) {
        QuickAction(stringResource(R.string.quickaction_breastleft)) { vm.onRecordFeeding(FeedingMethod.BREAST_LEFT) }
        QuickAction(stringResource(R.string.quickaction_breastright)) { vm.onRecordFeeding(FeedingMethod.BREAST_RIGHT) }
        QuickAction(stringResource(R.string.quickaction_bottle), onBottleTap)
        QuickAction(stringResource(R.string.quickaction_diaper)) { vm.onRecordDiaper(DiaperEvent.DiaperType.WET) }
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
