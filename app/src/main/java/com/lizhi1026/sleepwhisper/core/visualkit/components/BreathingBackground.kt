package com.lizhi1026.sleepwhisper.core.visualkit.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.lizhi1026.sleepwhisper.core.visualkit.LocalHeroBackdropController
import com.lizhi1026.sleepwhisper.core.visualkit.LocalReduceMotion
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor
import com.lizhi1026.sleepwhisper.core.visualkit.SWGradient
import com.lizhi1026.sleepwhisper.core.visualkit.SWMotion
import com.lizhi1026.sleepwhisper.core.visualkit.SWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.blendAmbient
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * # AuroraBackdrop — 全屏极光底层背景
 *
 * 所属层级：视觉层最底部（Z-order 最低），常作为所有屏幕的全屏基底。
 *
 * 视觉效果：
 * - 3 条斜向极光光带（ribbon），以 [SWMotion.backdropDriftMs]（约 18 秒）为周期
 *   做缓慢漂移，相位依次偏移 2π/3，形成柔和的轮流涌动感。
 * - 顶部叠加 20×20 胶片颗粒噪点，让画面质感高于普通渐变背景。
 * - DAY 配色：暖纸面 + 极轻柔光带；DARK 配色：深夜极光 + 紫橙光带；
 *   NIGHT 配色（凌晨护眼红光方案）：不播放动画，保持静止。
 *
 * 典型使用场景：
 * - 替代旧版 `BreathingBackground`，作为 HomeScreen / SleepingScreen 的全屏底层。
 * - 可通过 [LocalHeroBackdropController] 将音频预设颜色环境色透传给光带（混合权重 20%）。
 *
 * ⚠️ 无障碍：读取 [LocalReduceMotion]，当系统 ANIMATOR_DURATION_SCALE == 0 时
 * 动画自动暂停，光带保持静止。
 *
 * 已替换的旧 Composable：`BreathingBackground`（签名不同，旧调用方需手动迁移）。
 */
@Composable
fun AuroraBackdrop(modifier: Modifier = Modifier) {
    val scheme = LocalSWScheme.current
    val reduceMotion = LocalReduceMotion.current
    val ambient = LocalHeroBackdropController.current.ambient

    Box(
        modifier = modifier
            .fillMaxSize()
            .clearAndSetSemantics { }  // 装饰性组件，不向无障碍树暴露语义
            .background(SWColor.surface(scheme))
    ) {
        // 第 1 层：极光渐变底色（由 SWGradient.auroraBackdrop 按 scheme 选色）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SWGradient.auroraBackdrop(scheme))
        )

        // 第 2 层：3 条漂移极光光带（reduce-motion 或 NIGHT 配色时跳过）
        val animate = !reduceMotion && scheme != SWScheme.NIGHT
        if (animate) {
            // 单一无限动画驱动 3 条 ribbon 的共享 phase 值（0→1 往返，18s 周期）
            val t = rememberInfiniteTransition(label = "aurora-drift")
            val phase by t.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(SWMotion.backdropDriftMs, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "phase"
            )
            // ribbon 1：初始相位 0，与环境色混合 20%
            DriftingRibbon(phaseOffset = 0f,                           color = blendAmbient(rib1(scheme), ambient, 0.2f), phase = phase)
            // ribbon 2：初始相位偏移 2π/3（120°），形成交替涌动
            DriftingRibbon(phaseOffset = (2 * PI / 3).toFloat(),       color = blendAmbient(rib2(scheme), ambient, 0.2f), phase = phase)
            // ribbon 3：初始相位偏移 4π/3（240°），三条光带均匀分布圆周
            DriftingRibbon(phaseOffset = (4 * PI / 3).toFloat(),       color = blendAmbient(rib3(scheme), ambient, 0.2f), phase = phase)
        }

        // 第 3 层：静态胶片颗粒噪点（DAY 强度极低 0.025，DARK 强度 0.08）
        FilmGrain(intensity = if (scheme == SWScheme.DAY) 0.025f else 0.08f)
    }
}

