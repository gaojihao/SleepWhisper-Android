package com.lizhi1026.sleepwhisper.core.visualkit.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lizhi1026.sleepwhisper.core.toast.ToastCenter
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor
import com.lizhi1026.sleepwhisper.core.visualkit.SWFont
import com.lizhi1026.sleepwhisper.core.visualkit.SWMotion
import com.lizhi1026.sleepwhisper.core.visualkit.SWRadius
import com.lizhi1026.sleepwhisper.core.visualkit.SWSpacing
import kotlinx.coroutines.delay

/**
 * 全局瞬态 Toast 覆盖层，visualkit/components 层全局反馈组件。
 *
 * 锚定在父容器底部中央，显示来自 [ToastCenter] 的 [ToastCenter.ToastItem] 消息，
 * 3 秒后自动调用 [onDismiss] 关闭（通过 [LaunchedEffect] + `delay(3000)` 实现）。
 * 支持可选的 Undo / Edit 快捷操作按钮。
 *
 * **视觉效果**：从底部滑入 + 淡入（[slideInVertically] + [fadeIn]），消失时反向滑出 + 淡出；
 * 左侧 8dp 彩色圆点根据 [ToastCenter.Style] 展示语义颜色（信息/成功/警告/错误）
 *
 * **数据流**：ViewModel → [ToastCenter] LiveData → 本组件 `item` 参数 →
 * 3s 后 [onDismiss] 回调 → ViewModel 清空 LiveData
 *
 * **典型使用场景**：Activity/NavHost 根布局中叠加，全 App 唯一入口
 *
 * **层级**：visualkit/components — 设计系统全局反馈组件层
 *
 * @param item 当前要展示的 Toast 数据，为 null 时不显示
 * @param onDismiss 3秒超时或用户操作后的关闭回调，调用方负责清空 item
 * @param onUndo Undo 按钮点击回调（仅当 [ToastCenter.ToastItem.undo] 非 null 时展示）
 * @param onEditAction Edit 按钮点击回调（仅当 [ToastCenter.ToastItem.editAction] 非 null 时展示）
 * @param modifier 外部 Modifier，通常传入底部安全区内边距
 */
@Composable
fun ToastOverlay(
    item: ToastCenter.ToastItem?,
    onDismiss: () -> Unit,
    onUndo: (() -> Unit)? = null,
    onEditAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scheme = LocalSWScheme.current

    // 自动消失：每当 item 变化时重新计时，3秒后触发 onDismiss；item 为 null 时不启动计时
    LaunchedEffect(item) {
        if (item != null) {
            delay(3000)  // 3秒自动消失，与 iOS Toast.swift 保持一致
            onDismiss()
        }
    }

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
        // 进出场动画：底部滑入/滑出 + 渐显/渐隐，item 非 null 时可见
        AnimatedVisibility(
            visible = item != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            // safeItem：AnimatedVisibility 退出动画期间 item 可能变为 null，提前捕获防止空指针
            val safeItem = item ?: return@AnimatedVisibility
            // 解析消息文字：支持带占位符的字符串资源（args 非空时使用格式化版本）
            val text = if (safeItem.args.isNotEmpty())
                stringResource(safeItem.messageRes, *safeItem.args.toTypedArray())
            else stringResource(safeItem.messageRes)
            // 语义颜色：根据 Toast 样式映射到对应的主题语义色
            val tint: Color = when (safeItem.style) {
                ToastCenter.Style.INFO -> SWColor.primary(scheme)      // 信息：主色
                ToastCenter.Style.SUCCESS -> SWColor.success(scheme)   // 成功：绿色
                ToastCenter.Style.WARNING -> SWColor.warning(scheme)   // 警告：黄色
                ToastCenter.Style.ERROR -> SWColor.danger(scheme)      // 错误：红色
            }
            Box(
                modifier = Modifier
                    .padding(SWSpacing.md)
                    .clip(RoundedCornerShape(SWRadius.pill))  // 胶囊形外形，与 SoftButton 保持一致
                    .background(SWColor.surfaceElevated(scheme))
                    .padding(horizontal = SWSpacing.md, vertical = SWSpacing.sm)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SWSpacing.sm)) {
                    // 语义色圆点指示器：8dp 实心圆，颜色对应 Toast 类型
                    Box(Modifier.size(8.dp).background(tint, RoundedCornerShape(4.dp)))
                    BasicText(
                        text = text,
                        style = SWFont.bodyMD().copy(color = SWColor.textPrimary(scheme))
                    )
                    // Undo 操作按钮：仅当 ToastItem 携带 undo 数据且调用方提供回调时展示
                    if (safeItem.undo != null && onUndo != null) {
                        Spacer(Modifier.size(SWSpacing.xs))
                        ToastAction(label = "Undo", color = tint, onClick = onUndo)
                    }
                    // Edit 操作按钮：仅当 ToastItem 携带 editAction 数据且调用方提供回调时展示
                    if (safeItem.editAction != null && onEditAction != null) {
                        Spacer(Modifier.size(SWSpacing.xs))
                        ToastAction(label = "Edit", color = tint, onClick = onEditAction)
                    }
                }
            }
        }
    }
}

/**
 * Toast 内联操作按钮（私有辅助组件）。
 *
 * 渲染半透明圆角胶囊形的操作文字按钮，背景为语义色 12% 透明度，
 * 文字使用完整语义色，形成轻量"贴纸"视觉效果。
 *
 * @param label 按钮文字（如 "Undo"、"Edit"）
 * @param color 语义色，决定背景和文字颜色
 * @param onClick 点击回调
 */
@Composable
private fun ToastAction(label: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(SWRadius.pill))
            .background(color.copy(alpha = 0.12f))  // 12% 透明语义色背景，低调不抢主消息
            .clickable(onClick = onClick)
            .padding(horizontal = SWSpacing.sm, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        BasicText(text = label, style = SWFont.labelMD().copy(color = color))
    }
}
