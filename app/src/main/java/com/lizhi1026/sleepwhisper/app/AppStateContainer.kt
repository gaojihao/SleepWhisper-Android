package com.lizhi1026.sleepwhisper.app

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import com.lizhi1026.sleepwhisper.R
import com.lizhi1026.sleepwhisper.core.audio.AudioPlayerService
import com.lizhi1026.sleepwhisper.core.cry.CryDetectionService
import com.lizhi1026.sleepwhisper.core.foreground.PlaybackForegroundService
import com.lizhi1026.sleepwhisper.core.notifications.NotificationScheduler
import com.lizhi1026.sleepwhisper.core.persistence.Repository
import com.lizhi1026.sleepwhisper.core.persistence.SettingsStore
import com.lizhi1026.sleepwhisper.core.recommendation.RecommendationEngine
import com.lizhi1026.sleepwhisper.core.strings.displayKey
import com.lizhi1026.sleepwhisper.core.theme.ThemeProvider
import com.lizhi1026.sleepwhisper.core.toast.ToastCenter
import com.lizhi1026.sleepwhisper.model.Baby
import com.lizhi1026.sleepwhisper.model.DiaperEvent
import com.lizhi1026.sleepwhisper.model.EventEditTarget
import com.lizhi1026.sleepwhisper.model.FeedingEvent
import com.lizhi1026.sleepwhisper.model.FeedingEvent.FeedingMethod
import com.lizhi1026.sleepwhisper.model.SleepRecommendation
import com.lizhi1026.sleepwhisper.model.SleepSession
import com.lizhi1026.sleepwhisper.model.SleepSession.SleepType
import com.lizhi1026.sleepwhisper.model.SleepSummary
import com.lizhi1026.sleepwhisper.model.UserSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import com.lizhi1026.sleepwhisper.core.visualkit.HeroBackdropController
import com.lizhi1026.sleepwhisper.core.visualkit.SWMotion

/**
 * 全局状态协调器，对应 iOS 端 `AppState`（位于 `SleepWhisperApp.swift`）。
 *
 * **职责**：
 * - 持有所有跨屏幕共享的 [LiveData]，是应用唯一的"状态真相来源"。
 * - ⚠️ 所有写操作（记录睡眠、喂养、换尿布、修改设置等）**必须经由本类的公开方法完成**，
 *   不允许 Screen/ViewModel 直接调用 [Repository] 或 [SettingsStore] 写入数据。
 * - 驱动 hero 变形动画进度（`morphProgress`）和氛围背景色（`heroBackdrop`）。
 *
 * **所在层**：app 模块 / 应用状态层（进程单例，非 ViewModel）。
 *
 * **与谁交互**：
 * - [Repository]（core/persistence）— 数据持久化读写；⚠️ 所有方法均为 `suspend`，不返回 Flow，
 *   响应式更新靠本类在写入后手动将最新值 `postValue` 到对应 LiveData。
 * - [SettingsStore]（core/persistence）— 设置项 DataStore，通过 Flow 收集并推送到 `_settings`。
 * - [ThemeProvider]（core/theme）— 根据 [UserSettings] 变化实时更新主题。
 * - [AudioPlayerService]、[CryDetectionService]（core）— 音频播放与哭声检测服务。
 * - [NotificationScheduler]（core/notifications）— 睡眠签到提醒调度。
 * - [ToastCenter]（core/toast）— 全局轻提示（支持撤销、编辑快捷操作）。
 *
 * **关键约定**：
 * - 本类**不是** ViewModel，不继承 `ViewModel`，是通过 Hilt 注入的进程单例。
 * - 内部协程作用域使用 `SupervisorJob + Dispatchers.Main.immediate`，子协程失败不影响其他协程。
 * - IO 密集型操作（Room 写入、AlarmManager 调度）在 `Dispatchers.IO` 上执行。
 */
