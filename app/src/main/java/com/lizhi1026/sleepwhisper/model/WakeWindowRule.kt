/**
 * 基于月龄的清醒时长规则数据模型（model 领域层，纯 Kotlin，无 Android Framework 依赖）。
 *
 * **职责**：将宝宝月龄映射到科学建议的清醒时间窗口（最短–最长分钟数）。
 * 规则表共 9 档，覆盖 0–36 月龄。
 *
 * **数据来源**：直接移植自 iOS SleepWhisperApp.swift 中 `WakeWindowRule.table`，
 * 数值与 iOS 端完全一致，如需修改须两端同步。
 *
 * **交互方**：`RecommendationEngine`（查表计算推荐清醒时长）、
 * `BabyProfileViewModel`（展示当前月龄对应的参考范围）。
 */
package com.lizhi1026.sleepwhisper.model

import kotlinx.serialization.Serializable

// 值直接照搬 iOS SleepRecommendation.swift WakeWindowRule.table

/**
 * 单条清醒时长规则，描述 `[ageMinMonths, ageMaxMonths)` 月龄段对应的推荐清醒时窗。
 *
 * @property ageMinMonths 适用月龄下限（含，整月数）
 * @property ageMaxMonths 适用月龄上限（不含，整月数）；区间为半开区间 `[min, max)`
 * @property minMinutes   该月龄段建议清醒时长的最小值（分钟）
 * @property maxMinutes   该月龄段建议清醒时长的最大值（分钟）
 */
@Serializable
data class WakeWindowRule(
    val ageMinMonths: Int,
    val ageMaxMonths: Int,
    val minMinutes: Int,
    val maxMinutes: Int
) {
    companion object {
        /**
         * 月龄-清醒时窗对照表，共 9 档，覆盖 0–36 月龄。
         *
         * 档位划分（月龄区间 → 清醒时长范围）：
         * - 0–1 月  → 45–60 分钟
         * - 1–2 月  → 60–75 分钟
         * - 2–3 月  → 75–90 分钟
         * - 3–4 月  → 90–105 分钟
         * - 4–6 月  → 105–135 分钟
         * - 6–9 月  → 135–165 分钟
         * - 9–12 月 → 165–210 分钟
         * - 12–18 月 → 210–300 分钟
         * - 18–36 月 → 300–360 分钟
         */
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

        /**
         * 根据月龄查找对应的清醒时长规则。
         *
         * 匹配规则：遍历 [table]，返回第一条满足 `ageMinMonths ≤ forAgeMonths < ageMaxMonths` 的档位。
         *
         * ⚠️ **超出范围处理**：当 [forAgeMonths] > 36 时，没有任何档位匹配，
         * 函数返回 `table.last()`（即 18–36 月档，300–360 分钟），不会抛异常。
         * 负数月龄会匹配第一档（0–1 月）。
         *
         * @param forAgeMonths 宝宝月龄（整月数，0–36+ 均可传入）
         * @return 对应月龄的 [WakeWindowRule]，超出 36 月返回最后一档
         */
        fun rule(forAgeMonths: Int): WakeWindowRule =
            table.firstOrNull { forAgeMonths >= it.ageMinMonths && forAgeMonths < it.ageMaxMonths } ?: table.last()
    }
}
