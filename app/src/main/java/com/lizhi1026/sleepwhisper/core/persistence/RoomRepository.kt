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
 * Room-backed implementation of [Repository].
 *
 * Note: Room's suspend DAOs already dispatch to the database executor (IO) — we do NOT wrap
 * each call in `withContext(Dispatchers.IO)` again. Doing so just adds a redundant context
 * switch and was removed in the HIGH-tier cleanup. The downstream mapper is pure-CPU and
 * runs on the caller's dispatcher.
 *
 * All `append*` paths catch their conflict exceptions so a double-tap or background retry
 * never surfaces as an uncaught crash — Room throws SQLiteConstraintException at this layer
 * with ABORT, and even with IGNORE we defensively wrap.
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

    private inline fun safeWrite(label: String, block: () -> Unit) {
        try {
            block()
        } catch (t: Throwable) {
            // Conflicts (UUID collision, FK violation), IO errors. Swallowing here keeps the UI
            // alive; the caller already optimistically updated LiveData and the user keeps moving.
            Log.e(TAG, "$label failed", t)
        }
    }

    // Baby

    override suspend fun loadBaby(): Baby? = babyDao.loadLatest()?.toDomain()

    override suspend fun saveBaby(baby: Baby) = safeWrite("saveBaby") {
        babyDao.upsert(baby.toEntity())
    }

    // SleepSession

    override suspend fun appendSleep(session: SleepSession) = safeWrite("appendSleep") {
        sleepDao.insert(session.toEntity())
    }

    override suspend fun updateSleep(session: SleepSession) = safeWrite("updateSleep") {
        sleepDao.update(session.toEntity())
    }

    override suspend fun ongoingSleep(babyId: String): SleepSession? =
        sleepDao.ongoing(babyId)?.toDomain()

    override suspend fun sleepSessions(babyId: String, sinceMs: Long): List<SleepSession> =
        sleepDao.since(babyId, sinceMs.coerceAtLeast(0L)).map { it.toDomain() }

    // FeedingEvent

    override suspend fun appendFeeding(event: FeedingEvent) = safeWrite("appendFeeding") {
        feedingDao.insert(event.toEntity())
    }

    override suspend fun updateFeeding(event: FeedingEvent) = safeWrite("updateFeeding") {
        feedingDao.update(event.toEntity())
    }

    override suspend fun deleteFeeding(id: String) = safeWrite("deleteFeeding") {
        feedingDao.deleteById(id)
    }

    override suspend fun feedings(babyId: String, sinceMs: Long): List<FeedingEvent> =
        feedingDao.since(babyId, sinceMs.coerceAtLeast(0L)).map { it.toDomain() }

    // DiaperEvent

    override suspend fun appendDiaper(event: DiaperEvent) = safeWrite("appendDiaper") {
        diaperDao.insert(event.toEntity())
    }

    override suspend fun updateDiaper(event: DiaperEvent) = safeWrite("updateDiaper") {
        diaperDao.update(event.toEntity())
    }

    override suspend fun deleteDiaper(id: String) = safeWrite("deleteDiaper") {
        diaperDao.deleteById(id)
    }

    override suspend fun diapers(babyId: String, sinceMs: Long): List<DiaperEvent> =
        diaperDao.since(babyId, sinceMs.coerceAtLeast(0L)).map { it.toDomain() }

    // SleepRecommendation

    override suspend fun appendRecommendation(rec: SleepRecommendation) =
        safeWrite("appendRecommendation") {
            // Unique index on babyId + REPLACE conflict strategy = atomic upsert.
            recommendationDao.insert(rec.toEntity())
        }

    override suspend fun latestRecommendation(babyId: String): SleepRecommendation? =
        recommendationDao.latest(babyId)?.toDomain()
}
