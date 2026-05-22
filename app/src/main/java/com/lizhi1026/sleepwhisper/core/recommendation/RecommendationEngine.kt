package com.lizhi1026.sleepwhisper.core.recommendation

import com.lizhi1026.sleepwhisper.model.Baby
import com.lizhi1026.sleepwhisper.model.SleepRecommendation
import com.lizhi1026.sleepwhisper.model.SleepSession
import com.lizhi1026.sleepwhisper.model.WakeWindowRule
import java.util.UUID

/**
 * 睡眠推荐引擎（纯函数，无 Android 依赖）。
 *
 * 所在层：core/recommendation — 领域逻辑层，不依赖任何 Android Framework 类。
 * 交互对象：[Baby]、[SleepSession]（输入）；[SleepRecommendation]（输出）；[WakeWindowRule]（年龄表查找）。
 *
 * 算法 v1（启发式）：
 *   1. 按月龄查年龄表，获取 [minMinutes, maxMinutes] 清醒窗口范围。
 *   2. 若近 7 日有效睡眠（endAt != null 且时长 > 10 分钟）≥1 条，
 *      使用滚动均值作为个性化调整；否则取年龄表中位值。
 *   3. 推荐起始时刻 = 最后醒来时刻 + finalMinutes，窗口宽度 = max - min 分钟。
 *   4. 置信度：有历史 0.78，无历史 0.45。
 *
 * 单测重点：纯函数，可直接传入构造好的 Baby / List<SleepSession> 验证输出。
 * Note: averageWakeWindowMinutes 过滤 [20, 480] 分钟范围外的样本——
 *   过短可能是误点，过长可能是缺失 endAt 或跨天记录。
 */
object RecommendationEngine {

    /**
     * 核心计算入口：根据宝宝信息和近期睡眠记录，生成下一次睡眠窗口推荐。
     *
     * @param baby        宝宝档案，用于计算月龄。
     * @param recentSleeps 近期睡眠列表（建议传入最近 7 天数据）。
     * @param now         当前时间戳（毫秒），默认为系统时间，测试时可注入固定值。
     * @return [SleepRecommendation] 包含推荐窗口、置信度及推理键列表。
     */
    fun compute(
        baby: Baby,
        recentSleeps: List<SleepSession>,
        now: Long = System.currentTimeMillis()
    ): SleepRecommendation {
        val ageMonths = baby.ageInMonths(now)
        val rule = WakeWindowRule.rule(forAgeMonths = ageMonths) // 按月龄查年龄规则表

        val lastWake = lastWakeTime(recentSleeps) ?: now // 最后一次醒来时刻；无记录则用当前时刻
        val baseMinutes = rule.minMinutes + (rule.maxMinutes - rule.minMinutes) / 2 // 年龄表中位值

        // 过滤有效睡眠记录（已结束且时长超过 10 分钟）
        val validSleeps = recentSleeps.filter { it.endAt != null && it.durationSeconds(now) > 600 }
        val usableHistory = validSleeps.isNotEmpty()
        val personalAdjust = if (usableHistory) averageWakeWindowMinutes(validSleeps) else null // 个性化均值，无历史则为 null

        val finalMinutes = personalAdjust ?: baseMinutes // 优先使用个性化值，回退到年龄表中位值
        val confidence = if (usableHistory) 0.78 else 0.45 // 有历史数据置信度更高

        val nextStart = lastWake + finalMinutes * 60_000L            // 推荐入睡起始时刻（毫秒）
        val nextEnd = nextStart + (rule.maxMinutes - rule.minMinutes) * 60_000L // 推荐入睡窗口结束时刻

        // 构建推理键列表，供 UI 展示算法依据
        val reasoning = if (usableHistory)
            listOf("age_table_v2", "rolling_avg_7d", "last_wake_at")
        else
            listOf("age_table_v2", "insufficient_history", "last_wake_at")

        return SleepRecommendation(
            id = UUID.randomUUID().toString(),
            babyId = baby.id,
            computedAt = now,
            nextWindowStart = nextStart,
            nextWindowEnd = nextEnd,
            basedOnAgeMonths = ageMonths,
            wakeWindowMinutes = finalMinutes,
            confidence = confidence,
            reasoningKeys = reasoning
        )
    }

    /**
     * 首页卡片实时显示辅助函数：计算当前剩余清醒窗口时间。
     *
     * @return Pair<剩余分钟数, 总窗口分钟数>。剩余值可为负数，表示宝宝已超出推荐清醒时长。
     */
    fun currentRemainingWindow(
        baby: Baby,
        recentSleeps: List<SleepSession>,
        now: Long = System.currentTimeMillis()
    ): Pair<Int, Int> {
        val rule = WakeWindowRule.rule(forAgeMonths = baby.ageInMonths(now))
        val avg = (rule.minMinutes + rule.maxMinutes) / 2             // 年龄表清醒窗口中位值（分钟）
        val lastWake = lastWakeTime(recentSleeps) ?: return avg to avg // 无记录时直接返回中位值
        val elapsed = ((now - lastWake) / 60_000L).toInt()            // 已清醒时长（分钟）
        return (avg - elapsed) to avg
    }

    /**
     * 从睡眠列表中提取最后一次醒来（endAt）的时间戳。
     * 若列表为空或所有记录均未结束，则返回 null。
     */
    private fun lastWakeTime(sleeps: List<SleepSession>): Long? =
        sleeps.mapNotNull { it.endAt }.maxOrNull()

    /**
     * 计算相邻睡眠记录之间清醒时长的平均值（分钟）。
     *
     * 过滤逻辑：仅保留 (20, 480) 分钟范围内的清醒间隔——
     * 过短（≤20 min）视为误操作，过长（≥480 min）视为跨天或数据缺失。
     *
     * @return 平均清醒窗口分钟数；若无有效样本则返回 null。
     */
    private fun averageWakeWindowMinutes(sleeps: List<SleepSession>): Int? {
        val sorted = sleeps.sortedBy { it.startAt } // 按开始时间升序排列
        val windows = mutableListOf<Int>()
        for (i in 1 until sorted.size) {
            val prevEnd = sorted[i - 1].endAt ?: continue // 上一次睡眠未结束则跳过
            val diff = ((sorted[i].startAt - prevEnd) / 60_000L).toInt() // 相邻睡眠间隔（分钟）
            if (diff > 20 && diff < 480) windows.add(diff) // 仅保留合理范围内的样本
        }
        if (windows.isEmpty()) return null
        return windows.sum() / windows.size // 整数均值
    }
}
