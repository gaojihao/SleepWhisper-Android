/**
 * 音频播放引擎（Kotlin 单例，非 Android Service）。
 *
 * 所在层：core/audio — 应用核心层，不依赖 UI。
 * ⚠️ 注意：本类是 Kotlin 单例（@Singleton + Hilt 注入），不是 Android Service。
 *    真正的前台 Service 是 [PlaybackForegroundService]，它持有本类引用并管理生命周期。
 *
 * 交互对象：
 *   - [PlaybackForegroundService]：绑定 MediaSession 并驱动前台通知。
 *   - [CryDetectionService]：哭声触发时调用 [boostVolumeOnCry]。
 *   - ViewModel / Composable：通过 [stateLive]、[currentPresetLive]、[timerEndsAtLive] 观察状态。
 *
 * 关键约定：
 *   - ExoPlayer 必须在主线程操作；[boostVolumeOnCry] 内部通过 scope（Main.immediate）保证线程安全。
 *   - 淡入时长固定 1.5 s，与 iOS 端一致。
 *   - 播放模式：REPEAT_MODE_ONE（单曲循环）、WAKE_MODE_LOCAL（持锁保活）。
 *   - 完整实现 AudioFocus + handleAudioBecomingNoisy（耳机拔出/蓝牙断连自动暂停）。
 */
package com.lizhi1026.sleepwhisper.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn as OptInAnn
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import com.lizhi1026.sleepwhisper.R
import com.lizhi1026.sleepwhisper.model.AudioPreset
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max
import javax.inject.Inject
import javax.inject.Singleton

