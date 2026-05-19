package com.lizhi1026.sleepwhisper.model

import kotlinx.serialization.Serializable

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
    enum class RecommendationOutcome(val serializedName: String) {
        PENDING("pending"),
        ADOPTED("adopted"),
        MISSED("missed"),
        OVERRIDDEN("overridden")
    }

    /** 当前时间是否落在推荐窗口内。 */
    fun isActive(now: Long = System.currentTimeMillis()): Boolean = now in nextWindowStart..nextWindowEnd

    /** 推荐窗口是否已经过去。 */
    fun hasPassed(now: Long = System.currentTimeMillis()): Boolean = now > nextWindowEnd
}
