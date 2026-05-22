/**
 * ChevronTrail — 3 点渐隐向前指示符。
 *
 * 视觉效果：水平排列 3 个等间距小圆点，透明度从左到右递增（40% → 60% → 80%），
 * 在视觉上形成"向右渐强"的方向感，替代传统箭头图标。
 * 整体尺寸为 18×6dp，轻量无状态，适合内嵌在行尾。
 *
 * 所在层：core/visualkit — 设计令牌层（可视组件工具包）
 *
 * 典型使用场景：
 *   - [SerifMetricRow] 可点击行的右侧前进提示
 *   - SettingsScreen 各类可跳转列表项行尾
 */
package com.lizhi1026.sleepwhisper.core.visualkit.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor

/**
 * 3 点渐隐向前指示符 Composable。
 *
 * 通过 Canvas 绘制 3 个水平等间距小圆，透明度依次递增，
 * 形成视觉上的"前进方向"暗示，无需图标资源。
 *
 * @param modifier Compose Modifier，默认尺寸 18×6dp
 * @param tint     覆盖颜色；默认 [Color.Unspecified] 时使用 [SWColor.textTertiary]
 */
@Composable
fun ChevronTrail(modifier: Modifier = Modifier, tint: Color = Color.Unspecified) {
    val scheme = LocalSWScheme.current
    // 未指定颜色时回退到主题三级文字色（最淡，不喧宾夺主）
    val color = if (tint == Color.Unspecified) SWColor.textTertiary(scheme) else tint
    Canvas(modifier = modifier.size(width = 18.dp, height = 6.dp)) {
        val r = size.height / 3f          // 圆点半径 = 高度的 1/3，保持与行高协调
        val gap = (size.width - r * 6f) / 2f  // 3 个圆直径合计 r*6，剩余均分为两段间距
        for (i in 0 until 3) {
            drawCircle(
                // 透明度：0.4 + 0.2*i，即 40% / 60% / 80%，从左到右渐强
                color = color.copy(alpha = 0.4f + 0.2f * i),
                radius = r,
                center = Offset(r + i * (r * 2 + gap), size.height / 2)
            )
        }
    }
}
