/**
 * 睡眠会话实体模型。
 *
 * **职责**：记录一次完整或进行中的宝宝睡眠，包含类型、开始/结束时间、质量及辅助信息。
 *
 * **所在层**：model 层（领域模型），与 iOS `SleepSession.swift` 字段一一对应。
 *
 * **与谁交互**：
 * - [AppStateContainer]（app 层）— 持有 `_ongoingSleep`，通过 `startSleep` / `endSleep` 写入。
 * - [Repository]（core/persistence）— 通过 `appendSleep` / `updateSleep` / `sleepSessions` 读写。
 * - [RecommendationEngine]（core/recommendation）— 以历史会话列表作为输入计算推荐窗口。
 * - [SleepSummary]（model 层）— 由 [AppStateContainer.buildSleepSummary] 聚合多个会话生成。
 *
 * **关键约定**：
 * - `startAt` / `endAt` 均为 UTC epoch 毫秒；`endAt == null` 表示会话仍在进行中（见 [isOngoing]）。
 * - `audioPresetId` 为可空字符串，与 [AudioPreset.id] 关联但不强制外键约束。
 */
package com.lizhi1026.sleepwhisper.model

import kotlinx.serialization.Serializable

/**
 * 睡眠会话数据类。
 *
 * @property id               唯一标识符（UUID 字符串）。
 * @property babyId           关联的宝宝 ID。
 * @property type             睡眠类型（白天小睡、夜间睡眠或接触式小睡）。
 * @property startAt          睡眠开始时间，UTC epoch 毫秒。
 * @property endAt            睡眠结束时间，UTC epoch 毫秒；`null` 表示会话进行中。
 * @property quality          睡眠质量评级，默认 [SleepQuality.UNKNOWN]。
 * @property fallAsleepMinutes 入睡用时（分钟），可为 null（未记录）。
 * @property wakeCount        夜醒次数，默认 0。
 * @property audioPresetId    播放中的音频预设 ID，可为 null（未播放）。
 * @property isEdited         是否经过用户手动编辑，默认 false。
 * @property notes            用户备注，可为 null。
 */
@Serializable
data class SleepSession(
    val id: String,
    val babyId: String,
    val type: SleepType = SleepType.NAP,
    val startAt: Long, // epoch-millis UTC
    val endAt: Long? = null, // null → ongoing
    val quality: SleepQuality = SleepQuality.UNKNOWN,
    val fallAsleepMinutes: Int? = null,
    val wakeCount: Int = 0,
    val audioPresetId: String? = null,
    val isEdited: Boolean = false,
    val notes: String? = null
) {
    enum class SleepType(val serializedName: String) {
        NAP("nap"),
        NIGHT("night"),
        CONTACT_NAP("contact_nap")
    }

    enum class SleepQuality(val serializedName: String) {
        GOOD("good"),
        OK("ok"),
        FUSSY("fussy"),
        UNKNOWN("unknown")
    }

    /** 是否仍在进行中（用户没点"醒来"）。 */
    val isOngoing: Boolean get() = endAt == null

    /**
     * 计算会话的实时或最终时长（秒）。
     *
     * 若会话仍在进行中（[endAt] == null），则以 [now] 作为结束时间动态计算，
     * 可在 UI 层定时刷新以展示实时计时器。
     *
     * @param now 当前时刻，UTC epoch 毫秒，默认为系统当前时间。
     * @return 时长（秒），最小值为 0。
     */
    fun durationSeconds(now: Long = System.currentTimeMillis()): Long =
        ((endAt ?: now) - startAt) / 1000
}
