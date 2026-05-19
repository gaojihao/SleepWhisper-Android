package com.lizhi1026.sleepwhisper.features.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor
import com.lizhi1026.sleepwhisper.core.visualkit.SWFont
import com.lizhi1026.sleepwhisper.core.visualkit.SWSpacing
import com.lizhi1026.sleepwhisper.core.visualkit.components.BreathingBackground
import com.lizhi1026.sleepwhisper.core.visualkit.components.GlassCard
import com.lizhi1026.sleepwhisper.core.visualkit.components.SWSegmentedPicker
import com.lizhi1026.sleepwhisper.core.visualkit.components.SegmentedOption
import com.lizhi1026.sleepwhisper.core.visualkit.components.SoftButton
import com.lizhi1026.sleepwhisper.model.Baby

@Composable
fun OnboardingScreen(vm: OnboardingViewModel = hiltViewModel()) {
    val scheme = LocalSWScheme.current
    val name by vm.name.observeAsState("")
    val gender by vm.gender.observeAsState(Baby.BabyGender.UNKNOWN)
    val dob by vm.dobMs.observeAsState(null)
    val valid by vm.isValid.observeAsState(false)

    BreathingBackground()
    Column(
        modifier = Modifier.fillMaxSize().padding(SWSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(SWSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BasicText(
            text = "SleepWhisper",
            style = SWFont.titleXL().copy(color = SWColor.textPrimary(scheme))
        )
        GlassCard(modifier = Modifier.fillMaxSize().padding(0.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(SWSpacing.lg)) {
                LabelledField(label = "Name") {
                    BasicTextField(
                        value = name,
                        onValueChange = vm::onName,
                        textStyle = SWFont.bodyLG().copy(color = SWColor.textPrimary(scheme)),
                        singleLine = true
                    )
                }
                LabelledField(label = "Date of birth") {
                    BasicText(
                        text = dob?.let { java.text.SimpleDateFormat("yyyy-MM-dd").format(java.util.Date(it)) }
                            ?: "Tap to choose",
                        modifier = Modifier
                            .padding(vertical = SWSpacing.xs)
                            .size(width = 200.dp, height = 36.dp),
                        style = SWFont.bodyLG().copy(color = SWColor.textPrimary(scheme))
                    )
                    // Placeholder: real implementation uses DatePicker dialog. For MVP, seed -90 days.
                    if (dob == null) {
                        SoftButton(
                            text = "Set 90 days ago",
                            onClick = { vm.onDob(System.currentTimeMillis() - 90L * 86_400_000L) }
                        )
                    }
                }
                LabelledField(label = "Gender") {
                    SWSegmentedPicker(
                        options = listOf(
                            SegmentedOption(Baby.BabyGender.UNKNOWN, "Unknown"),
                            SegmentedOption(Baby.BabyGender.FEMALE, "Female"),
                            SegmentedOption(Baby.BabyGender.MALE, "Male")
                        ),
                        selected = gender,
                        onSelect = vm::onGender
                    )
                }
            }
        }
        SoftButton(
            text = "Begin",
            onClick = vm::submit,
            enabled = valid
        )
    }
}

@Composable
private fun LabelledField(label: String, content: @Composable () -> Unit) {
    val scheme = LocalSWScheme.current
    Column(verticalArrangement = Arrangement.spacedBy(SWSpacing.xs)) {
        BasicText(
            text = label,
            style = SWFont.labelMD().copy(color = SWColor.textSecondary(scheme))
        )
        content()
    }
}
