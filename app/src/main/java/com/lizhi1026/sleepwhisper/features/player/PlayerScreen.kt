package com.lizhi1026.sleepwhisper.features.player

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lizhi1026.sleepwhisper.R
import com.lizhi1026.sleepwhisper.core.strings.audioPresetNameKey
import com.lizhi1026.sleepwhisper.core.visualkit.LocalHeroBackdropController
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor
import com.lizhi1026.sleepwhisper.core.visualkit.SWFont
import com.lizhi1026.sleepwhisper.core.visualkit.SWGradient
import com.lizhi1026.sleepwhisper.core.visualkit.SWRadius
import com.lizhi1026.sleepwhisper.core.visualkit.SWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWSpacing
import com.lizhi1026.sleepwhisper.core.visualkit.components.AudioWaveform
import com.lizhi1026.sleepwhisper.core.visualkit.components.AuraParticles
import com.lizhi1026.sleepwhisper.core.visualkit.components.AuroraBackdrop
import com.lizhi1026.sleepwhisper.core.visualkit.components.ChevronTrail
import com.lizhi1026.sleepwhisper.core.visualkit.components.DragHandle
import com.lizhi1026.sleepwhisper.core.visualkit.components.GlassCard
import com.lizhi1026.sleepwhisper.core.visualkit.components.PresetAuraOrb
import com.lizhi1026.sleepwhisper.core.visualkit.components.innerHighlight
import com.lizhi1026.sleepwhisper.core.visualkit.components.SectionLabel as SharedSectionLabel
import com.lizhi1026.sleepwhisper.model.AudioPreset

