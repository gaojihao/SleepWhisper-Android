package com.lizhi1026.sleepwhisper.features.settings

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lizhi1026.sleepwhisper.R
import com.lizhi1026.sleepwhisper.core.strings.displayKey
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor
import com.lizhi1026.sleepwhisper.core.visualkit.SWFont
import com.lizhi1026.sleepwhisper.core.visualkit.SWSpacing
import com.lizhi1026.sleepwhisper.core.visualkit.components.BreathingBackground
import com.lizhi1026.sleepwhisper.core.visualkit.components.ElevationLevel
import com.lizhi1026.sleepwhisper.core.visualkit.components.GlassCard
import com.lizhi1026.sleepwhisper.core.visualkit.components.SWSegmentedPicker
import com.lizhi1026.sleepwhisper.core.visualkit.components.SegmentedOption
import com.lizhi1026.sleepwhisper.model.UserSettings

@Composable
fun SettingsScreen(vm: SettingsViewModel = hiltViewModel()) {
    val scheme = LocalSWScheme.current
    val baby by vm.baby.observeAsState(null)
    val s by vm.settings.observeAsState(UserSettings.DEFAULT)

    Box(modifier = Modifier.fillMaxSize()) {
        BreathingBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SWSpacing.lg)
                .padding(top = SWSpacing.md),
            verticalArrangement = Arrangement.spacedBy(SWSpacing.md)
        ) {
            BasicText(
                text = stringResource(R.string.settings_title),
                style = SWFont.serifItalic(28).copy(color = SWColor.textPrimary(scheme)),
                modifier = Modifier.padding(top = SWSpacing.xs, bottom = SWSpacing.xs)
            )

            baby?.let {
                SectionCard(title = stringResource(R.string.settings_section_baby)) {
                    LabelRow(
                        label = stringResource(R.string.settings_baby_name),
                        value = it.name
                    )
                    Divider()
                    LabelRow(
                        label = stringResource(R.string.settings_baby_age),
                        value = "${it.ageInMonths()} mo"
                    )
                }
            }

            SectionCard(title = stringResource(R.string.settings_section_playback)) {
                LabelRow(
                    label = stringResource(R.string.settings_defaulttimer),
                    value = "${s.defaultTimerMinutes} min"
                )
                Divider()
                LabelRow(
                    label = stringResource(R.string.settings_fadeaftersleep),
                    value = "${s.fadeAfterSleepMinutes} min"
                )
                Divider()
                ToggleRow(
                    label = stringResource(R.string.settings_haptics),
                    on = s.hapticFeedback,
                    onToggle = { vm.update { it.copy(hapticFeedback = !it.hapticFeedback) } }
                )
            }

            SectionCard(title = stringResource(R.string.settings_section_cry)) {
                ToggleRow(
                    label = stringResource(R.string.settings_cry_enable),
                    on = s.cryDetectionEnabled,
                    onToggle = {
                        val nowEnabled = !s.cryDetectionEnabled
                        vm.update { it.copy(cryDetectionEnabled = nowEnabled) }
                        if (!nowEnabled) vm.stopCryDetection()
                    }
                )
                if (s.cryDetectionEnabled) {
                    Divider()
                    LabelRow(
                        label = stringResource(R.string.settings_cry_sensitivity),
                        value = "${s.cryDetectionThresholdDb} dB"
                    )
                }
                Spacer(Modifier.height(SWSpacing.xs))
                BasicText(
                    text = stringResource(R.string.settings_cry_privacynote),
                    style = SWFont.labelSM().copy(color = SWColor.textTertiary(scheme))
                )
            }

            SectionCard(title = stringResource(R.string.settings_section_appearance)) {
                SWSegmentedPicker(
                    options = listOf(
                        SegmentedOption(UserSettings.AppearanceMode.AUTO, stringResource(UserSettings.AppearanceMode.AUTO.displayKey())),
                        SegmentedOption(UserSettings.AppearanceMode.LIGHT, stringResource(UserSettings.AppearanceMode.LIGHT.displayKey())),
                        SegmentedOption(UserSettings.AppearanceMode.DARK, stringResource(UserSettings.AppearanceMode.DARK.displayKey()))
                    ),
                    selected = s.appearance,
                    onSelect = { mode -> vm.update { it.copy(appearance = mode) } }
                )
                Divider()
                LabelRow(
                    label = stringResource(R.string.settings_nightmode_label),
                    value = "${s.nightModeStartHour}:00 – ${s.nightModeEndHour}:00"
                )
            }

            SectionCard(title = stringResource(R.string.settings_section_about)) {
                LabelRow(
                    label = stringResource(R.string.settings_version),
                    value = s.lastSeenVersion
                )
                Spacer(Modifier.height(SWSpacing.xs))
                BasicText(
                    text = stringResource(R.string.settings_tagline),
                    style = SWFont.labelMD().copy(color = SWColor.textSecondary(scheme))
                )
            }

            Spacer(Modifier.height(SWSpacing.huge))
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    val scheme = LocalSWScheme.current
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        contentPadding = PaddingValues(SWSpacing.lg),
        elevation = ElevationLevel.SOFT
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(SWSpacing.sm)) {
            BasicText(
                text = title.uppercase(),
                style = SWFont.labelMD().copy(
                    color = SWColor.textSecondary(scheme),
                    letterSpacing = 1.4.sp
                )
            )
            content()
        }
    }
}

@Composable
private fun LabelRow(label: String, value: String) {
    val scheme = LocalSWScheme.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicText(label, style = SWFont.bodyMD().copy(color = SWColor.textPrimary(scheme)))
        BasicText(value, style = SWFont.bodyMD().copy(color = SWColor.textSecondary(scheme)))
    }
}

@Composable
private fun ToggleRow(label: String, on: Boolean, onToggle: () -> Unit) {
    val scheme = LocalSWScheme.current
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicText(label, style = SWFont.bodyMD().copy(color = SWColor.textPrimary(scheme)))
        BasicText(
            text = if (on) "●" else "○",
            style = SWFont.titleMD().copy(
                color = if (on) SWColor.accent(scheme) else SWColor.textTertiary(scheme)
            )
        )
    }
}

@Composable
private fun Divider() {
    val scheme = LocalSWScheme.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(SWColor.border(scheme).copy(alpha = 0.15f))
    )
}
