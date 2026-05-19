package com.lizhi1026.sleepwhisper.core.persistence.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lizhi1026.sleepwhisper.core.persistence.entity.DiaperEventEntity

@Dao
interface DiaperDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: DiaperEventEntity)

    @Update
    suspend fun update(entity: DiaperEventEntity)

    @Delete
    suspend fun delete(entity: DiaperEventEntity)

    @Query("DELETE FROM diaper_event WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM diaper_event WHERE babyId = :babyId AND occurredAt >= :sinceMs ORDER BY occurredAt ASC")
    suspend fun since(babyId: String, sinceMs: Long): List<DiaperEventEntity>
}
