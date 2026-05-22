package com.lizhi1026.sleepwhisper.core.persistence.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 睡眠记录的 Room 数据库实体，对应表 `sleep_session`，属于 data / persistence（entity）层。
 *
 * 所在层：core.persistence.entity，由 [SleepDao] 操作，通过 Mapper 与领域模型 [SleepSession] 互转。
 *
 * 关键约定：
 * - `endAt IS NULL` 表示睡眠正在进行中（[SleepDao.ongoing] 依赖此约定）。
 * - `type`、`quality` 均以 `serializedName` 字符串存储，未知值由 Mapper fallback 到默认枚举值。
 * - 索引 `(babyId, startAt)` 加速时间范围查询；`endAt` 单列索引加速进行中记录筛选。
 * - ⚠️ `babyId` 为 FK CASCADE：父行（baby）被删时本行自动删除。
 */
@Entity(
    tableName = "sleep_session",
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
        Index("babyId", "startAt"),
        Index("endAt")
    ]
)
data class SleepSessionEntity(
    @PrimaryKey val id: String,
    val babyId: String,                // 关联宝宝 UUID，FK → baby.id
    val type: String,                  // SleepType.serializedName（如 "nap"、"night"）
    val startAt: Long,                 // 睡眠开始时间，UTC epoch-millis
    val endAt: Long?,                  // 睡眠结束时间，UTC epoch-millis；null 表示进行中
    val quality: String,               // SleepQuality.serializedName（如 "good"、"poor"）
    val fallAsleepMinutes: Int?,       // 入睡耗时（分钟），用户未填写时为 null
    val wakeCount: Int,                // 本次睡眠中醒来次数
    val audioPresetId: String?,        // 使用的白噪音预设 ID，未使用时为 null
    val isEdited: Boolean,             // 是否经过用户手动编辑
    val notes: String?                 // 用户备注，未填写时为 null
)
