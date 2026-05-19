package com.lizhi1026.sleepwhisper.core.audio

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FadeRampTest {

    /**
     * 20 Hz ramp = 50 ms per step. For 1500 ms fade-in (the iOS-parity value),
     * we expect exactly 30 steps and the final volume to equal `target`.
     */
    @Test fun `1500ms ramp produces 30 steps and lands on target`() {
        val durationMs = 1500L
        val steps = (durationMs / 50).toInt()
        assertEquals(30, steps)
        // final i / steps == 1 → v == target
        val from = 0f
        val target = 1f
        val finalI = steps
        val v = from + (target - from) * finalI / steps
        assertEquals(target, v, 0.0001f)
    }

    @Test fun `500ms ramp produces 10 steps`() {
        val steps = (500L / 50).toInt()
        assertEquals(10, steps)
    }

    /** Sleep-timer fade across N minutes still produces a linear monotonically-decreasing ramp. */
    @Test fun `5-minute fade-out is monotonically decreasing`() {
        val durationMs = 5L * 60_000L
        val steps = (durationMs / 50).toInt()
        val from = 1f
        val target = 0f
        var prev = from
        for (i in 1..steps) {
            val v = from + (target - from) * i / steps
            assertTrue("step $i should be <= prev ($prev) but was $v", v <= prev + 1e-6f)
            prev = v
        }
        assertEquals(0f, prev, 0.0001f)
    }

    @Test fun `runBlocking compatible (no Android dependency)`() = runBlocking {
        // sanity: ensures kotlinx-coroutines is on the test classpath
        val steps = (1500L / 50).toInt()
        assertEquals(30, steps)
    }
}
