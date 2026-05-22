/**
 * # NotificationChannels
 *
 * **职责**：集中注册并管理应用所有通知渠道，在应用冷启动时通过 [ensure] 方法
 * 一次性创建全部渠道（幂等操作，重复调用安全）。
 *
 * **所在层**：`core/notifications`——基础设施层，无业务逻辑。
 *
 * **三条渠道说明**：
 * - [CH_PLAYBACK]：`IMPORTANCE_LOW`，无声无震动，用于前台保活通知（[PlaybackForegroundService]）
 * - [CH_SLEEP_CHECKIN]：`IMPORTANCE_HIGH`，开启震动+LED 灯光，用于睡眠签到提醒
 * - [CH_REC_WINDOW]：`IMPORTANCE_DEFAULT`，开启震动，用于推荐睡眠窗口提醒
 */
package com.lizhi1026.sleepwhisper.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.lizhi1026.sleepwhisper.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 通知渠道注册器（单例）。
 *
 * 由 Hilt 以 `@Singleton` 注入，在 `Application.onCreate` 阶段调用 [ensure]
 * 完成全部渠道注册，后续无需重复注册。
 */
@Singleton
class NotificationChannels @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * 幂等注册全部通知渠道。
     *
     * 若渠道已存在则 Android 系统直接忽略重复创建；若发生渠道 ID 冲突等异常，
     * 则静默吞掉，确保冷启动流程不被通知渠道异常阻断。
     */
    fun ensure() {
        val nm = context.getSystemService(NotificationManager::class.java)
        try {
            // --- 渠道1：前台保活通知（低优先级，无声，不在角标显示）---
            nm.createNotificationChannel(
                NotificationChannel(
                    CH_PLAYBACK,
                    context.getString(R.string.fg_channel_name),
                    NotificationManager.IMPORTANCE_LOW  // 低优先级：无声，不弹出横幅
                ).apply {
                    description = context.getString(R.string.fg_channel_desc)
                    setShowBadge(false)         // 不在应用图标上显示角标
                    enableVibration(false)      // 无震动
                    setSound(null, null)        // 无提示音
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC  // 锁屏可见
                }
            )
            // --- 渠道2：睡眠签到提醒（高优先级，震动+LED 灯）---
            nm.createNotificationChannel(
                NotificationChannel(
                    CH_SLEEP_CHECKIN,
                    context.getString(R.string.ch_sleep_checkin),
                    NotificationManager.IMPORTANCE_HIGH  // 高优先级：弹出横幅，有提示音
                ).apply {
                    enableVibration(true)       // 开启震动
                    enableLights(true)          // 开启 LED 灯光提醒（部分机型支持）
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                }
            )
            // --- 渠道3：推荐睡眠窗口提醒（默认优先级，震动）---
            nm.createNotificationChannel(
                NotificationChannel(
                    CH_REC_WINDOW,
                    context.getString(R.string.ch_rec_window),
                    NotificationManager.IMPORTANCE_DEFAULT  // 默认优先级
                ).apply {
                    enableVibration(true)       // 开启震动
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                }
            )
        } catch (_: Throwable) {
            // createNotificationChannel can throw IllegalArgumentException if a previously
            // deleted channel id is reused with different settings; we swallow so cold start
            // is not bricked by a channel state quirk.
            // 静默吞掉异常，防止因渠道 ID 复用冲突等问题导致应用冷启动崩溃
        }
    }

    companion object {
        /** 前台保活通知渠道 ID（低优先级，无声无震动） */
        const val CH_PLAYBACK = "playback"
        /** 睡眠签到提醒渠道 ID（高优先级，震动+灯光） */
        const val CH_SLEEP_CHECKIN = "sleep_checkin"
        /** 推荐睡眠窗口提醒渠道 ID（默认优先级，震动） */
        const val CH_REC_WINDOW = "rec_window"
    }
}
