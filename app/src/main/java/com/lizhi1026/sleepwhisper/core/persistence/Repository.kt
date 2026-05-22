package com.lizhi1026.sleepwhisper.core.persistence

import com.lizhi1026.sleepwhisper.model.Baby
import com.lizhi1026.sleepwhisper.model.DiaperEvent
import com.lizhi1026.sleepwhisper.model.FeedingEvent
import com.lizhi1026.sleepwhisper.model.SleepRecommendation
import com.lizhi1026.sleepwhisper.model.SleepSession

/**
 * 持久化层统一入口接口，供领域层调用。
 *
 * 所在层：data / persistence（接口定义在 core.persistence，实现在 [RoomRepository]）。
 *
 * 关键约定：
 * - ⚠️ **所有方法均为 `suspend fun`，不返回 Flow**。响应式更新由上层
 *   `AppStateContainer` 在写操作完成后手动 reload，保持单向数据流。
 * - `UserSettings` 故意不在此接口中——它通过 DataStore / JSON 由 [SettingsStore] 独立管理，
 *   避免设置查询意外路由到 Room。
 * - 接口稳定性优先：下层切换存储方案时只需换实现，调用方代码不受影响。
 */
interface Repository {

    // ────────────────────────── Baby ──────────────────────────

    /**
     * 读取最新一条宝宝信息（按创建时间降序取首条）。
     *
     * @return 存在时返回 [Baby] 域对象，表为空时返回 `null`。
     * suspend：由 Room IO 调度器执行，调用方无需切换线程。
     */
    suspend fun loadBaby(): Baby?

    /**
     * 保存（新增或更新）宝宝信息。
     *
     * @param baby 包含完整字段的 [Baby] 域对象，`id` 为主键。
     * 底层使用 `@Upsert`：主键冲突时替换，否则插入。
     */
    suspend fun saveBaby(baby: Baby)

    // ────────────────────────── SleepSession ──────────────────────────

    /**
     * 追加一条新睡眠记录。
     *
     * @param session 待持久化的 [SleepSession]，`id` 须全局唯一（UUID）。
     * 冲突策略为 IGNORE：重复 id 时静默丢弃，不抛异常。
     */
    suspend fun appendSleep(session: SleepSession)

    /**
     * 更新已有睡眠记录（例如记录结束时间或修改备注）。
     *
     * @param session 携带最新数据的 [SleepSession]，以 `id` 定位行。
     */
    suspend fun updateSleep(session: SleepSession)

    /**
     * 查询指定宝宝当前正在进行的睡眠（`endAt IS NULL`）。
     *
     * @param babyId 宝宝 UUID。
     * @return 进行中的 [SleepSession]，不存在时返回 `null`。
     * SQL：`ORDER BY startAt DESC LIMIT 1`，取最近开始的未结束记录。
     */
    suspend fun ongoingSleep(babyId: String): SleepSession?

    /**
     * 查询指定宝宝在某时间点之后（含）的所有睡眠记录。
     *
     * @param babyId  宝宝 UUID。
     * @param sinceMs 起始时间戳（epoch-millis UTC），内部会被 `coerceAtLeast(0L)` 保护。
     * @return 按 `startAt` **升序**排列的 [SleepSession] 列表。
     */
    suspend fun sleepSessions(babyId: String, sinceMs: Long): List<SleepSession>

    // ────────────────────────── FeedingEvent ──────────────────────────

    /**
     * 追加一条喂养记录。
     *
     * @param event 待持久化的 [FeedingEvent]，`id` 须全局唯一（UUID）。
     */
    suspend fun appendFeeding(event: FeedingEvent)

    /**
     * 更新已有喂养记录。
     *
     * @param event 携带最新数据的 [FeedingEvent]，以 `id` 定位行。
     */
    suspend fun updateFeeding(event: FeedingEvent)

    /**
     * 按 id 删除一条喂养记录。
     *
     * @param id 目标记录的 UUID。
     */
    suspend fun deleteFeeding(id: String)

    /**
     * 查询指定宝宝在某时间点之后（含）的所有喂养记录。
     *
     * @param babyId  宝宝 UUID。
     * @param sinceMs 起始时间戳（epoch-millis UTC）。
     * @return 按 `startedAt` **升序**排列的 [FeedingEvent] 列表。
     */
    suspend fun feedings(babyId: String, sinceMs: Long): List<FeedingEvent>

    // ────────────────────────── DiaperEvent ──────────────────────────

    /**
     * 追加一条换尿布记录。
     *
     * @param event 待持久化的 [DiaperEvent]，`id` 须全局唯一（UUID）。
     */
    suspend fun appendDiaper(event: DiaperEvent)

    /**
     * 更新已有换尿布记录。
     *
     * @param event 携带最新数据的 [DiaperEvent]，以 `id` 定位行。
     */
    suspend fun updateDiaper(event: DiaperEvent)

    /**
     * 按 id 删除一条换尿布记录。
     *
     * @param id 目标记录的 UUID。
     */
    suspend fun deleteDiaper(id: String)

    /**
     * 查询指定宝宝在某时间点之后（含）的所有换尿布记录。
     *
     * @param babyId  宝宝 UUID。
     * @param sinceMs 起始时间戳（epoch-millis UTC）。
     * @return 按 `occurredAt` **升序**排列的 [DiaperEvent] 列表。
     */
    suspend fun diapers(babyId: String, sinceMs: Long): List<DiaperEvent>

    // ────────────────────────── SleepRecommendation ──────────────────────────

    /**
     * 追加（实为 upsert）一条睡眠建议。
     *
     * @param rec 待持久化的 [SleepRecommendation]。
     * ⚠️ 底层 `RecommendationDao.insert` 使用 `REPLACE` 冲突策略配合 `babyId` 唯一索引，
     * 实现"每宝宝仅保留最新一行"的原子 upsert，无需先删后插。
     */
    suspend fun appendRecommendation(rec: SleepRecommendation)

    /**
     * 查询指定宝宝最新的睡眠建议。
     *
     * @param babyId 宝宝 UUID。
     * @return 最新的 [SleepRecommendation]，不存在时返回 `null`。
     * SQL：`ORDER BY computedAt DESC LIMIT 1`。
     */
    suspend fun latestRecommendation(babyId: String): SleepRecommendation?
}
