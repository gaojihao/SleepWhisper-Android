/**
 * EmptyStateCard — 空状态卡片组件。
 *
 * 视觉效果：居中纵向布局，由上至下依次为：
 *   1. 64dp 主题色 18% 透明度圆形背景 + 28dp 图标（默认 ic_empty_sparkles）
 *   2. titleMD 主色标题文字
 *   3. bodyMD 次级色副标题说明文字
 * 图标圆形容器附带 [floatingY] 浮动动画（振幅 4dp，周期 5s），
 * 使空状态页面保持微弱的"生命感"，避免完全静止。
 *
 * 所在层：core/visualkit — 设计令牌层（可视组件工具包）
 *
 * 典型使用场景：
 *   - TrendsScreen 无历史数据时的占位卡片
 *   - 搜索/筛选结果为空时的提示区域
 *
 * ⚠️ floatingY 动画内部已读取 [LocalReduceMotion]，降低动效时浮动自动静止。
 *
 * 该组件为 iOS EmptyStateCard.swift 的 Compose 移植版。
 */
package com.lizhi1026.sleepwhisper.core.visualkit.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lizhi1026.sleepwhisper.R
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor
import com.lizhi1026.sleepwhisper.core.visualkit.SWFont
import com.lizhi1026.sleepwhisper.core.visualkit.SWSpacing
import com.lizhi1026.sleepwhisper.core.visualkit.floatingY

/**
 * 空状态卡片 Composable。
 *
 * 展示带浮动图标的空状态提示，包含标题与副标题，
 * 整体包裹在 [GlassCard] 中以保持与应用其他卡片风格一致。
 *
 * @param titleRes    标题字符串资源 ID
 * @param subtitleRes 副标题字符串资源 ID
 * @param modifier    Compose Modifier，默认填充父级宽度
 * @param iconRes     图标 drawable 资源 ID，默认为 ic_empty_sparkles
 */
@Composable
fun EmptyStateCard(
    titleRes: Int,
    subtitleRes: Int,
    modifier: Modifier = Modifier,
    @DrawableRes iconRes: Int = R.drawable.ic_empty_sparkles
) {
    val scheme = LocalSWScheme.current
    // GlassCard 提供统一的毛玻璃背景与圆角，xl 内边距保证内容不贴边
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        contentPadding = PaddingValues(SWSpacing.xl)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SWSpacing.sm)  // 三段内容间距统一
        ) {
            // 图标容器：主题色光晕圆背景 + 浮动动画
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(SWColor.accent(scheme).copy(alpha = 0.18f))  // 18% 透明度光晕
                    // floatingY 内部读取 LocalReduceMotion；振幅 4dp，周期 5000ms
                    .floatingY(amplitude = 4.dp, durationMillis = 5000),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,  // 装饰性图标，无障碍由标题文字描述
                    colorFilter = ColorFilter.tint(SWColor.accent(scheme)),  // 图标着色为主题强调色
                    modifier = Modifier.size(28.dp)
                )
            }
            // 标题：titleMD 字重，主色，居中
            BasicText(
                text = stringResource(titleRes),
                style = SWFont.titleMD().copy(
                    color = SWColor.textPrimary(scheme),
                    textAlign = TextAlign.Center
                )
            )
            // 副标题：bodyMD 字重，次级色，居中，补充说明空状态原因或引导操作
            BasicText(
                text = stringResource(subtitleRes),
                style = SWFont.bodyMD().copy(
                    color = SWColor.textSecondary(scheme),
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}
