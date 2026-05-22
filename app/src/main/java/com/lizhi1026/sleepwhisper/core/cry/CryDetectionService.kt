/**
 * 哭声检测引擎（Kotlin 单例，非 Android Service）。
 *
 * 所在层：core/cry — 应用核心层，不依赖 UI。
 * ⚠️ 注意：本类是 Kotlin 单例（@Singleton + Hilt 注入），不是 Android Service。
 *
 * 交互对象：
 *   - 调用方（通常为 ViewModel 或前台 Service）：通过 [start]/[stop] 控制生命周期。
 *   - [com.lizhi1026.sleepwhisper.core.audio.AudioPlayerService]：
 *     检测到哭声后通过 [onCryDetected] 回调触发 [AudioPlayerService.boostVolumeOnCry]。
 *   - ViewModel / Composable：通过 [stateLive]、[currentDbLive] 观察实时状态与音量。
 *
 * 核心算法：
 *   1. AudioRecord 以 16 kHz / Mono / PCM_16BIT 录音，每帧缓冲 0.5 s（8000 个 short）。
 *   2. 计算帧 RMS → dBFS：`db = max(0, (120 + 20 * log10(rms / 32767)).toInt())`。
 *   3. 连续 ≥3 s 超过阈值（默认 60 dB）则触发哭声事件。
 *   4. 触发后进入 10 s 冷却期，冷却结束后自动恢复 Listening。
 *
 * 省电模式处理：
 *   - [start] 时若系统处于省电模式则拒绝启动。
 *   - 运行中若收到 ACTION_POWER_SAVE_MODE_CHANGED 广播且省电模式开启，自动调用 [stop]。
 */
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
    /** 当前检测状态，对外暴露为 LiveData 供 UI 观察。 */
    private val _state = kotlinx.coroutines.flow.MutableStateFlow<CryDetectionState>(CryDetectionState.Disabled)
    val stateLive: LiveData<CryDetectionState> = _state.asLiveData()

    /** 当前帧的实时音量（dBFS 映射后的正整数，0~120+），供 UI 显示声纹波形。 */
    private val _currentDb = kotlinx.coroutines.flow.MutableStateFlow(0)
    val currentDbLive: LiveData<Int> = _currentDb.asLiveData()

    /**
     * 哭声触发回调，由外部注入（通常由前台 Service 在绑定时设置）。
     * 触发时在 IO 线程调用，实现方应自行切换线程（如 AudioPlayerService 通过 scope(Main) 保证）。
     */
    var onCryDetected: (() -> Unit)? = null

    // AudioRecord 实例，仅在 sampleJob 运行期间非 null
    private var recorder: AudioRecord? = null
    // 录音采样协程 Job，cancel() 即停止录音循环
    private var sampleJob: Job? = null
    // 记录音量首次超过阈值的时间戳，用于判断是否持续 ≥3s
    private var aboveThresholdSince: Long? = null

    /**
     * 动态阈值：冷却期间返回固定 60，其余情况返回常量 [LOOKBACK_THRESHOLD_DB]。
     * 冷却期提高阈值（实际相同值）是为防止冷却刚结束时的余波再次触发。
     */
    private val thresholdDb: Int
        get() {
            val cur = _state.value
            return if (cur is CryDetectionState.Cooldown) 60 else LOOKBACK_THRESHOLD_DB
        }

    // 主线程协程作用域，用于状态写入与冷却计时（ExoPlayer 和 LiveData 要求主线程）
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // 监听系统省电模式变化的广播接收器，运行期间注册，stop() 时注销
    private var powerSaveReceiver: BroadcastReceiver? = null

    companion object {
        /** AudioRecord 采样率：16 kHz，与婴儿哭声主频段（300 Hz~3 kHz）匹配，降低 CPU 开销。 */
        private const val SAMPLE_RATE_HZ = 16_000
        /** 触发哭声判定的 dBFS 阈值（映射后正整数，约等于中等哭声音量）。 */
        private const val LOOKBACK_THRESHOLD_DB = 60
        /** 连续超过阈值多少毫秒才视为有效哭声（防误触）。 */
        private const val REQUIRED_ABOVE_MS = 3_000
        /** 触发后的冷却时长（毫秒），冷却期间不重复触发。 */
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

    /**
     * 启动哭声检测。
     *
     * 启动前会依次检查：
     * 1. 是否已在运行（非 Disabled 则直接返回，避免重复启动）。
     * 2. 麦克风录音权限（未授权则静默返回，UI 层应提前弹出权限提示）。
     * 3. 系统省电模式（省电时拒绝启动，与 iOS 端行为保持一致）。
     *
     * AudioRecord 初始化及阻塞读循环均在 IO 线程运行，避免阻塞主线程触发 ANR。
     * 同时注册 [PowerManager.ACTION_POWER_SAVE_MODE_CHANGED] 广播，省电模式开启时自动停止。
     *
     * @param thresholdDb 音量阈值（dBFS 映射后正整数），超过此值持续 3s 触发哭声事件，默认 60。
     */
    fun start(thresholdDb: Int = 60) {
        // 守卫：已在运行则直接忽略，防止重复启动
        if (_state.value != CryDetectionState.Disabled) return

        // Inline check so Android Lint can flow-analyze the permission guard ahead of the
        // AudioRecord constructor inside setupRecorder().
        val micGranted = ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (!micGranted) {
            // 无麦克风权限 — 调用方应在调用 start() 前完成权限申请流程，
            // 此处不从 Service 上下文发起运行时权限请求。
            // 保持 Disabled 状态，权限授予后调用方可再次调用 start() 重新激活。
            return
        }

        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (pm.isPowerSaveMode) {
            // 省电模式开启 — 拒绝启动以节省电量（iOS 端同等行为）
            return
        }

        // AudioRecord 初始化 + startRecording() 在某些硬件上会阻塞 >1s（等待音频 HAL 分配缓冲），
        // 若在主线程执行会导致前台 Service 的 onStartCommand 超时触发 FGS ANR 看门狗。
        // 因此将 setupRecorder() 和阻塞读循环全部放到 IO 线程执行；_state 可在 IO 线程安全写入。
        sampleJob = scope.launch(Dispatchers.IO) {
            try {
                setupRecorder()
            } catch (t: Throwable) {
                // AudioRecord 初始化失败（如设备不支持或权限被撤销），清理资源并退出
                recorder?.runCatching { release() }
                recorder = null
                return@launch
            }
            // 初始化成功，切换状态为 Listening，进入采样循环
            _state.value = CryDetectionState.Listening
            sampleLoop()
        }

        // 注册省电模式广播：运行期间若系统切换到省电模式，自动停止检测
        powerSaveReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                // 收到广播后再次确认省电模式已开启（广播可能在模式变化后延迟到达）
                if (pm.isPowerSaveMode) stop()
            }
        }.also { r ->
            val filter = IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
            // Android 13+ 要求显式声明 RECEIVER_NOT_EXPORTED（非系统广播接收器）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(r, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(r, filter)
            }
        }
    }

    /**
     * 停止哭声检测，释放 AudioRecord 资源并注销广播接收器。
     * 幂等操作，可安全多次调用。
     */
    fun stop() {
        // 取消采样协程，阻塞中的 AudioRecord.read() 会在协程取消时自然退出
        sampleJob?.cancel()
        sampleJob = null
        // 先 stop() 再 release()，符合 AudioRecord 生命周期规范
        recorder?.runCatching { stop() }
        recorder?.runCatching { release() }
        recorder = null
        // 重置连续超阈值计时器
        aboveThresholdSince = null

        // 注销省电模式广播接收器，防止内存泄漏
        powerSaveReceiver?.let {
            try { context.unregisterReceiver(it) } catch (_: Exception) {}
        }
        powerSaveReceiver = null

        _state.value = CryDetectionState.Disabled
    }

    // -------------------------------------------------------------------------
    // Internal
    // -------------------------------------------------------------------------

    /**
     * 初始化 AudioRecord：16 kHz / Mono / PCM_16BIT，缓冲取系统最小值与 100 ms 中的较大值。
     * 需要 RECORD_AUDIO 权限（由调用方 [start] 保证已在构造 AudioRecord 前检查）。
     */
    @androidx.annotation.RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    private fun setupRecorder() {
        // 取系统最小缓冲与 100 ms 等效字节数的较大值，避免硬件缓冲不足导致初始化失败
        val bufSize = max(
            AudioRecord.getMinBufferSize(SAMPLE_RATE_HZ, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT),
            SAMPLE_RATE_HZ * 2 / 10   // 100 ms
        )
        val rec = AudioRecord(
            MediaRecorder.AudioSource.MIC,  // 使用主麦克风（无降噪/波束成形处理的原始信号）
            SAMPLE_RATE_HZ,                 // 16 kHz 采样率
            AudioFormat.CHANNEL_IN_MONO,    // 单声道，降低数据量
            AudioFormat.ENCODING_PCM_16BIT, // 16-bit 有符号整数，与 RMS 公式中 /32767 对应
            bufSize
        )
        recorder = rec
        rec.startRecording()
    }

    /**
     * 阻塞式采样循环，运行在 IO 线程（由 [start] 中的 `launch(Dispatchers.IO)` 保证）。
     *
     * 每次 AudioRecord.read 读取约 0.5 s 的 PCM 数据（8000 short @ 16 kHz），
     * 计算帧 RMS → dBFS → 整数 db，再交由 [evaluate] 判断是否触发哭声。
     *
     * dBFS 公式：`db = max(0, (120 + 20 * log10(rms / 32767)).toInt())`
     *   其中 120 为偏移量，将 [-120, 0] dBFS 映射到 [0, 120] 正整数区间。
     */
    private suspend fun sampleLoop() {
        // 每帧缓冲 0.5 s 的 short 样本（16 kHz × 0.5 s = 8000 个 short）
        val buf = ShortArray(SAMPLE_RATE_HZ / 2) // 0.5 s worth
        while (scope.isActive) {
            val rec = recorder ?: break
            // AudioRecord.read 为阻塞调用，天然提供 ~0.5 s 的帧步进节奏，无需额外 delay
            // 先读取再处理，确保不丢弃录音开始的前 500 ms 音频数据
            val read = rec.read(buf, 0, buf.size).coerceAtLeast(0)
            if (read <= 0) {
                // 读取失败或返回 0（硬件异常）：短暂退避，避免空转 busy-loop 烧 CPU
                delay(100)
                continue
            }

            // 计算当前帧的均方根（RMS）音量
            val samples = buf.take(read)
            val rms = sqrt(samples.map { it.toDouble() * it }.average())
            // 转换为 dBFS 并映射到正整数区间 [0, ~120]
            val dbfs = 20 * log10(rms / 32767.0)
            val db = max(0, (120 + dbfs).toInt())
            // 实时更新 UI 可观察的当前音量
            _currentDb.value = db

            // 将当前帧音量送入状态机判断是否需要触发哭声事件
            evaluate(db)
        }
    }

    /**
     * 单帧音量评估：判断是否已连续超过阈值 [REQUIRED_ABOVE_MS] 毫秒。
     *
     * - 仅在 [CryDetectionState.Listening] 状态下执行评估（冷却期直接跳过）。
     * - 超过阈值时记录首次超阈时间戳；若累计时长 ≥3s 则调用 [trigger]。
     * - 低于阈值时重置计时器（非连续则重新计时）。
     *
     * @param db 当前帧的 dBFS 映射正整数音量。
     */
    private fun evaluate(db: Int) {
        val currentState = _state.value
        // 只处理 Listening 和 Cooldown 状态，其他状态（Disabled/Triggered）直接忽略
        if (currentState !is CryDetectionState.Listening && currentState !is CryDetectionState.Cooldown) return

        if (currentState is CryDetectionState.Cooldown) {
            // 冷却期内静默：不评估、不触发，等待冷却结束后 trigger() 中的协程切回 Listening
            return
        }

        if (db >= thresholdDb) {
            val now = System.currentTimeMillis()
            if (aboveThresholdSince == null) {
                // 首次超过阈值：记录起始时间戳
                aboveThresholdSince = now
            } else if (now - aboveThresholdSince!! >= REQUIRED_ABOVE_MS) {
                // 已连续超阈值 ≥3s：触发哭声事件
                trigger()
            }
        } else {
            // 低于阈值：重置连续计时（非连续超阈值不触发）
            aboveThresholdSince = null
        }
    }

    /**
     * 触发哭声事件：
     * 1. 切换状态为 [CryDetectionState.Cooldown]（10 s 冷却期）。
     * 2. 调用 [onCryDetected] 回调，由外部（通常是 AudioPlayerService）提升音量。
     * 3. 启动协程在 10 s 后自动恢复为 [CryDetectionState.Listening]。
     */
    private fun trigger() {
        // 进入冷却态，记录冷却结束时间戳供 UI 展示倒计时
        _state.value = CryDetectionState.Cooldown(
            untilMs = System.currentTimeMillis() + COOLDOWN_MS
        )
        // 通知外部（AudioPlayerService.boostVolumeOnCry()），提升播放音量
        onCryDetected?.invoke()

        // 冷却计时协程：10s 后切回 Listening，同时重置超阈计时器防止立即再次触发
        scope.launch {
            delay(COOLDOWN_MS)
            val s = _state.value
            // 二次确认仍处于冷却态（防止 stop() 在冷却期间被调用导致状态错乱）
            if (s is CryDetectionState.Cooldown) {
                _state.value = CryDetectionState.Listening
                aboveThresholdSince = null
            }
        }
    }
}
