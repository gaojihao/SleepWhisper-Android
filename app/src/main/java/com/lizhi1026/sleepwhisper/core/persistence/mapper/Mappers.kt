package com.lizhi1026.sleepwhisper.core.persistence.mapper

import com.lizhi1026.sleepwhisper.model.Baby
import com.lizhi1026.sleepwhisper.model.DiaperEvent
import com.lizhi1026.sleepwhisper.model.FeedingEvent
import com.lizhi1026.sleepwhisper.model.SleepRecommendation
import com.lizhi1026.sleepwhisper.model.SleepSession
import com.lizhi1026.sleepwhisper.core.persistence.entity.BabyEntity
import com.lizhi1026.sleepwhisper.core.persistence.entity.DiaperEventEntity
import com.lizhi1026.sleepwhisper.core.persistence.entity.FeedingEventEntity
import com.lizhi1026.sleepwhisper.core.persistence.entity.SleepRecommendationEntity
import com.lizhi1026.sleepwhisper.core.persistence.entity.SleepSessionEntity
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }
private val stringListSerializer = ListSerializer(String.serializer())

// Baby

fun Baby.toEntity(): BabyEntity = BabyEntity(
    id = id,
    name = name,
    gender = gender.serializedName,
    dateOfBirth = dateOfBirth,
    avatarLocalPath = avatarLocalPath,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun BabyEntity.toDomain(): Baby = Baby(
    id = id,
    name = name,
    gender = Baby.BabyGender.entries.first { it.serializedName == gender },
    dateOfBirth = dateOfBirth,
    avatarLocalPath = avatarLocalPath,
    createdAt = createdAt,
    updatedAt = updatedAt
)

// SleepSession

fun SleepSession.toEntity(): SleepSessionEntity = SleepSessionEntity(
    id = id,
    babyId = babyId,
    type = type.serializedName,
    startAt = startAt,
    endAt = endAt,
    quality = quality.serializedName,
    fallAsleepMinutes = fallAsleepMinutes,
    wakeCount = wakeCount,
    audioPresetId = audioPresetId,
    isEdited = isEdited,
    notes = notes
)

fun SleepSessionEntity.toDomain(): SleepSession = SleepSession(
    id = id,
    babyId = babyId,
    type = SleepSession.SleepType.entries.first { it.serializedName == type },
    startAt = startAt,
    endAt = endAt,
    quality = SleepSession.SleepQuality.entries.first { it.serializedName == quality },
    fallAsleepMinutes = fallAsleepMinutes,
    wakeCount = wakeCount,
    audioPresetId = audioPresetId,
    isEdited = isEdited,
    notes = notes
)

// FeedingEvent

fun FeedingEvent.toEntity(): FeedingEventEntity = FeedingEventEntity(
    id = id,
    babyId = babyId,
    method = method.serializedName,
    amountMl = amountMl,
    durationSeconds = durationSeconds,
    startedAt = startedAt,
    isEdited = isEdited
)

fun FeedingEventEntity.toDomain(): FeedingEvent = FeedingEvent(
    id = id,
    babyId = babyId,
    method = FeedingEvent.FeedingMethod.entries.first { it.serializedName == method },
    amountMl = amountMl,
    durationSeconds = durationSeconds,
    startedAt = startedAt,
    isEdited = isEdited
)

// DiaperEvent

fun DiaperEvent.toEntity(): DiaperEventEntity = DiaperEventEntity(
    id = id,
    babyId = babyId,
    type = type.serializedName,
    occurredAt = occurredAt
)

fun DiaperEventEntity.toDomain(): DiaperEvent = DiaperEvent(
    id = id,
    babyId = babyId,
    type = DiaperEvent.DiaperType.entries.first { it.serializedName == type },
    occurredAt = occurredAt
)

// SleepRecommendation

fun SleepRecommendation.toEntity(): SleepRecommendationEntity = SleepRecommendationEntity(
    id = id,
    babyId = babyId,
    computedAt = computedAt,
    nextWindowStart = nextWindowStart,
    nextWindowEnd = nextWindowEnd,
    basedOnAgeMonths = basedOnAgeMonths,
    wakeWindowMinutes = wakeWindowMinutes,
    confidence = confidence,
    reasoningKeysJson = json.encodeToString(stringListSerializer, reasoningKeys),
    outcome = outcome.serializedName
)

fun SleepRecommendationEntity.toDomain(): SleepRecommendation = SleepRecommendation(
    id = id,
    babyId = babyId,
    computedAt = computedAt,
    nextWindowStart = nextWindowStart,
    nextWindowEnd = nextWindowEnd,
    basedOnAgeMonths = basedOnAgeMonths,
    wakeWindowMinutes = wakeWindowMinutes,
    confidence = confidence,
    reasoningKeys = if (reasoningKeysJson.isBlank()) emptyList()
    else json.decodeFromString(stringListSerializer, reasoningKeysJson),
    outcome = SleepRecommendation.RecommendationOutcome.entries
        .first { it.serializedName == outcome }
)
