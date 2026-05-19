package com.lizhi1026.sleepwhisper.core.persistence

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomRepository @Inject constructor(
    private val babyDao: BabyDao,
    private val sleepDao: SleepDao,
    private val feedingDao: FeedingDao,
    private val diaperDao: DiaperDao,
    private val recommendationDao: RecommendationDao
) : Repository {

    // Baby

    override suspend fun loadBaby(): Baby? = withContext(Dispatchers.IO) {
        babyDao.loadLatest()?.toDomain()
    }

    override suspend fun saveBaby(baby: Baby) = withContext(Dispatchers.IO) {
        babyDao.upsert(baby.toEntity())
    }

    // SleepSession

    override suspend fun appendSleep(session: SleepSession) = withContext(Dispatchers.IO) {
        sleepDao.insert(session.toEntity())
    }

    override suspend fun updateSleep(session: SleepSession) = withContext(Dispatchers.IO) {
        sleepDao.update(session.toEntity())
    }

    override suspend fun ongoingSleep(babyId: String): SleepSession? = withContext(Dispatchers.IO) {
        sleepDao.ongoing(babyId)?.toDomain()
    }

    override suspend fun sleepSessions(
        babyId: String,
        sinceMs: Long
    ): List<SleepSession> = withContext(Dispatchers.IO) {
        sleepDao.since(babyId, sinceMs).map { it.toDomain() }
    }

    // FeedingEvent

    override suspend fun appendFeeding(event: FeedingEvent) = withContext(Dispatchers.IO) {
        feedingDao.insert(event.toEntity())
    }

    override suspend fun updateFeeding(event: FeedingEvent) = withContext(Dispatchers.IO) {
        feedingDao.update(event.toEntity())
    }

    override suspend fun deleteFeeding(id: String) = withContext(Dispatchers.IO) {
        feedingDao.deleteById(id)
    }

    override suspend fun feedings(
        babyId: String,
        sinceMs: Long
    ): List<FeedingEvent> = withContext(Dispatchers.IO) {
        feedingDao.since(babyId, sinceMs).map { it.toDomain() }
    }

    // DiaperEvent

    override suspend fun appendDiaper(event: DiaperEvent) = withContext(Dispatchers.IO) {
        diaperDao.insert(event.toEntity())
    }

    override suspend fun updateDiaper(event: DiaperEvent) = withContext(Dispatchers.IO) {
        diaperDao.update(event.toEntity())
    }

    override suspend fun deleteDiaper(id: String) = withContext(Dispatchers.IO) {
        diaperDao.deleteById(id)
    }

    override suspend fun diapers(
        babyId: String,
        sinceMs: Long
    ): List<DiaperEvent> = withContext(Dispatchers.IO) {
        diaperDao.since(babyId, sinceMs).map { it.toDomain() }
    }

    // SleepRecommendation

    override suspend fun appendRecommendation(rec: SleepRecommendation) =
        withContext(Dispatchers.IO) {
            recommendationDao.appendPurging(rec.toEntity())
        }

    override suspend fun latestRecommendation(babyId: String): SleepRecommendation? =
        withContext(Dispatchers.IO) {
            recommendationDao.latest(babyId)?.toDomain()
        }
}
