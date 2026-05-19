package com.lizhi1026.sleepwhisper.core.recommendation

import com.lizhi1026.sleepwhisper.model.Baby
import com.lizhi1026.sleepwhisper.model.SleepRecommendation
import com.lizhi1026.sleepwhisper.model.SleepSession
import com.lizhi1026.sleepwhisper.model.WakeWindowRule
import java.util.UUID

/**
 * Pure recommendation engine — direct port of iOS Core/Recommendation/RecommendationEngine.swift.
 *
 * Algorithm (v1, heuristic):
 *   1. Look up age table for [minMinutes, maxMinutes] window.
 *   2. If recent 7-day history contains ≥1 valid sleep (endAt != null and duration > 10 min),
 *      use the rolling-average wake-window as the personalized adjustment;
 *      otherwise use the age-table median.
 *   3. Next window = lastWakeTime + finalMinutes, width = (max - min) minutes.
 *   4. Confidence: 0.78 with history, 0.45 without.
 *
 * Note: averageWakeWindowMinutes filters samples outside [20, 480] minutes — too short is likely
 * accidental tap; too long is likely missing endAt or cross-day session.
 */
object RecommendationEngine {

    fun compute(
        baby: Baby,
        recentSleeps: List<SleepSession>,
        now: Long = System.currentTimeMillis()
    ): SleepRecommendation {
        val ageMonths = baby.ageInMonths(now)
        val rule = WakeWindowRule.rule(forAgeMonths = ageMonths)

        val lastWake = lastWakeTime(recentSleeps) ?: now
        val baseMinutes = rule.minMinutes + (rule.maxMinutes - rule.minMinutes) / 2

        val validSleeps = recentSleeps.filter { it.endAt != null && it.durationSeconds(now) > 600 }
        val usableHistory = validSleeps.isNotEmpty()
        val personalAdjust = if (usableHistory) averageWakeWindowMinutes(validSleeps) else null

        val finalMinutes = personalAdjust ?: baseMinutes
        val confidence = if (usableHistory) 0.78 else 0.45

        val nextStart = lastWake + finalMinutes * 60_000L
        val nextEnd = nextStart + (rule.maxMinutes - rule.minMinutes) * 60_000L

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
     * Real-time display helper for Home card.
     * Returns (remainingMinutes, totalWindowMinutes). `remainingMinutes` can be negative
     * (means baby has been awake longer than recommended window).
     */
    fun currentRemainingWindow(
        baby: Baby,
        recentSleeps: List<SleepSession>,
        now: Long = System.currentTimeMillis()
    ): Pair<Int, Int> {
        val rule = WakeWindowRule.rule(forAgeMonths = baby.ageInMonths(now))
        val avg = (rule.minMinutes + rule.maxMinutes) / 2
        val lastWake = lastWakeTime(recentSleeps) ?: return avg to avg
        val elapsed = ((now - lastWake) / 60_000L).toInt()
        return (avg - elapsed) to avg
    }

    private fun lastWakeTime(sleeps: List<SleepSession>): Long? =
        sleeps.mapNotNull { it.endAt }.maxOrNull()

    private fun averageWakeWindowMinutes(sleeps: List<SleepSession>): Int? {
        val sorted = sleeps.sortedBy { it.startAt }
        val windows = mutableListOf<Int>()
        for (i in 1 until sorted.size) {
            val prevEnd = sorted[i - 1].endAt ?: continue
            val diff = ((sorted[i].startAt - prevEnd) / 60_000L).toInt()
            if (diff > 20 && diff < 480) windows.add(diff)
        }
        if (windows.isEmpty()) return null
        return windows.sum() / windows.size
    }
}
