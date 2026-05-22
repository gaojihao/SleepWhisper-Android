/**
 * # PlaybackForegroundService
 *
 * **职责**：作为应用的前台保活服务，统一托管音频播放（[AudioPlayerService]）
 * 和哭声检测（[CryDetectionService]）两个引擎。只要任意一个引擎处于活跃状态，
 * 本服务就持续以前台服务形式运行；当两个引擎**同时**进入空闲/停止状态时，
 * 服务自动调用 `stopSelf()` 退出，避免长期占用系统资源。
 *
 * **所在层**：`core/foreground`——基础设施层，不包含业务逻辑。
 *
 * **与谁交互**：
 * - 上游：`AppStateContainer`（业务层）通过静态工厂方法发送 Intent 指令
 * - 下游：[AudioPlayerService]（音频引擎）、[CryDetectionService]（哭声检测引擎）
 * - 通知：[NotificationChannels.CH_PLAYBACK]（低优先级，无声前台保活通知）
 * - 媒体中心：`MediaStyleNotificationHelper.MediaStyle` 接入锁屏/快捷面板
 */
package com.lizhi1026.sleepwhisper.core.foreground

import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.Observer
import com.lizhi1026.sleepwhisper.R
import com.lizhi1026.sleepwhisper.app.MainActivity
import com.lizhi1026.sleepwhisper.core.audio.AudioPlayerService
import com.lizhi1026.sleepwhisper.core.cry.CryDetectionService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * 前台保活服务。
 *
 * 通过 Hilt 注入 [AudioPlayerService] 与 [CryDetectionService]，
 * 订阅两者的状态 LiveData；任一引擎活跃则维持前台通知，双双空闲则退出。
 */
@AndroidEntryPoint
class PlaybackForegroundService : LifecycleService() {

    /** Hilt 注入：音频播放引擎 */
    @Inject lateinit var audioPlayer: AudioPlayerService
    /** Hilt 注入：哭声检测引擎 */
    @Inject lateinit var cryDetection: CryDetectionService

    companion object {
        // Actions
        /** 启动音频播放 */
        const val ACTION_START_PLAYBACK = "action.start_playback"
        /** 停止音频播放 */
        const val ACTION_STOP_PLAYBACK = "action.stop_playback"
        /** 启动哭声检测 */
        const val ACTION_START_CRY = "action.start_cry"
        /** 停止哭声检测 */
        const val ACTION_STOP_CRY = "action.stop_cry"

        /** 前台通知固定 ID，保证只存在一条保活通知 */
        private const val NOTIF_ID = 1001

        // Extras
        /** Extra：音效预设 ID */
        const val EXTRA_PRESET_ID = "extra.preset_id"
        /** Extra：播放时长（秒），null 表示无限播放 */
        const val EXTRA_DURATION_SECONDS = "extra.duration_seconds"
        /** Extra：哭声检测分贝阈值 */
        const val EXTRA_CRY_THRESHOLD = "extra.cry_threshold"

        /**
         * 启动音频播放的便捷工厂方法。
         *
         * @param presetId       要播放的音效预设 ID
         * @param durationSeconds 播放时长（秒），null 表示不限时
         */
        fun startAudio(ctx: android.content.Context, presetId: String, durationSeconds: Int?) {
            val intent = android.content.Intent(ctx, PlaybackForegroundService::class.java).apply {
                action = ACTION_START_PLAYBACK
                putExtra(EXTRA_PRESET_ID, presetId)
                putExtra(EXTRA_DURATION_SECONDS, durationSeconds)
            }
            ContextCompat.startForegroundService(ctx, intent)
        }

        /**
         * 停止音频播放的便捷工厂方法。
         */
        fun stopAudio(ctx: android.content.Context) {
            val intent = android.content.Intent(ctx, PlaybackForegroundService::class.java)
                .setAction(ACTION_STOP_PLAYBACK)
            ContextCompat.startForegroundService(ctx, intent)
        }

        /**
         * 启动哭声检测的便捷工厂方法。
         *
         * @param thresholdDb 触发哭声事件的分贝阈值
         */
        fun startCry(ctx: android.content.Context, thresholdDb: Int) {
            val intent = android.content.Intent(ctx, PlaybackForegroundService::class.java).apply {
                action = ACTION_START_CRY
                putExtra(EXTRA_CRY_THRESHOLD, thresholdDb)
            }
            ContextCompat.startForegroundService(ctx, intent)
        }

        /**
         * 停止哭声检测的便捷工厂方法。
         */
        fun stopCry(ctx: android.content.Context) {
            val intent = android.content.Intent(ctx, PlaybackForegroundService::class.java)
                .setAction(ACTION_STOP_CRY)
            ContextCompat.startForegroundService(ctx, intent)
        }
    }

