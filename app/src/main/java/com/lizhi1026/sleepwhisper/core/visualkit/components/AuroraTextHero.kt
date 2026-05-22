package com.lizhi1026.sleepwhisper.core.visualkit.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import com.lizhi1026.sleepwhisper.core.visualkit.LocalReduceMotion
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor
import com.lizhi1026.sleepwhisper.core.visualkit.SWMotion

/**
 * # AuroraTextHero — 极光渐变扫光文字
 *
 * 所属层级：内容层，叠加于背景组件（[AuroraBackdrop] / [NightSkyCanvas]）之上。
 *
 * 视觉效果：
 * - 文字填充使用线性渐变（主色 → 次强调色 → 主色），渐变终点的 x 坐标随时间
 *   线性平移，形成"光斑从左向右扫过字形"的极光流光效果。
 * - 动画周期为 [SWMotion.backdropDriftMs] / 2（约 9 秒），与底层背景光带同步
 *   但频率更快，让文字更有活力。
 * - reduce-motion 开启时：渐变 phase 固定为 0，渐变静止，不影响可读性。
 *
 * 典型使用场景：
 * - HomeScreen / SleepingScreen 的大标题（displayXL / displayLG 级别）。
 * - ⚠️ 仅用于大字号——应用于正文会造成视觉噪点，降低可读性。
 *
 * ⚠️ 无障碍：读取 [LocalReduceMotion]，为 true 时扫光停止，渐变静止显示。
 *
 * @param text 要显示的文字内容。
 * @param style 文字样式（建议使用 SWType.displayXL 或 displayLG）。
 * @param modifier 外部布局修饰符。
 */
@Composable
fun AuroraTextHero(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier
) {
    val scheme = LocalSWScheme.current
    val reduce = LocalReduceMotion.current

    // reduce-motion 时直接使用 phase=0（静止渐变），否则启动无限动画
    val phase = if (reduce) 0f else {
        val t = rememberInfiniteTransition(label = "aurora-text")
        val v by t.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                // 周期为背景漂移的一半，扫光速度更快更活跃
                animation = tween(SWMotion.backdropDriftMs / 2, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "phase"
        )
        v
    }

    // 线性渐变：起点固定在左上角，终点 x 随 phase 平移（0..1000px 范围内移动 500px）
    // 形成"光斑从左向右扫过文字"的视觉效果
    val brush = Brush.linearGradient(
        colors = listOf(
            SWColor.textPrimary(scheme),       // 主色（通常为白/近白）
            SWColor.accentSecondary(scheme),   // 次强调色（极光蓝紫）
            SWColor.textPrimary(scheme)        // 回到主色，确保渐变可循环
        ),
        start = Offset(0f, 0f),
        end = Offset(1000f * (0.5f + phase), 200f)  // 终点 x 随 phase 在 500..1000 间移动
    )

    BasicText(
        text = text,
        style = style.copy(brush = brush),  // 将渐变画笔注入文字样式
        modifier = modifier
    )
}
