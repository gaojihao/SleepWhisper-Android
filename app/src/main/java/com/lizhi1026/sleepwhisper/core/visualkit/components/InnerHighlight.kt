package com.lizhi1026.sleepwhisper.core.visualkit.components

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWScheme

/**
 * "液态玻璃"内描边渐变 Modifier 扩展，visualkit/components 层视觉增强工具。
 *
 * 在任意抬升曲面顶部绘制 1dp 渐变描边，模拟玻璃/金属表面的内侧高光反射，
 * 是 Aurora 设计语言"液态玻璃"质感的核心视觉暗示。
 *
 * **视觉效果**（按主题区分）：
 * - DAY：白色 45% → 透明（渐变止于 45%），模拟强光照射
 * - DARK：白色 18% → 透明，低调内高光避免与深色背景冲突
 * - NIGHT：暖粉红色 10% → 透明，与夜间暖色调月光主题呼应
 *
 * **典型使用场景**：所有 GlassCard 变体、PlayerArtwork 圆形容器、SettingsRow 容器
 *
 * **层级**：visualkit/components — 设计系统 Modifier 扩展层
 *
 * **注意**：使用 [composed] 读取 CompositionLocal，调用方无需感知当前主题
 *
 * @param cornerRadius 圆角半径，需与宿主容器圆角保持一致，默认 16.dp
 */
fun Modifier.innerHighlight(cornerRadius: Dp = 16.dp): Modifier = composed {
    val scheme = LocalSWScheme.current
    // 根据当前主题生成对应渐变描边：顶部高亮→中部透明，模拟光源从上方照射的反射效果
    val brush = when (scheme) {
        SWScheme.DAY -> Brush.verticalGradient(
            0f to Color.White.copy(alpha = 0.45f),  // 强光：白天光线充足，高光较强
            0.45f to Color.Transparent
        )
        SWScheme.DARK -> Brush.verticalGradient(
            0f to Color.White.copy(alpha = 0.18f),  // 柔光：暗色模式低调内高光
            0.45f to Color.Transparent
        )
        SWScheme.NIGHT -> Brush.verticalGradient(
            0f to Color(1.00f, 0.75f, 0.75f).copy(alpha = 0.10f),  // 暖粉：夜间月光暖调反射
            0.45f to Color.Transparent
        )
    }
    // 叠加 1dp 渐变边框，圆角与宿主容器保持一致，确保描边不超出裁切范围
    border(1.dp, brush, RoundedCornerShape(cornerRadius))
}
