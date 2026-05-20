package com.lizhi1026.sleepwhisper.core.persistence.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lizhi1026.sleepwhisper.core.persistence.entity.FeedingEventEntity

@Dao
interface FeedingDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: FeedingEventEntity)

    @Update
    suspend fun update(entity: FeedingEventEntity)

    @Delete
    suspend fun delete(entity: FeedingEventEntity)

    @Query("DELETE FROM feeding_event WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM feeding_event WHERE babyId = :babyId AND startedAt >= :sinceMs ORDER BY startedAt ASC")
    suspend fun since(babyId: String, sinceMs: Long): List<FeedingEventEntity>
}
