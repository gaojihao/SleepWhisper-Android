package com.lizhi1026.sleepwhisper.core.visualkit

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity

/**
 * visualkit 层的自定义 Modifier 扩展集合。
 *
 * 职责：提供带节奏感的动画 Modifier，供 Home 唤醒窗口卡片、SleepingScreen 倒计时等
 * 界面元素使用。所有动画 Modifier 均须在使用前读取 [LocalReduceMotion]，
 * ⚠️ 当系统 ANIMATOR_DURATION_SCALE == 0 时必须静止，不能播放任何无限动画。
 *
 * 归属层：visualkit — 被 Home、SleepingScreen 等 Composable 调用。
 */
/**
 * 慢速垂直浮动动画 Modifier，移植自 iOS `.floatingY(amplitude:duration:)`。
 * 用于 Home 唤醒窗口卡片与 SleepingScreen 倒计时的漂浮效果。
 *
 * ⚠️ 必须读取 [LocalReduceMotion]：当减少动态效果开启时直接返回原 Modifier，保持静止。
 *
 * @param amplitude      垂直浮动幅度，默认 3.dp
 * @param durationMillis 单程动画时长（毫秒），默认 8000ms（8 秒一个来回）
 * @return 附加了垂直浮动动画的 Modifier；减少动态效果开启时返回原 Modifier 不变
 */
fun Modifier.floatingY(
    amplitude: Dp = 3.dp,
    durationMillis: Int = 8000
): Modifier = composed {
    // ⚠️ 减少动态效果：系统 ANIMATOR_DURATION_SCALE == 0 时直接跳过动画
    if (LocalReduceMotion.current) return@composed this
    val transition = rememberInfiniteTransition(label = "floating-y")
    val phase by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse  // 到达目标后反向，形成来回浮动
        ),
        label = "phase"
    )
    val density = LocalDensity.current
    // 将 Dp 幅度转换为像素并乘以相位（-1f .. 1f），得到当前垂直偏移量
    val offsetPx = with(density) { amplitude.toPx() } * phase
    this.graphicsLayer { translationY = offsetPx }
}
