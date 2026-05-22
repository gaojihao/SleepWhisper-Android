/**
 * SleepingScreen.kt — 睡眠进行中界面（UI 层 / features/sleeping）
 *
 * 用途：宝宝进入睡眠后的全屏沉浸式守护页，展示计时器、音频状态卡与唤醒按钮。
 * 主题：强制使用 [SWScheme.DARK]（深蓝星空调色板），与全局 ThemeProvider 完全解耦，
 *       确保夜间视觉一致性，不受用户系统深色/浅色模式影响。
 * 用户交互流程：
 *   1. 由共享元素动画（MorphingHeroSurface → sleepHeroDestination）过渡进入；
 *   2. 单击屏幕任意区域切换"调暗（dim）"状态，减弱 UI 干扰；
 *   3. **长按中央 WakeButton（阈值 = SWMotion.longPressMs）** 才结束睡眠，防误触；
 *   4. 音频播放时显示 PlayerStatusCard，可点击展开 PlayerSheet；
 *   5. PausePill 支持暂停 / 继续白噪音播放。
 */
package com.lizhi1026.sleepwhisper.features.sleeping

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.lizhi1026.sleepwhisper.R
import com.lizhi1026.sleepwhisper.core.audio.PlayerState
import com.lizhi1026.sleepwhisper.core.strings.audioPresetNameKey
import com.lizhi1026.sleepwhisper.core.visualkit.LocalReduceMotion
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.features.player.PlayerSheet
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor
import com.lizhi1026.sleepwhisper.core.visualkit.SWFont
import com.lizhi1026.sleepwhisper.core.visualkit.SWGradient
import com.lizhi1026.sleepwhisper.core.visualkit.SWMotion
import com.lizhi1026.sleepwhisper.core.visualkit.SWRadius
import com.lizhi1026.sleepwhisper.core.visualkit.SWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWSpacing
import com.lizhi1026.sleepwhisper.core.visualkit.floatingY
import com.lizhi1026.sleepwhisper.core.visualkit.components.AudioWaveform
import com.lizhi1026.sleepwhisper.core.visualkit.components.ElevationLevel
import com.lizhi1026.sleepwhisper.core.visualkit.components.GlassCard
import com.lizhi1026.sleepwhisper.core.visualkit.components.LiquidProgressRing
import com.lizhi1026.sleepwhisper.core.visualkit.components.PulseRing
import com.lizhi1026.sleepwhisper.core.visualkit.components.NightSkyCanvas
import com.lizhi1026.sleepwhisper.core.visualkit.components.sleepHeroDestination
import com.lizhi1026.sleepwhisper.model.AudioPreset
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 睡眠进行中的主屏幕 Composable。
 *
 * 强制注入 [SWScheme.DARK] 主题，与全局 ThemeProvider 解耦，确保星空调色板始终生效。
 * 共享元素参数 [sharedScope] / [animScope] 对接 MorphingHeroSurface 的过渡动画。
 *
 * @param sharedScope 共享元素动画作用域，由父级 SharedTransitionLayout 提供
 * @param animScope   AnimatedVisibility 作用域，控制共享元素在本屏的可见性过渡
 * @param vm          由 Hilt 注入的 [SleepingViewModel]
 */
