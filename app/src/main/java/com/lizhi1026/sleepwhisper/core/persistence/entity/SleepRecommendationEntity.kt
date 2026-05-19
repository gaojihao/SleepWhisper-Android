package com.lizhi1026.sleepwhisper.core.persistence.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sleep_recommendation",
    indices = [Index("babyId")]
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
