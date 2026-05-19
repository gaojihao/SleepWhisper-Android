package com.lizhi1026.sleepwhisper.core.persistence.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "baby")
data class BabyEntity(
    @PrimaryKey val id: String,
    val name: String,
    val gender: String,        // BabyGender.serializedName
    val dateOfBirth: Long,     // epoch-millis UTC
    val avatarLocalPath: String?,
    val createdAt: Long,
    val updatedAt: Long
)
