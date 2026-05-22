package com.lizhi1026.sleepwhisper.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lizhi1026.sleepwhisper.core.visualkit.SWMotion
import com.lizhi1026.sleepwhisper.core.visualkit.components.PulseIntensity
import com.lizhi1026.sleepwhisper.core.visualkit.components.SectionLabel
import com.lizhi1026.sleepwhisper.core.visualkit.components.sleepHeroOrigin
import androidx.hilt.navigation.compose.hiltViewModel
import com.lizhi1026.sleepwhisper.R
import com.lizhi1026.sleepwhisper.core.audio.PlayerState
import com.lizhi1026.sleepwhisper.features.player.PlayerSheet
import com.lizhi1026.sleepwhisper.features.player.syncToPlayer
import com.lizhi1026.sleepwhisper.core.strings.audioPresetNameKey
import com.lizhi1026.sleepwhisper.core.strings.greetingForHour
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.LocalHeroBackdropController
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor
import com.lizhi1026.sleepwhisper.core.visualkit.SWFont
import com.lizhi1026.sleepwhisper.core.visualkit.SWGradient
import kotlinx.coroutines.launch
import com.lizhi1026.sleepwhisper.core.visualkit.SWRadius
import com.lizhi1026.sleepwhisper.core.visualkit.SWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWSpacing
import com.lizhi1026.sleepwhisper.core.visualkit.components.innerHighlight
import com.lizhi1026.sleepwhisper.core.visualkit.floatingY
import com.lizhi1026.sleepwhisper.core.visualkit.components.AudioWaveform
import com.lizhi1026.sleepwhisper.core.visualkit.components.AuroraBackdrop
import com.lizhi1026.sleepwhisper.core.visualkit.components.ChevronTrail
import com.lizhi1026.sleepwhisper.core.visualkit.components.ElevationLevel
import com.lizhi1026.sleepwhisper.core.visualkit.components.GlassCard
import com.lizhi1026.sleepwhisper.core.visualkit.components.Hairline
import com.lizhi1026.sleepwhisper.core.visualkit.components.PulseRing
import com.lizhi1026.sleepwhisper.core.visualkit.components.RollingNumber
import com.lizhi1026.sleepwhisper.model.DiaperEvent
import com.lizhi1026.sleepwhisper.model.FeedingEvent.FeedingMethod
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * 首页主屏幕（features/home 层）
 *
 * 职责：
 * - 展示问候语、清醒窗口倒计时、睡眠 CTA 圆按钮、正在播放卡片与快捷操作网格。
 * - 接收 [SharedTransitionScope] 与 [AnimatedVisibilityScope]，通过 `sleepHeroOrigin` 修饰符
 *   与 SleepingScreen 实现 Hero 共享元素过渡动画。
 * - 通过 [HomeViewModel] 观察 `baby / cachedWakeWindow / playerState / currentPreset /
 *   hasSeenHints` 五条 LiveData；所有写操作委托给 `vm.app.xxx()`（AppStateContainer 门面）。
 * - 用户交互：点击 SleepCTA → 启动睡眠并触发 Hero 跳转；长按 SleepCTA → 弹出 SleepTypePicker；
 *   快捷瓦片一键记录母乳左/右、奶瓶（弹出计量弹层）、纸尿裤。
 */