/**
 * 单条漂移极光光带。
 *
 * 通过正弦/余弦将归一化的 [phase] 映射为圆心在画布内的平滑轨迹，
 * 绘制一个大半径径向渐变圆形来模拟宽扁光带效果。
 *
 * @param phaseOffset 各 ribbon 初始相位差（弧度），用于错开三条光带的运动节奏。
 * @param color 光带颜色（已与环境色混合后传入）。
 * @param phase 全局动画进度（0..1），来自父级 rememberInfiniteTransition。
 */
@Composable
private fun DriftingRibbon(phaseOffset: Float, color: Color, phase: Float) {
    // 将线性 phase 换算为圆周角度，加上 phaseOffset 错开三条光带的相位
    val angle = phase * 2 * PI.toFloat() + phaseOffset
    // 圆心在画布上的归一化坐标（±25% 范围内做圆周游走）
    val cx = (0.5f + 0.25f * cos(angle))
    val cy = (0.5f + 0.25f * sin(angle))
    Canvas(modifier = Modifier.fillMaxSize()) {
        // 光带半径取画布最大边的 60%，确保光带足够宽且边缘柔和
        val r = size.maxDimension * 0.6f
        drawCircle(
            brush = Brush.radialGradient(
                // 中心 alpha 0.35 → 边缘完全透明，形成柔和光晕效果
                colors = listOf(color.copy(alpha = 0.35f), color.copy(alpha = 0f)),
                center = Offset(size.width * cx, size.height * cy),
                radius = r
            ),
            radius = r,
            center = Offset(size.width * cx, size.height * cy)
        )
    }
}

/**
 * 静态胶片颗粒噪点层。
 *
 * 使用固定随机种子预生成 20×20 = 400 个亮度值，首帧之后不再重新计算，
 * 避免每帧重新生成颗粒导致的闪烁与额外电量消耗。
 *
 * @param intensity 颗粒可见强度乘数（DAY 模式 0.025，DARK 模式 0.08）。
 */
@Composable
private fun FilmGrain(intensity: Float) {
    // 预生成颗粒亮度数组（种子固定保证稳定）——逐帧重新计算会耗电且产生闪烁
    val grain = remember {
        val random = Random(0xA110A4)
        IntArray(400) { random.nextInt(0, 256) }
    }
    Canvas(modifier = Modifier.fillMaxSize()) {
        // 将画布均分为 20×20 格，每格中心绘制一个半径 0.7px 的白点
        val cell = size.width / 20f
        var i = 0
        for (y in 0 until 20) for (x in 0 until 20) {
            val v = grain[i++] / 255f  // 归一化亮度值（0..1）
            drawCircle(
                color = Color.White.copy(alpha = intensity * v),
                radius = 0.7f,  // 固定极小半径，保持颗粒感而不遮挡底层
                center = Offset(x * cell + cell / 2, y * (size.height / 20f) + (size.height / 20f) / 2)
            )
        }
    }
}

// ---------- 各 scheme 下三条 ribbon 的基础颜色 ----------

// ribbon 1：DAY=暖橙，DARK=极光紫，NIGHT=深红（护眼静止，仅提供颜色备用）
private fun rib1(s: SWScheme): Color = when (s) {
    SWScheme.DAY   -> Color(0xFFFFB088)
    SWScheme.DARK  -> Color(0xFFB8A4FF)
    SWScheme.NIGHT -> Color(0.78f, 0.45f, 0.45f)
}
// ribbon 2：DAY=淡紫，DARK=黄昏橙，NIGHT=暗红
private fun rib2(s: SWScheme): Color = when (s) {
    SWScheme.DAY   -> Color(0xFFB8A4FF)
    SWScheme.DARK  -> Color(0xFFFFB088)
    SWScheme.NIGHT -> Color(0.55f, 0.28f, 0.28f)
}
// ribbon 3：DAY=珍珠白，DARK=深夜靛蓝，NIGHT=极暗红（几乎不可见）
private fun rib3(s: SWScheme): Color = when (s) {
    SWScheme.DAY   -> Color(0xFFFAE9DC)
    SWScheme.DARK  -> Color(0xFF1F3A8C)
    SWScheme.NIGHT -> Color(0.24f, 0.06f, 0.06f)
}
