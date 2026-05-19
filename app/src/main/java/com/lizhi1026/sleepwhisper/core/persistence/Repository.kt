package com.lizhi1026.sleepwhisper.core.persistence

import com.lizhi1026.sleepwhisper.model.Baby
import com.lizhi1026.sleepwhisper.model.DiaperEvent
import com.lizhi1026.sleepwhisper.model.FeedingEvent
import com.lizhi1026.sleepwhisper.model.SleepRecommendation
import com.lizhi1026.sleepwhisper.model.SleepSession

/**
 * Single entry point for all domain-layer persistence operations.
 *
 * UserSettings is intentionally excluded — it is persisted via DataStore / JSON
 * in a separate SettingsStore (Phase 2C).  This keeps the interface stable and
 * ensures no query accidentally lands in Room.
 */
interface Repository {

    // Baby

    suspend fun loadBaby(): Baby?
    suspend fun saveBaby(baby: Baby)

    // SleepSession

    suspend fun appendSleep(session: SleepSession)
    suspend fun updateSleep(session: SleepSession)
    suspend fun ongoingSleep(babyId: String): SleepSession?
    suspend fun sleepSessions(babyId: String, sinceMs: Long): List<SleepSession>

    // FeedingEvent

    suspend fun appendFeeding(event: FeedingEvent)
    suspend fun updateFeeding(event: FeedingEvent)
    suspend fun deleteFeeding(id: String)
    suspend fun feedings(babyId: String, sinceMs: Long): List<FeedingEvent>

    // DiaperEvent

    suspend fun appendDiaper(event: DiaperEvent)
    suspend fun updateDiaper(event: DiaperEvent)
    suspend fun deleteDiaper(id: String)
    suspend fun diapers(babyId: String, sinceMs: Long): List<DiaperEvent>

    // SleepRecommendation

    suspend fun appendRecommendation(rec: SleepRecommendation)
    suspend fun latestRecommendation(babyId: String): SleepRecommendation?
}
