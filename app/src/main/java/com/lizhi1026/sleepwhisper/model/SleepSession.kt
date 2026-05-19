package com.lizhi1026.sleepwhisper.model

import kotlinx.serialization.Serializable

@Serializable
data class SleepSession(
    val id: String,
    val babyId: String,
    val type: SleepType = SleepType.NAP,
    val startAt: Long, // epoch-millis UTC
    val endAt: Long? = null, // null → ongoing
    val quality: SleepQuality = SleepQuality.UNKNOWN,
    val fallAsleepMinutes: Int? = null,
    val wakeCount: Int = 0,
    val audioPresetId: String? = null,
    val isEdited: Boolean = false,
    val notes: String? = null
) {
    enum class SleepType(val serializedName: String) {
        NAP("nap"),
        NIGHT("night"),
        CONTACT_NAP("contact_nap")
    }

    enum class SleepQuality(val serializedName: String) {
        GOOD("good"),
        OK("ok"),
        FUSSY("fussy"),
        UNKNOWN("unknown")
    }

    /** 是否仍在进行中（用户没点"醒来"）。 */
    val isOngoing: Boolean get() = endAt == null

    /** 实时/最终时长（秒）。ongoing 时按当前时间算。 */
    fun durationSeconds(now: Long = System.currentTimeMillis()): Long =
        ((endAt ?: now) - startAt) / 1000
}
