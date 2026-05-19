package com.lizhi1026.sleepwhisper.core.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lizhi1026.sleepwhisper.core.persistence.entity.SleepSessionEntity

@Dao
interface SleepDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: SleepSessionEntity)

    @Update
    suspend fun update(entity: SleepSessionEntity)

    @Query("SELECT * FROM sleep_session WHERE babyId = :babyId AND endAt IS NULL LIMIT 1")
    suspend fun ongoing(babyId: String): SleepSessionEntity?

    @Query("SELECT * FROM sleep_session WHERE babyId = :babyId AND startAt >= :sinceMs ORDER BY startAt ASC")
    suspend fun since(babyId: String, sinceMs: Long): List<SleepSessionEntity>
}
