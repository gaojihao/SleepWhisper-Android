package com.lizhi1026.sleepwhisper.core.persistence.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lizhi1026.sleepwhisper.core.persistence.entity.DiaperEventEntity

/**
 * 换尿布事件数据访问对象，属于 data / persistence（DAO）层。
 *
 * 所在层：core.persistence.dao，由 Room 自动生成实现，通过 [SleepWhisperDatabase.diaperDao] 获取。
 *
 * 关键约定：
 * - `insert` 冲突策略为 IGNORE：UUID 重复时静默丢弃，不抛异常。
 * - [deleteById] 优于 [delete]（无需先查实体对象），在 [RoomRepository] 中统一使用 `deleteById`。
 * - `since` 结果按 `occurredAt` **升序**排列，适合时间轴展示。
 * - ⚠️ 所有行的 `babyId` 为 FK CASCADE，宝宝被删时本表对应行自动清空。
 * - 所有方法均为 suspend，Room 内部已切换至 IO 调度器。
 */
@Dao
interface DiaperDao {
    /**
     * 插入一条新换尿布记录，主键冲突时静默忽略（IGNORE）。
     *
     * @param entity 待插入的 [DiaperEventEntity]，`id` 须为全局唯一 UUID。suspend。
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: DiaperEventEntity)

    /**
     * 更新已有换尿布记录（以 `id` 定位行，替换全部字段）。
     *
     * @param entity 携带最新数据的 [DiaperEventEntity]。suspend。
     */
    @Update
    suspend fun update(entity: DiaperEventEntity)

    /**
     * 按实体对象删除换尿布记录（通过主键匹配）。
     *
     * @param entity 要删除的 [DiaperEventEntity]，以 `id` 字段定位行。suspend。
     */
    @Delete
    suspend fun delete(entity: DiaperEventEntity)

    /**
     * 按 UUID 字符串删除一条换尿布记录，无需预先加载实体对象。
     *
     * SQL：`DELETE FROM diaper_event WHERE id = :id`
     *
     * @param id 目标记录的 UUID 字符串。suspend。
     */
    @Query("DELETE FROM diaper_event WHERE id = :id")
    suspend fun deleteById(id: String)

    /**
     * 查询指定宝宝在某时间点之后（含）的所有换尿布记录，结果按 `occurredAt` **升序**排列。
     *
     * SQL：`SELECT * FROM diaper_event WHERE babyId = :babyId AND occurredAt >= :sinceMs ORDER BY occurredAt ASC`
     *
     * @param babyId  宝宝 UUID。
     * @param sinceMs 起始时间戳（epoch-millis UTC，含边界值）。
     * @return 按 `occurredAt` 升序排列的 [DiaperEventEntity] 列表。suspend。
     */
    @Query("SELECT * FROM diaper_event WHERE babyId = :babyId AND occurredAt >= :sinceMs ORDER BY occurredAt ASC")
    suspend fun since(babyId: String, sinceMs: Long): List<DiaperEventEntity>
}
