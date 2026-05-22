/**
 * RecentEventsList.kt — 近 24 小时事件列表（UI 层 / features/trends）
 *
 * 职责：
 * - 渲染 ViewModel 提供的近 24 h 喂食 + 尿布合并列表（最多 8 条）
 * - 每行展示事件类型标签、详情（喂食量 / "—"）和发生时间
 * - **长按任意行** → 调用 [onLongPress] → ViewModel 调用
 *   `AppStateContainer.requestEdit(target)` → 弹出 EventEditSheet 进行编辑
 * - 列表为空时显示占位符"—"
 */
package com.lizhi1026.sleepwhisper.features.trends

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import com.lizhi1026.sleepwhisper.core.strings.displayKey
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor
import com.lizhi1026.sleepwhisper.core.visualkit.SWFont
import com.lizhi1026.sleepwhisper.core.visualkit.SWSpacing
import com.lizhi1026.sleepwhisper.core.visualkit.components.GlassCard
import com.lizhi1026.sleepwhisper.model.FeedingEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 近 24 h 事件列表 Composable（最多显示 8 条）。
 *
 * 包含标题、长按操作提示文字，以及每条事件的 [EventRow]。
 * 列表为空时显示"—"占位符而非空白区域。
 *
 * @param events 来自 [TrendsViewModel.recentEvents] 的事件列表
 * @param onLongPress 长按某行时的回调，参数为被长按的 [RecentEvent]；
 *   调用方通常将其绑定到 [TrendsViewModel.beginEdit]
 * @param modifier 外部传入的 Modifier
 */
@Composable
fun RecentEventsList(
    events: List<RecentEvent>,
    onLongPress: (RecentEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalSWScheme.current
    // 时间格式化器，复用于所有行，用 remember 避免重组时重新分配
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    GlassCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(SWSpacing.sm)) {
            // 列表区段标题
            BasicText(
                text = stringResource(com.lizhi1026.sleepwhisper.R.string.trends_recentevents_title),
                style = SWFont.titleMD().copy(color = SWColor.textPrimary(scheme))
            )
            // 长按操作提示（三级文字色，较淡）
            BasicText(
                text = stringResource(com.lizhi1026.sleepwhisper.R.string.trends_recentevents_hint),
                style = SWFont.labelSM().copy(color = SWColor.textTertiary(scheme))
            )
            if (events.isEmpty()) {
                // 无近期事件时显示破折号占位，避免空白
                BasicText(
                    text = "—",
                    style = SWFont.bodyMD().copy(color = SWColor.textSecondary(scheme))
                )
            } else {
                // 遍历事件列表，每项渲染为一行可长按的 EventRow
                events.forEach { item ->
                    EventRow(item, timeFormat, onLongPress)
                }
            }
        }
    }
}

/**
 * 单条事件行：左侧显示事件名称 + 详情，右侧显示时间戳（HH:mm）。
 *
 * 通过 [pointerInput] + [detectTapGestures] 监听长按手势，
 * 长按触发 [onLongPress] 回调，进而由 ViewModel 打开 EventEditSheet。
 *
 * @param event 要渲染的事件（[RecentEvent.OfFeeding] 或 [RecentEvent.OfDiaper]）
 * @param timeFormat 共享的 SimpleDateFormat 实例（HH:mm）
 * @param onLongPress 长按回调
 */
@Composable
private fun EventRow(
    event: RecentEvent,
    timeFormat: SimpleDateFormat,
    onLongPress: (RecentEvent) -> Unit
) {
    val scheme = LocalSWScheme.current
    // 根据事件类型解构出展示标签和详情文字
    val (label, detail) = when (event) {
        // 喂食：标签来自喂食方式国际化字符串，详情为毫升数
        is RecentEvent.OfFeeding -> stringResource(event.event.method.displayKey()) to feedingDetail(event.event)
        // 尿布：标签来自尿布类型国际化字符串，详情固定为"—"
        is RecentEvent.OfDiaper -> stringResource(event.event.type.displayKey()) to "—"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(event) {
                // 监听长按手势，触发编辑入口
                detectTapGestures(onLongPress = { onLongPress(event) })
            }
            .padding(vertical = SWSpacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            // 主标签（事件类型名）
            BasicText(
                text = label,
                style = SWFont.bodyMD().copy(color = SWColor.textPrimary(scheme))
            )
            // 副标签（喂食量 / "—"）
            BasicText(
                text = detail,
                style = SWFont.labelSM().copy(color = SWColor.textSecondary(scheme))
            )
        }
        // 右侧时间戳（HH:mm 格式）
        BasicText(
            text = timeFormat.format(Date(event.timestamp)),
            style = SWFont.labelMD().copy(color = SWColor.textSecondary(scheme))
        )
    }
}

/**
 * 从喂食事件中提取可读的毫升数详情字符串。
 * 若 [FeedingEvent.amountMl] 为 null（未记录喂食量）则返回"—"。
 */
@Composable
private fun feedingDetail(f: FeedingEvent): String {
    val ml = f.amountMl ?: return "—"
    return "$ml ml"
}
