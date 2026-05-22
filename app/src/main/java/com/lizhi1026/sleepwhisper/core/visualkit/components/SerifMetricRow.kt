/**
 * SerifMetricRow — 标签·数值横排展示行。
 *
 * 视觉效果：单行水平布局，左侧显示主文字标签，右侧显示衬线数值文字；
 * 可选在数值右侧附加 [ChevronTrail] 向前指示符，暗示行可点击进入详情。
 * 颜色由 [LocalSWScheme] 自动适配深色/浅色主题。
 *
 * 所在层：core/visualkit — 设计令牌层（可视组件工具包）
 *
 * 典型使用场景：
 *   - TrendsScreen 睡眠数据统计行（平均睡眠时长、深度睡眠比例等）
 *   - SettingsScreen 设置项展示行（账户信息、订阅状态等）
 */
package com.lizhi1026.sleepwhisper.core.visualkit.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor
import com.lizhi1026.sleepwhisper.core.visualkit.SWFont
import com.lizhi1026.sleepwhisper.core.visualkit.SWSpacing

/**
 * 标签·数值横排行 Composable。
 *
 * 左右两端对齐：左侧为主色标签，右侧为次级色数值（可选附 ChevronTrail）。
 * 当 [onClick] 不为 null 时，整行可点击，适用于跳转详情或触发弹层的场景。
 *
 * @param label       左侧标签文字（如"平均睡眠"、"账户"）
 * @param value       右侧数值文字（如"7h 32m"、"已订阅"）
 * @param modifier    Compose Modifier，作用于外层 Row
 * @param showChevron 是否在数值右侧显示 [ChevronTrail] 向前指示符，默认 false
 * @param onClick     点击整行的回调；传 null 时行不响应点击
 */
@Composable
fun SerifMetricRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    showChevron: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val scheme = LocalSWScheme.current
    // 有点击回调时追加 clickable，无回调时保持普通行（不产生涟漪反馈）
    val rowMod = if (onClick != null) modifier.fillMaxWidth().clickable(onClick = onClick)
                 else modifier.fillMaxWidth()
    Row(
        modifier = rowMod.padding(vertical = SWSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween  // 左右两端对齐
    ) {
        // 左侧标签：主色，bodyLG 字重
        BasicText(
            text = label,
            style = SWFont.bodyLG().copy(color = SWColor.textPrimary(scheme))
        )
        // 右侧容器：数值 + 可选 ChevronTrail 水平排列
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SWSpacing.xs)
        ) {
            // 数值文字：次级色，视觉重量低于左侧标签
            BasicText(
                text = value,
                style = SWFont.bodyLG().copy(color = SWColor.textSecondary(scheme))
            )
            // 按需渲染 ChevronTrail，给可点击行提供前进方向暗示
            if (showChevron) ChevronTrail()
        }
    }
}
