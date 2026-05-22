package com.lizhi1026.sleepwhisper.core.persistence

import android.util.Log
import com.lizhi1026.sleepwhisper.model.Baby
import com.lizhi1026.sleepwhisper.model.DiaperEvent
import com.lizhi1026.sleepwhisper.model.FeedingEvent
import com.lizhi1026.sleepwhisper.model.SleepRecommendation
import com.lizhi1026.sleepwhisper.model.SleepSession
import com.lizhi1026.sleepwhisper.core.persistence.dao.BabyDao
import com.lizhi1026.sleepwhisper.core.persistence.dao.DiaperDao
import com.lizhi1026.sleepwhisper.core.persistence.dao.FeedingDao
import com.lizhi1026.sleepwhisper.core.persistence.dao.RecommendationDao
import com.lizhi1026.sleepwhisper.core.persistence.dao.SleepDao
import com.lizhi1026.sleepwhisper.core.persistence.mapper.toDomain
import com.lizhi1026.sleepwhisper.core.persistence.mapper.toEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [Repository] 接口的 Room 数据库实现，属于 data / persistence 层。
 *
 * 所在层：core.persistence（基础设施层），由 Hilt 以 @Singleton 提供。
 *
 * 关键约定：
 * - Room 的 suspend DAO 方法内部已切换到数据库 IO 执行器，**无需再套 `withContext(Dispatchers.IO)`**，
 *   否则只会产生多余的上下文切换。Mapper 为纯 CPU 运算，在调用方协程调度器上执行。
 * - ⚠️ **所有写操作均通过 [safeWrite] 包裹**：捕获 `SQLiteConstraintException`、
 *   `IOException` 等异常后仅打印日志，**UI 不会感知失败**。
 *   若需向用户反馈写入错误，应在业务层调用 `ToastCenter.show(...)` 等机制。
 * - ⚠️ 接口所有方法均为 `suspend fun`，**不返回 Flow**；响应式刷新由上层
 *   `AppStateContainer` 在写操作完成后手动 reload 触发。
 */
private const val TAG = "sw.repo"

