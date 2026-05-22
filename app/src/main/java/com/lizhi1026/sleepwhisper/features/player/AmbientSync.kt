/**
 * AmbientSync — 播放预设与 HeroBackdrop 背景色同步工具（UI 层 / features/player）
 *
 * 职责：
 *   - 监听 [PlayerViewModel.currentPreset] 的 [AuraColors]，
 *     将其 mid 色写入 [HeroBackdropController]，使全屏极光背景跟随预设变色。
 *   - 以 [HeroBackdropController] 扩展函数形式提供，调用方只需传入当前预设与可选的
 *     fallback 颜色，无需了解底层 setAmbient API。
 *
 * 优先级（高到低）：
 *   1. currentPreset.auraColors.mid（预设激活时）
 *   2. fallback（调用方提供的上下文色，如 Home 屏的时间段默认色）
 *   3. null（HeroBackdropController 使用默认极光调色板）
 *
 * 典型调用位置：PlayerScreen 的 LaunchedEffect(current, embedded) 内。
 */
package com.lizhi1026.sleepwhisper.features.player

import androidx.compose.ui.graphics.Color
import com.lizhi1026.sleepwhisper.core.visualkit.HeroBackdropController
import com.lizhi1026.sleepwhisper.model.AudioPreset

/**
 * 将激活预设的 mid-aura 颜色写入 [HeroBackdropController]，驱动背景色同步。
 * 若无激活预设则回退到 [fallback]；[fallback] 也为 null 时 backdrop 使用默认极光色。
 *
 * @param currentPreset 当前正在播放的音频预设；停止播放时传 null。
 * @param fallback      调用方的上下文颜色兜底（如 Home 的时间段色）；无兜底需求时传 null。
 */
fun HeroBackdropController.syncToPlayer(
    currentPreset: AudioPreset?,
    fallback: Color?
) {
    // 优先取预设的 mid 色；预设为 null 时降级到 fallback（可能也为 null）。
    setAmbient(currentPreset?.auraColors?.mid ?: fallback)
}