    /** 监听音频引擎状态变化，每次变化都重新判断是否可以停止服务 */
    private val audioObserver = Observer<com.lizhi1026.sleepwhisper.core.audio.PlayerState> {
        considerStoppingIfIdle()
    }
    /** 监听哭声检测引擎状态变化，每次变化都重新判断是否可以停止服务 */
    private val cryObserver = Observer<com.lizhi1026.sleepwhisper.core.cry.CryDetectionState> {
        considerStoppingIfIdle()
    }

    override fun onCreate() {
        super.onCreate()

        // CRITICAL ordering: call startForeground BEFORE registering observers.
        //
        // observeForever dispatches the LiveData's cached value synchronously to a new
        // observer. audioPlayer.stateLive is typically cached as Idle (subscribed elsewhere
        // by SleepingScreen/PlayerScreen). The synchronous callback then runs
        // considerStoppingIfIdle() → stopSelf(). If stopSelf() lands before startForeground()
        // the system raises ForegroundServiceDidNotStartInTimeException on Android 12+.
        //
        // Default service type is MEDIA_PLAYBACK only — it does NOT require RECORD_AUDIO,
        // so we can come up even when the caller is just sending a stop-cry message and the
        // user has denied microphone permission. The MICROPHONE bit is added on demand when
        // ACTION_START_CRY arrives (caller has already verified RECORD_AUDIO).
        // 必须在注册观察者之前先调用 startForeground，防止同步回调触发 stopSelf() 早于 startForeground
        startForegroundCompat(ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)

        // 注册状态观察者，任意引擎状态变化均触发空闲检测
        audioPlayer.stateLive.observeForever(audioObserver)
        cryDetection.stateLive.observeForever(cryObserver)
    }

