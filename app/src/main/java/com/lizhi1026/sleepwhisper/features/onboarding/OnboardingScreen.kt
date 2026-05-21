package com.lizhi1026.sleepwhisper.features.onboarding

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
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
import com.lizhi1026.sleepwhisper.core.visualkit.SWTheme
import com.lizhi1026.sleepwhisper.core.visualkit.components.AuroraBackdrop
import com.lizhi1026.sleepwhisper.core.visualkit.components.GlassCard
import com.lizhi1026.sleepwhisper.core.visualkit.components.Hairline
import com.lizhi1026.sleepwhisper.core.visualkit.components.NightSkyCanvas
import com.lizhi1026.sleepwhisper.core.visualkit.components.PulseRing
import com.lizhi1026.sleepwhisper.core.visualkit.components.SWSegmentedPicker
import com.lizhi1026.sleepwhisper.core.visualkit.components.SectionLabel
import com.lizhi1026.sleepwhisper.core.visualkit.components.SegmentedOption
import com.lizhi1026.sleepwhisper.core.visualkit.components.SoftButton
import com.lizhi1026.sleepwhisper.core.visualkit.components.SoftButtonStyle
import com.lizhi1026.sleepwhisper.core.visualkit.components.innerHighlight
import com.lizhi1026.sleepwhisper.model.Baby
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun OnboardingScreen(vm: OnboardingViewModel = hiltViewModel()) {
    val name by vm.name.observeAsState("")
    val gender by vm.gender.observeAsState(Baby.BabyGender.UNKNOWN)
    val dob by vm.dobMs.observeAsState(null)
    val valid by vm.isValid.observeAsState(false)

    OnboardingContent(
        name = name,
        gender = gender,
        dob = dob,
        valid = valid,
        onNameChange = vm::onName,
        onDobChange = vm::onDob,
        onGenderChange = vm::onGender,
        onSubmit = vm::submit
    )
}

