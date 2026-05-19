package com.lizhi1026.sleepwhisper.core.cry

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.PowerManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.lizhi1026.sleepwhisper.core.cry.CryDetectionState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sqrt

@Singleton
class CryDetectionService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _state = kotlinx.coroutines.flow.MutableStateFlow<CryDetectionState>(CryDetectionState.Disabled)
    val stateLive: LiveData<CryDetectionState> = _state.asLiveData()

    private val _currentDb = kotlinx.coroutines.flow.MutableStateFlow(0)
    val currentDbLive: LiveData<Int> = _currentDb.asLiveData()

    var onCryDetected: (() -> Unit)? = null

    private var recorder: AudioRecord? = null
    private var sampleJob: Job? = null
    private var aboveThresholdSince: Long? = null
    private var sessionRequested = false

    private val thresholdDb: Int
        get() {
            val cur = _state.value
            return if (cur is CryDetectionState.Cooldown) 60 else LOOKBACK_THRESHOLD_DB
        }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var powerSaveReceiver: BroadcastReceiver? = null

    companion object {
        private const val SAMPLE_RATE_HZ = 16_000
        private const val LOOKBACK_THRESHOLD_DB = 60
        private const val REQUIRED_ABOVE_MS = 3_000
        private const val COOLDOWN_MS = 10_000L
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    fun start(thresholdDb: Int = 60) {
        // Guard: already running
        if (_state.value != CryDetectionState.Disabled) return

        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (pm.isPowerSaveMode) {
            // Low-power mode active — silently refuse to start (iOS parity).
            return
        }

        scope.launch {
            val granted = requestMicrophonePermission()
            if (!granted) {
                // Permission denied — stay disabled; do not repeatedly prompt.
                return@launch
            }

            setupRecorder()
            _state.value = CryDetectionState.Listening
            sampleJob = scope.launch { sampleLoop() }

            // Low-power broadcast receiver stays registered only while active.
            powerSaveReceiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    val en = pm.isPowerSaveMode
                    if (en) stop()
                }
            }.also { r ->
                context.registerReceiver(
                    r,
                    IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
                )
            }
        }
    }

    fun stop() {
        sampleJob?.cancel()
        sampleJob = null
        recorder?.stop()
        recorder?.release()
        recorder = null
        aboveThresholdSince = null

        powerSaveReceiver?.let {
            try { context.unregisterReceiver(it) } catch (_: Exception) {}
        }
        powerSaveReceiver = null

        _state.value = CryDetectionState.Disabled
    }

    // -------------------------------------------------------------------------
    // Internal
    // -------------------------------------------------------------------------

    private suspend fun requestMicrophonePermission(): Boolean =
        suspendCancellableCoroutine { cont ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                android.Manifest.permission.RECORD_AUDIO
            } else {
                cont.resume(true)
                return@suspendCancellableCoroutine
            }
            // Runtime permission check: if already granted, short-circuit.
            val pm = context.getSystemService(Context.POWER_SERVICE) as android.app.ActivityManager
            // Reuse Application context check via PackageManager
            @Suppress("DEPRECATION")
            val has = context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
            if (has) { cont.resume(true); return@suspendCancellableCoroutine }

            // Permission is not yet granted — request it from an activity context would be
            // required. Since this is started from a Service/Foreground flow, we record the
            // denied state and stop.
            cont.resume(false)
        }

    private fun setupRecorder() {
        val bufSize = max(
            AudioRecord.getMinBufferSize(SAMPLE_RATE_HZ, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT),
            SAMPLE_RATE_HZ * 2 / 10   // 100 ms
        )
        val rec = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE_HZ,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufSize
        )
        rec.startRecording()
        recorder = rec
    }

    private suspend fun sampleLoop() {
        val buf = ShortArray(SAMPLE_RATE_HZ / 2) // 0.5 s worth
        while (scope.isActive) {
            delay(500)
            val rec = recorder ?: continue
            val read = rec.read(buf, 0, buf.size).coerceAtLeast(0)
            if (read <= 0) continue

            val samples = buf.take(read)
            val rms = sqrt(samples.map { it.toDouble() * it }.average())
            val dbfs = 20 * log10(rms / 32767.0)
            val db = max(0, (120 + dbfs).toInt())
            _currentDb.value = db

            evaluate(db)
        }
    }

    private fun evaluate(db: Int) {
        val currentState = _state.value
        if (currentState !is CryDetectionState.Listening && currentState !is CryDetectionState.Cooldown) return

        if (currentState is CryDetectionState.Cooldown) {
            // Stay quiet during cooldown; nothing else to evaluate.
            return
        }

        if (db >= thresholdDb) {
            val now = System.currentTimeMillis()
            if (aboveThresholdSince == null) {
                aboveThresholdSince = now
            } else if (now - aboveThresholdSince!! >= REQUIRED_ABOVE_MS) {
                trigger()
            }
        } else {
            aboveThresholdSince = null
        }
    }

    private fun trigger() {
        _state.value = CryDetectionState.Triggered
        onCryDetected?.invoke()

        scope.launch {
            delay(COOLDOWN_MS)
            // Only re-arm if still in cooldown (not transitioned by user stop/start in meantime).
            val s = _state.value
            if (s is CryDetectionState.Cooldown) {
                _state.value = CryDetectionState.Listening
                aboveThresholdSince = null
            }
        }
        _state.value = CryDetectionState.Cooldown(
            untilMs = System.currentTimeMillis() + COOLDOWN_MS
        )
    }
}