@OptIn(
    androidx.compose.animation.ExperimentalSharedTransitionApi::class
)
@Composable
fun SleepingScreen(
    sharedScope: androidx.compose.animation.SharedTransitionScope,
    animScope: androidx.compose.animation.AnimatedVisibilityScope,
    vm: SleepingViewModel = hiltViewModel()
) {
    val ongoing by vm.ongoingSleep.observeAsState(null)
    val playerState by vm.playerState.observeAsState(PlayerState.Idle)
    val baby by vm.baby.observeAsState(null)

    // 当前时刻（毫秒），用于计算已睡时长：elapsedSec = (nowMs - startAt) / 1000
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    // 计时器心跳：每秒更新 nowMs，仅在生命周期 STARTED 时运行，后台自动暂停
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                nowMs = System.currentTimeMillis()
                delay(1000) // 1 秒间隔，驱动计时器 recomposition
            }
        }
    }

    // 已睡秒数：会话存在时实时计算，否则为 0（尚未开始）
    val elapsedSec = ongoing?.let { (nowMs - it.startAt) / 1000 } ?: 0L

    // 强制 DARK scheme：星空/冷辉光视觉方案，与系统主题解耦
    CompositionLocalProvider(LocalSWScheme provides SWScheme.DARK) {
        // morphProgress：跟踪英雄共享元素变形进度（0→1），用于星空密度渐变
        val morphProgress by remember(vm.app.morphProgress) {
            derivedStateOf { vm.app.morphProgress.value }
        }
        val morphScope = rememberCoroutineScope()
        // dimmed：单击屏幕任意处切换调暗状态，减少深夜视觉干扰
        var dimmed by remember { mutableStateOf(false) }
        var showMiniPlayer by remember { mutableStateOf(false) }
        val dimAlpha by animateFloatAsState(
            targetValue = if (dimmed) 0.24f else 1f,
            animationSpec = tween(if (LocalReduceMotion.current) 100 else 300),
            label = "dim-alpha"
        )

        // 调暗/展开播放器时拦截返回手势，优先收起浮层而非退出页面
        androidx.activity.compose.BackHandler(enabled = dimmed || showMiniPlayer) {
            when {
                showMiniPlayer -> showMiniPlayer = false
                dimmed -> dimmed = false
            }
        }

        Box(modifier = Modifier.fillMaxSize().background(SWColor.surface(SWScheme.DARK))) {
            // 星空画布：morphProgress = 1 时星点密度最大（完整 80 颗）
            NightSkyCanvas(
                modifier = Modifier.fillMaxSize(),
                morphProgress = morphProgress
            )

            // 共享元素目标锚点（600dp 不可见占位框）：为英雄圆从首页飞入提供落点
            Box(
                modifier = Modifier
                    .size(600.dp)
                    .align(Alignment.Center)
                    .sleepHeroDestination(sharedScope, animScope)
            )

            // 主内容列：以屏幕中心为锚点垂直对齐，避免 WakeButton 在小屏/大字号下溢出底部
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .padding(horizontal = SWSpacing.xl)
                    .alpha(dimAlpha)
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { dimmed = !dimmed })
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(SWSpacing.xxl, Alignment.CenterVertically)
            ) {
                TitleSection(babyName = baby?.name ?: "")

                CountdownSection(elapsedSec)

                if (playerState is PlayerState.Playing) {
                    val ps = playerState as PlayerState.Playing
                    PlayerStatusCard(
                        presetId = ps.presetId,
                        endsAt = ps.endsAt,
                        nowMs = nowMs,
                        onClick = { showMiniPlayer = true }
                    )
                }

                WakeButton(
                    onWake = {
                        // 长按完成后：先执行退出形变动画，再调用 ViewModel 结束会话
                        morphScope.launch { vm.app.endSleepMorph() }
                        vm.onWake()
                    }
                )

                if (playerState is PlayerState.Playing) {
                    PausePill(label = stringResource(R.string.sleeping_pause), onClick = vm::pausePlayer)
                } else if (playerState is PlayerState.Paused) {
                    PausePill(label = stringResource(R.string.sleeping_resume), onClick = vm::resumePlayer)
                }
            }

            if (showMiniPlayer) {
                PlayerSheet(onDismiss = { showMiniPlayer = false })
            }
        }
    }
}

/**
 * 标题区域：展示守护文案、宝宝名与"正在做梦"副标题。
 *
 * @param babyName 宝宝姓名，用于填入本地化字符串模板
 */
@Composable
private fun TitleSection(babyName: String) {
    Column(
        verticalArrangement = Arrangement.spacedBy(SWSpacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BasicText(
            text = stringResource(R.string.sleeping_title_protecting, babyName),
            style = SWFont.serif(17).copy(
                color = Color.White.copy(alpha = 0.95f),
                textAlign = TextAlign.Center,
                letterSpacing = 1.4.sp
            )
        )
        BasicText(
            text = babyName,
            style = SWFont.titleXL().copy(
                color = Color.White.copy(alpha = 0.95f),
                letterSpacing = 8.sp,
                textAlign = TextAlign.Center
            )
        )
        BasicText(
            text = stringResource(R.string.sleeping_title_dreaming),
            style = SWFont.bodyMD().copy(
                color = Color.White.copy(alpha = 0.5f),
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )
        )
    }
}

/**
 * 计时器区域：将已睡秒数格式化并以悬浮动画展示。
 *
 * 超过 1 小时切换为 64sp（displayLG），否则使用 96sp（displayXL），两者均为等宽字体。
 * floatingY 修饰符提供 ±2dp 的轻微浮动动画（周期 8 秒）。
 *
 * @param elapsedSec 已睡总秒数，由 Screen 内 LaunchedEffect 每秒计算传入
 */
@Composable
private fun CountdownSection(elapsedSec: Long) {
    val h = (elapsedSec / 3600).toInt()
    val m = ((elapsedSec % 3600) / 60).toInt()
    val s = (elapsedSec % 60).toInt()
    val displayText = if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    val moonlitSilver = Color(0xFFECF2F8)
    val style = if (h > 0)
        SWFont.displayLGTabular().copy(color = moonlitSilver)   // 64sp (fits in 280dp ring)
    else
        SWFont.displayXLTabular().copy(color = moonlitSilver)   // 96sp (only used briefly for MM:SS)
    BasicText(
        text = displayText,
        style = style,
        modifier = Modifier
            .padding(horizontal = 0.dp)
            .floatingY(amplitude = 2.dp, durationMillis = 8000)
    )
}

/**
 * 音频播放状态卡片：仅当 PlayerState 为 Playing 时显示。
 *
 * 展示当前预设名称、图标、音波动画及剩余时长（如果有定时）。
 * 点击卡片展开 PlayerSheet 底部面板。
 *
 * @param presetId  当前播放的音频预设 ID
 * @param endsAt    定时结束时间戳（毫秒），null 表示不限时
 * @param nowMs     当前时刻（毫秒），用于计算剩余分钟
 * @param onClick   点击卡片的回调
 */
@Composable
private fun PlayerStatusCard(presetId: String, endsAt: Long?, nowMs: Long, onClick: () -> Unit) {
    val preset = remember(presetId) { AudioPreset.byId(presetId) }
    val scheme = SWScheme.DARK
    val ctx = LocalContext.current
    val presetIconRes = remember(preset?.iconName) {
        preset?.iconName?.let { name ->
            ctx.resources.getIdentifier(name, "drawable", ctx.packageName).takeIf { it != 0 }
        }
    }
    val cardSurface = remember(preset?.auraColors?.mid) {
        val base = Color(0xFF5C4A5A).copy(alpha = 0.90f)
        preset?.auraColors?.mid?.let { aura ->
            lerp(base, aura.copy(alpha = 0.90f), 0.05f)
        } ?: base
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable { onClick() },
        cornerRadius = 18.dp,
        contentPadding = PaddingValues(horizontal = SWSpacing.md, vertical = 4.dp),
        surfaceTintOverride = cardSurface,
        elevation = ElevationLevel.MEDIUM
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SWSpacing.md)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(preset?.auraColors?.bottom ?: SWColor.primary(scheme)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = presetIconRes ?: R.drawable.ic_empty_moon),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(Color.White),
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                BasicText(
                    text = preset?.nameKey?.let { stringResource(audioPresetNameKey(it)) } ?: "",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = SWFont.titleMD().copy(color = SWColor.textPrimary(scheme))
                )
            }

            androidx.compose.foundation.layout.Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SWSpacing.xs)
            ) {
                AudioWaveform(
                    modifier = Modifier.width(36.dp).height(22.dp),
                    isPlaying = true,
                    color = SWColor.accent(scheme),
                    barCount = 8
                )
                endsAt?.let {
                    val remainingMin = ((it - nowMs) / 60_000).coerceAtLeast(0L)
                    BasicText(
                        text = stringResource(R.string.sleeping_minremaining, remainingMin),
                        maxLines = 1,
                        style = SWFont.labelMD().copy(color = SWColor.textSecondary(scheme))
                    )
                }
            }
        }
    }
}

