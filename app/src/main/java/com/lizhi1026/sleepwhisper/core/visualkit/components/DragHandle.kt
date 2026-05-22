package com.lizhi1026.sleepwhisper.core.visualkit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor
import com.lizhi1026.sleepwhisper.core.visualkit.SWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWSpacing

/**
 * 底部弹层拖拽把手，visualkit/components 层装饰基础元件。
 *
 * 渲染一个 36×4dp 的胶囊形色块，固定位于弹层顶部中央，
 * 向用户传达"可向下拖拽关闭"的交互提示（遵循 iOS 底部弹层规范）。
 *
 * **视觉效果**：
 * - DAY 主题：三级文字色 50% 透明度（浅灰）
 * - DARK/NIGHT 主题：纯白 20% 透明度（低调白色）
 *
 * **典型使用场景**：
 * - SleepTimerSheet、SoundPickerSheet 等所有 ModalBottomSheet 顶部
 * - 任何需要标识可拖拽区域的底部面板
 *
 * **层级**：visualkit/components — 设计系统排版/装饰类基础组件层
 *
 * @param modifier 外部 Modifier，通常用于控制水平居中对齐
 */
@Composable
fun DragHandle(modifier: Modifier = Modifier) {
    val scheme = LocalSWScheme.current
    // 亮色主题用半透明灰，暗色/夜间主题用低透白色，确保在不同背景上均有足够对比度
    val color = if (scheme == SWScheme.DAY)
        SWColor.textTertiary(scheme).copy(alpha = 0.5f)  // DAY：浅灰，避免太突兀
    else
        Color.White.copy(alpha = 0.2f)  // DARK/NIGHT：低透白，与深色背景形成微妙对比
    Box(
        modifier = modifier
            .padding(top = 8.dp)  // 顶部留 8dp 触摸区域，避免把手贴顶难以触达
            .size(width = 36.dp, height = 4.dp)  // 36×4dp 遵循 iOS HIG 把手尺寸规范
            .clip(RoundedCornerShape(2.dp))  // 圆角半径 = 高度的一半，形成完整胶囊形
            .background(color)
    )
}
