package com.lizhi1026.sleepwhisper.core.persistence.mapper

import android.util.Log
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

private const val TAG = "sw.mapper"
private val json = Json { ignoreUnknownKeys = true }
private val stringListSerializer = ListSerializer(String.serializer())

private inline fun <reified T : Enum<T>> Array<T>.findBySerializedOr(
    value: String,
    selector: (T) -> String,
    fallback: T
): T {
    val match = firstOrNull { selector(it) == value }
    if (match != null) return match
    Log.e(TAG, "unknown ${T::class.simpleName} serialized=\"$value\", falling back to $fallback")
    return fallback
}

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
    gender = Baby.BabyGender.entries.toTypedArray()
        .findBySerializedOr(gender, { it.serializedName }, Baby.BabyGender.UNKNOWN),
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
    type = SleepSession.SleepType.entries.toTypedArray()
        .findBySerializedOr(type, { it.serializedName }, SleepSession.SleepType.NAP),
    startAt = startAt,
    endAt = endAt,
    quality = SleepSession.SleepQuality.entries.toTypedArray()
        .findBySerializedOr(quality, { it.serializedName }, SleepSession.SleepQuality.UNKNOWN),
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
    method = FeedingEvent.FeedingMethod.entries.toTypedArray()
        .findBySerializedOr(method, { it.serializedName }, FeedingEvent.FeedingMethod.BOTTLE),
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
    type = DiaperEvent.DiaperType.entries.toTypedArray()
        .findBySerializedOr(type, { it.serializedName }, DiaperEvent.DiaperType.WET),
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
    else try {
        json.decodeFromString(stringListSerializer, reasoningKeysJson)
    } catch (t: Throwable) {
        Log.e(TAG, "bad reasoningKeysJson=\"$reasoningKeysJson\"", t)
        emptyList()
    },
    outcome = SleepRecommendation.RecommendationOutcome.entries.toTypedArray()
        .findBySerializedOr(outcome, { it.serializedName }, SleepRecommendation.RecommendationOutcome.PENDING)
)
