package com.lizhi1026.sleepwhisper.model

import org.junit.Assert.assertEquals
import org.junit.Test

class WakeWindowRuleTest {

    @Test fun `age 0 falls into 0-1 month bucket`() {
        val r = WakeWindowRule.rule(forAgeMonths = 0)
        assertEquals(45, r.minMinutes)
        assertEquals(60, r.maxMinutes)
    }

    @Test fun `age 1 falls into 1-2 month bucket (right-open boundary)`() {
        val r = WakeWindowRule.rule(forAgeMonths = 1)
        assertEquals(60, r.minMinutes)
        assertEquals(75, r.maxMinutes)
    }

    @Test fun `age 17 falls into 12-18 bucket`() {
        val r = WakeWindowRule.rule(forAgeMonths = 17)
        assertEquals(210, r.minMinutes)
        assertEquals(300, r.maxMinutes)
    }

    @Test fun `age 18 falls into 18-36 bucket`() {
        val r = WakeWindowRule.rule(forAgeMonths = 18)
        assertEquals(300, r.minMinutes)
        assertEquals(360, r.maxMinutes)
    }

    @Test fun `age 50 (beyond max) falls back to last bucket`() {
        val r = WakeWindowRule.rule(forAgeMonths = 50)
        assertEquals(300, r.minMinutes)
        assertEquals(360, r.maxMinutes)
    }

    @Test fun `table has exactly 9 buckets`() {
        assertEquals(9, WakeWindowRule.table.size)
    }
}
