package com.lizhi1026.sleepwhisper.core.persistence

import androidx.room.Database
import androidx.room.RoomDatabase
import com.lizhi1026.sleepwhisper.core.persistence.dao.BabyDao
import com.lizhi1026.sleepwhisper.core.persistence.dao.DiaperDao
import com.lizhi1026.sleepwhisper.core.persistence.dao.FeedingDao
import com.lizhi1026.sleepwhisper.core.persistence.dao.RecommendationDao
import com.lizhi1026.sleepwhisper.core.persistence.dao.SleepDao
import com.lizhi1026.sleepwhisper.core.persistence.entity.BabyEntity
import com.lizhi1026.sleepwhisper.core.persistence.entity.DiaperEventEntity
import com.lizhi1026.sleepwhisper.core.persistence.entity.FeedingEventEntity
import com.lizhi1026.sleepwhisper.core.persistence.entity.SleepRecommendationEntity
import com.lizhi1026.sleepwhisper.core.persistence.entity.SleepSessionEntity

/**
 * SleepWhisper 应用的 Room 数据库声明，属于 data / persistence（基础设施）层。
 *
 * 所在层：core.persistence，由 Hilt 模块以单例形式提供给各 DAO。
 *
 * 关键约定：
 * - ⚠️ **当前版本为 `version = 2`**，`schemas/` 目录下已固化 `1.json` 与 `2.json`。
 *   任何表结构变更（新增字段、修改列类型、新增表等）**必须：**
 *   1. 编写对应的 `Migration(old, new)` 对象并注册到 Room builder；
 *   2. 将 `version` 升至下一个整数。
 *   切勿使用 `fallbackToDestructiveMigration()`，否则用户数据将被清空。
 * - 所有子表均通过 `ForeignKey(onDelete = CASCADE)` 关联 `baby.id`，
 *   删除宝宝记录会级联清空该宝宝的全部睡眠、喂养、换尿布及建议数据。
 * - `exportSchema = true` 确保每次版本升级后 schema 快照被写入 `schemas/` 供 CI 回归校验。
 */
@Database(
    entities = [
        BabyEntity::class,
        SleepSessionEntity::class,
        FeedingEventEntity::class,
        DiaperEventEntity::class,
        SleepRecommendationEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class SleepWhisperDatabase : RoomDatabase() {
    /** 返回宝宝信息 DAO，用于宝宝的查询与 Upsert。 */
    abstract fun babyDao(): BabyDao

    /** 返回睡眠记录 DAO，用于睡眠的增删改查及进行中记录查询。 */
    abstract fun sleepDao(): SleepDao

    /** 返回喂养记录 DAO，用于喂养事件的增删改查。 */
    abstract fun feedingDao(): FeedingDao

    /** 返回换尿布记录 DAO，用于换尿布事件的增删改查。 */
    abstract fun diaperDao(): DiaperDao

    /** 返回睡眠建议 DAO，用于建议的 upsert 与最新记录查询。 */
    abstract fun recommendationDao(): RecommendationDao
}
