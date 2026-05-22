package com.lizhi1026.sleepwhisper.core.cry

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.lizhi1026.sleepwhisper.core.cry.CryDetectionState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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

        /** Public so callers can pre-flight before invoking [start] and surface a UI prompt. */
        fun hasMicrophonePermission(context: Context): Boolean =
            ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    fun start(thresholdDb: Int = 60) {
        // Guard: already running
        if (_state.value != CryDetectionState.Disabled) return

        // Inline check so Android Lint can flow-analyze the permission guard ahead of the
        // AudioRecord constructor inside setupRecorder().
        val micGranted = ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (!micGranted) {
            // No permission — caller is expected to have surfaced a toast / settings prompt
            // before invoking start(). We do not attempt a runtime request from a Service
            // context. Stay Disabled so subsequent calls can re-arm after the user grants.
            return
        }

        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (pm.isPowerSaveMode) {
            // Low-power mode active — silently refuse to start (iOS parity).
            return
        }

        // AudioRecord construction + startRecording() block until the audio HAL hands back
        // a buffer (observed > 1 s on Pixel hardware), which would stall the foreground
        // service's onStartCommand on the main thread and trip the FGS ANR watchdog.
        // Push setup + the blocking read loop onto IO; flip _state from there.
        sampleJob = scope.launch(Dispatchers.IO) {
            try {
                setupRecorder()
            } catch (t: Throwable) {
                recorder?.runCatching { release() }
                recorder = null
                return@launch
            }
            _state.value = CryDetectionState.Listening
            sampleLoop()
        }

        powerSaveReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                if (pm.isPowerSaveMode) stop()
            }
        }.also { r ->
            val filter = IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(r, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(r, filter)
            }
        }
    }

    fun stop() {
        sampleJob?.cancel()
        sampleJob = null
        recorder?.runCatching { stop() }
        recorder?.runCatching { release() }
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

    @androidx.annotation.RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
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
        recorder = rec
        rec.startRecording()
    }

    private suspend fun sampleLoop() {
        val buf = ShortArray(SAMPLE_RATE_HZ / 2) // 0.5 s worth
        while (scope.isActive) {
            val rec = recorder ?: break
            // AudioRecord.read is blocking — by reading first we never drop the leading 500 ms
            // of audio. The loop's natural pacing is the read duration (~500 ms at 16 kHz mono).
            val read = rec.read(buf, 0, buf.size).coerceAtLeast(0)
            if (read <= 0) {
                // Read failed or zero samples — back off briefly to avoid a busy loop.
                delay(100)
                continue
            }

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
        _state.value = CryDetectionState.Cooldown(
            untilMs = System.currentTimeMillis() + COOLDOWN_MS
        )
        onCryDetected?.invoke()

        scope.launch {
            delay(COOLDOWN_MS)
            val s = _state.value
            if (s is CryDetectionState.Cooldown) {
                _state.value = CryDetectionState.Listening
                aboveThresholdSince = null
            }
        }
    }
}
