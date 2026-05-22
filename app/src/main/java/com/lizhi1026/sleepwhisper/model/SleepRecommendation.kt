/**
 * 单次睡眠推荐窗口数据模型（model 领域层，纯 Kotlin，无 Android Framework 依赖）。
 *
 * **职责**：表示推荐引擎为某宝宝计算出的下一次最佳入睡窗口，包含窗口时间范围、
 * 基准年龄、清醒时长以及置信度，同时记录推荐最终的采纳状态。
 *
 * **交互方**：`RecommendationRepository`（持久化 & 查询）、`RecommendationEngine`
 * （生产此对象）、`HomeViewModel` / `PlayerViewModel`（消费并在 UI 中展示推荐提示）。
 *
 * **时间字段约定**：所有 `Long` 时间字段均为 **epoch-millis UTC**，
 * 与 `System.currentTimeMillis()` 同单位，可直接比较。
 */
package com.lizhi1026.sleepwhisper.model

import kotlinx.serialization.Serializable

/**
 * 睡眠推荐窗口实体，不可变数据类。
 *
 * @property id               推荐记录的唯一标识符（UUID 字符串）
 * @property babyId           所属宝宝的 ID，与 `Baby.id` 对应
 * @property computedAt       推荐计算完成的时间戳（epoch-millis UTC）
 * @property nextWindowStart  推荐入睡窗口开始时间（epoch-millis UTC）
 * @property nextWindowEnd    推荐入睡窗口结束时间（epoch-millis UTC）
 * @property basedOnAgeMonths 计算本条推荐时宝宝的月龄（整月数）
 * @property wakeWindowMinutes 本次推荐所依据的清醒时长上限（分钟）
 * @property confidence       推荐置信度，范围 0.0–1.0，越高表示推荐越可靠
 * @property reasoningKeys    推理依据的国际化 key 列表，用于 UI 展示推荐理由
 * @property outcome          推荐的最终采纳状态，见 [RecommendationOutcome]
 */
@Serializable
data class SleepRecommendation(
    val id: String,
    val babyId: String,
    val computedAt: Long, // epoch-millis UTC
    val nextWindowStart: Long, // epoch-millis UTC
    val nextWindowEnd: Long, // epoch-millis UTC
    val basedOnAgeMonths: Int,
    val wakeWindowMinutes: Int,
    val confidence: Double,
    val reasoningKeys: List<String> = emptyList(),
    val outcome: RecommendationOutcome = RecommendationOutcome.PENDING
) {
    /**
     * 推荐结果的最终采纳状态枚举。
     *
     * @property serializedName JSON 序列化字符串值，需与 iOS / 后端保持一致
     */
    enum class RecommendationOutcome(val serializedName: String) {
        /** 尚未处理（默认状态，窗口未到达或用户未操作） */
        PENDING("pending"),
        /** 用户按推荐时间让宝宝入睡 */
        ADOPTED("adopted"),
        /** 窗口已过，用户未按推荐操作 */
        MISSED("missed"),
        /** 用户手动覆盖了推荐（如提前/延后入睡） */
        OVERRIDDEN("overridden")
    }

    /**
     * 判断当前时刻是否落在推荐的入睡窗口内。
     *
     * ⚠️ 使用 `System.currentTimeMillis()` 作为默认时钟，不注入外部 clock，
     * 生产代码直接调用无参版本即可。测试时可通过 [now] 参数注入固定时间。
     *
     * @param now 当前时间戳（epoch-millis UTC），默认 `System.currentTimeMillis()`
     * @return true 表示窗口正在进行中，此时应高亮提示用户
     */
    fun isActive(now: Long = System.currentTimeMillis()): Boolean = now in nextWindowStart..nextWindowEnd

    /**
     * 判断推荐窗口是否已完全过去。
     *
     * ⚠️ 同 [isActive]，默认时钟为 `System.currentTimeMillis()`，
     * 测试时可通过 [now] 参数传入固定时间戳。
     *
     * @param now 当前时间戳（epoch-millis UTC），默认 `System.currentTimeMillis()`
     * @return true 表示窗口已结束，推荐状态应更新为 MISSED 或其他终态
     */
    fun hasPassed(now: Long = System.currentTimeMillis()): Boolean = now > nextWindowEnd
}
