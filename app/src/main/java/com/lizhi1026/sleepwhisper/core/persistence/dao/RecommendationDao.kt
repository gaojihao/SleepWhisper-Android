package com.lizhi1026.sleepwhisper.core.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lizhi1026.sleepwhisper.core.persistence.entity.SleepRecommendationEntity

/**
 * 睡眠建议数据访问对象，属于 data / persistence（DAO）层。
 *
 * 所在层：core.persistence.dao，由 Room 自动生成实现，通过 [SleepWhisperDatabase.recommendationDao] 获取。
 *
 * 关键约定：
 * - ⚠️ **[insert] 配合 `babyId` 唯一索引 + `REPLACE` 冲突策略，实现"每宝宝仅保留最新一行"的原子 upsert**，
 *   无需先执行 `DELETE` 再 `INSERT`（消除了竞态窗口）。
 * - [deleteForBaby] 在测试或强制重置场景下手动清空指定宝宝的建议数据。
 * - [latest] 按 `computedAt DESC` 取最新一条，正常情况下 `babyId` 唯一索引保证只有一行。
 * - ⚠️ 所有行的 `babyId` 为 FK CASCADE，宝宝被删时本表对应行自动清空。
 * - 所有方法均为 suspend，Room 内部已切换至 IO 调度器。
 */
@Dao
interface RecommendationDao {
    /**
     * 原子 upsert：插入或替换睡眠建议，实现"每宝宝一行"语义。
     *
     * ⚠️ `babyId` 字段有唯一索引，配合 `REPLACE` 冲突策略：
     * 同一宝宝的旧建议行会被**删除后重新插入**（非原地更新），
     * 关联外键自增列会重置，但业务上每宝宝仅一行，不影响查询结果。
     *
     * @param entity 待插入/替换的 [SleepRecommendationEntity]。suspend。
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SleepRecommendationEntity)

    /**
     * 删除指定宝宝的所有睡眠建议记录（通常用于测试重置或强制刷新）。
     *
     * SQL：`DELETE FROM sleep_recommendation WHERE babyId = :babyId`
     *
     * @param babyId 宝宝 UUID。suspend。
     */
    @Query("DELETE FROM sleep_recommendation WHERE babyId = :babyId")
    suspend fun deleteForBaby(babyId: String)

    /**
     * 查询指定宝宝最新的睡眠建议（按 `computedAt DESC` 取首条）。
     *
     * SQL：`SELECT * FROM sleep_recommendation WHERE babyId = :babyId ORDER BY computedAt DESC LIMIT 1`
     *
     * @param babyId 宝宝 UUID。
     * @return 最新的 [SleepRecommendationEntity]，不存在时返回 `null`。suspend。
     */
    @Query("SELECT * FROM sleep_recommendation WHERE babyId = :babyId ORDER BY computedAt DESC LIMIT 1")
    suspend fun latest(babyId: String): SleepRecommendationEntity?
}