@Singleton
class AppStateContainer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repo: Repository,
    private val settingsStore: SettingsStore,
    val themeProvider: ThemeProvider,
    val audioPlayer: AudioPlayerService,
    val cryDetection: CryDetectionService,
    private val notif: NotificationScheduler,
    val toast: ToastCenter
) {
    // ─── 跨屏幕 LiveData ──────────────────────────────────────────────────────

    private val _baby = MutableLiveData<Baby?>(null)
    val baby: LiveData<Baby?> get() = _baby

    private val _settings = MutableLiveData(UserSettings.DEFAULT)
    val settings: LiveData<UserSettings> get() = _settings

    private val _ongoingSleep = MutableLiveData<SleepSession?>(null)
    val ongoingSleep: LiveData<SleepSession?> get() = _ongoingSleep

    private val _currentRecommendation = MutableLiveData<SleepRecommendation?>(null)
    val currentRecommendation: LiveData<SleepRecommendation?> get() = _currentRecommendation

    private val _lastSleepSummary = MutableLiveData<SleepSummary?>(null)
    val lastSleepSummary: LiveData<SleepSummary?> get() = _lastSleepSummary

    private val _cachedWakeWindow = MutableLiveData<Pair<Int, Int>?>(null)
    val cachedWakeWindow: LiveData<Pair<Int, Int>?> get() = _cachedWakeWindow

    private val _pendingWelcome = MutableLiveData(false)
    val pendingWelcome: LiveData<Boolean> get() = _pendingWelcome

    private val _hasSeenOnboardingHints = MutableLiveData(false)
    val hasSeenOnboardingHints: LiveData<Boolean> get() = _hasSeenOnboardingHints

    private val _pendingEditTarget = MutableLiveData<EventEditTarget?>(null)
    val pendingEditTarget: LiveData<EventEditTarget?> get() = _pendingEditTarget

    /** 派生的根导航目标键值："onboarding" | "welcome" | "sleeping" | "main"。
     *
     * ⚠️ 这是导航的唯一入口；改变导航目标必须通过修改 [_baby]、[_pendingWelcome] 或
     * [_ongoingSleep] 来间接驱动，不要在外部直接操作此 LiveData。
     */
    val rootKey: LiveData<String> = MediatorLiveData<String>().apply {
        val update = {
            value = when {
                _baby.value == null -> "onboarding"
                _pendingWelcome.value == true -> "welcome"
                _ongoingSleep.value != null -> "sleeping"
                else -> "main"
            }
        }
        addSource(_baby) { update() }
        addSource(_pendingWelcome) { update() }
        addSource(_ongoingSleep) { update() }
        update()
    }

    // ─── 内部私有字段 ─────────────────────────────────────────────────────────

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val hintPrefs: SharedPreferences =
        context.getSharedPreferences("sw.hint", Context.MODE_PRIVATE)
    private var lastWakeWindowComputedAtMs: Long = 0
    private val wakeWindowThrottleMs: Long = 30_000

    // ─── Hero 变形动画状态（阶段 1）────────────────────────────────────────────

    /**
     * Sleep CTA → Sleeping 屏幕的 hero 变形进度，取值范围 0..1。
     *
     * 存放在 [AppStateContainer]（进程单例）中，使其在配置变更（如屏幕旋转）后仍能存活，
     * 同时允许 Home 和 Sleeping 两个屏幕共同读取，以便各自驱动二级动画
     * （内容错开淡入淡出、星星密度渐变等）。
     */
    val morphProgress: Animatable<Float, AnimationVector1D> = Animatable(0f, Float.VectorConverter)

    /**
     * 应用"环境色"的单一真相来源，用于驱动 AuroraBackdrop 的渐变色调。
     *
     * 阶段 1：在 Home 屏幕根据一天中的时刻提供傍晚暗示色；
     * 阶段 3（规划中）：将由播放器根据当前音频预设的 aura 色更新此值。
     */
    val heroBackdrop: HeroBackdropController = HeroBackdropController()

    /**
     * 驱动正向变形动画（Home → Sleeping），挂起约 [SWMotion.heroMorphMs] 毫秒后返回。
     *
     * **副作用**：将 [morphProgress] 动画到 1.0f。
     * **线程要求**：必须在协程中调用（suspend）。
     */
    suspend fun beginSleepMorph() {
        morphProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(SWMotion.heroMorphMs, easing = LinearEasing)
        )
    }

    /**
     * 驱动反向变形动画（Sleeping → Home），挂起约 [SWMotion.heroMorphMs] 毫秒后返回。
     *
     * **副作用**：将 [morphProgress] 动画到 0.0f。
     * **线程要求**：必须在协程中调用（suspend）。
     */
    suspend fun endSleepMorph() {
        morphProgress.animateTo(
            targetValue = 0f,
            animationSpec = tween(SWMotion.heroMorphMs, easing = LinearEasing)
        )
    }

    /**
     * 将 [morphProgress] 瞬间对齐到当前 [rootKey] 对应的目标值。
     *
     * 应在 `Lifecycle.Event.ON_RESUME` 时调用（见 [RootRoute] 中的 DisposableEffect），
     * 避免应用在变形动画进行到一半时被切入后台，回到前台后重播残余动画帧。
     */
    fun snapMorphToCurrent() {
        val target = if (rootKey.value == "sleeping") 1f else 0f
        // 在内部协程作用域中瞬间跳到目标值（非动画，不挂起主线程）。
        scope.launch { morphProgress.snapTo(target) }
    }

    init {
        _hasSeenOnboardingHints.value = hintPrefs.getBoolean(KEY_HINT_SEEN, false)
        // 从 SettingsStore Flow 持续收集设置变化，推送到 LiveData 并同步更新主题。
        scope.launch {
            settingsStore.settings.collectLatest { s ->
                _settings.postValue(s)
                themeProvider.update(s)
            }
        }
        scope.launch { bootstrap() }
        // 将哭声检测回调绑定到音频播放器的音量提升逻辑。
        cryDetection.onCryDetected = { audioPlayer.boostVolumeOnCry() }
    }

    /**
     * 在应用启动时加载持久化数据，完成状态初始化。
     *
     * 加载宝宝信息 → 若存在则加载进行中的睡眠会话和最新建议 → 刷新唤醒窗口缓存。
     * 此方法在 `init` 块中以协程方式调用，执行完毕后各 LiveData 才有初始值。
     */
    private suspend fun bootstrap() {
        val b = repo.loadBaby()
        _baby.postValue(b)
        if (b != null) {
            _ongoingSleep.postValue(repo.ongoingSleep(b.id))
            _currentRecommendation.postValue(repo.latestRecommendation(b.id))
            refreshWakeWindow(force = true)
        }
    }

    // ─── 公开 API（与 iOS AppState 方法一一对应）──────────────────────────────

    /**
     * 保存宝宝信息。若为首次保存（之前没有宝宝），则触发欢迎仪式流程并标记引导提示已读。
     *
     * **副作用**：写入 [Repository]；更新 [_baby]；首次保存时设置 [_pendingWelcome] = true。
     * **线程要求**：suspend，内部在 IO 线程执行数据库写入。
     *
     * @param b 要保存的 [Baby] 实体。
     */
    suspend fun saveBaby(b: Baby) {
        repo.saveBaby(b)
        val firstEver = _baby.value == null
        _baby.postValue(b)
        if (firstEver) {
            // 首次创建宝宝，触发欢迎仪式屏并自动标记引导提示已读。
            _pendingWelcome.postValue(true)
            markHintsSeen()
        }
    }

    fun dismissWelcome() { _pendingWelcome.value = false }

    /**
     * 保存用户设置。实际写入 [SettingsStore]，[_settings] 通过 Flow 收集器自动更新，无需手动 postValue。
     *
     * @param s 要保存的 [UserSettings] 实例。
     */
    suspend fun saveSettings(s: UserSettings) {
        settingsStore.update(s)
        // _settings 将由 Flow 收集器自动更新，此处无需手动 postValue。
    }

    fun markHintsSeen() {
        hintPrefs.edit { putBoolean(KEY_HINT_SEEN, true) }
        _hasSeenOnboardingHints.value = true
    }

    /**
     * 根据当前本地时刻判断应使用的默认睡眠类型。
     *
     * 内部通过 [UserSettings.isInNightTime] 判断当前小时是否在夜间窗口内（支持跨午夜区间）。
     *
     * @param at 判断时刻的时间戳（毫秒），默认为当前系统时间。
     * @return 若当前时刻在夜间窗口内则返回 [SleepType.NIGHT]，否则返回 [SleepType.NAP]。
     */
    fun defaultSleepType(at: Long = System.currentTimeMillis()): SleepType {
        val hour = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(at), ZoneId.systemDefault()).hour
        val s = _settings.value ?: UserSettings.DEFAULT
        return if (s.isInNightTime(hour)) SleepType.NIGHT else SleepType.NAP
    }

    suspend fun startSleep(type: SleepType? = null) {
        val b = _baby.value ?: return
        val resolvedType = type ?: defaultSleepType()
        val session = SleepSession(
            id = UUID.randomUUID().toString(),
            babyId = b.id,
            type = resolvedType,
            startAt = System.currentTimeMillis()
        )
        // Room 写入和 AlarmManager 调度都是阻塞系统调用，切换到 IO 线程执行，
        // 避免在 Home→Sleeping 变形动画和前台服务启动同时进行时触发 FGS ANR 看门狗。
        withContext(Dispatchers.IO) {
            repo.appendSleep(session)
            notif.scheduleSleepCheckIn(b.id, afterHours = 2.0)
        }
        _ongoingSleep.postValue(session)
        // 启动前台服务并按需开启哭声检测（哭声检测受设置项及麦克风权限双重门控）。
        val s = _settings.value ?: UserSettings.DEFAULT
        if (s.cryDetectionEnabled) {
            if (CryDetectionService.hasMicrophonePermission(context)) {
                PlaybackForegroundService.startCry(context, s.cryDetectionThresholdDb)
            } else {
                toast.show(R.string.toast_mic_perm_needed, style = ToastCenter.Style.WARNING)
            }
        }
    }

    suspend fun endSleep() {
        val b = _baby.value ?: return
        val ongoing = _ongoingSleep.value ?: return
        val now = System.currentTimeMillis()
        val finished = ongoing.copy(endAt = now)
        repo.updateSleep(finished)
        _ongoingSleep.postValue(null)
        _lastSleepSummary.postValue(buildSleepSummary(b.id, finished))
        notif.cancel(NotificationScheduler.KIND_CHECKIN, b.id)
        PlaybackForegroundService.stopCry(context)
        recomputeRecommendation()
        refreshWakeWindow(force = true)
    }

    fun clearSleepSummary() { _lastSleepSummary.value = null }

    suspend fun recordFeeding(method: FeedingMethod, amountMl: Int? = null) {
        val b = _baby.value ?: return
        val event = FeedingEvent(
            id = UUID.randomUUID().toString(),
            babyId = b.id,
            method = method,
            amountMl = amountMl,
            startedAt = System.currentTimeMillis()
        )
        repo.appendFeeding(event)
        val methodLabel = context.getString(method.displayKey())
        val (messageRes, args) = if (method == FeedingMethod.BOTTLE && amountMl != null) {
            R.string.toast_loggedml to listOf<Any>(methodLabel, amountMl)
        } else {
            R.string.toast_logged to listOf<Any>(methodLabel)
        }
        toast.show(
            messageRes = messageRes,
            args = args,
            style = ToastCenter.Style.SUCCESS,
            undo = { scope.launch { repo.deleteFeeding(event.id) } },
            editAction = { _pendingEditTarget.value = EventEditTarget.Feeding(event) }
        )
    }

    suspend fun updateFeeding(event: FeedingEvent) {
        repo.updateFeeding(event.copy(isEdited = true))
    }

    suspend fun recordDiaper(type: DiaperEvent.DiaperType) {
        val b = _baby.value ?: return
        val event = DiaperEvent(
            id = UUID.randomUUID().toString(),
            babyId = b.id,
            type = type,
            occurredAt = System.currentTimeMillis()
        )
        repo.appendDiaper(event)
        toast.show(
            messageRes = R.string.toast_logged,
            args = listOf(context.getString(type.displayKey())),
            style = ToastCenter.Style.SUCCESS,
            undo = { scope.launch { repo.deleteDiaper(event.id) } },
            editAction = { _pendingEditTarget.value = EventEditTarget.Diaper(event) }
        )
    }

    suspend fun updateDiaper(event: DiaperEvent) {
        repo.updateDiaper(event)
    }

    fun clearPendingEdit() { _pendingEditTarget.value = null }

    fun requestEdit(target: EventEditTarget) { _pendingEditTarget.value = target }

    suspend fun recomputeRecommendation() {
        val b = _baby.value ?: return
        val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 3600_000L
        val sleeps = repo.sleepSessions(b.id, sinceMs = sevenDaysAgo)
        val rec = RecommendationEngine.compute(b, sleeps)
        repo.appendRecommendation(rec)
        _currentRecommendation.postValue(rec)
    }

    suspend fun refreshWakeWindow(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && now - lastWakeWindowComputedAtMs < wakeWindowThrottleMs) return
        lastWakeWindowComputedAtMs = now
        val b = _baby.value ?: return
        val sevenDaysAgo = now - 7 * 24 * 3600_000L
        val sleeps = repo.sleepSessions(b.id, sinceMs = sevenDaysAgo)
        _cachedWakeWindow.postValue(RecommendationEngine.currentRemainingWindow(b, sleeps, now))
    }

    // ─── 辅助私有方法 ─────────────────────────────────────────────────────────

    /**
     * 构建本次睡眠结束后的摘要数据，用于展示给用户的"醒来反馈卡"。
     *
     * 计算逻辑：
     * 1. 获取本次睡眠时长（秒）。
     * 2. 查询今日（自午夜起）所有睡眠会话，合计今日睡眠总时长。
     * 3. 查询昨日睡眠数据，计算与今日的差值（分钟）。
     *
     * @param babyId   宝宝 ID，用于查询数据库。
     * @param finished 已结束（已设置 `endAt`）的睡眠会话。
     * @return 包含本次时长、今日合计、昨日对比差值的 [SleepSummary]。
     */
    private suspend fun buildSleepSummary(babyId: String, finished: SleepSession): SleepSummary {
        val justFinishedSec = finished.durationSeconds()
        val todayStart = ZonedDateTime.now().with(LocalTime.MIDNIGHT).toInstant().toEpochMilli()
        val yesterdayStart = todayStart - 24 * 3600_000L

        val todaySleeps = repo.sleepSessions(babyId, sinceMs = todayStart)
        val todayTotal = todaySleeps.sumOf { it.durationSeconds() }

        val ydaySleeps = repo.sleepSessions(babyId, sinceMs = yesterdayStart)
            .filter { it.startAt < todayStart }
        val ydayTotal = ydaySleeps.sumOf { it.durationSeconds() }
        val hasYesterday = ydaySleeps.isNotEmpty()
        val diffMin = if (hasYesterday) ((todayTotal - ydayTotal) / 60).toInt() else 0

        return SleepSummary(
            justFinishedDurationSec = justFinishedSec,
            todayTotalSec = todayTotal,
            diffVsYesterdayMin = diffMin,
            hasYesterdayData = hasYesterday
        )
    }

    companion object {
        private const val KEY_HINT_SEEN = "hint_seen_v1"
    }
}
