/**
 * PlayerScreen — 白噪音播放器主屏幕（UI 层 / features/player）
 *
 * 职责：
 *   - 展示可供选择的 [AudioPreset] 列表，按宝宝月龄分为「推荐」和「全部」两个区段。
 *   - 顶部 [DurationPickerCard] 提供 15/30/45/60/90 分钟计时芯片，
 *     选中值持久化至 [UserSettings.defaultTimerMinutes]。
 *   - 点击预设行：未播放则调用 [PlayerViewModel.playPreset]；已播放则调用
 *     [PlayerViewModel.stop] 切换暂停（tap-to-toggle）。
 *   - 通过 [AmbientSync] 扩展函数将当前预设的 AuraColors 写入
 *     [HeroBackdropController]，驱动全屏背景跟随预设变色。
 *
 * 形态双模式：
 *   - `embedded = false`（默认）：独立 Tab 全屏展示，带 [AuroraBackdrop] 和 [DragHandle]。
 *   - `embedded = true`：作为 [PlayerSheet] 的内嵌内容，隐藏 Backdrop 与 DragHandle，
 *     由 ModalBottomSheet 本身提供拖拽手柄。
 */
package com.lizhi1026.sleepwhisper.features.player

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lizhi1026.sleepwhisper.R
import com.lizhi1026.sleepwhisper.core.strings.audioPresetNameKey
import com.lizhi1026.sleepwhisper.core.visualkit.LocalHeroBackdropController
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor
import com.lizhi1026.sleepwhisper.core.visualkit.SWFont
import com.lizhi1026.sleepwhisper.core.visualkit.SWGradient
import com.lizhi1026.sleepwhisper.core.visualkit.SWRadius
import com.lizhi1026.sleepwhisper.core.visualkit.SWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWSpacing
import com.lizhi1026.sleepwhisper.core.visualkit.components.AudioWaveform
import com.lizhi1026.sleepwhisper.core.visualkit.components.AuraParticles
import com.lizhi1026.sleepwhisper.core.visualkit.components.AuroraBackdrop
import com.lizhi1026.sleepwhisper.core.visualkit.components.ChevronTrail
import com.lizhi1026.sleepwhisper.core.visualkit.components.DragHandle
import com.lizhi1026.sleepwhisper.core.visualkit.components.GlassCard
import com.lizhi1026.sleepwhisper.core.visualkit.components.PresetAuraOrb
import com.lizhi1026.sleepwhisper.core.visualkit.components.innerHighlight
import com.lizhi1026.sleepwhisper.core.visualkit.components.SectionLabel as SharedSectionLabel
import com.lizhi1026.sleepwhisper.model.AudioPreset

/**
 * 播放器主 Composable。
 *
 * @param embedded 是否以嵌入模式渲染：
 *   - `false`（默认）：独立 Tab，渲染全屏 AuroraBackdrop 和顶部 DragHandle。
 *   - `true`：被 [PlayerSheet] 嵌套调用，略去 Backdrop 与 DragHandle，避免重复渲染。
 * @param vm 通过 Hilt 注入的 [PlayerViewModel]。
 */
