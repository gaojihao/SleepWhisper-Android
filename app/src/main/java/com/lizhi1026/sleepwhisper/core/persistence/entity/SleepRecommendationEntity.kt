package com.lizhi1026.sleepwhisper.core.persistence.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 睡眠建议的 Room 数据库实体，对应表 `sleep_recommendation`，属于 data / persistence（entity）层。
 *
 * 所在层：core.persistence.entity，由 [RecommendationDao] 操作，通过 Mapper 与领域模型 [SleepRecommendation] 互转。
 *
 * 关键约定：
 * - ⚠️ `babyId` 字段具有**唯一索引**，配合 [RecommendationDao.insert] 的 `REPLACE` 冲突策略，
 *   确保每个宝宝在表中始终只有一行建议记录，实现原子 upsert。
 * - `reasoningKeysJson` 存储 `List<String>` 的 JSON 序列化结果，由 Mapper 层负责编解码。
 * - `outcome` 以 `serializedName` 字符串存储，未知值由 Mapper fallback 到 `PENDING`。
 * - 复合索引 `(babyId, computedAt DESC)` 为防御性优化，确保多行场景下 ORDER BY 仍高效。
 * - ⚠️ `babyId` 为 FK CASCADE：父行（baby）被删时本行自动删除。
 */
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
        // ⚠️ babyId 唯一索引保证"每宝宝一行"不变量；配合 REPLACE 冲突策略实现原子 upsert，
        // 消除了先 DELETE 后 INSERT 的竞态窗口。
        Index(value = ["babyId"], unique = true),
        // 防御性复合索引：若 babyId 唯一索引失效导致多行存在时，ORDER BY computedAt DESC LIMIT 1 仍高效。
        Index(value = ["babyId", "computedAt"], orders = [androidx.room.Index.Order.ASC, androidx.room.Index.Order.DESC])
    ]
)
data class SleepRecommendationEntity(
    @PrimaryKey val id: String,
    val babyId: String,               // 关联宝宝 UUID，FK → baby.id（唯一索引）
    val computedAt: Long,             // 建议计算时间，UTC epoch-millis
    val nextWindowStart: Long,        // 建议下次入睡窗口开始时间，UTC epoch-millis
    val nextWindowEnd: Long,          // 建议下次入睡窗口结束时间，UTC epoch-millis
    val basedOnAgeMonths: Int,        // 计算时宝宝月龄（月）
    val wakeWindowMinutes: Int,       // 建议清醒窗口时长（分钟）
    val confidence: Double,           // 建议置信度，范围 [0.0, 1.0]
    val reasoningKeysJson: String,    // 推理依据键列表的 JSON 字符串（List<String>），Mapper 负责编解码
    val outcome: String               // RecommendationOutcome.serializedName（如 "pending"、"accepted"）
)
