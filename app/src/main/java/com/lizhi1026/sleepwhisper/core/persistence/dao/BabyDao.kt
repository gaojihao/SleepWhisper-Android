package com.lizhi1026.sleepwhisper.core.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.lizhi1026.sleepwhisper.core.persistence.entity.BabyEntity

/**
 * 宝宝信息数据访问对象，属于 data / persistence（DAO）层。
 *
 * 所在层：core.persistence.dao，由 Room 自动生成实现，通过 [SleepWhisperDatabase.babyDao] 获取。
 *
 * 关键约定：
 * - 当前仅支持单宝宝场景：[loadLatest] 按 `createdAt DESC` 取首条。
 * - [upsert] 使用 `@Upsert`（`INSERT OR REPLACE`）语义，主键存在时替换，否则插入。
 * - ⚠️ 删除宝宝行会通过 FK CASCADE 级联清空所有关联子表数据（睡眠、喂养、换尿布、建议）。
 * - 所有方法均为 suspend，Room 内部已切换至 IO 调度器。
 */
@Dao
interface BabyDao {
    /**
     * 查询最新创建的宝宝信息（按创建时间降序取首条）。
     *
     * SQL：`SELECT * FROM baby ORDER BY createdAt DESC LIMIT 1`
     *
     * @return 存在时返回 [BabyEntity]，表为空时返回 `null`。suspend。
     */
    @Query("SELECT * FROM baby ORDER BY createdAt DESC LIMIT 1")
    suspend fun loadLatest(): BabyEntity?

    /**
     * 插入或替换宝宝信息（Upsert 语义）。
     *
     * @param entity 完整的 [BabyEntity]，`id` 为主键。
     * 主键冲突时替换整行；不存在时插入新行。suspend。
     */
    @Upsert
    suspend fun upsert(entity: BabyEntity)
}