@Composable
fun PlayerScreen(
    embedded: Boolean = false,
    vm: PlayerViewModel = hiltViewModel()
) {
    val scheme = LocalSWScheme.current
    val baby by vm.baby.observeAsState(null)
    val settings by vm.settings.observeAsState(null)
    val current by vm.currentPreset.observeAsState(null)
    // 使用 currentPreset 作为"当前选中"的唯一真实来源——
    // 可以跨越 Loading / FadingOut / Paused 等状态转换，
    // 避免直接 cast PlayerState.Playing 导致状态丢失。
    val currentId = current?.id

    // 从设置读取当前默认计时分钟；未就绪时回退 30 分钟。
    val minutesNow = settings?.defaultTimerMinutes ?: 30
    // 根据宝宝月龄划分推荐列表；baby 为 null 时月龄视为 0。
    val ageMonths = remember(baby) { baby?.ageInMonths() ?: 0 }
    val rec = vm.recommended(ageMonths)
    // "全部"区段 = 全量预设中去掉推荐部分。
    val rest = vm.allPresets - rec.toSet()

    val heroBackdrop = LocalHeroBackdropController.current
    // 从 HeroBackdropController 中派生当前 ambient 颜色，
    // 用于为未播放的预设行卡片添加微弱的环境色渲染。
    val ambient by remember(heroBackdrop) {
        derivedStateOf { heroBackdrop.ambient }
    }

    // 将当前激活预设的 mid-aura 颜色同步到 HeroBackdropController，
    // 实现背景随预设变色。current 变为 null（停止播放）时，
    // fallback=null 由 Home 屏的 LaunchedEffect 负责恢复时间段默认色。
    // 独立 Tab 与嵌入 Sheet 两种形态均需同步，故以 embedded 为依赖之一。
    LaunchedEffect(current, embedded) {
        heroBackdrop.syncToPlayer(currentPreset = current, fallback = null)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!embedded) {
            AuroraBackdrop()
        }
        Column(modifier = Modifier.fillMaxSize()) {
            if (!embedded) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                    DragHandle()
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = SWSpacing.lg),
                contentPadding = PaddingValues(top = SWSpacing.md, bottom = SWSpacing.huge),
                verticalArrangement = Arrangement.spacedBy(SWSpacing.xl)
            ) {
                item {
                    Header(ageMonths = ageMonths, babyName = baby?.name)
                }

                item {
                    DurationPickerCard(
                        selected = minutesNow,
                        onSelect = { vm.saveDefaultTimer(it) }
                    )
                }

                if (rec.isNotEmpty()) {
                    item {
                        SharedSectionLabel(stringResource(R.string.player_section_recommended, ageMonths))
                    }
                    items(rec, key = { it.id }) { p ->
                        PresetRow(p, currentId == p.id, ambient) { onTap(p, minutesNow, vm) }
                    }
                }

                item {
                    SharedSectionLabel(stringResource(R.string.player_section_all))
                }
                items(rest, key = { it.id }) { p ->
                    PresetRow(p, currentId == p.id, ambient) { onTap(p, minutesNow, vm) }
                }
            }
        }
        // Aura 粒子浮层——仅在有激活预设时显示，绘制在 LazyColumn 之上，
        // 但不拦截触摸事件，点击穿透到底层列表项。
        current?.let { activePreset ->
            AuraParticles(
                tint = activePreset.auraColors.top,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * 屏幕顶部标题区：显示"播放器"大标题和宝宝月龄副标题。
 *
 * @param ageMonths 宝宝当前月龄，用于副标题文案。
 * @param babyName  宝宝姓名（保留参数，供未来个性化文案扩展）。
 */
@Composable
private fun Header(ageMonths: Int, babyName: String?) {
    val scheme = LocalSWScheme.current
    Column(verticalArrangement = Arrangement.spacedBy(SWSpacing.xs)) {
        BasicText(
            text = stringResource(R.string.player_header),
            style = SWFont.serif(22).copy(color = SWColor.textPrimary(scheme))
        )
        BasicText(
            text = stringResource(R.string.player_subheader, "$ageMonths").uppercase(),
            style = SWFont.labelMD().copy(
                color = SWColor.textSecondary(scheme),
                letterSpacing = 1.2.sp
            )
        )
    }
}

/**
 * 计时时长选择卡片，提供 15/30/45/60/90 分钟五个芯片。
 * 选中值实时通过 [onSelect] 回调写入 [UserSettings.defaultTimerMinutes]。
 *
 * @param selected 当前已选中的分钟数。
 * @param onSelect 用户点击芯片时的回调，参数为所选分钟数。
 */
@Composable
private fun DurationPickerCard(selected: Int, onSelect: (Int) -> Unit) {
    val scheme = LocalSWScheme.current
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 18.dp,
        contentPadding = PaddingValues(SWSpacing.md)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(SWSpacing.xs)) {
            BasicText(
                text = stringResource(R.string.player_duration_label).uppercase(),
                style = SWFont.labelMD().copy(
                    color = SWColor.textSecondary(scheme),
                    letterSpacing = 1.2.sp
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth().animateContentSize(),
                horizontalArrangement = Arrangement.spacedBy(SWSpacing.xs)
            ) {
                // 遍历固定时长选项，逐一渲染芯片，权重均分水平空间。
                listOf(15, 30, 45, 60, 90).forEach { m ->
                    DurationChip(
                        minutes = m,
                        active = m == selected,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelect(m) }
                    )
                }
            }
        }
    }
}

