package com.lizhi1026.sleepwhisper.model

import kotlinx.serialization.Serializable

// ─── Feeding ─────────────────────────────────────────────────────────────────

@Serializable
data class FeedingEvent(
    val id: String,
    val babyId: String,
    val method: FeedingMethod,
    val amountMl: Int? = null,
    val durationSeconds: Int? = null,
    val startedAt: Long, // epoch-millis UTC
    val isEdited: Boolean = false
) {
    enum class FeedingMethod(val serializedName: String) {
        BREAST_LEFT("breast_left"),
        BREAST_RIGHT("breast_right"),
        BOTTLE("bottle"),
        SOLID("solid")
    }

    val isValid: Boolean
        get() {
            if (method == FeedingMethod.BOTTLE && (amountMl ?: 0) < 0) return false
            if ((durationSeconds ?: 0) < 0) return false
            if (startedAt > System.currentTimeMillis()) return false
            return true
        }
}

// ─── Diaper ──────────────────────────────────────────────────────────────────

@Serializable
data class DiaperEvent(
    val id: String,
    val babyId: String,
    val type: DiaperType,
    val occurredAt: Long // epoch-millis UTC
    // 故意不加 isEdited，与 DiaperEvent iOS 一致
) {
    enum class DiaperType(val serializedName: String) {
        WET("wet"),
        DIRTY("dirty"),
        MIXED("mixed"),
        DRY("dry")
    }
}
