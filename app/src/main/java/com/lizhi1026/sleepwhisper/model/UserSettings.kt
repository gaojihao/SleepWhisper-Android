package com.lizhi1026.sleepwhisper.model

import com.lizhi1026.sleepwhisper.BuildConfig
import kotlinx.serialization.Serializable

@Serializable
data class UserSettings(
    val activeBabyId: String? = null,
    val cryDetectionEnabled: Boolean = false,
    val cryDetectionThresholdDb: Int = 60,
    val defaultTimerMinutes: Int = 30,
    val fadeAfterSleepMinutes: Int = 5,
    val hapticFeedback: Boolean = true,
    val appearance: AppearanceMode = AppearanceMode.AUTO,
    val nightModeStartHour: Int = 22,
    val nightModeEndHour: Int = 6,
    val locale: String = java.util.Locale.getDefault().toLanguageTag(),
    val lastSeenVersion: String = BuildConfig.VERSION_NAME,
    val wakeLongPressEnabled: Boolean = true
    // iOS 有 iCloudSyncEnabled，Android 不做同步， intentionally omitted
) {
    enum class AppearanceMode(val serializedName: String) {
        AUTO("auto"),
        LIGHT("light"),
        DARK("dark")
    }

    companion object {
        val DEFAULT: UserSettings = UserSettings()
    }

    /** 给定小时是否落在用户配置的"夜间时段"。支持跨午夜区间（22→6）。 */
    fun isInNightTime(hour: Int = java.time.LocalTime.now().hour): Boolean =
        if (nightModeStartHour > nightModeEndHour) {
            hour >= nightModeStartHour || hour < nightModeEndHour
        } else {
            hour in nightModeStartHour until nightModeEndHour
        }
}
