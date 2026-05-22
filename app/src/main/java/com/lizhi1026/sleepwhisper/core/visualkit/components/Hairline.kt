package com.lizhi1026.sleepwhisper.core.visualkit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor

/**
 * 极细分隔线，visualkit/components 层排版装饰基础元件。
 *
 * 以 [SWColor.border] 60% 透明度渲染，默认 0.5dp 高度（物理约 0.5 像素），
 * 为眼睛提供轻柔视觉落脚点，避免重色分隔线破坏暗色系整体氛围。
 *
 * **视觉效果**：宽度撑满父容器，高度 0.5dp，边框色降至 60% alpha
 *
 * **典型使用场景**：
 * - GlassCard 底部内边缘分隔内容区与操作区
 * - 设置页列表行间分隔
 * - PlayerScreen 音频项列表行间分隔
 *
 * **层级**：visualkit/components — 设计系统排版/装饰类基础组件层
 *
 * @param modifier 外部 Modifier，可调整水平内边距或对齐方式
 * @param thickness 分隔线厚度，默认 0.5dp；可传 1.dp 获取标准单像素线
 */
@Composable
fun Hairline(modifier: Modifier = Modifier, thickness: Dp = 0.5.dp) {
    val scheme = LocalSWScheme.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(thickness)  // 使用参数化厚度，默认 0.5dp 实现极细效果
            .background(SWColor.border(scheme).copy(alpha = 0.6f))  // 降至 60% 避免分隔线过重
    )
}