/**
 * 唤醒按钮（中央长按按钮）：防误触设计，必须持续长按满 [SWMotion.longPressMs] 毫秒才触发结束睡眠。
 *
 * 视觉层：外层 PulseRing（脉冲涟漪）→ LiquidProgressRing（液态进度环，随长按充填）→
 *         内层 112dp 渐变圆形按钮。
 * 交互：按下时启动 fillProgress 动画，松手若未达 0.99 则弹回 0；
 *       达到后等待 420ms（弹跳缓冲）再回调 onWake。
 *
 * @param onWake 长按完成后的回调，由父级触发形变动画并调用 ViewModel
 */
@Composable
private fun WakeButton(onWake: () -> Unit) {
    val fillProgress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val scheme = SWScheme.DARK

    Box(
        modifier = Modifier
            .size(180.dp)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitPointerEvent(PointerEventPass.Main)
                        if (down.changes.any { it.pressed }) {
                            // 手指按下：开始填充动画，时长 = longPressMs
                            val job = scope.launch {
                                fillProgress.animateTo(
                                    1f,
                                    tween(durationMillis = SWMotion.longPressMs.toInt())
                                )
                                if (fillProgress.value >= 0.99f) {
                                    delay(420) // 弹跳缓冲，给 LiquidProgressRing 留完成感
                                    onWake()
                                }
                            }
                            while (true) {
                                val ev = awaitPointerEvent(PointerEventPass.Main)
                                if (ev.changes.all { !it.pressed }) {
                                    if (fillProgress.value < 0.99f) {
                                        // 提前松手：取消动画并弹回 0
                                        job.cancel()
                                        scope.launch { fillProgress.animateTo(0f, tween(200)) }
                                    }
                                    break
                                }
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        PulseRing(color = SWColor.accent(scheme), radius = 90.dp)
        LiquidProgressRing(
            progress = fillProgress.value,
            color = SWColor.accent(scheme),
            radius = 65.dp
        )
        Box(
            modifier = Modifier
                .size(112.dp)
                .clip(CircleShape)
                .background(SWGradient.auroraGlow(scheme)),
            contentAlignment = Alignment.Center
        ) {
            BasicText(
                text = stringResource(R.string.sleeping_wake_longpress),
                style = SWFont.labelMD().copy(
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}

/**
 * 暂停 / 继续胶囊按钮：半透明毛玻璃样式，音频播放时显示"暂停"，暂停时显示"继续"。
 *
 * @param label   按钮文字（由调用方传入本地化字符串）
 * @param onClick 点击回调
 */
@Composable
private fun PausePill(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(SWRadius.pill))
            .background(Color.White.copy(alpha = 0.06f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.18f),
                shape = RoundedCornerShape(SWRadius.pill)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = SWSpacing.lg, vertical = SWSpacing.sm)
    ) {
        BasicText(
            text = label,
            style = SWFont.bodyMD().copy(color = Color.White.copy(alpha = 0.7f))
        )
    }
}
