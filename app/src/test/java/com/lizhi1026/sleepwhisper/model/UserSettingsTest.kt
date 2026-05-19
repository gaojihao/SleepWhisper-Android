package com.lizhi1026.sleepwhisper.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class UserSettingsTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test fun `night time inclusive of start hour`() {
        val s = UserSettings(nightModeStartHour = 22, nightModeEndHour = 6)
        assertEquals(true, s.isInNightTime(22))
    }

    @Test fun `night time exclusive of end hour`() {
        val s = UserSettings(nightModeStartHour = 22, nightModeEndHour = 6)
        assertEquals(true, s.isInNightTime(5))
        assertEquals(false, s.isInNightTime(6))
    }

    @Test fun `night time before start is day`() {
        val s = UserSettings(nightModeStartHour = 22, nightModeEndHour = 6)
        assertEquals(false, s.isInNightTime(21))
    }

    @Test fun `night time non-wrapping range`() {
        val s = UserSettings(nightModeStartHour = 1, nightModeEndHour = 5)
        assertEquals(true, s.isInNightTime(2))
        assertEquals(false, s.isInNightTime(0))
        assertEquals(false, s.isInNightTime(5))
    }

    @Test fun `json roundtrip preserves all fields`() {
        val original = UserSettings(
            activeBabyId = "uuid-x",
            cryDetectionEnabled = true,
            cryDetectionThresholdDb = 72,
            defaultTimerMinutes = 45,
            fadeAfterSleepMinutes = 10,
            hapticFeedback = false,
            appearance = UserSettings.AppearanceMode.DARK,
            nightModeStartHour = 21,
            nightModeEndHour = 7,
            locale = "zh-CN",
            lastSeenVersion = "1.0.1",
            wakeLongPressEnabled = false
        )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<UserSettings>(encoded)
        assertEquals(original, decoded)
        assertNotEquals(UserSettings.DEFAULT, decoded)
    }

    @Test fun `default settings encode and decode to same default`() {
        val encoded = json.encodeToString(UserSettings.DEFAULT)
        val decoded = json.decodeFromString<UserSettings>(encoded)
        // locale is locale-dependent; compare other fields
        assertEquals(UserSettings.DEFAULT.cryDetectionEnabled, decoded.cryDetectionEnabled)
        assertEquals(UserSettings.DEFAULT.cryDetectionThresholdDb, decoded.cryDetectionThresholdDb)
        assertEquals(UserSettings.DEFAULT.appearance, decoded.appearance)
        assertEquals(UserSettings.DEFAULT.nightModeStartHour, decoded.nightModeStartHour)
        assertEquals(UserSettings.DEFAULT.nightModeEndHour, decoded.nightModeEndHour)
    }
}
