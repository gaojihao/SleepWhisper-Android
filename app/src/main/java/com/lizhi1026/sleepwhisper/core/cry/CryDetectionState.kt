/**
 * 哭声检测服务状态机（sealed class）。
 *
 * 所在层：core/cry — 应用核心层，不依赖 UI。
 * 交互对象：
 *   - [CryDetectionService]：负责写入状态转换。
 *   - ViewModel / Composable：通过 [CryDetectionService.stateLive] 观察并渲染 UI。
 *
 * 状态流转概览：
 *   Disabled → Listening → Triggered → Cooldown → Listening（冷却结束后自动恢复）
 *   任意状态 → Disabled（调用 stop() 后立即进入）
 *
 * 备注：[Triggered] 为瞬时态，实际代码中触发后立即写入 [Cooldown]，
 * 当前未对外暴露，保留以便后续扩展（如动画/埋点）。
 */
package com.lizhi1026.sleepwhisper.core.cry

sealed class CryDetectionState {
    /** 检测未启动（初始态或调用 stop() 后）。 */
    data object Disabled : CryDetectionState()

    /** 麦克风录音中，正在逐帧评估音量是否持续超过阈值。 */
    data object Listening : CryDetectionState()

    /**
     * 已判定为哭声（持续 ≥3s 超过 60dBFS 阈值），回调 onCryDetected 触发后立即
     * 转入 [Cooldown]；此状态为瞬时态，UI 可选择忽略或做短暂动画。
     */
    data object Triggered : CryDetectionState()

    /**
     * 冷却期（触发后 10 s 内不再重复触发）。
     * @param untilMs 冷却结束的绝对时间戳（毫秒，epoch），可用于 UI 倒计时展示。
     */
    data class Cooldown(val untilMs: Long) : CryDetectionState()
}
