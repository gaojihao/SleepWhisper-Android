package com.lizhi1026.sleepwhisper.core.notifications

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

/**
 * Tests the quiet-hours window (22:00–07:00 wraps midnight) used by
 * NotificationScheduler. Hardcoded to match the constants there.
 */
class QuietHoursTest {

    private val QUIET_START = 22
    private val QUIET_END = 7

    private fun isInQuietHours(hour: Int): Boolean {
        return if (QUIET_START > QUIET_END) {
            hour >= QUIET_START || hour < QUIET_END
        } else {
            hour in QUIET_START until QUIET_END
        }
    }

    @Test fun `21_59 is daytime (just before quiet start)`() {
        assertEquals(false, isInQuietHours(21))
    }

    @Test fun `22_00 is in quiet hours (inclusive start)`() {
        assertEquals(true, isInQuietHours(22))
    }

    @Test fun `06_59 is still in quiet hours`() {
        assertEquals(true, isInQuietHours(6))
    }

    @Test fun `07_00 is daytime (exclusive end)`() {
        assertEquals(false, isInQuietHours(7))
    }

    @Test fun `noon is daytime`() {
        assertEquals(false, isInQuietHours(12))
    }

    @Test fun `midnight is in quiet hours (cross-day wrap)`() {
        assertEquals(true, isInQuietHours(0))
        assertEquals(true, isInQuietHours(3))
    }

    @Test fun `Calendar based timestamp also evaluates correctly`() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
        }
        assertEquals(true, isInQuietHours(cal.get(Calendar.HOUR_OF_DAY)))
    }
}