@Composable
private fun OnboardingContent(
    name: String,
    gender: Baby.BabyGender,
    dob: Long?,
    valid: Boolean,
    onNameChange: (String) -> Unit,
    onDobChange: (Long) -> Unit,
    onGenderChange: (Baby.BabyGender) -> Unit,
    onSubmit: () -> Unit
) {
    val scheme = LocalSWScheme.current
    val ctx = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    fun openDatePicker() {
        val cal = Calendar.getInstance().apply {
            timeInMillis = dob ?: (System.currentTimeMillis() - 90L * 86_400_000L)
        }
        DatePickerDialog(
            ctx,
            { _, y, m, d ->
                val picked = Calendar.getInstance().apply {
                    clear(); set(y, m, d, 0, 0, 0)
                }
                onDobChange(picked.timeInMillis)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).apply { datePicker.maxDate = System.currentTimeMillis() }.show()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AuroraBackdrop()
        NightSkyCanvas(modifier = Modifier.fillMaxSize(), morphProgress = 1f)

        // Single-page layout — no scroll. Form content centered/wrapped at top;
        // CTA pinned to bottom via Box alignment (avoids weight-distribution
        // edge case that was inflating the GlassCard).
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = SWSpacing.xl)
                .padding(top = SWSpacing.lg, bottom = 96.dp + SWSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SWSpacing.lg)
        ) {
            // Hero section — slightly smaller circle so the form has room
            Box(contentAlignment = Alignment.Center) {
                PulseRing(
                    color = SWColor.accent(scheme),
                    radius = 50.dp,
                    intensity = if (scheme == SWScheme.DAY)
                        com.lizhi1026.sleepwhisper.core.visualkit.components.PulseIntensity.SOFT
                    else
                        com.lizhi1026.sleepwhisper.core.visualkit.components.PulseIntensity.STRONG
                )
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(SWGradient.auroraGlow(scheme))
                        .innerHighlight(cornerRadius = 36.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BasicText(
                    text = stringResource(R.string.onboarding_title),
                    style = SWFont.serif(24).copy(
                        color = SWColor.textPrimary(scheme),
                        textAlign = TextAlign.Center
                    )
                )
                Spacer(Modifier.height(SWSpacing.xxs))
                BasicText(
                    text = stringResource(R.string.onboarding_tagline),
                    style = SWFont.bodyMD().copy(
                        color = SWColor.textSecondary(scheme),
                        textAlign = TextAlign.Center
                    )
                )
            }

            // Form section — gaps tightened from .lg (20dp) to .md (16dp).
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.wrapContentHeight(),
                    verticalArrangement = Arrangement.spacedBy(SWSpacing.md)
                ) {
                    Field(label = stringResource(R.string.onboarding_babyname_label)) {
                        InputBox {
                            BasicTextField(
                                value = name,
                                onValueChange = onNameChange,
                                textStyle = SWFont.titleMD().copy(color = SWColor.textPrimary(scheme)),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
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
                    }
                    Hairline()
                    Field(label = stringResource(R.string.onboarding_dob_label)) {
                        // Date display IS the trigger — tap anywhere on it to open the
                        // system DatePickerDialog. No separate Save button needed.
                        val display = dob?.let { dateFormat.format(Date(it)) }
                            ?: stringResource(R.string.onboarding_dob_hint)
                        InputBox(onClick = ::openDatePicker) {
                            BasicText(
                                text = display,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .alpha(if (dob == null) 0.55f else 1f),
                                style = SWFont.titleMD().copy(color = SWColor.textPrimary(scheme))
                            )
                        }
                    }
                    Hairline()
                    Field(label = stringResource(R.string.onboarding_gender_label)) {
                        SWSegmentedPicker(
                            options = listOf(
                                SegmentedOption(Baby.BabyGender.UNKNOWN, stringResource(Baby.BabyGender.UNKNOWN.displayKey())),
                                SegmentedOption(Baby.BabyGender.FEMALE, stringResource(Baby.BabyGender.FEMALE.displayKey())),
                                SegmentedOption(Baby.BabyGender.MALE, stringResource(Baby.BabyGender.MALE.displayKey()))
                            ),
                            selected = gender,
                            onSelect = onGenderChange
                        )
                    }
                }
            }
        }

        // CTA pinned to bottom via Box alignment — separated from the form Column
        // because weight-distribution on a child of the Column with fillMaxSize()
        // was causing the GlassCard to inflate vertically.
        SoftButton(
            text = stringResource(R.string.onboarding_cta),
            onClick = onSubmit,
            enabled = valid,
            style = SoftButtonStyle.ACCENT,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = SWSpacing.xl, vertical = SWSpacing.lg)
                .imePadding()
        )
    }
}

@Composable
private fun Field(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(SWSpacing.xs)) {
        SectionLabel(label)
        content()
    }
}

/**
 * Visual container for an input row — bordered, rounded, sunken-surface.
 * Used by the name field (text input) and the date field (tappable display).
 * Aurora visual rules: 1dp border in token color, RoundedCornerShape(12.dp),
 * surfaceSunken background, generous internal padding for tap target.
 */
@Composable
private fun InputBox(
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val scheme = LocalSWScheme.current
    val shape = RoundedCornerShape(12.dp)
    val base = Modifier
        .fillMaxWidth()
        .clip(shape)
        .background(SWColor.surfaceSunken(scheme))
        .border(1.dp, SWColor.border(scheme), shape)
    val withClick = if (onClick != null) base.clickable(onClick = onClick) else base
    Box(
        modifier = withClick.padding(horizontal = SWSpacing.md, vertical = SWSpacing.sm)
    ) {
        content()
    }
}

@Preview(name = "Onboarding - Day", showBackground = true)
@Composable
fun OnboardingScreenDayPreview() {
    SWTheme(SWScheme.DAY) {
        OnboardingContent(
            name = "Lily",
            gender = Baby.BabyGender.FEMALE,
            dob = System.currentTimeMillis() - 30L * 86_400_000L,
            valid = true,
            onNameChange = {},
            onDobChange = {},
            onGenderChange = {},
            onSubmit = {}
        )
    }
}

@Preview(name = "Onboarding - Night", showBackground = true)
@Composable
fun OnboardingScreenNightPreview() {
    SWTheme(SWScheme.NIGHT) {
        OnboardingContent(
            name = "",
            gender = Baby.BabyGender.UNKNOWN,
            dob = null,
            valid = false,
            onNameChange = {},
            onDobChange = {},
            onGenderChange = {},
            onSubmit = {}
        )
    }
}