@OptIn(
    androidx.compose.animation.ExperimentalSharedTransitionApi::class
)
@Composable
fun HomeScreen(
    sharedScope: androidx.compose.animation.SharedTransitionScope,
    animScope: androidx.compose.animation.AnimatedVisibilityScope,
    vm: HomeViewModel = hiltViewModel()
) {
    val scheme = LocalSWScheme.current
    // 观察婴儿档案、清醒窗口、播放器状态、当前音效预设及新手提示是否已读
    val baby by vm.baby.observeAsState(null)
    val wakeWindow by vm.cachedWakeWindow.observeAsState(null)
    val playerState by vm.playerState.observeAsState(PlayerState.Idle)
    val currentPreset by vm.currentPreset.observeAsState(null)
    val hasSeenHints by vm.hasSeenHints.observeAsState(false)

    // 每 60 秒轮询当前小时值，跨越整点时（如 19:00）重新触发 LaunchedEffect，
    // 使傍晚环境光提示能在用户保持首页不退出的情况下自动切换。
    val hour by androidx.compose.runtime.produceState(
        initialValue = java.time.LocalTime.now().hour
    ) {
        while (true) {
            value = java.time.LocalTime.now().hour
            kotlinx.coroutines.delay(60_000L)
        }
    }

    val heroBackdrop = LocalHeroBackdropController.current
    LaunchedEffect(hour, currentPreset) {
        // 优先级：正在播放的预设 mid 颜色 > 傍晚时段橙色提示 > null。
        // 预设播放时其光晕颜色胜出；停止后自动恢复时段提示颜色。
        val eveningHint = if (hour >= 19 || hour < 6) Color(0xFFFFB088) else null
        heroBackdrop.syncToPlayer(currentPreset = currentPreset, fallback = eveningHint)
    }

    // 各浮层显示状态
    var showBottleSheet by remember { mutableStateOf(false) }
    var showSleepTypePicker by remember { mutableStateOf(false) }
    var showPlayerSheet by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        AuroraBackdrop()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SWSpacing.lg)
                .padding(top = SWSpacing.xxs),
            verticalArrangement = Arrangement.spacedBy(SWSpacing.sm)
        ) {
            GreetingSection(babyName = baby?.name, dobMs = baby?.dateOfBirth, hour = hour)

            // 仅当 ViewModel 拿到清醒窗口数据时才展示（首次启动可能为 null）
            wakeWindow?.let { (remaining, total) ->
                WakeWindowCard(remaining = remaining, total = total)
            }

            // morphProgress 由 AppStateContainer 控制，防止 Hero 动画期间重复触发
            val morphScope = rememberCoroutineScope()
            val morphProgress by remember(vm.app.morphProgress) {
                derivedStateOf { vm.app.morphProgress.value }
            }
            SleepCTA(
                sharedScope = sharedScope,
                animScope = animScope,
                onTap = {
                    // 点击：仅在未处于过渡状态时才启动 morphProgress 动画并调用 startSleep()
                    if (morphProgress <= 0f || morphProgress >= 1f) {
                        morphScope.launch { vm.app.beginSleepMorph() }
                        vm.onTapSleep()
                    }
                },
                onLongPress = { showSleepTypePicker = true } // 长按：弹出睡眠类型选择器
            )

            // 新手提示：用户未读时展示，调用 vm::onDismissHints 后永久隐藏
            if (!hasSeenHints) {
                OnboardingHintsCard(onDismiss = vm::onDismissHints)
            }

            NowPlayingCard(
                presetIconName = currentPreset?.iconName,
                presetName = currentPreset?.let { stringResource(audioPresetNameKey(it.nameKey)) },
                isPlaying = playerState is PlayerState.Playing,
                onClick = { showPlayerSheet = true }
            )

            // 快捷操作网格：奶瓶需先填写毫升数，故通过回调弹出 BottleAmountSheet
            QuickActionsGrid(vm, onBottleTap = { showBottleSheet = true })

            Spacer(Modifier.height(SWSpacing.lg))
        }

        if (showBottleSheet) {
            // 奶瓶喂养：确认时带毫升数记录，跳过时毫升数为 null
            BottleAmountSheet(
                onConfirm = { ml -> vm.onRecordFeeding(FeedingMethod.BOTTLE, ml = ml) },
                onSkip = { vm.onRecordFeeding(FeedingMethod.BOTTLE, ml = null) },
                onDismiss = { showBottleSheet = false }
            )
        }
        if (showSleepTypePicker) {
            // 睡眠类型选择：由 AppStateContainer 推断默认类型（小睡/夜睡/接觉）
            SleepTypePicker(
                suggestedType = vm.app.defaultSleepType(),
                onPick = { type -> vm.onPickSleepType(type) },
                onDismiss = { showSleepTypePicker = false }
            )
        }
        if (showPlayerSheet) {
            // 播放器面板：全屏覆盖，关闭即回到首页
            PlayerSheet(onDismiss = { showPlayerSheet = false })
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// 子组合函数（Section composables）
// ──────────────────────────────────────────────────────────────────────────────

/**
 * 顶部问候区：根据当前小时显示时段问候语，展示宝宝姓名与出生天数。
 *
 * @param babyName 宝宝姓名，为 null 时只显示问候语。
 * @param dobMs 出生时间戳（毫秒），用于计算出生天数。
 * @param hour 当前小时（0-23），由 HomeScreen 每分钟刷新传入。
 */
@Composable
private fun GreetingSection(babyName: String?, dobMs: Long?, hour: Int) {
    val scheme = LocalSWScheme.current
    // 将出生毫秒时间戳换算为距今天数，仅在 dobMs 变化时重算
    val daysOld = remember(dobMs) {
        dobMs?.let {
            val birth = LocalDate.ofEpochDay(it / 86_400_000L)
            val today = LocalDate.now(ZoneId.systemDefault())
            ChronoUnit.DAYS.between(birth, today).toInt().coerceAtLeast(0)
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(SWSpacing.xxs)) {
        BasicText(
            text = stringResource(greetingForHour(hour)),
            style = SWFont.serif(22).copy(color = SWColor.textSecondary(scheme))
        )
        if (!babyName.isNullOrBlank() || daysOld != null) {
            Row(verticalAlignment = Alignment.Bottom) {
                babyName?.takeIf { it.isNotBlank() }?.let {
                    BasicText(
                        text = it,
                        modifier = if (daysOld != null) Modifier.weight(1f, fill = false) else Modifier,
                        style = SWFont.titleXL().copy(color = SWColor.textPrimary(scheme)),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                daysOld?.let {
                    BasicText(
                        text = stringResource(R.string.home_daycount, it),
                        style = SWFont.titleMD().copy(color = SWColor.textTertiary(scheme)),
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
            }
        }
        // 短横线装饰，类似书章节标题下方的章节线，仅作视觉分隔而非完整分割线。
        Hairline(modifier = Modifier.width(52.dp).padding(top = SWSpacing.xxs))
    }
}

/**
 * 清醒窗口卡片：显示剩余清醒分钟数及进度条。
 *
 * @param remaining 剩余清醒分钟数；≤20 时变橙色警告，≤0 时变红色危险提示。
 * @param total 当前宝宝年龄对应的清醒窗口总分钟数，用于计算进度比例。
 */
@Composable
private fun WakeWindowCard(remaining: Int, total: Int) {
    val scheme = LocalSWScheme.current
    // 根据剩余时间决定颜色：正常→主色，接近→警告橙，超时→危险红
    val color = when {
        remaining > 20 -> SWColor.textPrimary(scheme)
        remaining > 0 -> SWColor.warning(scheme)
        else -> SWColor.danger(scheme)
    }
    GlassCard(
        modifier = Modifier.fillMaxWidth().floatingY(),
        contentPadding = PaddingValues(horizontal = SWSpacing.lg, vertical = SWSpacing.md),
        elevation = ElevationLevel.HERO,
        hero = true
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(SWSpacing.xs)) {
            SectionLabel(stringResource(R.string.home_wakewindow_title))
            Row(verticalAlignment = Alignment.Bottom) {
                RollingNumber(
                    value = remaining,
                    style = SWFont.displayLGTabular().copy(
                        color = color,
                        fontSize = 52.sp,
                        lineHeight = 52.sp
                    )
                )
                Spacer(Modifier.width(SWSpacing.xs))
                BasicText(
                    text = stringResource(R.string.home_wakewindow_unit),
                    style = androidx.compose.ui.text.TextStyle(
                        fontSize = 22.sp,
                        color = SWColor.textSecondary(scheme)
                    ),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(2.5f.dp))
                    .background(SWColor.border(scheme).copy(alpha = 0.3f))
            ) {
                val frac = (remaining.toFloat() / total.coerceAtLeast(1)).coerceIn(0f, 1f)
                // 渐变进度条：以 auroraGlow 渐变色填充，宽度按剩余比例缩放
                Box(
                    modifier = Modifier
                        .fillMaxWidth(frac)
                        .height(5.dp)
                        .clip(RoundedCornerShape(2.5f.dp))
                        .background(SWGradient.auroraGlow(scheme))
                )
            }
        }
    }
}

/**
 * 正在播放卡片：展示当前音效预设图标、名称及播放波形动画。
 *
 * @param presetIconName drawable 资源名称字符串（运行时动态解析），为 null 时显示默认月亮图标。
 * @param presetName 预设显示名称，为 null 时显示占位文案。
 * @param isPlaying 是否正在播放，控制波形动画显示。
 * @param onClick 点击卡片后回调，父级负责弹出 PlayerSheet。
 */
@Composable
private fun NowPlayingCard(
    presetIconName: String?,
    presetName: String?,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val scheme = LocalSWScheme.current
    val ctx = androidx.compose.ui.platform.LocalContext.current
    // 动态查找 drawable 资源 ID，找不到时 fallback 为 null（后续使用默认月亮图标）
    val presetIconRes = remember(presetIconName) {
        presetIconName?.let { name ->
            ctx.resources.getIdentifier(name, "drawable", ctx.packageName).takeIf { it != 0 }
        }
    }
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        contentPadding = PaddingValues(SWSpacing.md)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SWSpacing.md)
        ) {
            // 左侧图标圆圈（52dp）：有预设图标时显示，否则显示默认月亮占位图
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        if (scheme == SWScheme.DAY) SWColor.softLilac(scheme)
                        else SWColor.primary(scheme)
                    )
                    .innerHighlight(cornerRadius = 26.dp),
                contentAlignment = Alignment.Center
            ) {
                val iconRes = presetIconRes ?: R.drawable.ic_empty_moon
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = iconRes),
                    contentDescription = null,
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                        if (scheme == SWScheme.DAY) SWColor.primary(scheme) else androidx.compose.ui.graphics.Color.White
                    ),
                    modifier = Modifier.size(24.dp)
                )
            }
            // 中间：预设名称 + 播放时显示音频波形动画
            Column(modifier = Modifier.weight(1f)) {
                BasicText(
                    text = presetName ?: stringResource(R.string.home_nowplaying_placeholder),
                    style = SWFont.titleMD().copy(color = SWColor.textPrimary(scheme))
                )
                if (isPlaying) {
                    AudioWaveform(
                        modifier = Modifier.height(18.dp).padding(top = 4.dp),
                        isPlaying = true,
                        color = SWColor.accent(scheme),
                        barCount = 24
                    )
                }
            }
            // 右侧：ChevronTrail 替换原有静态箭头图标，提供更丰富的视觉引导
            ChevronTrail()
        }
    }
}

