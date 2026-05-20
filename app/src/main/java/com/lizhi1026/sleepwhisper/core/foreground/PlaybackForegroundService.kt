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

@AndroidEntryPoint
class PlaybackForegroundService : LifecycleService() {

    @Inject lateinit var audioPlayer: AudioPlayerService
    @Inject lateinit var cryDetection: CryDetectionService

    companion object {
        // Actions
        const val ACTION_START_PLAYBACK = "action.start_playback"
        const val ACTION_STOP_PLAYBACK = "action.stop_playback"
        const val ACTION_START_CRY = "action.start_cry"
        const val ACTION_STOP_CRY = "action.stop_cry"

        private const val NOTIF_ID = 1001

        // Extras
        const val EXTRA_PRESET_ID = "extra.preset_id"
        const val EXTRA_DURATION_SECONDS = "extra.duration_seconds"
        const val EXTRA_CRY_THRESHOLD = "extra.cry_threshold"

        fun startAudio(ctx: android.content.Context, presetId: String, durationSeconds: Int?) {
            val intent = android.content.Intent(ctx, PlaybackForegroundService::class.java).apply {
                action = ACTION_START_PLAYBACK
                putExtra(EXTRA_PRESET_ID, presetId)
                putExtra(EXTRA_DURATION_SECONDS, durationSeconds)
            }
            ContextCompat.startForegroundService(ctx, intent)
        }

        fun stopAudio(ctx: android.content.Context) {
            val intent = android.content.Intent(ctx, PlaybackForegroundService::class.java)
                .setAction(ACTION_STOP_PLAYBACK)
            ContextCompat.startForegroundService(ctx, intent)
        }

        fun startCry(ctx: android.content.Context, thresholdDb: Int) {
            val intent = android.content.Intent(ctx, PlaybackForegroundService::class.java).apply {
                action = ACTION_START_CRY
                putExtra(EXTRA_CRY_THRESHOLD, thresholdDb)
            }
            ContextCompat.startForegroundService(ctx, intent)
        }

        fun stopCry(ctx: android.content.Context) {
            val intent = android.content.Intent(ctx, PlaybackForegroundService::class.java)
                .setAction(ACTION_STOP_CRY)
            ContextCompat.startForegroundService(ctx, intent)
        }
    }

    private val audioObserver = Observer<com.lizhi1026.sleepwhisper.core.audio.PlayerState> {
        considerStoppingIfIdle()
    }
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
        startForegroundCompat(ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)

        audioPlayer.stateLive.observeForever(audioObserver)
        cryDetection.stateLive.observeForever(cryObserver)
    }

    private fun considerStoppingIfIdle() {
        val a = audioPlayer.stateLive.value
        val c = cryDetection.stateLive.value
        if (isIdlePair(a, c)) {
            // Remove foreground state — notification drops with STOP_FOREGROUND_REMOVE.
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun isIdlePair(
        a: com.lizhi1026.sleepwhisper.core.audio.PlayerState?,
        c: com.lizhi1026.sleepwhisper.core.cry.CryDetectionState?
    ): Boolean {
        val audioIdle = when (a) {
            null, com.lizhi1026.sleepwhisper.core.audio.PlayerState.Idle,
            is com.lizhi1026.sleepwhisper.core.audio.PlayerState.Stopped,
            is com.lizhi1026.sleepwhisper.core.audio.PlayerState.Error -> true
            else -> false
        }
        val cryIdle = when (c) {
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
        val typeMask = if (action == ACTION_START_CRY) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK or
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        } else {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        }
        startForegroundCompat(typeMask)

        when (action) {
            ACTION_START_PLAYBACK -> {
                val presetId = intent.getStringExtra(EXTRA_PRESET_ID) ?: return START_NOT_STICKY
                val duration = intent.getIntExtra(EXTRA_DURATION_SECONDS, -1).let {
                    if (it > 0) it else null
                }
                audioPlayer.play(presetId, duration)
            }
            ACTION_STOP_PLAYBACK -> {
                audioPlayer.stop("foreground_stop", 500)
            }
            ACTION_START_CRY -> {
                val threshold = intent.getIntExtra(EXTRA_CRY_THRESHOLD, 60)
                cryDetection.start(thresholdDb = threshold)
            }
            ACTION_STOP_CRY -> {
                cryDetection.stop()
            }
        }

        return START_NOT_STICKY
    }

    private fun startForegroundCompat(typeMask: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, buildNotification(), typeMask)
        } else {
            startForeground(NOTIF_ID, buildNotification())
        }
    }

    override fun onDestroy() {
        audioPlayer.stateLive.removeObserver(audioObserver)
        cryDetection.stateLive.removeObserver(cryObserver)
        super.onDestroy()
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    private fun buildNotification(): android.app.Notification {
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
            .setOngoing(true)
            .setContentIntent(tapPending)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)

        // Attach MediaStyle so the notification participates in the system media center
        // (lock screen, quick-settings media panel, Android Auto, Wear OS).
        audioPlayer.mediaSession?.let { session ->
            builder.setStyle(
                androidx.media3.session.MediaStyleNotificationHelper.MediaStyle(session)
            )
        }
        return builder.build()
    }
}
