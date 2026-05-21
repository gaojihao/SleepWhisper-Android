package com.lizhi1026.sleepwhisper.features.settings

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.lizhi1026.sleepwhisper.R
import com.lizhi1026.sleepwhisper.core.strings.displayKey
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor
import com.lizhi1026.sleepwhisper.core.visualkit.SWFont
import com.lizhi1026.sleepwhisper.core.visualkit.SWSpacing
import com.lizhi1026.sleepwhisper.core.visualkit.components.AuroraBackdrop
import com.lizhi1026.sleepwhisper.core.visualkit.components.ElevationLevel
import com.lizhi1026.sleepwhisper.core.visualkit.components.GlassCard
import com.lizhi1026.sleepwhisper.core.visualkit.components.Hairline
import com.lizhi1026.sleepwhisper.core.visualkit.components.SWSegmentedPicker
import com.lizhi1026.sleepwhisper.core.visualkit.components.SectionLabel
import com.lizhi1026.sleepwhisper.core.visualkit.components.SegmentedOption
import com.lizhi1026.sleepwhisper.core.visualkit.components.SerifMetricRow
import com.lizhi1026.sleepwhisper.model.Baby
import com.lizhi1026.sleepwhisper.model.UserSettings

@Composable
fun SettingsScreen(vm: SettingsViewModel = hiltViewModel()) {
    val scheme = LocalSWScheme.current
    val context = LocalContext.current
    val baby by vm.baby.observeAsState(null)
    val s by vm.settings.observeAsState(UserSettings.DEFAULT)
    val forceNightPreview by vm.forceNightPreview.observeAsState(false)

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            vm.update { it.copy(cryDetectionEnabled = true) }
        }
        // If denied, the toggle stays off; user can retry from the system Settings or by toggling again.
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AuroraBackdrop()
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
            Hairline(modifier = Modifier.width(60.dp).padding(bottom = SWSpacing.xs))

            baby?.let { b ->
                SectionCard(title = stringResource(R.string.settings_section_baby)) {
                    SerifMetricRow(
                        label = stringResource(R.string.settings_baby_name),
                        value = b.name,
                        showChevron = false
                    )
                    Hairline()
                    SerifMetricRow(
                        label = stringResource(R.string.settings_baby_age),
                        value = formatAge(b),
                        showChevron = false
                    )
                }
            }

            SectionCard(title = stringResource(R.string.settings_section_playback)) {
                StepperRow(
                    label = stringResource(R.string.settings_defaulttimer_label),
                    valueText = stringResource(R.string.settings_minutes_value, s.defaultTimerMinutes),
                    onDecrement = {
                        vm.update { it.copy(defaultTimerMinutes = (it.defaultTimerMinutes - 5).coerceAtLeast(5)) }
                    },
                    onIncrement = {
                        vm.update { it.copy(defaultTimerMinutes = (it.defaultTimerMinutes + 5).coerceAtMost(120)) }
                    }
                )
                Hairline()
                StepperRow(
                    label = stringResource(R.string.settings_fadeaftersleep_label),
                    valueText = stringResource(R.string.settings_minutes_value, s.fadeAfterSleepMinutes),
                    onDecrement = {
                        vm.update { it.copy(fadeAfterSleepMinutes = (it.fadeAfterSleepMinutes - 1).coerceAtLeast(1)) }
                    },
                    onIncrement = {
                        vm.update { it.copy(fadeAfterSleepMinutes = (it.fadeAfterSleepMinutes + 1).coerceAtMost(15)) }
                    }
                )
                Hairline()
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
                        if (nowEnabled) {
                            val granted = ContextCompat.checkSelfPermission(
                                context, android.Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                vm.update { it.copy(cryDetectionEnabled = true) }
                            } else {
                                // Defer enabling until the user grants the runtime permission;
                                // the launcher callback writes back the toggled value on success.
                                micPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                            }
                        } else {
                            vm.update { it.copy(cryDetectionEnabled = false) }
                            vm.stopCryDetection()
                        }
                    }
                )
                if (s.cryDetectionEnabled) {
                    Hairline()
                    StepperRow(
                        label = stringResource(R.string.settings_cry_sensitivity_label),
                        valueText = stringResource(R.string.settings_db_value, s.cryDetectionThresholdDb),
                        onDecrement = {
                            vm.update { it.copy(cryDetectionThresholdDb = (it.cryDetectionThresholdDb - 5).coerceAtLeast(40)) }
                        },
                        onIncrement = {
                            vm.update { it.copy(cryDetectionThresholdDb = (it.cryDetectionThresholdDb + 5).coerceAtMost(90)) }
                        }
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
                Hairline()
                SerifMetricRow(
                    label = stringResource(R.string.settings_nightmode_label),
                    value = "${s.nightModeStartHour}:00 – ${s.nightModeEndHour}:00",
                    showChevron = false
                )
                Hairline()
                ToggleRow(
                    label = stringResource(R.string.settings_nightmode_preview),
                    on = forceNightPreview,
                    onToggle = { vm.setForceNightPreview(!forceNightPreview) }
                )
            }

            SectionCard(title = stringResource(R.string.settings_section_about)) {
                SerifMetricRow(
                    label = stringResource(R.string.settings_version),
                    value = s.lastSeenVersion,
                    showChevron = false
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
private fun formatAge(b: Baby): String {
    val parts = b.ageParts()
    return when {
        parts.years > 0 -> stringResource(
            R.string.settings_age_display_yearsmonthsdays,
            parts.years, parts.months, parts.days
        )
        parts.months > 0 -> stringResource(
            R.string.settings_age_display_monthsdays,
            parts.months, parts.days
        )
        else -> stringResource(R.string.settings_age_display_days, parts.days)
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        contentPadding = PaddingValues(SWSpacing.lg),
        elevation = ElevationLevel.SOFT
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(SWSpacing.sm)) {
            SectionLabel(title)
            content()
        }
    }
}

@Composable
private fun ToggleRow(label: String, on: Boolean, onToggle: () -> Unit) {
    val scheme = LocalSWScheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(
                value = on,
                role = androidx.compose.ui.semantics.Role.Switch,
                onValueChange = { onToggle() }
            ),
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

/**
 * Row with a label on the left, a current-value chip and two round +/- buttons
 * on the right. Stand-in for iOS Stepper since Compose Foundation has no Stepper.
 */
@Composable
private fun StepperRow(
    label: String,
    valueText: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit
) {
    val scheme = LocalSWScheme.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicText(label, style = SWFont.bodyMD().copy(color = SWColor.textPrimary(scheme)))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SWSpacing.sm)
        ) {
            StepperButton(label = "−", onClick = onDecrement)
            BasicText(
                text = valueText,
                modifier = Modifier.padding(horizontal = SWSpacing.xs),
                style = SWFont.bodyMD().copy(
                    color = SWColor.textSecondary(scheme),
                    textAlign = TextAlign.Center
                )
            )
            StepperButton(label = "+", onClick = onIncrement)
        }
    }
}

@Composable
private fun StepperButton(label: String, onClick: () -> Unit) {
    val scheme = LocalSWScheme.current
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(SWColor.surfaceSunken(scheme))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = label,
            style = SWFont.titleMD().copy(color = SWColor.textPrimary(scheme))
        )
    }
}
