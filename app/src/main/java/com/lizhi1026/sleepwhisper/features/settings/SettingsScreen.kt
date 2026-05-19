package com.lizhi1026.sleepwhisper.features.settings

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.lizhi1026.sleepwhisper.R
import com.lizhi1026.sleepwhisper.core.strings.displayKey
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor
import com.lizhi1026.sleepwhisper.core.visualkit.SWFont
import com.lizhi1026.sleepwhisper.core.visualkit.SWSpacing
import com.lizhi1026.sleepwhisper.core.visualkit.components.BreathingBackground
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
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(SWSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(SWSpacing.lg)
        ) {
            BasicText(
                stringResource(R.string.settings_title),
                style = SWFont.titleXL().copy(color = SWColor.textPrimary(scheme))
            )
            baby?.let {
                Card(stringResource(R.string.settings_section_baby)) {
                    BasicText(it.name, style = SWFont.bodyLG().copy(color = SWColor.textPrimary(scheme)))
                    BasicText(
                        stringResource(R.string.settings_baby_age) + ": " + it.ageInMonths(),
                        style = SWFont.labelMD().copy(color = SWColor.textSecondary(scheme))
                    )
                }
            }
            Card(stringResource(R.string.settings_section_appearance)) {
                SWSegmentedPicker(
                    options = listOf(
                        SegmentedOption(UserSettings.AppearanceMode.AUTO, stringResource(UserSettings.AppearanceMode.AUTO.displayKey())),
                        SegmentedOption(UserSettings.AppearanceMode.LIGHT, stringResource(UserSettings.AppearanceMode.LIGHT.displayKey())),
                        SegmentedOption(UserSettings.AppearanceMode.DARK, stringResource(UserSettings.AppearanceMode.DARK.displayKey()))
                    ),
                    selected = s.appearance,
                    onSelect = { mode -> vm.update { it.copy(appearance = mode) } }
                )
            }
            Card(stringResource(R.string.settings_section_cry)) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable {
                        vm.update { it.copy(cryDetectionEnabled = !it.cryDetectionEnabled) }
                        if (s.cryDetectionEnabled) vm.stopCryDetection()
                    },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicText(stringResource(R.string.settings_cry_enable),
                        style = SWFont.bodyMD().copy(color = SWColor.textPrimary(scheme)))
                    BasicText(
                        if (s.cryDetectionEnabled) "●" else "○",
                        style = SWFont.titleMD().copy(color = SWColor.accent(scheme))
                    )
                }
                BasicText(stringResource(R.string.settings_cry_privacynote),
                    style = SWFont.labelSM().copy(color = SWColor.textTertiary(scheme)))
            }
            Card(stringResource(R.string.settings_section_about)) {
                BasicText(
                    stringResource(R.string.settings_version) + " " + s.lastSeenVersion,
                    style = SWFont.bodyMD().copy(color = SWColor.textPrimary(scheme))
                )
                BasicText(stringResource(R.string.settings_tagline),
                    style = SWFont.labelMD().copy(color = SWColor.textSecondary(scheme)))
            }
        }
    }
}

@Composable
private fun Card(title: String, content: @Composable () -> Unit) {
    val scheme = LocalSWScheme.current
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(SWSpacing.sm)) {
            BasicText(title, style = SWFont.titleMD().copy(color = SWColor.textPrimary(scheme)))
            content()
        }
    }
}
