package com.lizhi1026.sleepwhisper.core.persistence.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "diaper_event",
    indices = [
        Index("babyId"),
        Index("occurredAt")
    ]
)
data class DiaperEventEntity(
    @PrimaryKey val id: String,
    val babyId: String,
    val type: String,               // DiaperType.serializedName
    val occurredAt: Long            // epoch-millis UTC
)
