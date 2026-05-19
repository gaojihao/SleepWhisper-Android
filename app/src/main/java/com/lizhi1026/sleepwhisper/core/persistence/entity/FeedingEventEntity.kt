package com.lizhi1026.sleepwhisper.core.persistence.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "feeding_event",
    indices = [
        Index("babyId"),
        Index("startedAt")
    ]
)
data class FeedingEventEntity(
    @PrimaryKey val id: String,
    val babyId: String,
    val method: String,             // FeedingMethod.serializedName
    val amountMl: Int?,
    val durationSeconds: Int?,
    val startedAt: Long,            // epoch-millis UTC
    val isEdited: Boolean
)
