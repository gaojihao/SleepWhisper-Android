package com.lizhi1026.sleepwhisper.model

import kotlinx.serialization.Serializable

// 值直接照搬 iOS SleepRecommendation.swift WakeWindowRule.table

@Serializable
data class WakeWindowRule(
    val ageMinMonths: Int,
    val ageMaxMonths: Int,
    val minMinutes: Int,
    val maxMinutes: Int
) {
    companion object {
        val table: List<WakeWindowRule> = listOf(
            WakeWindowRule(0, 1, 45, 60),
            WakeWindowRule(1, 2, 60, 75),
            WakeWindowRule(2, 3, 75, 90),
            WakeWindowRule(3, 4, 90, 105),
            WakeWindowRule(4, 6, 105, 135),
            WakeWindowRule(6, 9, 135, 165),
            WakeWindowRule(9, 12, 165, 210),
            WakeWindowRule(12, 18, 210, 300),
            WakeWindowRule(18, 36, 300, 360)
        )

        /** 查表：[min, max) 半开区间，超出最大档默认取最后一档。 */
        fun rule(forAgeMonths: Int): WakeWindowRule =
            table.firstOrNull { forAgeMonths >= it.ageMinMonths && forAgeMonths < it.ageMaxMonths } ?: table.last()
    }
}
