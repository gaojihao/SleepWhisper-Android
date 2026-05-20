package com.lizhi1026.sleepwhisper.core.persistence.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "diaper_event",
    foreignKeys = [
        ForeignKey(
            entity = BabyEntity::class,
            parentColumns = ["id"],
            childColumns = ["babyId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [Index("babyId", "occurredAt")]
)
data class DiaperEventEntity(
    @PrimaryKey val id: String,
    val babyId: String,
    val type: String,               // DiaperType.serializedName
    val occurredAt: Long            // epoch-millis UTC
)
