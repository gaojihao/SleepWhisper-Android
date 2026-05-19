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
import androidx.hilt.navigation.compose.hiltViewModel
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
            BasicText("Settings", style = SWFont.titleXL().copy(color = SWColor.textPrimary(scheme)))
            baby?.let {
                Card("Baby") {
                    BasicText(it.name, style = SWFont.bodyLG().copy(color = SWColor.textPrimary(scheme)))
                    BasicText("${it.ageInMonths()} months", style = SWFont.labelMD().copy(color = SWColor.textSecondary(scheme)))
                }
            }
            Card("Appearance") {
                SWSegmentedPicker(
                    options = listOf(
                        SegmentedOption(UserSettings.AppearanceMode.AUTO, "Auto"),
                        SegmentedOption(UserSettings.AppearanceMode.LIGHT, "Light"),
                        SegmentedOption(UserSettings.AppearanceMode.DARK, "Dark")
                    ),
                    selected = s.appearance,
                    onSelect = { mode -> vm.update { it.copy(appearance = mode) } }
                )
            }
            Card("Cry detection") {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable {
                        vm.update { it.copy(cryDetectionEnabled = !it.cryDetectionEnabled) }
                        if (s.cryDetectionEnabled) vm.stopCryDetection()
                    },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicText("Enable", style = SWFont.bodyMD().copy(color = SWColor.textPrimary(scheme)))
                    BasicText(if (s.cryDetectionEnabled) "ON" else "OFF",
                        style = SWFont.labelMD().copy(color = SWColor.accent(scheme)))
                }
            }
            Card("About") {
                BasicText("Version ${s.lastSeenVersion}",
                    style = SWFont.bodyMD().copy(color = SWColor.textPrimary(scheme)))
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