    /**
     * 检查两个引擎是否同时处于空闲状态；若是则移除前台通知并停止服务。
     */
    private fun considerStoppingIfIdle() {
        val a = audioPlayer.stateLive.value
        val c = cryDetection.stateLive.value
        if (isIdlePair(a, c)) {
            // 双引擎均空闲：撤销前台状态（同时移除通知），再停止服务
            // Remove foreground state — notification drops with STOP_FOREGROUND_REMOVE.
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    /**
     * 判断音频引擎与哭声检测引擎是否同时处于空闲/停止/错误状态。
     *
     * @param a 当前音频引擎状态，null 视为空闲
     * @param c 当前哭声检测引擎状态，null 视为空闲
     * @return 两者均空闲时返回 true
     */
    private fun isIdlePair(
        a: com.lizhi1026.sleepwhisper.core.audio.PlayerState?,
        c: com.lizhi1026.sleepwhisper.core.cry.CryDetectionState?
    ): Boolean {
        val audioIdle = when (a) {
            // null / Idle / Stopped / Error 均视为音频引擎空闲
            null, com.lizhi1026.sleepwhisper.core.audio.PlayerState.Idle,
            is com.lizhi1026.sleepwhisper.core.audio.PlayerState.Stopped,
            is com.lizhi1026.sleepwhisper.core.audio.PlayerState.Error -> true
            else -> false
        }
        val cryIdle = when (c) {
            // null / Disabled 均视为哭声检测引擎空闲
            null, com.lizhi1026.sleepwhisper.core.cry.CryDetectionState.Disabled -> true
            else -> false
        }
        return audioIdle && cryIdle
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        val action = intent?.action
        // Promote the foreground-service type to include MICROPHONE only when actually
        // starting cry detection. The caller (AppStateContainer.startSleep) verifies
        // RECORD_AUDIO before issuing ACTION_START_CRY, so the system permission check
        // for the microphone type will succeed.
        // 仅在启动哭声检测时才追加 MICROPHONE 前台类型，其余操作只需 MEDIA_PLAYBACK
        val typeMask = if (action == ACTION_START_CRY) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK or
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        } else {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        }
        startForegroundCompat(typeMask)

        when (action) {
            ACTION_START_PLAYBACK -> {
                // 读取预设 ID，缺失则视为无效指令直接返回
                val presetId = intent.getStringExtra(EXTRA_PRESET_ID) ?: return START_NOT_STICKY
                // 读取播放时长，≤0 时转为 null（无限播放）
                val duration = intent.getIntExtra(EXTRA_DURATION_SECONDS, -1).let {
                    if (it > 0) it else null
                }
                audioPlayer.play(presetId, duration)
            }
            ACTION_STOP_PLAYBACK -> {
                // 带 500ms 淡出停止音频播放
                audioPlayer.stop("foreground_stop", 500)
            }
            ACTION_START_CRY -> {
                // 读取分贝阈值，默认 60dB，启动哭声检测
                val threshold = intent.getIntExtra(EXTRA_CRY_THRESHOLD, 60)
                cryDetection.start(thresholdDb = threshold)
            }
            ACTION_STOP_CRY -> {
                // 停止哭声检测
                cryDetection.stop()
            }
        }

        return START_NOT_STICKY
    }

    /**
     * 兼容不同 Android 版本调用 `startForeground`。
     * Android Q（API 29）及以上需要传入前台服务类型掩码。
     *
     * @param typeMask 前台服务类型位掩码（MEDIA_PLAYBACK 和/或 MICROPHONE）
     */
    private fun startForegroundCompat(typeMask: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // API 29+ 需要明确声明前台服务类型
            startForeground(NOTIF_ID, buildNotification(), typeMask)
        } else {
            startForeground(NOTIF_ID, buildNotification())
        }
    }

    override fun onDestroy() {
        // 服务销毁时移除状态观察者，防止内存泄漏
        audioPlayer.stateLive.removeObserver(audioObserver)
        cryDetection.stateLive.removeObserver(cryObserver)
        super.onDestroy()
    }

    /**
     * 构建前台保活通知。
     *
     * 使用 [NotificationChannels.CH_PLAYBACK]（低优先级、无声）渠道，
     * 点击通知跳转到 [MainActivity]。若音频引擎持有 MediaSession，
     * 则附加 `MediaStyle` 接入系统媒体中心（锁屏、快捷面板、Android Auto、Wear OS）。
     */
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    private fun buildNotification(): android.app.Notification {
        // 构建点击通知时跳转到主界面的 PendingIntent
        val tapIntent = android.content.Intent(this, MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val tapPending = PendingIntent.getActivity(
            this,
            0,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, com.lizhi1026.sleepwhisper.core.notifications.NotificationChannels.CH_PLAYBACK)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(getString(R.string.fg_notification_title))
            .setContentText(getString(R.string.fg_notification_text))
            .setOngoing(true)          // 用户无法手动划除该通知
            .setContentIntent(tapPending)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)

        // Attach MediaStyle so the notification participates in the system media center
        // (lock screen, quick-settings media panel, Android Auto, Wear OS).
        // 若 MediaSession 可用，附加 MediaStyle 使通知显示在系统媒体中心
        audioPlayer.mediaSession?.let { session ->
            builder.setStyle(
                androidx.media3.session.MediaStyleNotificationHelper.MediaStyle(session)
            )
        }
        return builder.build()
    }
}
