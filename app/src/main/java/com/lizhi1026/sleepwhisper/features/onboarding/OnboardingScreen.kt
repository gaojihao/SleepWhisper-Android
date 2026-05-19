package com.lizhi1026.sleepwhisper.features.onboarding

import android.app.DatePickerDialog
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
import com.lizhi1026.sleepwhisper.core.visualkit.components.SoftButton
import com.lizhi1026.sleepwhisper.model.Baby
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun OnboardingScreen(vm: OnboardingViewModel = hiltViewModel()) {
    val scheme = LocalSWScheme.current
    val ctx = LocalContext.current
    val name by vm.name.observeAsState("")
    val gender by vm.gender.observeAsState(Baby.BabyGender.UNKNOWN)
    val dob by vm.dobMs.observeAsState(null)
    val valid by vm.isValid.observeAsState(false)

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    BreathingBackground()
    Column(
        modifier = Modifier.fillMaxSize().padding(SWSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(SWSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BasicText(
            text = stringResource(R.string.onboarding_title),
            style = SWFont.titleXL().copy(color = SWColor.textPrimary(scheme))
        )
        BasicText(
            text = stringResource(R.string.onboarding_tagline),
            style = SWFont.bodyMD().copy(color = SWColor.textSecondary(scheme))
        )
        GlassCard(modifier = Modifier.fillMaxSize().padding(0.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(SWSpacing.lg)) {
                LabelledField(label = stringResource(R.string.onboarding_babyname_label)) {
                    BasicTextField(
                        value = name,
                        onValueChange = vm::onName,
                        textStyle = SWFont.bodyLG().copy(color = SWColor.textPrimary(scheme)),
                        singleLine = true,
                        decorationBox = { inner ->
                            if (name.isEmpty()) {
                                BasicText(
                                    stringResource(R.string.onboarding_babyname_placeholder),
                                    style = SWFont.bodyLG().copy(color = SWColor.textTertiary(scheme))
                                )
                            }
                            inner()
                        }
                    )
                }
                LabelledField(label = stringResource(R.string.onboarding_dob_label)) {
                    val display = dob?.let { dateFormat.format(Date(it)) }
                        ?: stringResource(R.string.onboarding_dob_hint)
                    SoftButton(
                        text = display,
                        onClick = {
                            val cal = Calendar.getInstance().apply {
                                timeInMillis = dob ?: (System.currentTimeMillis() - 90L * 86_400_000L)
                            }
                            DatePickerDialog(
                                ctx,
                                { _, y, m, d ->
                                    val picked = Calendar.getInstance().apply {
                                        clear()
                                        set(y, m, d, 0, 0, 0)
                                    }
                                    vm.onDob(picked.timeInMillis)
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).apply {
                                datePicker.maxDate = System.currentTimeMillis()
                            }.show()
                        },
                        style = com.lizhi1026.sleepwhisper.core.visualkit.components.SoftButtonStyle.GHOST
                    )
                }
                LabelledField(label = stringResource(R.string.onboarding_gender_unknown)) {
                    SWSegmentedPicker(
                        options = listOf(
                            SegmentedOption(Baby.BabyGender.UNKNOWN, stringResource(Baby.BabyGender.UNKNOWN.displayKey())),
                            SegmentedOption(Baby.BabyGender.FEMALE, stringResource(Baby.BabyGender.FEMALE.displayKey())),
                            SegmentedOption(Baby.BabyGender.MALE, stringResource(Baby.BabyGender.MALE.displayKey()))
                        ),
                        selected = gender,
                        onSelect = vm::onGender
                    )
                }
            }
        }
        SoftButton(
            text = stringResource(R.string.onboarding_cta),
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
