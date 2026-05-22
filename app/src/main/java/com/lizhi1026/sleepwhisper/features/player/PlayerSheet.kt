/**
 * PlayerSheet — 白噪音播放器的底部弹窗容器（UI 层 / features/player）
 *
 * 职责：
 *   - 以 Material3 [ModalBottomSheet] 包裹 [PlayerScreen]（embedded = true），
 *     提供从 Home / Sleeping 等屏幕快速呼出播放器的能力。
 *   - 强制注入 [SWScheme.DARK] CompositionLocal，确保弹窗在亮色或深色背景下
 *     均呈现一致的深色视觉风格。
 *   - 弹窗自带 M3 系统拖拽手柄；PlayerScreen 内部的 DragHandle 在
 *     embedded=true 时自动隐藏，避免重复。
 *
 * 调用方只需传入 [onDismiss] 回调，弹窗状态由内部管理。
 */
package com.lizhi1026.sleepwhisper.features.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor
import com.lizhi1026.sleepwhisper.core.visualkit.SWScheme

// 白噪音选择器的底部弹窗包装器。
// 强制 DARK 方案 + 深色容器背景，无论从亮色（Home 极光背景）或
// 深色（Sleeping 沉浸背景）屏幕唤起，弹窗外观均保持一致。
// ModalBottomSheet 的默认 M3 拖拽手柄保留；
// PlayerScreen 内部 DragHandle 在 embedded=true 时不渲染。
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSheet(onDismiss: () -> Unit) {
    // skipPartiallyExpanded=false 允许弹窗停留在半展开状态，便于单手操作。
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SWColor.surface(SWScheme.DARK)
    ) {
        // 向下层 Composable 注入 DARK 主题方案，覆盖系统主题。
        CompositionLocalProvider(LocalSWScheme provides SWScheme.DARK) {
            Box(modifier = Modifier.fillMaxSize()) {
                PlayerScreen(embedded = true)
            }
        }
    }
}
