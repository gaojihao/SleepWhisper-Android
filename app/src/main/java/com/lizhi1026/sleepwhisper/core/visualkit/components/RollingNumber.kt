/**
 * RollingNumber — 逐位滚动数字显示组件。
 *
 * 视觉效果：将整数按位拆分，每位数字独立以垂直滚动动画切换：
 *   - 数字上升（变大）：新数字从下方滑入，旧数字向上滑出
 *   - 数字下降（变小）：新数字从上方滑入，旧数字向下滑出
 *   - reduce-motion 模式：改为淡入淡出，消除运动感
 *
 * 所在层：core/visualkit — 设计令牌层（可视组件工具包）
 *
 * 典型使用场景：
 *   - SleepingScreen 计时器倒计时显示
 *   - Trends 屏睡眠时长数值展示
 *
 * 建议配合等宽衬线字体（如 `SWFont.displayLGTabular()`）使用，
 * 避免各位数字宽度不同导致布局抖动。
 */
package com.lizhi1026.sleepwhisper.core.visualkit.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import com.lizhi1026.sleepwhisper.core.visualkit.LocalReduceMotion
import com.lizhi1026.sleepwhisper.core.visualkit.SWFont
import com.lizhi1026.sleepwhisper.core.visualkit.SWMotion

/**
 * 逐位垂直滚动数字 Composable。
 *
 * 将 [value] 转为字符串后按位遍历，每位用 [AnimatedContent] 独立驱动滚动动画，
 * 方向由相邻帧的数位大小关系决定。自动读取 [LocalReduceMotion]。
 *
 * @param value    要显示的整数，支持任意位数
 * @param modifier Compose Modifier，作用于外层 [Row]
 * @param style    文字样式，建议使用等宽衬线字体避免宽度抖动
 */
@Composable
fun RollingNumber(
    value: Int,
    modifier: Modifier = Modifier,
    style: TextStyle = SWFont.displayLG()
) {
    // 读取无障碍降低动效标志，控制动画类型
    val reduce = LocalReduceMotion.current
    val digits = value.toString()  // 将数值按字符串拆分，便于逐位处理

    Row(modifier = modifier) {
        digits.forEachIndexed { i, ch ->
            // 每一位数字独立驱动 AnimatedContent，互不干扰
            AnimatedContent(
                targetState = ch,
                transitionSpec = {
                    if (reduce) {
                        // 降低动效：仅淡入淡出，无位移
                        fadeIn(tween(SWMotion.numberSwitchMs)) togetherWith fadeOut(tween(SWMotion.numberSwitchMs))
                    } else {
                        // 判断数字是上升还是下降，决定滑动方向
                        val rising = (initialState.digitToIntOrNull() ?: 0) < (targetState.digitToIntOrNull() ?: 0)
                        // rising=true → dir=1，新数字从下（+h）进，旧数字向上（-h）出
                        // rising=false → dir=-1，新数字从上（-h）进，旧数字向下（+h）出
                        val dir = if (rising) 1 else -1
                        (slideInVertically(tween(SWMotion.numberSwitchMs)) { h -> dir * h } + fadeIn(tween(SWMotion.numberSwitchMs))) togetherWith
                            (slideOutVertically(tween(SWMotion.numberSwitchMs)) { h -> -dir * h } + fadeOut(tween(SWMotion.numberSwitchMs)))
                    }
                },
                label = "digit-$i"  // 唯一 label，方便 Compose 工具追踪动画
            ) { c ->
                BasicText(text = c.toString(), style = style)
            }
        }
    }
}
