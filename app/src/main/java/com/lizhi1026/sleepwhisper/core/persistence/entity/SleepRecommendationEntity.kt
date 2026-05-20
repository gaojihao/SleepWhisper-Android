package com.lizhi1026.sleepwhisper.core.persistence.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sleep_recommendation",
    foreignKeys = [
        ForeignKey(
            entity = BabyEntity::class,
            parentColumns = ["id"],
            childColumns = ["babyId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [
        // unique on babyId enforces the "one recommendation per baby" invariant at DB level;
        // with OnConflictStrategy.REPLACE, append() becomes an atomic upsert and the manual
        // delete+insert race in appendPurging is gone.
        Index(value = ["babyId"], unique = true),
        // Defensive composite for ORDER BY computedAt DESC LIMIT 1 if multiple rows ever exist.
        Index(value = ["babyId", "computedAt"], orders = [androidx.room.Index.Order.ASC, androidx.room.Index.Order.DESC])
    ]
)
data class SleepRecommendationEntity(
    @PrimaryKey val id: String,
    val babyId: String,
    val computedAt: Long,
    val nextWindowStart: Long,
    val nextWindowEnd: Long,
    val basedOnAgeMonths: Int,
    val wakeWindowMinutes: Int,
    val confidence: Double,
    val reasoningKeysJson: String,  // JSON-encoded List<String>
    val outcome: String             // RecommendationOutcome.serializedName
)
