package com.lizhi1026.sleepwhisper.core.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.lizhi1026.sleepwhisper.core.persistence.entity.BabyEntity

@Dao
interface BabyDao {
    @Query("SELECT * FROM baby ORDER BY createdAt DESC LIMIT 1")
    suspend fun loadLatest(): BabyEntity?

    @Upsert
    suspend fun upsert(entity: BabyEntity)
}