@OptInAnn(androidx.media3.common.util.UnstableApi::class)
@Singleton
class AudioPlayerService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _state = kotlinx.coroutines.flow.MutableStateFlow<PlayerState>(PlayerState.Idle)
    val stateLive: LiveData<PlayerState> = _state.asLiveData()

    private val _currentPreset = kotlinx.coroutines.flow.MutableStateFlow<AudioPreset?>(null)
    val currentPresetLive: LiveData<AudioPreset?> = _currentPreset.asLiveData()

    private val _timerEndsAt = kotlinx.coroutines.flow.MutableStateFlow<Long?>(null)
    val timerEndsAtLive: LiveData<Long?> = _timerEndsAt.asLiveData()

    private var player: ExoPlayer? = null
    private var mediaSessionInternal: MediaSession? = null

    /** Exposed to PlaybackForegroundService so the foreground notification can attach a MediaStyle. */
    val mediaSession: MediaSession? get() = mediaSessionInternal

    private var fadeJob: Job? = null
    private var timerJob: Job? = null

    // Focus state: distinguish transient-pause vs duck so we restore correctly on GAIN.
    private var audioFocusLossTransient = false
    private var preDuckVolume: Float? = null
    private var sessionRequested = false
    private var audioFocusRequest: AudioFocusRequest? = null

    private val audioManager: AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val focusListener = OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                // Permanent loss (e.g. another media app gained focus). Tear down.
                stop(reason = "focus_loss", fadeOverMs = 200)
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                val current = _state.value
                if (current is PlayerState.Playing) {
                    _state.value = PlayerState.Interrupted(restoreTo = current.presetId)
                    player?.pause()
                    audioFocusLossTransient = true
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Default contract: app ducks itself.
                if (preDuckVolume == null) {
                    preDuckVolume = player?.volume ?: 1f
                    player?.volume = DUCK_VOLUME
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                // Un-duck first.
                preDuckVolume?.let { saved ->
                    player?.volume = saved
                    preDuckVolume = null
                }
                if (audioFocusLossTransient) {
                    audioFocusLossTransient = false
                    // Resume using the current preset, not the snapshot — user may have switched
                    // while we were interrupted.
                    val resumeId = _currentPreset.value?.id
                        ?: (_state.value as? PlayerState.Interrupted)?.restoreTo
                    if (resumeId != null) play(resumeId, null)
                }
            }
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            _state.value = PlayerState.Error(error.message ?: "audio.error.playFailed")
            // Tear down so isIdlePair() lets the foreground service exit; UI can re-arm via play().
            scope.launch { stop(reason = "player_error", fadeOverMs = 0) }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mainHandler = Handler(Looper.getMainLooper())

    companion object {
        private const val DUCK_VOLUME = 0.2f
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    fun play(presetId: String, durationSeconds: Int? = null) {
        val preset = AudioPreset.byId(presetId)
            ?: run { _state.value = PlayerState.Error("audio.error.notFound"); return }
        _state.value = PlayerState.Loading(presetId)
        _currentPreset.value = preset

        if (!sessionRequested) {
            requestAudioFocusIfNeeded()
            sessionRequested = true
        }

        val uri = android.net.Uri.parse(
            "android.resource://${context.packageName}/raw/${preset.fileBundleName}"
        )
        val titleRes = context.resources.getIdentifier(
            preset.nameKey.replace('.', '_'), "string", context.packageName
        )
        val title = if (titleRes != 0) context.getString(titleRes) else preset.nameKey
        val artist = context.getString(R.string.nowplaying_defaultartist)
        val item = MediaItem.Builder()
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setAlbumTitle(context.getString(R.string.nowplaying_album))
                    .build()
            )
            .build()

        val exo = player ?: ExoPlayer.Builder(context)
            // Auto-pause on headphone unplug / Bluetooth disconnect.
            .setHandleAudioBecomingNoisy(true)
            .build()
            .apply {
                // Keep CPU awake during playback so the timer/auto-stop fires on schedule.
                setWakeMode(C.WAKE_MODE_LOCAL)
                addListener(playerListener)
            }
            .also { player = it }
        exo.repeatMode = Player.REPEAT_MODE_ONE
        exo.volume = 0f
        exo.setMediaItem(item)
        exo.prepare()
        exo.play()

        if (mediaSessionInternal == null) {
            mediaSessionInternal = MediaSession.Builder(context, exo).build()
        }
        _currentPreset.value = preset

        // 1.5 s fade-in iOS-parity
        fadeTo(target = 1f, durationMs = 1500)

        val dur = durationSeconds ?: preset.defaultDurationSeconds
        val endsAt = System.currentTimeMillis() + dur * 1000L
        _timerEndsAt.value = endsAt
        scheduleAutoStop(at = endsAt)

        _state.value = PlayerState.Playing(presetId, endsAt)
    }

    fun pause() {
        val current = _state.value as? PlayerState.Playing ?: return
        val presetId = current.presetId
        val remaining = current.endsAt?.let { it - System.currentTimeMillis() }?.takeIf { it > 0 }
        player?.pause()
        cancelTimer()
        _state.value = PlayerState.Paused(presetId, remaining)
    }

    fun resume() {
        val current = _state.value as? PlayerState.Paused ?: return
        val presetId = current.presetId

        player?.play()
        val endsAt: Long? = if (current.remainingMs != null && current.remainingMs > 0) {
            val e = System.currentTimeMillis() + current.remainingMs!!
            scheduleAutoStop(at = e)
            e
        } else {
            null
        }
        _timerEndsAt.value = endsAt
        _state.value = PlayerState.Playing(presetId, endsAt)
    }

    fun stop(reason: String = "user_stop", fadeOverMs: Long = 500) {
        // Guard: already fading-out → ignore nested calls.
        if (_state.value is PlayerState.FadingOut) return

        val currentPreset = _currentPreset.value
        val pid = currentPreset?.id

        if (player == null) {
            _timerEndsAt.value = null
            // Don't overwrite an Error state with Stopped — UI loses the surface.
            if (_state.value !is PlayerState.Error) _state.value = PlayerState.Stopped
            releaseSessionIfNeeded()
            return
        }

        if (pid != null && _state.value !is PlayerState.Error) {
            _state.value = PlayerState.FadingOut(pid)
        }
        cancelTimer()

        fadeTo(
            target = 0f,
            durationMs = fadeOverMs,
            onComplete = {
                player?.removeListener(playerListener)
                player?.stop()
                player?.release()
                player = null
                mediaSessionInternal?.release()
                mediaSessionInternal = null
                _timerEndsAt.value = null
                if (_state.value !is PlayerState.Error) _state.value = PlayerState.Stopped
                releaseSessionIfNeeded()
            }
        )
    }

    fun fadeOutAfterSleep(minutes: Int) {
        val durationMs = max(1, minutes) * 60_000L
        stop(reason = "fade_after_sleep", fadeOverMs = durationMs)
    }

    /** Called externally (e.g. CryDetectionService) when a cry is detected. */
    fun boostVolumeOnCry() {
        // CryDetectionService.sampleLoop runs on Dispatchers.IO (it owns a blocking
        // AudioRecord.read), but ExoPlayer requires all access from the thread it was
        // built on — main. Hop via scope (Main.immediate), so same-thread callers still
        // run synchronously and IO-thread callers get safely dispatched to main.
        scope.launch {
            when (_state.value) {
                is PlayerState.Playing -> {
                    player?.volume = 1f
                }
                is PlayerState.FadingOut -> {
                    // Cancel the fade-out, restore to full volume, transition back to Playing.
                    fadeJob?.cancel()
                    player?.volume = 1f
                    val preset = _currentPreset.value ?: return@launch
                    _state.value = PlayerState.Playing(preset.id, _timerEndsAt.value)
                }
                else -> {
                    val id = _currentPreset.value?.id ?: return@launch
                    play(id, null)
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private fun scheduleAutoStop(at: Long) {
        cancelTimer()
        timerJob = scope.launch {
            val delayMs = at - System.currentTimeMillis()
            if (delayMs > 0) delay(delayMs)
            if (!currentCoroutineContext().isActive) return@launch
            stop(reason = "timer", fadeOverMs = 500)
        }
    }

    private fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun releaseSessionIfNeeded() {
        if (!sessionRequested) return
        abandonAudioFocusIfNeeded()
        sessionRequested = false
    }

    private fun abandonAudioFocusIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            audioFocusRequest = null
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(focusListener)
        }
        audioFocusLossTransient = false
        preDuckVolume = null
    }

    /**
     * Smooth volume ramp via 20 Hz steps on the main thread. Serializes against any in-flight
     * fade by cancel-and-join — prevents the previous fade's onComplete from racing with a new
     * play()/stop() and operating on the wrong player instance.
     */
    private fun fadeTo(target: Float, durationMs: Long, onComplete: (() -> Unit)? = null) {
        val prev = fadeJob
        fadeJob = scope.launch {
            prev?.cancelAndJoin()
            val from = player?.volume ?: 1f
            val steps = max(1, (durationMs / 50).toInt())
            for (i in 1..steps) {
                delay(50)
                if (!isActive) return@launch
                val v = from + (target - from) * i / steps
                player?.volume = v
            }
            if (isActive) onComplete?.invoke()
        }
    }

    @Suppress("DEPRECATION")
    private fun requestAudioFocusIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attrs)
                .setOnAudioFocusChangeListener(focusListener, mainHandler)
                .build()
            audioFocusRequest = req
            audioManager.requestAudioFocus(req)
        } else {
            audioManager.requestAudioFocus(
                focusListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
    }
}
