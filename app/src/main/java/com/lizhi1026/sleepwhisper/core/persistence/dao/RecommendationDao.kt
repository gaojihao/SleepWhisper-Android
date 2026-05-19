package com.lizhi1026.sleepwhisper.core.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.lizhi1026.sleepwhisper.core.persistence.entity.SleepRecommendationEntity

@Dao
abstract class RecommendationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(entity: SleepRecommendationEntity)

    @Query("DELETE FROM sleep_recommendation WHERE babyId = :babyId")
    abstract suspend fun deleteForBaby(babyId: String)

    @Query("SELECT * FROM sleep_recommendation WHERE babyId = :babyId ORDER BY computedAt DESC LIMIT 1")
    abstract suspend fun latest(babyId: String): SleepRecommendationEntity?

    /** Each baby keeps at most 1 record — purge first, then insert. */
    @Transaction
    open suspend fun appendPurging(entity: SleepRecommendationEntity) {
        deleteForBaby(entity.babyId)
        insert(entity)
    }
}
