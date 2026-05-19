package com.lizhi1026.sleepwhisper.core.persistence.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sleep_session",
    indices = [
        Index("babyId"),
        Index("endAt"),
        Index("startAt")
    ]
)
data class SleepSessionEntity(
    @PrimaryKey val id: String,
    val babyId: String,
    val type: String,               // SleepType.serializedName
    val startAt: Long,              // epoch-millis UTC
    val endAt: Long?,               // null → ongoing
    val quality: String,            // SleepQuality.serializedName
    val fallAsleepMinutes: Int?,
    val wakeCount: Int,
    val audioPresetId: String?,
    val isEdited: Boolean,
    val notes: String?
)
