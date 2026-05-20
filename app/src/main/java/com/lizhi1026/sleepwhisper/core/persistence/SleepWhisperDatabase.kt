package com.lizhi1026.sleepwhisper.core.persistence

import androidx.room.Database
import androidx.room.RoomDatabase
import com.lizhi1026.sleepwhisper.core.persistence.dao.BabyDao
import com.lizhi1026.sleepwhisper.core.persistence.dao.DiaperDao
import com.lizhi1026.sleepwhisper.core.persistence.dao.FeedingDao
import com.lizhi1026.sleepwhisper.core.persistence.dao.RecommendationDao
import com.lizhi1026.sleepwhisper.core.persistence.dao.SleepDao
import com.lizhi1026.sleepwhisper.core.persistence.entity.BabyEntity
import com.lizhi1026.sleepwhisper.core.persistence.entity.DiaperEventEntity
import com.lizhi1026.sleepwhisper.core.persistence.entity.FeedingEventEntity
import com.lizhi1026.sleepwhisper.core.persistence.entity.SleepRecommendationEntity
import com.lizhi1026.sleepwhisper.core.persistence.entity.SleepSessionEntity

@Database(
    entities = [
        BabyEntity::class,
        SleepSessionEntity::class,
        FeedingEventEntity::class,
        DiaperEventEntity::class,
        SleepRecommendationEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class SleepWhisperDatabase : RoomDatabase() {
    abstract fun babyDao(): BabyDao
    abstract fun sleepDao(): SleepDao
    abstract fun feedingDao(): FeedingDao
    abstract fun diaperDao(): DiaperDao
    abstract fun recommendationDao(): RecommendationDao
}
