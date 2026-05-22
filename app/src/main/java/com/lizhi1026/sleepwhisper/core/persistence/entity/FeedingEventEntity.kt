package com.lizhi1026.sleepwhisper.core.persistence.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 喂养事件的 Room 数据库实体，对应表 `feeding_event`，属于 data / persistence（entity）层。
 *
 * 所在层：core.persistence.entity，由 [FeedingDao] 操作，通过 Mapper 与领域模型 [FeedingEvent] 互转。
 *
 * 关键约定：
 * - `method` 以 `serializedName` 字符串存储（如 "bottle"、"breast"）。
 * - `amountMl` 与 `durationSeconds` 互斥使用：奶瓶喂养记录毫升数，母乳喂养记录时长秒数，
 *   另一方为 `null`；两者均 `null` 表示仅记录喂养事件而无详细数据。
 * - 索引 `(babyId, startedAt)` 加速时间范围查询。
 * - ⚠️ `babyId` 为 FK CASCADE：父行（baby）被删时本行自动删除。
 */
@Entity(
    tableName = "feeding_event",
    foreignKeys = [
        ForeignKey(
            entity = BabyEntity::class,
            parentColumns = ["id"],
            childColumns = ["babyId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [Index("babyId", "startedAt")]
)
data class FeedingEventEntity(
    @PrimaryKey val id: String,
    val babyId: String,               // 关联宝宝 UUID，FK → baby.id
    val method: String,               // FeedingMethod.serializedName（如 "bottle"、"breast"）
    val amountMl: Int?,               // 喂奶量（毫升），奶瓶喂养时填写，母乳时为 null
    val durationSeconds: Int?,        // 喂养时长（秒），母乳喂养时填写，奶瓶时为 null
    val startedAt: Long,              // 喂养开始时间，UTC epoch-millis
    val isEdited: Boolean             // 是否经过用户手动编辑
)
