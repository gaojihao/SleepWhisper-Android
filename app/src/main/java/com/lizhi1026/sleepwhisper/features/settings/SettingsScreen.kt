/**
 * SettingsScreen — 应用设置主屏幕（UI 层 / features/settings）
 *
 * 布局分为五个区域卡片：
 *   1. **Baby**        — 展示宝宝姓名与精确年龄（年/月/天）。
 *   2. **Playback**    — 默认计时时长步进器、入睡后渐弱时长步进器、触觉反馈开关。
 *   3. **Cry Detection** — 哭声检测总开关（开启前校验 RECORD_AUDIO 运行时权限）；
 *                         开启后显示灵敏度分贝阈值步进器。
 *   4. **Appearance**  — AUTO / LIGHT / DARK 三段式主题选择器、夜间模式时段展示、
 *                        夜间预览开关（直接调 themeProvider.setForceNightPreview）。
 *   5. **About**       — 当前版本号与品牌 tagline。
 *
 * 权限处理：
 *   开启哭声检测时先检查 RECORD_AUDIO 是否已授权；未授权则通过
 *   [rememberLauncherForActivityResult] 发起运行时权限请求，
 *   用户拒绝时开关保持关闭状态。
 */
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

/**
 * 设置主屏 Composable，从 Hilt ViewModel 读取全部状态并分区渲染。
 *
 * @param vm 通过 Hilt 注入的 [SettingsViewModel]。
 */
@Composable
fun SettingsScreen(vm: SettingsViewModel = hiltViewModel()) {
    val scheme = LocalSWScheme.current
    val context = LocalContext.current
    val baby by vm.baby.observeAsState(null)
    // settings 未就绪时使用 DEFAULT 兜底，避免 null 检查冗余。
    val s by vm.settings.observeAsState(UserSettings.DEFAULT)
    // 夜间预览开关状态，来自 themeProvider，不持久化到 UserSettings。
    val forceNightPreview by vm.forceNightPreview.observeAsState(false)

    // 麦克风权限请求 launcher；授权成功后写入 cryDetectionEnabled = true，
    // 拒绝时开关保持原始关闭状态，用户可从系统设置或再次点击重试。
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
                style = SWFont.serif(28).copy(color = SWColor.textPrimary(scheme)),
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
                            // 开启检测前先检查 RECORD_AUDIO 权限。
                            val granted = ContextCompat.checkSelfPermission(
                                context, android.Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                // 已有权限，直接开启。
                                vm.update { it.copy(cryDetectionEnabled = true) }
                            } else {
                                // Defer enabling until the user grants the runtime permission;
                                // the launcher callback writes back the toggled value on success.
                                // 权限未授予，发起系统权限弹窗；成功后由 launcher 回调写入。
                                micPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                            }
                        } else {
                            // 关闭检测：同步更新设置并停止后台检测服务。
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
                // AUTO/LIGHT/DARK 三段选择器，选中后立即通过 ViewModel 持久化。
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
                    // 夜间预览开关直接调 themeProvider，不写入 UserSettings（临时预览状态）。
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

/**
 * 将宝宝年龄格式化为本地化字符串（年/月/天 三级降级）。
 * 有年份时展示"X 岁 X 月 X 天"；无年份时展示"X 月 X 天"；否则只展示天数。
 *
 * @param b 宝宝档案数据对象。
 * @return 格式化后的年龄字符串。
 */
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

/**
 * 通用分区卡片，提供统一的圆角玻璃卡片容器和分区标题。
 *
 * @param title   分区标题文本（对应各设置组名称）。
 * @param content 卡片内容 Composable lambda。
 */
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

/**
 * 通用开关行：左侧文字标签，右侧以 ●/○ 符号表示开/关状态。
 * 整行区域可点击，最小高度 48dp 满足无障碍触控尺寸要求。
 *
 * @param label    功能描述文本。
 * @param on       当前开关状态。
 * @param onToggle 点击切换回调。
 */
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
 * 左侧标签 + 右侧「−  当前值  +」三控件组合，模拟 iOS 步进器行为。
 *
 * @param label       功能描述文本。
 * @param valueText   当前值的格式化字符串（如"30 分钟"）。
 * @param onDecrement 点击 − 按钮时的回调，调用方负责边界检查。
 * @param onIncrement 点击 + 按钮时的回调，调用方负责边界检查。
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

/**
 * 步进器的单个圆形按钮（+ 或 −）。
 * 32dp 圆形背景，满足最小触控区域推荐尺寸。
 *
 * @param label   按钮显示文字（"+" 或 "−"）。
 * @param onClick 点击回调。
 */
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