/**
 * 单个计时时长芯片。激活时使用极光渐变背景；非激活时在深色方案下显示细边框。
 *
 * @param minutes 该芯片代表的时长（分钟）。
 * @param active  是否为当前选中状态。
 * @param modifier 外部传入的布局修饰符。
 * @param onClick 点击回调。
 */
@Composable
private fun DurationChip(
    minutes: Int,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val scheme = LocalSWScheme.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(SWRadius.pill))
            .background(
                if (active) SWGradient.auroraGlow(scheme)
                else androidx.compose.ui.graphics.Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = if (scheme == SWScheme.DAY) 0.4f else 0.05f),
                        Color.White.copy(alpha = if (scheme == SWScheme.DAY) 0.4f else 0.05f)
                    )
                )
            )
            .then(
                if (!active && scheme != SWScheme.DAY)
                    Modifier.border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(SWRadius.pill))
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(vertical = SWSpacing.xs),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = stringResource(R.string.player_duration_minutes, minutes),
            style = SWFont.labelMD().copy(
                color = if (active) Color.White else SWColor.textSecondary(scheme),
                textAlign = TextAlign.Center
            )
        )
    }
}

/**
 * 预设列表行：显示预设的 Aura 光球、名称、适用月龄范围及播放状态指示器。
 * 播放中显示 [AudioWaveform] 波形动画；未播放时显示 [ChevronTrail] 箭头引导图标。
 * 未激活但有环境色时，卡片背景会轻微混入 ambient 色，营造沉浸感。
 *
 * @param preset    要展示的音频预设数据。
 * @param isPlaying 该预设当前是否正在播放。
 * @param ambient   当前 HeroBackdropController 的环境色（可为 null）。
 * @param onTap     点击整行时的回调。
 */
@Composable
private fun PresetRow(
    preset: AudioPreset,
    isPlaying: Boolean,
    ambient: Color?,
    onTap: () -> Unit
) {
    val scheme = LocalSWScheme.current
    // 未激活且有环境色时，将卡片底色向 ambient 方向混色 12%，视觉上更融入背景。
    val tint = if (!isPlaying && ambient != null) {
        lerp(SWColor.surfaceElevated(scheme), ambient, 0.12f)
    } else null

    GlassCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onTap),
        cornerRadius = 18.dp,
        contentPadding = PaddingValues(SWSpacing.md),
        surfaceTintOverride = tint
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SWSpacing.md)) {
            PresetAuraOrb(
                aura = preset.auraColors,
                isActive = isPlaying,
                sizeDp = 48.dp
            )
            Column(modifier = Modifier.weight(1f)) {
                BasicText(
                    text = stringResource(audioPresetNameKey(preset.nameKey)),
                    style = SWFont.titleMD().copy(color = SWColor.textPrimary(scheme))
                )
                BasicText(
                    text = stringResource(
                        R.string.player_preset_agerange,
                        preset.recommendedAgeMinMonths,
                        preset.recommendedAgeMaxMonths
                    ),
                    style = SWFont.labelSM().copy(color = SWColor.textSecondary(scheme))
                )
            }
            if (isPlaying) {
                AudioWaveform(
                    modifier = Modifier
                        .width(36.dp)
                        .height(22.dp),
                    isPlaying = true,
                    color = preset.auraColors.top,
                    barCount = 8
                )
            } else {
                ChevronTrail()
            }
        }
    }
}

/**
 * 预设行点击处理逻辑（非 Composable 普通函数）。
 * 若点击的预设已在播放，则调用 [PlayerViewModel.stop] 停止；
 * 否则调用 [PlayerViewModel.playPreset] 以当前选中的 [minutes] 启动播放。
 */
private fun onTap(preset: AudioPreset, minutes: Int, vm: PlayerViewModel) {
    if (vm.currentPreset.value?.id == preset.id) {
        vm.stop()
    } else {
        vm.playPreset(preset.id, minutes)
    }
}
