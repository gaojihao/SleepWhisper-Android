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

/**
 * 持久化层与领域层之间的双向映射函数集合，属于 data / persistence（mapper）层。
 *
 * 所在层：core.persistence.mapper，纯 CPU 运算，在调用方协程调度器上执行（无 IO）。
 *
 * 关键约定：
 * - ⚠️ **枚举未知值不会抛异常**，统一由 [findBySerializedOr] 打印错误日志后 fallback 到指定默认值，
 *   保证旧数据（含已废弃枚举值）在新版本中仍可正常展示。
 * - `reasoningKeysJson` 的 JSON 编解码仅在此层处理，DAO / Entity 层不感知序列化细节。
 * - `reasoningKeysJson` 解析失败时 fallback 到空列表，不传播异常。
 * - 所有 `toEntity()` / `toDomain()` 均为扩展函数，便于链式调用，不持有状态。
 */
private const val TAG = "sw.mapper"

/** 仅用于解码 `reasoningKeysJson`，`ignoreUnknownKeys` 防止 JSON 格式变更时崩溃。 */
private val json = Json { ignoreUnknownKeys = true }

/** `List<String>` 的 kotlinx.serialization 序列化器，用于 `reasoningKeysJson` 的编解码。 */
private val stringListSerializer = ListSerializer(String.serializer())

/**
 * 在枚举数组中按序列化名称查找对应枚举值，找不到时 fallback 到默认值。
 *
 * ⚠️ 未知值**不会抛异常**，仅通过 [Log.e] 记录错误后返回 [fallback]，
 * 保证旧版本存储的已废弃枚举值在新版本中仍可安全读取。
 *
 * @param value    待匹配的序列化字符串（来自数据库列值）。
 * @param selector 从枚举实例提取其序列化名称的函数。
 * @param fallback 未找到匹配时返回的默认枚举值。
 * @return 匹配的枚举值，或 [fallback]。
 */
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

// ────────────────────────── Baby ──────────────────────────

/** 将 [Baby] 领域模型转换为 Room 持久化实体 [BabyEntity]。 */
fun Baby.toEntity(): BabyEntity = BabyEntity(
    id = id,
    name = name,
    gender = gender.serializedName,
    dateOfBirth = dateOfBirth,
    avatarLocalPath = avatarLocalPath,
    createdAt = createdAt,
    updatedAt = updatedAt
)

/**
 * 将 [BabyEntity] 持久化实体转换为 [Baby] 领域模型。
 *
 * `gender` 未知值 fallback 到 [Baby.BabyGender.UNKNOWN]。
 */
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

// ────────────────────────── SleepSession ──────────────────────────

/** 将 [SleepSession] 领域模型转换为 Room 持久化实体 [SleepSessionEntity]。 */
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

/**
 * 将 [SleepSessionEntity] 持久化实体转换为 [SleepSession] 领域模型。
 *
 * - `type` 未知值 fallback 到 [SleepSession.SleepType.NAP]。
 * - `quality` 未知值 fallback 到 [SleepSession.SleepQuality.UNKNOWN]。
 */
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

// ────────────────────────── FeedingEvent ──────────────────────────

/** 将 [FeedingEvent] 领域模型转换为 Room 持久化实体 [FeedingEventEntity]。 */
fun FeedingEvent.toEntity(): FeedingEventEntity = FeedingEventEntity(
    id = id,
    babyId = babyId,
    method = method.serializedName,
    amountMl = amountMl,
    durationSeconds = durationSeconds,
    startedAt = startedAt,
    isEdited = isEdited
)

/**
 * 将 [FeedingEventEntity] 持久化实体转换为 [FeedingEvent] 领域模型。
 *
 * `method` 未知值 fallback 到 [FeedingEvent.FeedingMethod.BOTTLE]（最常见默认值）。
 */
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

// ────────────────────────── DiaperEvent ──────────────────────────

/** 将 [DiaperEvent] 领域模型转换为 Room 持久化实体 [DiaperEventEntity]。 */
fun DiaperEvent.toEntity(): DiaperEventEntity = DiaperEventEntity(
    id = id,
    babyId = babyId,
    type = type.serializedName,
    occurredAt = occurredAt
)

/**
 * 将 [DiaperEventEntity] 持久化实体转换为 [DiaperEvent] 领域模型。
 *
 * `type` 未知值 fallback 到 [DiaperEvent.DiaperType.WET]。
 */
fun DiaperEventEntity.toDomain(): DiaperEvent = DiaperEvent(
    id = id,
    babyId = babyId,
    type = DiaperEvent.DiaperType.entries.toTypedArray()
        .findBySerializedOr(type, { it.serializedName }, DiaperEvent.DiaperType.WET),
    occurredAt = occurredAt
)

// ────────────────────────── SleepRecommendation ──────────────────────────

/**
 * 将 [SleepRecommendation] 领域模型转换为 Room 持久化实体 [SleepRecommendationEntity]。
 *
 * `reasoningKeys` 列表通过 kotlinx.serialization 编码为 JSON 字符串存入 `reasoningKeysJson` 列；
 * 解码逻辑见 [SleepRecommendationEntity.toDomain]。
 */
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

/**
 * 将 [SleepRecommendationEntity] 持久化实体转换为 [SleepRecommendation] 领域模型。
 *
 * - `reasoningKeysJson` 为空字符串时直接返回空列表。
 * - ⚠️ JSON 解码失败**不抛异常**：记录日志后 fallback 到空列表，保证旧数据格式破损不影响新版本启动。
 * - `outcome` 未知值 fallback 到 [SleepRecommendation.RecommendationOutcome.PENDING]。
 */
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
