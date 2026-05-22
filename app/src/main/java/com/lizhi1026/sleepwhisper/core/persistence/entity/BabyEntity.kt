package com.lizhi1026.sleepwhisper.core.persistence.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 宝宝信息的 Room 数据库实体，对应表 `baby`，属于 data / persistence（entity）层。
 *
 * 所在层：core.persistence.entity，由 [BabyDao] 操作，通过 Mapper 与领域模型 [Baby] 互转。
 *
 * 关键约定：
 * - `id` 为 UUID 字符串，由领域层生成，在此作为主键，不使用自增整型。
 * - 枚举字段 `gender` 以 `serializedName` 字符串形式存储，Mapper 层负责双向转换。
 * - 所有时间戳字段均为 `Long`，存储 UTC epoch-millis，不依赖时区。
 * - ⚠️ 删除此行会通过 FK CASCADE 触发所有子表（睡眠、喂养、换尿布、建议）的级联删除。
 */
@Entity(tableName = "baby")
data class BabyEntity(
    @PrimaryKey val id: String,
    val name: String,
    val gender: String,        // BabyGender.serializedName（枚举序列化字符串，如 "male"）
    val dateOfBirth: Long,     // 出生日期，UTC epoch-millis
    val avatarLocalPath: String?,  // 头像本地文件路径，未设置时为 null
    val createdAt: Long,       // 记录创建时间，UTC epoch-millis
    val updatedAt: Long        // 记录最后更新时间，UTC epoch-millis
)
