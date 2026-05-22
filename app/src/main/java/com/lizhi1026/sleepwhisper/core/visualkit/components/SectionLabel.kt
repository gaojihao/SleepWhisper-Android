package com.lizhi1026.sleepwhisper.core.visualkit.components

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor
import com.lizhi1026.sleepwhisper.core.visualkit.SWFont

/**
 * 区块标题标签，visualkit/components 层排版基础元件。
 *
 * 将传入文字强制转为全大写并以 [SWFont.labelMD] + 次级文字色渲染，
 * 作为页面内容分区的视觉分隔线替代方案（无实体分隔线，仅靠字体样式区分层级）。
 *
 * **视觉效果**：全大写 + 字符间距扩展（由 [SWFont.labelMD] 决定）+ 次级灰色
 *
 * **典型使用场景**：
 * - HomeScreen："WAKE WINDOW"、"ALL SOUNDS"
 * - TrendsScreen："THIS WEEK"、"LAST 7 DAYS"
 * - PlayerScreen 音频列表分组标题
 *
 * **层级**：visualkit/components — 设计系统排版/装饰类基础组件层
 *
 * @param text 标签文字，内部自动调用 `uppercase()` 处理，无需调用方预处理
 * @param modifier 外部 Modifier，可用于控制间距、对齐
 */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    val scheme = LocalSWScheme.current
    BasicText(
        text = text.uppercase(),  // 强制大写，统一排版风格
        style = SWFont.labelMD().copy(color = SWColor.textSecondary(scheme)),
        modifier = modifier
    )
}
