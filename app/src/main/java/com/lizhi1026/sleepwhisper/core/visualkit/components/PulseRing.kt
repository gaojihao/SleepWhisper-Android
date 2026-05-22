package com.lizhi1026.sleepwhisper.core.visualkit.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lizhi1026.sleepwhisper.core.visualkit.LocalReduceMotion
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor

enum class PulseIntensity { SOFT, STRONG }

/**
 * # PulseRing — 极光三环脉冲光晕
 *
 * 所属层级：内容层，通常叠加在 CTA 按钮（如 HomeScreen 的"开始睡眠"）中心。
 *
 * 视觉效果：
 * - 3 个同心圆环，相位依次偏移 1/3 周期（约 933ms），形成向外扩散的连续光波。
 * - 颜色从内到外渐变：橙色（[color]）→ 橙紫混合（lerp 50%）→ 星光紫（[SWColor.accentSecondary]）。
 * - 每环随 ringPhase（0..1）半径从 35% 扩至 65%，alpha 从 maxAlpha 线性衰减至 0，
 *   形成"从中心涌出、向外消隐"的脉冲光晕感。
 * - SOFT 强度：最大 alpha 0.18；STRONG 强度：最大 alpha 0.32，用于强调 CTA。
 *
 * 典型使用场景：HomeScreen 睡眠 CTA 按钮的脉冲光晕效果。
 *
 * ⚠️ 无障碍：读取 [LocalReduceMotion]，为 true 时渲染三个固定透明度静止环，不播放动画。
 *
 * @param modifier 外部布局修饰符。
 * @param color 最内环颜色（通常为 scheme 的 accent 橙色）。
 * @param radius 组件半径（决定 size = radius * 2），默认 100dp。
 * @param intensity 脉冲强度，影响环的最大 alpha 值。
 */
@Composable
fun PulseRing(
    modifier: Modifier = Modifier,
    color: Color,
    radius: Dp = 100.dp,
    intensity: PulseIntensity = PulseIntensity.SOFT
) {
    val reduce = LocalReduceMotion.current
    val scheme = LocalSWScheme.current
    val secondary = SWColor.accentSecondary(scheme)  // 外环紫色

    if (reduce) {
        // reduce-motion：渲染三个静止环，透明度按由内到外递减
        Canvas(modifier = modifier.size(radius * 2)) {
            val maxAlpha = if (intensity == PulseIntensity.STRONG) 0.18f else 0.10f
            val center = Offset(size.width / 2, size.height / 2)
            // 内环：橙色，最大 alpha × 0.8
            drawCircle(color.copy(alpha = maxAlpha * 0.8f), radius = size.minDimension * 0.45f, center = center, style = Stroke(width = 2f))
            // 中环：橙紫混合，最大 alpha × 0.6
            drawCircle(lerp(color, secondary, 0.5f).copy(alpha = maxAlpha * 0.6f), radius = size.minDimension * 0.55f, center = center, style = Stroke(width = 2f))
            // 外环：紫色，最大 alpha × 0.4
            drawCircle(secondary.copy(alpha = maxAlpha * 0.4f), radius = size.minDimension * 0.65f, center = center, style = Stroke(width = 2f))
        }
        return
    }

    // 动画：单一 phase（0..1），2800ms 一个完整脉冲周期
    val t = rememberInfiniteTransition(label = "pulse")
    val period = 2800
    val phase by t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(period, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )
    Canvas(modifier = modifier.size(radius * 2)) {
        val maxAlpha = if (intensity == PulseIntensity.STRONG) 0.32f else 0.18f
        val center = Offset(size.width / 2, size.height / 2)
        // 三环依次绘制（层叠顺序：内环最后绘制，覆盖外环边缘）
        for (i in 0 until 3) {
            // 各环相位错开 1/3，使扩散波不同步，形成连续流动感
            val ringPhase = ((phase + i / 3f) % 1f)
            // 颜色插值：i=0 橙，i=1 橙紫混合，i=2 紫
            val ringColor = when (i) {
                0 -> color
                1 -> lerp(color, secondary, 0.5f)
                else -> secondary
            }
            // 半径从 35% 扩展到 65%（以 size.minDimension 为参考）
            val ringRadius = size.minDimension * (0.35f + 0.30f * ringPhase)
            // alpha 随扩散线性衰减，从 maxAlpha 降至 0
            val ringAlpha = maxAlpha * (1f - ringPhase)
            drawCircle(
                color = ringColor.copy(alpha = ringAlpha),
                radius = ringRadius,
                center = center,
                style = Stroke(width = 2f)  // 细线圆环，宽度固定 2px
            )
        }
    }
}
