package com.lizhi1026.sleepwhisper.core.persistence.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 换尿布事件的 Room 数据库实体，对应表 `diaper_event`，属于 data / persistence（entity）层。
 *
 * 所在层：core.persistence.entity，由 [DiaperDao] 操作，通过 Mapper 与领域模型 [DiaperEvent] 互转。
 *
 * 关键约定：
 * - `type` 以 `serializedName` 字符串存储（如 "wet"、"dirty"、"both"）。
 * - 当前 Entity 字段较少（无备注），如需扩展请同步更新领域模型并写 Migration。
 * - 索引 `(babyId, occurredAt)` 加速时间范围查询。
 * - ⚠️ `babyId` 为 FK CASCADE：父行（baby）被删时本行自动删除。
 */
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
    val babyId: String,               // 关联宝宝 UUID，FK → baby.id
    val type: String,                 // DiaperType.serializedName（如 "wet"、"dirty"、"both"）
    val occurredAt: Long              // 换尿布发生时间，UTC epoch-millis
)
