package com.lizhi1026.sleepwhisper.core.recommendation

import com.lizhi1026.sleepwhisper.model.Baby
import com.lizhi1026.sleepwhisper.model.SleepSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.UUID

class RecommendationEngineTest {

    /** Anchor "now" to a fixed point so age-from-DOB is deterministic. */
    private val now: Long = ZonedDateTime.of(2026, 5, 19, 12, 0, 0, 0, ZoneOffset.UTC).toInstant().toEpochMilli()
    private val sixMonthsAgo: Long = ZonedDateTime.of(2025, 11, 19, 12, 0, 0, 0, ZoneOffset.UTC).toInstant().toEpochMilli()

    private fun makeBaby(dobMs: Long = sixMonthsAgo) = Baby(
        id = UUID.randomUUID().toString(),
        name = "Test",
        gender = Baby.BabyGender.UNKNOWN,
        dateOfBirth = dobMs,
        avatarLocalPath = null,
        createdAt = now - 1000L,
        updatedAt = now - 1000L
    )

    private fun makeSleep(start: Long, end: Long?, babyId: String) = SleepSession(
        id = UUID.randomUUID().toString(),
        babyId = babyId,
        type = SleepSession.SleepType.NAP,
        startAt = start,
        endAt = end
    )

    @Test fun `no history yields confidence 0_45 and uses age table median`() {
        val baby = makeBaby()  // 6 months → rule (6,9,135,165), median = 150
        val rec = RecommendationEngine.compute(baby, recentSleeps = emptyList(), now = now)
        assertEquals(0.45, rec.confidence, 0.0001)
        assertEquals(150, rec.wakeWindowMinutes)
        assertTrue(rec.reasoningKeys.contains("insufficient_history"))
    }

    @Test fun `with history yields confidence 0_78`() {
        val baby = makeBaby()
        // Build several valid sleeps with wake-windows around 120 min, all > 10 min duration
        val s1 = makeSleep(start = now - 12 * 3600_000L, end = now - 11 * 3600_000L, baby.id)
        // wake window between s1.end and s2.start = 120 min
        val s2 = makeSleep(start = now - 9 * 3600_000L,  end = now - 8  * 3600_000L, baby.id)
        // wake window 120 min
        val s3 = makeSleep(start = now - 6 * 3600_000L,  end = now - 5  * 3600_000L, baby.id)
        val rec = RecommendationEngine.compute(baby, listOf(s1, s2, s3), now = now)
        assertEquals(0.78, rec.confidence, 0.0001)
        assertTrue(rec.reasoningKeys.contains("rolling_avg_7d"))
        assertEquals(120, rec.wakeWindowMinutes)
    }

    @Test fun `wake window samples outside 20-480 min range are filtered out`() {
        val baby = makeBaby()
        // first wake window = 15 min (too short, filtered), second = 100 min (valid)
        val s1 = makeSleep(start = now - 10 * 3600_000L, end = now - 9 * 3600_000L, baby.id)
        val s2 = makeSleep(start = now - (9 * 3600_000L - 15 * 60_000L), end = now - 7 * 3600_000L, baby.id)
        val s3 = makeSleep(start = now - (7 * 3600_000L - 100 * 60_000L), end = now - 5 * 3600_000L, baby.id)
        val rec = RecommendationEngine.compute(baby, listOf(s1, s2, s3), now = now)
        // Average of [100] = 100
        assertEquals(100, rec.wakeWindowMinutes)
    }

    @Test fun `ongoing sleep (endAt null) is excluded from history`() {
        val baby = makeBaby()
        val ongoing = makeSleep(start = now - 30 * 60_000L, end = null, baby.id)
        val rec = RecommendationEngine.compute(baby, listOf(ongoing), now = now)
        // Only one sleep, and it's ongoing → no valid history
        assertEquals(0.45, rec.confidence, 0.0001)
    }

    @Test fun `next window width equals max minus min of age rule`() {
        val baby = makeBaby()  // 6 months → rule.max-min = 165-135 = 30 min
        val rec = RecommendationEngine.compute(baby, emptyList(), now = now)
        val widthMs = rec.nextWindowEnd - rec.nextWindowStart
        assertEquals(30 * 60_000L, widthMs)
    }

    @Test fun `currentRemainingWindow returns expected pair when no history`() {
        val baby = makeBaby()
        val (remaining, total) = RecommendationEngine.currentRemainingWindow(baby, emptyList(), now)
        assertEquals(total, remaining)  // 没有历史时初始与总宽相等
        assertEquals(150, total) // (135+165)/2 = 150
    }
}
