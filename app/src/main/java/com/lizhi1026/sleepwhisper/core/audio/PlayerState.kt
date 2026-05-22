/**
 * 音频播放器状态机（sealed class）。
 *
 * 所在层：core/audio — 应用核心层，不依赖 UI。
 * 交互对象：
 *   - [AudioPlayerService]：负责写入状态转换。
 *   - ViewModel / Composable：通过 [AudioPlayerService.stateLive] 观察并渲染 UI。
 *
 * 状态流转概览：
 *   Idle → Loading → Playing ⇄ Paused
 *                          ↓
 *                     FadingOut → Stopped
 *   Playing → Interrupted（短暂音频焦点丢失）→ Playing（焦点恢复后自动恢复）
 *   任意状态 → Error（播放器抛出异常）
 */
package com.lizhi1026.sleepwhisper.core.audio

sealed class PlayerState {
    /** 初始态：播放器尚未被使用，无任何资源占用。 */
    data object Idle : PlayerState()

    /**
     * 正在加载资源（ExoPlayer prepare 阶段）。
     * @param presetId 正在加载的音频预设 ID。
     */
    data class Loading(val presetId: String) : PlayerState()

    /**
     * 正在播放中。
     * @param presetId 当前播放的音频预设 ID。
     * @param endsAt   定时停止的绝对时间戳（毫秒，epoch）；null 表示不限时播放。
     */
    data class Playing(val presetId: String, val endsAt: Long?) : PlayerState()

    /**
     * 用户主动暂停。
     * @param presetId    暂停时的音频预设 ID。
     * @param remainingMs 暂停时剩余的定时毫秒数；null 表示原本不限时。
     */
    data class Paused(val presetId: String, val remainingMs: Long?) : PlayerState()

    /**
     * 正在执行淡出动画（音量渐降至 0），淡出完成后转为 [Stopped]。
     * @param presetId 正在淡出的音频预设 ID。
     */
    data class FadingOut(val presetId: String) : PlayerState()

    /** 已完全停止，ExoPlayer 资源已释放，音频焦点已归还。 */
    data object Stopped : PlayerState()

    /**
     * 被系统或其他应用短暂抢占音频焦点（AUDIOFOCUS_LOSS_TRANSIENT）而暂停。
     * 焦点恢复后（AUDIOFOCUS_GAIN）将自动恢复播放。
     * @param restoreTo 恢复播放时使用的音频预设 ID；null 表示无法确定，不自动恢复。
     */
    data class Interrupted(val restoreTo: String?) : PlayerState()

    /**
     * 播放器发生不可恢复错误。
     * @param message 错误描述，可直接展示给用户或用于日志上报。
     */
    data class Error(val message: String) : PlayerState()
}