@Singleton
class RoomRepository @Inject constructor(
    private val babyDao: BabyDao,
    private val sleepDao: SleepDao,
    private val feedingDao: FeedingDao,
    private val diaperDao: DiaperDao,
    private val recommendationDao: RecommendationDao
) : Repository {

    /**
     * 写操作安全包装器，捕获所有异常以防止 UI 崩溃。
     *
     * ⚠️ **吞掉异常**（含 UUID 碰撞导致的主键冲突、外键违反、IO 错误等），
     * 仅通过 [Log.e] 记录到日志。调用方已乐观更新 LiveData/State，
     * 用户可继续操作；若需要用户可见的错误反馈，请在上层调用 `ToastCenter.show(...)`。
     *
     * @param label 日志标签，用于区分具体写操作（如 "saveBaby"、"appendSleep"）。
     * @param block 实际执行写操作的 lambda。
     */
    private inline fun safeWrite(label: String, block: () -> Unit) {
        try {
            block()
        } catch (t: Throwable) {
            // 捕获冲突（UUID 碰撞、FK 违反）与 IO 错误，保持 UI 可用性；
            // 调用方已乐观更新界面状态，用户无感知失败。
            Log.e(TAG, "$label failed", t)
        }
    }

    // ────────────────────────── Baby ──────────────────────────

    /**
     * 读取最新宝宝信息，委托给 [BabyDao.loadLatest] 并将 Entity 映射为域对象。
     *
     * @return 存在时返回 [Baby]，表为空时返回 `null`。suspend。
     */
    override suspend fun loadBaby(): Baby? = babyDao.loadLatest()?.toDomain()

    /**
     * 保存宝宝信息（Upsert：主键存在则替换，否则插入）。
     *
     * @param baby 待保存的 [Baby] 域对象。写操作由 [safeWrite] 保护。
     */
    override suspend fun saveBaby(baby: Baby) = safeWrite("saveBaby") {
        babyDao.upsert(baby.toEntity())
    }

    // ────────────────────────── SleepSession ──────────────────────────

    /**
     * 追加一条睡眠记录；冲突时（IGNORE）静默跳过。
     *
     * @param session 待插入的 [SleepSession] 域对象。
     */
    override suspend fun appendSleep(session: SleepSession) = safeWrite("appendSleep") {
        sleepDao.insert(session.toEntity())
    }

    /**
     * 更新已有睡眠记录（例如填写结束时间、修改质量评分）。
     *
     * @param session 携带最新数据的 [SleepSession]，以 `id` 定位行。
     */
    override suspend fun updateSleep(session: SleepSession) = safeWrite("updateSleep") {
        sleepDao.update(session.toEntity())
    }

    /**
     * 查询指定宝宝当前正在进行的睡眠（`endAt IS NULL`，取最近一条）。
     *
     * @param babyId 宝宝 UUID。
     * @return 进行中的 [SleepSession]，不存在时返回 `null`。suspend。
     */
    override suspend fun ongoingSleep(babyId: String): SleepSession? =
        sleepDao.ongoing(babyId)?.toDomain()

    /**
     * 查询指定宝宝在指定时间戳之后的所有睡眠记录。
     *
     * @param babyId  宝宝 UUID。
     * @param sinceMs 起始时间戳（epoch-millis UTC），负数会被 `coerceAtLeast(0L)` 修正。
     * @return 按 `startAt` 升序排列的 [SleepSession] 列表。suspend。
     */
    override suspend fun sleepSessions(babyId: String, sinceMs: Long): List<SleepSession> =
        sleepDao.since(babyId, sinceMs.coerceAtLeast(0L)).map { it.toDomain() }

    // ────────────────────────── FeedingEvent ──────────────────────────

    /**
     * 追加一条喂养记录；冲突时（IGNORE）静默跳过。
     *
     * @param event 待插入的 [FeedingEvent] 域对象。
     */
    override suspend fun appendFeeding(event: FeedingEvent) = safeWrite("appendFeeding") {
        feedingDao.insert(event.toEntity())
    }

    /**
     * 更新已有喂养记录。
     *
     * @param event 携带最新数据的 [FeedingEvent]，以 `id` 定位行。
     */
    override suspend fun updateFeeding(event: FeedingEvent) = safeWrite("updateFeeding") {
        feedingDao.update(event.toEntity())
    }

    /**
     * 按 id 删除一条喂养记录。
     *
     * @param id 目标记录的 UUID 字符串。
     */
    override suspend fun deleteFeeding(id: String) = safeWrite("deleteFeeding") {
        feedingDao.deleteById(id)
    }

    /**
     * 查询指定宝宝在指定时间戳之后的所有喂养记录。
     *
     * @param babyId  宝宝 UUID。
     * @param sinceMs 起始时间戳（epoch-millis UTC），负数会被 `coerceAtLeast(0L)` 修正。
     * @return 按 `startedAt` 升序排列的 [FeedingEvent] 列表。suspend。
     */
    override suspend fun feedings(babyId: String, sinceMs: Long): List<FeedingEvent> =
        feedingDao.since(babyId, sinceMs.coerceAtLeast(0L)).map { it.toDomain() }

    // ────────────────────────── DiaperEvent ──────────────────────────

    /**
     * 追加一条换尿布记录；冲突时（IGNORE）静默跳过。
     *
     * @param event 待插入的 [DiaperEvent] 域对象。
     */
    override suspend fun appendDiaper(event: DiaperEvent) = safeWrite("appendDiaper") {
        diaperDao.insert(event.toEntity())
    }

    /**
     * 更新已有换尿布记录。
     *
     * @param event 携带最新数据的 [DiaperEvent]，以 `id` 定位行。
     */
    override suspend fun updateDiaper(event: DiaperEvent) = safeWrite("updateDiaper") {
        diaperDao.update(event.toEntity())
    }

    /**
     * 按 id 删除一条换尿布记录。
     *
     * @param id 目标记录的 UUID 字符串。
     */
    override suspend fun deleteDiaper(id: String) = safeWrite("deleteDiaper") {
        diaperDao.deleteById(id)
    }

    /**
     * 查询指定宝宝在指定时间戳之后的所有换尿布记录。
     *
     * @param babyId  宝宝 UUID。
     * @param sinceMs 起始时间戳（epoch-millis UTC），负数会被 `coerceAtLeast(0L)` 修正。
     * @return 按 `occurredAt` 升序排列的 [DiaperEvent] 列表。suspend。
     */
    override suspend fun diapers(babyId: String, sinceMs: Long): List<DiaperEvent> =
        diaperDao.since(babyId, sinceMs.coerceAtLeast(0L)).map { it.toDomain() }

    // ────────────────────────── SleepRecommendation ──────────────────────────

    /**
     * 追加（实为原子 upsert）一条睡眠建议。
     *
     * @param rec 待持久化的 [SleepRecommendation]。
     */
    override suspend fun appendRecommendation(rec: SleepRecommendation) =
        safeWrite("appendRecommendation") {
            // ⚠️ babyId 唯一索引 + REPLACE 冲突策略 = 原子 upsert，每宝宝始终只保留最新一行。
            recommendationDao.insert(rec.toEntity())
        }

    /**
     * 查询指定宝宝最新的睡眠建议（按 `computedAt DESC` 取首条）。
     *
     * @param babyId 宝宝 UUID。
     * @return 最新的 [SleepRecommendation]，不存在时返回 `null`。suspend。
     */
    override suspend fun latestRecommendation(babyId: String): SleepRecommendation? =
        recommendationDao.latest(babyId)?.toDomain()
}
