package com.lizhi1026.sleepwhisper.core.visualkit.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.lizhi1026.sleepwhisper.core.visualkit.LocalReduceMotion
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor
import kotlin.math.PI
import kotlin.math.sin

/**
 * # AudioWaveform — 仿 FFT 音频波形条
 *
 * 所属层级：内容层，用于 Player 屏幕的播放器 UI 区域。
 *
 * 视觉效果：
 * - 若干竖条（默认 14 根）排列成波形，高度由双正弦叠加的包络函数决定，
 *   模拟真实 FFT 频谱的外观（并非实际音频分析，而是纯视觉仿真）。
 * - 每根条使用竖向线性渐变，颜色由橙（[SWColor.accent]）到紫（[SWColor.accentSecondary]）。
 * - 播放时（[isPlaying] = true）包络相位随时间线性滚动，波形"律动"；
 *   停止时相位固定，呈现静止对称波形。
 *
 * 典型使用场景：Player 屏幕音频播放状态可视化，底部播放控制栏缩略图。
 *
 * ⚠️ 无障碍：读取 [LocalReduceMotion]，为 true 时动画停止（phase 固定为 0.5）。
 *
 * @param modifier 外部布局修饰符，建议指定固定尺寸。
 * @param isPlaying 是否正在播放；false 时动画暂停。
 * @param color 主色调，传入 [Color.Unspecified] 时自动使用当前 scheme 的 accent 色。
 * @param barCount 波形条数量，默认 14 根。
 */
@Composable
fun AudioWaveform(
    modifier: Modifier = Modifier,
    isPlaying: Boolean,
    color: Color,
    barCount: Int = 14
) {
    val scheme = LocalSWScheme.current
    val reduce = LocalReduceMotion.current
    // 未指定颜色时回退到 scheme 的主强调色（橙色系）
    val tint = if (color == Color.Unspecified) SWColor.accent(scheme) else color
    val secondary = SWColor.accentSecondary(scheme)  // 次强调色（紫色系），用于渐变终点

    // 播放中且未开启 reduce-motion 时才驱动动画
    val animate = isPlaying && !reduce
    val phase = if (animate) {
        // 无限线性动画，1800ms 一个周期，phase 值 0→1 循环
        val t = rememberInfiniteTransition(label = "wave")
        val v by t.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1800, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "phase"
        )
        v
    } else 0.5f  // 静止时固定相位 0.5，使波形呈现对称非平坦姿态

    Canvas(modifier = modifier.fillMaxSize()) {
        // 计算条宽和间距（条宽占总宽约 1/1.7barCount，余量均分为间隙）
        val barWidth = size.width / (barCount * 1.7f)
        val gap = (size.width - barWidth * barCount) / (barCount - 1).coerceAtLeast(1)
        val mid = size.height / 2f
        // 渐变方向：从上（tint/橙）到下（secondary/紫），使每根条都有颜色过渡
        val brush = Brush.verticalGradient(colors = listOf(tint, secondary))
        for (i in 0 until barCount) {
            val t = i.toFloat() / (barCount - 1).coerceAtLeast(1)
            // 双正弦包络：第一项产生中高两侧低的基础形状，第二项随 phase 滚动产生律动感
            val envelope = (sin(PI * t * 2) * 0.5 + 0.5 +
                            sin((phase + t) * PI * 2) * 0.35).toFloat()
            // 条高：最小值为条宽（避免完全消失），最大不超过画布高度 78%
            val barH = (size.height * 0.18f + size.height * 0.6f * envelope).coerceAtLeast(barWidth)
            val x = i * (barWidth + gap)
            // 从中线向上下对称延伸，绘制圆角矩形条
            drawRoundRect(
                brush = brush,
                topLeft = Offset(x, mid - barH / 2),
                size = Size(barWidth, barH),
                cornerRadius = CornerRadius(barWidth / 2)  // 圆角半径 = 条宽一半，形成胶囊形
            )
        }
    }
}
