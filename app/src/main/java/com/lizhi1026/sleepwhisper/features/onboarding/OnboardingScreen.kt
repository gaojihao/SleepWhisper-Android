package com.lizhi1026.sleepwhisper.features.onboarding

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lizhi1026.sleepwhisper.R
import com.lizhi1026.sleepwhisper.core.strings.displayKey
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor
import com.lizhi1026.sleepwhisper.core.visualkit.SWFont
import com.lizhi1026.sleepwhisper.core.visualkit.SWGradient
import com.lizhi1026.sleepwhisper.core.visualkit.SWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWSpacing
import com.lizhi1026.sleepwhisper.core.visualkit.components.BreathingBackground
import com.lizhi1026.sleepwhisper.core.visualkit.components.GlassCard
import com.lizhi1026.sleepwhisper.core.visualkit.components.PulseRing
import com.lizhi1026.sleepwhisper.core.visualkit.components.SWSegmentedPicker
import com.lizhi1026.sleepwhisper.core.visualkit.components.SegmentedOption
import com.lizhi1026.sleepwhisper.core.visualkit.components.SoftButton
import com.lizhi1026.sleepwhisper.core.visualkit.components.SoftButtonStyle
import com.lizhi1026.sleepwhisper.core.visualkit.components.Starfield
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

    Box(modifier = Modifier.fillMaxSize()) {
        BreathingBackground()
        Starfield(modifier = Modifier.fillMaxSize(), density = if (scheme == SWScheme.DAY) 30 else 70)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SWSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SWSpacing.xxl)
        ) {
            Spacer(Modifier.height(80.dp))

            // Hero section
            Box(contentAlignment = Alignment.Center) {
                PulseRing(
                    color = SWColor.accent(scheme),
                    radius = 60.dp,
                    intensity = if (scheme == SWScheme.DAY)
                        com.lizhi1026.sleepwhisper.core.visualkit.components.PulseIntensity.SOFT
                    else
                        com.lizhi1026.sleepwhisper.core.visualkit.components.PulseIntensity.STRONG
                )
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(SWGradient.accent(scheme))
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BasicText(
                    text = stringResource(R.string.onboarding_title),
                    style = SWFont.serifItalic(32).copy(
                        color = SWColor.textPrimary(scheme),
                        textAlign = TextAlign.Center
                    )
                )
                Spacer(Modifier.height(SWSpacing.xs))
                BasicText(
                    text = stringResource(R.string.onboarding_tagline),
                    style = SWFont.bodyLG().copy(
                        color = SWColor.textSecondary(scheme),
                        textAlign = TextAlign.Center
                    )
                )
            }

            // Form section
            GlassCard(modifier = Modifier) {
                Column(verticalArrangement = Arrangement.spacedBy(SWSpacing.lg)) {
                    Field(label = stringResource(R.string.onboarding_babyname_label)) {
                        BasicTextField(
                            value = name,
                            onValueChange = vm::onName,
                            textStyle = SWFont.titleMD().copy(color = SWColor.textPrimary(scheme)),
                            singleLine = true,
                            decorationBox = { inner ->
                                if (name.isEmpty()) {
                                    BasicText(
                                        stringResource(R.string.onboarding_babyname_placeholder),
                                        style = SWFont.titleMD().copy(color = SWColor.textTertiary(scheme))
                                    )
                                }
                                inner()
                            }
                        )
                    }
                    Divider()
                    Field(label = stringResource(R.string.onboarding_dob_label)) {
                        val display = dob?.let { dateFormat.format(Date(it)) }
                            ?: stringResource(R.string.onboarding_dob_hint)
                        BasicText(
                            text = display,
                            modifier = Modifier
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                                .background(SWColor.surfaceSunken(scheme))
                                .padding(horizontal = SWSpacing.md, vertical = SWSpacing.sm)
                                .alpha(if (dob == null) 0.55f else 1f),
                            style = SWFont.titleMD().copy(color = SWColor.textPrimary(scheme))
                        )
                        SoftButton(
                            text = stringResource(if (dob == null) R.string.common_save else R.string.common_save),
                            onClick = {
                                val cal = Calendar.getInstance().apply {
                                    timeInMillis = dob ?: (System.currentTimeMillis() - 90L * 86_400_000L)
                                }
                                DatePickerDialog(
                                    ctx,
                                    { _, y, m, d ->
                                        val picked = Calendar.getInstance().apply {
                                            clear(); set(y, m, d, 0, 0, 0)
                                        }
                                        vm.onDob(picked.timeInMillis)
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).apply { datePicker.maxDate = System.currentTimeMillis() }.show()
                            },
                            style = SoftButtonStyle.GHOST
                        )
                    }
                    Divider()
                    Field(label = stringResource(R.string.onboarding_gender_unknown)) {
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
                enabled = valid,
                style = SoftButtonStyle.ACCENT,
                modifier = Modifier.alpha(if (valid) 1f else 0.4f)
            )

            Spacer(Modifier.height(SWSpacing.huge))
        }
    }
}

@Composable
private fun Field(label: String, content: @Composable () -> Unit) {
    val scheme = LocalSWScheme.current
    Column(verticalArrangement = Arrangement.spacedBy(SWSpacing.xs)) {
        BasicText(
            text = label.uppercase(),
            style = SWFont.labelMD().copy(
                color = SWColor.textSecondary(scheme),
                letterSpacing = 1.4.sp()
            )
        )
        content()
    }
}

@Composable
private fun Divider() {
    val scheme = LocalSWScheme.current
    Box(
        modifier = Modifier
            .height(1.dp)
            .background(SWColor.border(scheme).copy(alpha = 0.2f))
    )
}

private fun Double.sp(): androidx.compose.ui.unit.TextUnit =
    androidx.compose.ui.unit.TextUnit(this.toFloat(), androidx.compose.ui.unit.TextUnitType.Sp)