/**
 * 快捷操作网格：两行两列瓦片，一键记录母乳左/右、奶瓶、纸尿裤。
 *
 * @param vm 首页 ViewModel，写操作通过 vm.onRecordFeeding / vm.onRecordDiaper 委托给 AppStateContainer。
 * @param onBottleTap 奶瓶瓦片点击回调，父级负责弹出 BottleAmountSheet 让用户填写毫升数。
 */
@Composable
private fun QuickActionsGrid(vm: HomeViewModel, onBottleTap: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(SWSpacing.sm)) {
        // 第一行：母乳左乳、母乳右乳
        Row(horizontalArrangement = Arrangement.spacedBy(SWSpacing.sm)) {
            QuickActionTile(
                label = stringResource(R.string.quickaction_breastleft),
                tint = TileTint.PEACH,
                iconRes = R.drawable.ic_action_breast_left,
                onTap = { vm.onRecordFeeding(FeedingMethod.BREAST_LEFT) },
                modifier = Modifier.weight(1f)
            )
            QuickActionTile(
                label = stringResource(R.string.quickaction_breastright),
                tint = TileTint.PEACH,
                iconRes = R.drawable.ic_action_breast_right,
                onTap = { vm.onRecordFeeding(FeedingMethod.BREAST_RIGHT) },
                modifier = Modifier.weight(1f)
            )
        }
        // 第二行：奶瓶（点击弹出计量弹层，长按直接记录不填量）、纸尿裤（默认湿尿片类型）
        Row(horizontalArrangement = Arrangement.spacedBy(SWSpacing.sm)) {
            QuickActionTile(
                label = stringResource(R.string.quickaction_bottle),
                tint = TileTint.LILAC,
                iconRes = R.drawable.ic_action_bottle,
                onTap = onBottleTap,
                onLongPress = { vm.onRecordFeeding(FeedingMethod.BOTTLE, ml = null) },
                modifier = Modifier.weight(1f)
            )
            QuickActionTile(
                label = stringResource(R.string.quickaction_diaper),
                tint = TileTint.MINT,
                iconRes = R.drawable.ic_action_diaper,
                onTap = { vm.onRecordDiaper(DiaperEvent.DiaperType.WET) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * 睡眠 CTA 圆按钮：首页核心交互入口。
 *
 * - **点击**：调用 `onTap`，父级触发 `app.beginSleepMorph()` + `app.startSleep()`，
 *   同时通过 `sleepHeroOrigin` 修饰符启动与 SleepingScreen 的 Hero 共享元素过渡。
 * - **长按**：调用 `onLongPress`，父级弹出 [SleepTypePicker] 让用户选择小睡/夜睡/接觉。
 *
 * @param sharedScope 来自导航宿主的 SharedTransitionScope，用于 Hero 动画。
 * @param animScope 来自导航宿主的 AnimatedVisibilityScope，配合 SharedTransitionScope 使用。
 * @param onTap 单击回调。
 * @param onLongPress 长按回调。
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SleepCTA(
    sharedScope: SharedTransitionScope,
    animScope: AnimatedVisibilityScope,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    val scheme = LocalSWScheme.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = SWSpacing.xs),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape)
                .background(SWColor.surfaceElevated(scheme).copy(alpha = 0.32f))
                .innerHighlight(cornerRadius = 100.dp)
        )
        PulseRing(
            color = SWColor.accent(scheme),
            radius = 80.dp,
            intensity = PulseIntensity.STRONG
        )
        Box(
            modifier = Modifier
                .size(120.dp)
                .sleepHeroOrigin(sharedScope, animScope)
                .clip(CircleShape)
                .background(SWGradient.auroraGlow(scheme))
                .innerHighlight(cornerRadius = 60.dp)
                .pointerInput(onTap, onLongPress) {
                    detectTapGestures(
                        onLongPress = { onLongPress() },
                        onTap = { onTap() }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_moon_zzz),
                    contentDescription = null,
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.White),
                    modifier = Modifier.size(28.dp)
                )
                BasicText(
                    text = stringResource(R.string.home_sleepcta),
                    style = SWFont.labelMD().copy(
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                )
            }
        }
    }
}
