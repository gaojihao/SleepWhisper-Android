/**
 * SWSegmentedPicker — 泛型分段选择器组件。
 *
 * 视觉效果：水平排列若干等宽段，选中段下方有一个渐变指示器 Pill，
 * 切换选中项时指示器以 [SWMotion.screenInMs] 时长水平滑动到对应位置。
 *
 * 所在层：core/visualkit — 设计令牌层（可视组件工具包）
 *
 * 典型使用场景（泛型复用）：
 *   - OnboardingScreen 性别选择（Male / Female / Other）
 *   - SleepTimerSheet 定时时长选择（15min / 30min / 45min / 60min）
 *   - SettingsScreen 其他双/多选切换场景
 *
 * 该组件为 iOS SWSegmentedPicker.swift 的 Compose 移植版。
 */
package com.lizhi1026.sleepwhisper.core.visualkit.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor
import com.lizhi1026.sleepwhisper.core.visualkit.SWFont
import com.lizhi1026.sleepwhisper.core.visualkit.SWGradient
import com.lizhi1026.sleepwhisper.core.visualkit.SWMotion
import com.lizhi1026.sleepwhisper.core.visualkit.SWRadius

/**
 * 分段选择器的单个选项数据模型。
 *
 * @param T     选项值的泛型类型（通常为枚举或简单数据类）
 * @property value 选项的业务值，用于回调与选中比对
 * @property label 选项的显示文本
 */
data class SegmentedOption<T>(val value: T, val label: String)

/**
 * 泛型分段选择器 Composable。
 *
 * 以 [BoxWithConstraints] 获取可用宽度后均分给每个选项段，
 * 滑动指示器通过 [animateDpAsState] 驱动水平偏移，切换流畅无跳变。
 *
 * @param T        选项值的泛型类型
 * @param options  所有可选项列表，顺序决定显示顺序，不可为空
 * @param selected 当前已选中的值，用于高亮对应段
 * @param onSelect 用户点击某段时的回调，传入被选中的 [T] 值
 * @param modifier Compose Modifier，建议外部控制水平边距
 */
@Composable
fun <T> SWSegmentedPicker(
    options: List<SegmentedOption<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    // 空列表防御：无选项时直接返回，不渲染任何内容
    if (options.isEmpty()) return
    val scheme = LocalSWScheme.current
    val pill = RoundedCornerShape(SWRadius.pill)  // 全圆角胶囊形状，统一用于背景和指示器
    // 找到当前选中项的索引，找不到时兜底为 0 防止越界
    val selectedIndex = options.indexOfFirst { it.value == selected }.coerceAtLeast(0)

    BoxWithConstraints(
        modifier = modifier
            .height(44.dp)
            .fillMaxWidth()
            .clip(pill)
            .background(SWColor.surfaceSunken(scheme))  // 凹陷背景色，呈现容器质感
    ) {
        val density = LocalDensity.current
        // 每段等宽：总宽 ÷ 段数
        val segmentWidth = maxWidth / options.size
        // 指示器目标偏移 = 段宽 × 选中索引，切换时以 screenInMs 动画滑动
        val targetOffset by animateDpAsState(
            targetValue = segmentWidth * selectedIndex,
            animationSpec = tween(durationMillis = SWMotion.screenInMs),
            label = "seg-offset"
        )

        // 滑动指示器 Pill（位于文字层下方，通过 Z 层叠关系覆盖背景）
        Box(
            modifier = Modifier
                .offset(x = targetOffset)  // 动画驱动水平位移
                .height(44.dp)
                .padding(3.dp)             // 内缩 3dp，使指示器不顶到容器边缘
                .background(SWGradient.primary(scheme), pill)
                .clip(pill),
        ) {
            // indicator
            // 占位 Box 撑开指示器宽度（段宽 - 左右各 3dp padding）
            Box(modifier = Modifier.width(segmentWidth - 6.dp).height(38.dp))
        }
        // 文字层：覆盖在指示器上方，点击区域与段等宽
        Row(modifier = Modifier.fillMaxWidth().height(44.dp)) {
            options.forEachIndexed { i, opt ->
                val isSelected = i == selectedIndex
                Box(
                    modifier = Modifier
                        .width(segmentWidth)
                        .height(44.dp)
                        .clickable { onSelect(opt.value) },  // 点击整个段区域均可选中
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        text = opt.label,
                        style = SWFont.labelMD().copy(
                            // 选中项白色文字（指示器背景已为深色），未选中为次级色
                            color = if (isSelected) Color.White else SWColor.textSecondary(scheme),
                            textAlign = TextAlign.Center
                        )
                    )
                }
            }
        }
    }
}