@Composable
fun PlayerScreen(
    embedded: Boolean = false,
    vm: PlayerViewModel = hiltViewModel()
) {
    val scheme = LocalSWScheme.current
    val baby by vm.baby.observeAsState(null)
    val settings by vm.settings.observeAsState(null)
    val current by vm.currentPreset.observeAsState(null)
    // Use currentPreset as the source of truth for "what's selected" — survives state
    // transitions (Loading / FadingOut / Paused) that a `PlayerState.Playing` cast would miss.
    val currentId = current?.id

    val minutesNow = settings?.defaultTimerMinutes ?: 30
    val ageMonths = remember(baby) { baby?.ageInMonths() ?: 0 }
    val rec = vm.recommended(ageMonths)
    val rest = vm.allPresets - rec.toSet()

    val heroBackdrop = LocalHeroBackdropController.current
    val ambient by remember(heroBackdrop) {
        derivedStateOf { heroBackdrop.ambient }
    }

    // Push the active preset's mid color to the backdrop. Persists across screens
    // until playback stops (current → null), at which point Home's LaunchedEffect
    // restores its time-of-day fallback. Works in both standalone tab and embedded
    // mini-sheet (Phase 2 Sleeping integration) modes.
    LaunchedEffect(current, embedded) {
        heroBackdrop.syncToPlayer(currentPreset = current, fallback = null)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!embedded) {
            AuroraBackdrop()
        }
        Column(modifier = Modifier.fillMaxSize()) {
            if (!embedded) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                    DragHandle()
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = SWSpacing.lg),
                contentPadding = PaddingValues(top = SWSpacing.md, bottom = SWSpacing.huge),
                verticalArrangement = Arrangement.spacedBy(SWSpacing.xl)
            ) {
                item {
                    Header(ageMonths = ageMonths, babyName = baby?.name)
                }

                item {
                    DurationPickerCard(
                        selected = minutesNow,
                        onSelect = { vm.saveDefaultTimer(it) }
                    )
                }

                if (rec.isNotEmpty()) {
                    item {
                        SharedSectionLabel(stringResource(R.string.player_section_recommended, ageMonths))
                    }
                    items(rec, key = { it.id }) { p ->
                        PresetRow(p, currentId == p.id, ambient) { onTap(p, minutesNow, vm) }
                    }
                }

                item {
                    SharedSectionLabel(stringResource(R.string.player_section_all))
                }
                items(rest, key = { it.id }) { p ->
                    PresetRow(p, currentId == p.id, ambient) { onTap(p, minutesNow, vm) }
                }
            }
        }
        // Aura particles overlay — visible only when a preset is active. Drawn on
        // top of the LazyColumn but transparent to pointer input (clearAndSetSemantics
        // inside AuraParticles disables any focus claim) so taps pass through.
        current?.let { activePreset ->
            AuraParticles(
                tint = activePreset.auraColors.top,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun Header(ageMonths: Int, babyName: String?) {
    val scheme = LocalSWScheme.current
    Column(verticalArrangement = Arrangement.spacedBy(SWSpacing.xs)) {
        BasicText(
            text = stringResource(R.string.player_header),
            style = SWFont.serif(22).copy(color = SWColor.textPrimary(scheme))
        )
        BasicText(
            text = stringResource(R.string.player_subheader, "$ageMonths").uppercase(),
            style = SWFont.labelMD().copy(
                color = SWColor.textSecondary(scheme),
                letterSpacing = 1.2.sp
            )
        )
    }
}

@Composable
private fun DurationPickerCard(selected: Int, onSelect: (Int) -> Unit) {
    val scheme = LocalSWScheme.current
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 18.dp,
        contentPadding = PaddingValues(SWSpacing.md)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(SWSpacing.xs)) {
            BasicText(
                text = stringResource(R.string.player_duration_label).uppercase(),
                style = SWFont.labelMD().copy(
                    color = SWColor.textSecondary(scheme),
                    letterSpacing = 1.2.sp
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth().animateContentSize(),
                horizontalArrangement = Arrangement.spacedBy(SWSpacing.xs)
            ) {
                listOf(15, 30, 45, 60, 90).forEach { m ->
                    DurationChip(
                        minutes = m,
                        active = m == selected,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelect(m) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DurationChip(
    minutes: Int,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val scheme = LocalSWScheme.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(SWRadius.pill))
            .background(
                if (active) SWGradient.auroraGlow(scheme)
                else androidx.compose.ui.graphics.Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = if (scheme == SWScheme.DAY) 0.4f else 0.05f),
                        Color.White.copy(alpha = if (scheme == SWScheme.DAY) 0.4f else 0.05f)
                    )
                )
            )
            .then(
                if (!active && scheme != SWScheme.DAY)
                    Modifier.border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(SWRadius.pill))
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(vertical = SWSpacing.xs),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = stringResource(R.string.player_duration_minutes, minutes),
            style = SWFont.labelMD().copy(
                color = if (active) Color.White else SWColor.textSecondary(scheme),
                textAlign = TextAlign.Center
            )
        )
    }
}

@Composable
private fun PresetRow(
    preset: AudioPreset,
    isPlaying: Boolean,
    ambient: Color?,
    onTap: () -> Unit
) {
    val scheme = LocalSWScheme.current
    val tint = if (!isPlaying && ambient != null) {
        lerp(SWColor.surfaceElevated(scheme), ambient, 0.12f)
    } else null

    GlassCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onTap),
        cornerRadius = 18.dp,
        contentPadding = PaddingValues(SWSpacing.md),
        surfaceTintOverride = tint
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SWSpacing.md)) {
            PresetAuraOrb(
                aura = preset.auraColors,
                isActive = isPlaying,
                sizeDp = 48.dp
            )
            Column(modifier = Modifier.weight(1f)) {
                BasicText(
                    text = stringResource(audioPresetNameKey(preset.nameKey)),
                    style = SWFont.titleMD().copy(color = SWColor.textPrimary(scheme))
                )
                BasicText(
                    text = stringResource(
                        R.string.player_preset_agerange,
                        preset.recommendedAgeMinMonths,
                        preset.recommendedAgeMaxMonths
                    ),
                    style = SWFont.labelSM().copy(color = SWColor.textSecondary(scheme))
                )
            }
            if (isPlaying) {
                AudioWaveform(
                    modifier = Modifier
                        .width(36.dp)
                        .height(22.dp),
                    isPlaying = true,
                    color = preset.auraColors.top,
                    barCount = 8
                )
            } else {
                ChevronTrail()
            }
        }
    }
}

private fun onTap(preset: AudioPreset, minutes: Int, vm: PlayerViewModel) {
    if (vm.currentPreset.value?.id == preset.id) {
        vm.stop()
    } else {
        vm.playPreset(preset.id, minutes)
    }
}
