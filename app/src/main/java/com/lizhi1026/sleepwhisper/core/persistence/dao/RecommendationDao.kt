package com.lizhi1026.sleepwhisper.core.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lizhi1026.sleepwhisper.core.persistence.entity.SleepRecommendationEntity

@Dao
interface RecommendationDao {
    /**
     * Atomic upsert by babyId — the unique index on babyId combined with REPLACE makes
     * insert behave as "one row per baby". No separate purge step needed.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SleepRecommendationEntity)

    @Query("DELETE FROM sleep_recommendation WHERE babyId = :babyId")
    suspend fun deleteForBaby(babyId: String)

    @Query("SELECT * FROM sleep_recommendation WHERE babyId = :babyId ORDER BY computedAt DESC LIMIT 1")
    suspend fun latest(babyId: String): SleepRecommendationEntity?
}
