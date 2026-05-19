package com.lizhi1026.sleepwhisper.app.di

import android.content.Context
import androidx.room.Room
import com.lizhi1026.sleepwhisper.core.persistence.Repository
import com.lizhi1026.sleepwhisper.core.persistence.RoomRepository
import com.lizhi1026.sleepwhisper.core.persistence.SleepWhisperDatabase
import com.lizhi1026.sleepwhisper.core.persistence.dao.BabyDao
import com.lizhi1026.sleepwhisper.core.persistence.dao.DiaperDao
import com.lizhi1026.sleepwhisper.core.persistence.dao.FeedingDao
import com.lizhi1026.sleepwhisper.core.persistence.dao.RecommendationDao
import com.lizhi1026.sleepwhisper.core.persistence.dao.SleepDao
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): SleepWhisperDatabase =
        Room.databaseBuilder(ctx, SleepWhisperDatabase::class.java, "sleepwhisper.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideBabyDao(db: SleepWhisperDatabase): BabyDao = db.babyDao()
    @Provides fun provideSleepDao(db: SleepWhisperDatabase): SleepDao = db.sleepDao()
    @Provides fun provideFeedingDao(db: SleepWhisperDatabase): FeedingDao = db.feedingDao()
    @Provides fun provideDiaperDao(db: SleepWhisperDatabase): DiaperDao = db.diaperDao()
    @Provides fun provideRecommendationDao(db: SleepWhisperDatabase): RecommendationDao = db.recommendationDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindRepository(impl: RoomRepository): Repository
}
