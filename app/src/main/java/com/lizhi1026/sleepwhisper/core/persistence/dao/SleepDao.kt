package com.lizhi1026.sleepwhisper.core.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lizhi1026.sleepwhisper.core.persistence.entity.SleepSessionEntity

/**
 * 睡眠记录数据访问对象，属于 data / persistence（DAO）层。
 *
 * 所在层：core.persistence.dao，由 Room 自动生成实现，通过 [SleepWhisperDatabase.sleepDao] 获取。
 *
 * 关键约定：
 * - `insert` 冲突策略为 IGNORE：UUID 重复时静默丢弃，不抛异常（双击防重）。
 * - `ongoing` 通过 `endAt IS NULL` 判断进行中状态，一个宝宝理论上只有一条进行中记录。
 * - `since` 结果按 `startAt` **升序**排列，适合图表时序渲染。
 * - ⚠️ 所有行的 `babyId` 为 FK CASCADE，宝宝被删时本表对应行自动清空。
 * - 所有方法均为 suspend，Room 内部已切换至 IO 调度器。
 */
@Dao
interface SleepDao {
    /**
     * 插入一条新睡眠记录，主键冲突时静默忽略（IGNORE）。
     *
     * @param entity 待插入的 [SleepSessionEntity]，`id` 须为全局唯一 UUID。
     * 副作用：若 `id` 已存在，该次插入被丢弃，不修改已有数据。suspend。
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: SleepSessionEntity)

    /**
     * 更新已有睡眠记录（以 `id` 定位行，替换全部字段）。
     *
     * @param entity 携带最新数据的 [SleepSessionEntity]。suspend。
     */
    @Update
    suspend fun update(entity: SleepSessionEntity)

    /**
     * 查询指定宝宝当前正在进行的睡眠（`endAt IS NULL`，按 `startAt DESC` 取最近一条）。
     *
     * SQL：`SELECT * FROM sleep_session WHERE babyId = :babyId AND endAt IS NULL ORDER BY startAt DESC LIMIT 1`
     *
     * @param babyId 宝宝 UUID。
     * @return 进行中的 [SleepSessionEntity]，不存在时返回 `null`。suspend。
     */
    @Query("SELECT * FROM sleep_session WHERE babyId = :babyId AND endAt IS NULL ORDER BY startAt DESC LIMIT 1")
    suspend fun ongoing(babyId: String): SleepSessionEntity?

    /**
     * 查询指定宝宝在某时间点之后（含）的所有睡眠记录，结果按 `startAt` **升序**排列。
     *
     * SQL：`SELECT * FROM sleep_session WHERE babyId = :babyId AND startAt >= :sinceMs ORDER BY startAt ASC`
     *
     * @param babyId  宝宝 UUID。
     * @param sinceMs 起始时间戳（epoch-millis UTC，含边界值）。
     * @return 按 `startAt` 升序排列的 [SleepSessionEntity] 列表。suspend。
     */
    @Query("SELECT * FROM sleep_session WHERE babyId = :babyId AND startAt >= :sinceMs ORDER BY startAt ASC")
    suspend fun since(babyId: String, sinceMs: Long): List<SleepSessionEntity>
}
