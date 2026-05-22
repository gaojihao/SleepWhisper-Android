package com.lizhi1026.sleepwhisper.core.visualkit.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lizhi1026.sleepwhisper.core.visualkit.LocalReduceMotion
import kotlin.math.PI
import kotlin.math.sin

/**
 * # LiquidProgressRing — 液态正弦填充进度环
 *
 * 所属层级：内容层，通常作为 Player 屏幕或睡眠统计的圆形进度指示器。
 *
 * 视觉效果：
 * - 外圆轨道：低透明度（0.3）圆形描边，作为未填充部分的参考轨迹。
 * - 液态填充：内部以正弦波形路径切割圆形区域，液面高度对应 [progress] 值：
 *   progress = 0 时液面在最底部，progress = 1 时液面溢至顶部。
 * - 正弦波振幅 4dp，相位随时间线性滚动（2400ms 一周期），产生"液体晃动"效果。
 * - reduce-motion 开启时：targetValue 固定为 0f，phase 不变化，波形静止。
 *
 * 典型使用场景：
 * - SleepingScreen 睡眠进度圆环（移植自 iOS LiquidProgressRing in SleepingView.swift）。
 * - 任何需要圆形液态填充视觉的进度指示场景。
 *
 * ⚠️ 无障碍：读取 [LocalReduceMotion]，为 true 时 targetValue = 0f 使 phase 始终为 0，
 * 波形不滚动但仍显示液面高度（进度信息保留）。
 *
 * @param progress 进度值（0..1），超出范围自动 clamp。
 * @param modifier 外部布局修饰符。
 * @param color 液体填充颜色（轨道和液体共用，透明度各异）。
 * @param radius 圆环半径，组件实际尺寸为 radius * 2，默认 80dp。
 */
@Composable
fun LiquidProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color,
    radius: Dp = 80.dp
) {
    val clampedProgress = progress.coerceIn(0f, 1f)  // 防止越界导致液面超出圆形范围
    val reduceMotion = LocalReduceMotion.current
    val phaseTransition = rememberInfiniteTransition(label = "liquid-phase")
    // reduce-motion 时 targetValue = 0f，动画值始终为 0（静止波形）
    val phase by phaseTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (reduceMotion) 0f else (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart  // 重启模式：相位 0→2π 线性滚动后重置
        ),
        label = "phase"
    )

    Canvas(
        modifier = modifier
            .size(radius * 2)
            .clip(CircleShape)  // 裁剪为圆形，液态填充不会超出圆边界
    ) {
        val r = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        // 绘制层 1：外圆轨道（低 alpha 描边，表示未填充区域）
        drawCircle(
            color = color.copy(alpha = 0.3f),
            radius = r - 2.dp.toPx(),  // 内缩 2dp 防止描边被裁剪
            center = center,
            style = Stroke(width = 4.dp.toPx())
        )

        // 绘制层 2：液态正弦填充路径
        // waterLevel：液面 y 坐标（0 = 顶部满，size.height = 底部空）
        val waterLevel = size.height * (1f - clampedProgress)
        val amplitude = 4.dp.toPx()  // 波形振幅 4dp，保持视觉优雅不夸张
        val path = Path().apply {
            moveTo(0f, size.height)          // 从左下角开始
            lineTo(0f, waterLevel)           // 垂直移动到液面高度
            // 沿 x 方向分 32 步绘制正弦波面（步数越多波形越平滑）
            val steps = 32
            for (i in 0..steps) {
                val x = size.width * i / steps
                // 正弦波：phase 驱动水平移动，产生液体晃动视觉
                val y = waterLevel + amplitude * sin(phase + i * 2f * PI.toFloat() / steps)
                lineTo(x, y)
            }
            lineTo(size.width, size.height)  // 移动到右下角
            close()                          // 封闭路径形成填充区域
        }
        // alpha 0.55：液体半透明，可透见底层轨道和背景颜色
        drawPath(path, color = color.copy(alpha = 0.55f))
    }
}
