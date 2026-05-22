/**
 * OnboardingScreen.kt — 首次使用时的宝宝信息录入界面（UI 层 / features/onboarding）
 *
 * 用途：引导用户填写宝宝基本信息以完成初始化设置，是应用进入主流程的前置单页表单。
 * 布局：**单页表单，无分步索引**，包含宝宝名字输入、生日选择（DatePickerDialog）、
 *       性别选择（可选，默认 UNKNOWN）三个字段，CTA 按钮固定于底部。
 * 用户交互流程：
 *   1. 用户填写宝宝名字（文本输入框）；
 *   2. 点击日期行任意位置弹出系统 DatePickerDialog，选择生日（不可超今天）；
 *   3. 性别通过 SWSegmentedPicker 选择（可选）；
 *   4. name 合法 && dobMs != null && dobMs <= now 时 isValid = true，CTA 按钮激活；
 *   5. 点击提交 → ViewModel.submit()，AppStateContainer.baby 变化驱动导航离开本页。
 */
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

/**
 * Onboarding 入口 Composable：从 ViewModel 读取状态并转发给无状态的 [OnboardingContent]。
 *
 * @param vm 由 Hilt 注入的 [OnboardingViewModel]
 */
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

/**
 * Onboarding 表单的无状态内容层，便于预览和测试。
 *
 * 单页布局（无分步索引）：名字输入 + 生日选择 + 性别选择，CTA 钉在底部。
 */
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

    // 本地函数：弹出系统 DatePickerDialog；最大日期限制为今天，防止选未来日期
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

/**
 * 带标签的表单字段容器，将标签与具体输入控件垂直排列。
 *
 * @param label   字段标签文字
 * @param content 实际输入控件（InputBox、SWSegmentedPicker 等）
 */
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
