/**
 * Hilt 依赖注入模块：数据层绑定配置。
 *
 * **职责**：
 * - [DatabaseModule]：构建并提供 Room 数据库单例（[SleepWhisperDatabase]），并为每个 DAO 提供绑定。
 * - [RepositoryModule]：将 [RoomRepository]（实现类）绑定到 [Repository]（接口），
 *   使上层代码仅依赖接口而非具体实现。
 *
 * **所在层**：app/di 模块 / 依赖注入层。
 *
 * **与谁交互**：
 * - [SleepWhisperDatabase]（core/persistence）— Room 数据库实体。
 * - 各 DAO 接口（BabyDao、SleepDao 等）— 由 Room 自动实现，注入给 [RoomRepository]。
 * - [Repository] / [RoomRepository]（core/persistence）— 数据访问抽象与实现。
 *
 * **关键约定**：
 * - 数据库使用 `fallbackToDestructiveMigration()`，Schema 变更时会销毁并重建数据库，
 *   生产版本发布前需替换为正式的迁移脚本。
 * - 所有 Provides / Binds 均为 `@Singleton`，保证全局唯一实例。
 */
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

/**
 * 提供数据库实例及各 DAO 的 Hilt 模块，安装在 [SingletonComponent] 中（进程级单例）。
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * 构建并提供 [SleepWhisperDatabase] 单例。
     *
     * ⚠️ 当前使用 `fallbackToDestructiveMigration()`：Schema 升级时数据库会被销毁重建，
     * 正式发布版本应替换为明确的 Migration 对象以避免用户数据丢失。
     *
     * @param ctx 应用 Context，用于确定数据库文件路径。
     * @return 数据库单例实例。
     */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): SleepWhisperDatabase =
        Room.databaseBuilder(ctx, SleepWhisperDatabase::class.java, "sleepwhisper.db")
            .fallbackToDestructiveMigration()
            .build()

    /** 提供 [BabyDao]，由 Room 根据 [SleepWhisperDatabase] 自动实现。 */
    @Provides fun provideBabyDao(db: SleepWhisperDatabase): BabyDao = db.babyDao()
    /** 提供 [SleepDao]，由 Room 根据 [SleepWhisperDatabase] 自动实现。 */
    @Provides fun provideSleepDao(db: SleepWhisperDatabase): SleepDao = db.sleepDao()
    /** 提供 [FeedingDao]，由 Room 根据 [SleepWhisperDatabase] 自动实现。 */
    @Provides fun provideFeedingDao(db: SleepWhisperDatabase): FeedingDao = db.feedingDao()
    /** 提供 [DiaperDao]，由 Room 根据 [SleepWhisperDatabase] 自动实现。 */
    @Provides fun provideDiaperDao(db: SleepWhisperDatabase): DiaperDao = db.diaperDao()
    /** 提供 [RecommendationDao]，由 Room 根据 [SleepWhisperDatabase] 自动实现。 */
    @Provides fun provideRecommendationDao(db: SleepWhisperDatabase): RecommendationDao = db.recommendationDao()
}

/**
 * 将 [RoomRepository] 实现绑定到 [Repository] 接口的抽象 Hilt 模块，安装在 [SingletonComponent] 中。
 *
 * 使用 `@Binds` 而非 `@Provides` 以减少不必要的包装代码，Hilt 会直接复用已有的 [RoomRepository] 实例。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    /** 将 [RoomRepository] 绑定为 [Repository] 接口的唯一实现（进程单例）。 */
    @Binds
    @Singleton
    abstract fun bindRepository(impl: RoomRepository): Repository
}
