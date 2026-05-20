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
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
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
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
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
    private var mediaSession: MediaSession? = null
    private var fadeJob: Job? = null
    private var timerJob: Job? = null
    private var savedVolumeOnFadeStart: Float = 1f
    private var aboveThresholdSince: Long? = null
    private var audioFocusLossTransient = false
    private var sessionRequested = false

    private val audioManager: AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val focusListener = OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                val current = _state.value
                if (current is PlayerState.Playing) {
                    _state.value = PlayerState.Interrupted(restoreTo = current.presetId)
                    player?.pause()
                    audioFocusLossTransient = true
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (audioFocusLossTransient) {
                    audioFocusLossTransient = false
                    val s = _state.value
                    if (s is PlayerState.Interrupted && s.restoreTo != null) {
                        play(s.restoreTo, null)
                    }
                }
            }
            else -> { /* other focus changes — no-op */ }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        // Audio focus is requested per-playback via AudioFocusRequest (API 26+); listener is bound
        // to the request, not pre-attached at construction time.
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

        val exo = (player ?: ExoPlayer.Builder(context).build().also { player = it }).apply {
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f
            setMediaItem(item)
            prepare()
            play()
        }

        if (mediaSession == null) {
            mediaSession = MediaSession.Builder(context, exo).build()
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
            _state.value = PlayerState.Stopped
            releaseSessionIfNeeded()
            return
        }

        if (pid != null) _state.value = PlayerState.FadingOut(pid)
        cancelTimer()

        fadeTo(
            target = 0f,
            durationMs = fadeOverMs,
            onComplete = {
                player?.stop()
                player?.release()
                player = null
                mediaSession?.release()
                mediaSession = null
                _timerEndsAt.value = null
                _state.value = PlayerState.Stopped
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
        when (val s = _state.value) {
            is PlayerState.Playing -> {
                player?.volume = 1f
            }
            else -> {
                val id = _currentPreset.value?.id ?: return
                play(id, null)
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
        // Placeholder for AudioSessionCoordinator parity when that abstraction is added.
        if (!sessionRequested) return
        sessionRequested = false
    }

    /**
     * Smooth volume ramp via 20 Hz steps on the main thread.
     *
     * @param target      final volume in [0, 1]
     * @param durationMs  total ramp duration
     * @param onComplete  run on the main thread after the last step
     */
    private fun fadeTo(target: Float, durationMs: Long, onComplete: (() -> Unit)? = null) {
        fadeJob?.cancel()
        savedVolumeOnFadeStart = player?.volume ?: 1f

        fadeJob = scope.launch {
            val steps = max(1, (durationMs / 50).toInt())
            val from = savedVolumeOnFadeStart
            for (i in 1..steps) {
                delay(50)
                val v = from + (target - from) * i / steps
                player?.volume = v
                if (!currentCoroutineContext().isActive) return@launch
            }
            onComplete?.invoke()
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
