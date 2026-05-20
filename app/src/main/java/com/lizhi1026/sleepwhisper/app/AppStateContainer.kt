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
 * Central state coordinator — port of iOS App/SleepWhisperApp.swift `AppState`.
 *
 * Holds every cross-screen LiveData. Screens read via `app.xxx`, write via methods that
 * touch repository / settings / services in a single place.
 *
 * **Not** a ViewModel itself; it's a process singleton that ViewModels inject and pass-through.
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
    // ─── Cross-screen LiveData ───────────────────────────────────────────────

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

    /** Derived root destination key: "onboarding" | "welcome" | "sleeping" | "main". */
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

    // ─── Internal ────────────────────────────────────────────────────────────

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val hintPrefs: SharedPreferences =
        context.getSharedPreferences("sw.hint", Context.MODE_PRIVATE)
    private var lastWakeWindowComputedAtMs: Long = 0
    private val wakeWindowThrottleMs: Long = 30_000

    // ─── Hero morph state (Phase 1) ──────────────────────────────────────────

    /**
     * Sleep CTA → Sleeping screen morph progress, 0..1.
     * Held in AppStateContainer (singleton) so it survives configuration changes
     * and so both Home and Sleeping screens can read it to stage their own
     * secondary animations (content stagger fade, star density ramp, etc).
     */
    val morphProgress: Animatable<Float, AnimationVector1D> = Animatable(0f, Float.VectorConverter)

    /**
     * Single source of truth for the app's "ambient color" — drives the
     * AuroraBackdrop's tint. Phase 1 uses it for the time-of-day evening hint
     * on Home; Phase 3 will use it for audio-preset auras from Player.
     */
    val heroBackdrop: HeroBackdropController = HeroBackdropController()

    /** Drive the forward morph (Home → Sleeping). Suspends ~heroMorphMs. */
    suspend fun beginSleepMorph() {
        morphProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(SWMotion.heroMorphMs, easing = LinearEasing)
        )
    }

    /** Drive the reverse morph (Sleeping → Home). Suspends ~heroMorphMs. */
    suspend fun endSleepMorph() {
        morphProgress.animateTo(
            targetValue = 0f,
            animationSpec = tween(SWMotion.heroMorphMs, easing = LinearEasing)
        )
    }

    /**
     * Snap morphProgress to the value that matches the current rootKey. Call
     * this from Lifecycle.onResume so re-entering the app after a background
     * doesn't replay a half-finished morph.
     */
    fun snapMorphToCurrent() {
        val target = if (rootKey.value == "sleeping") 1f else 0f
        scope.launch { morphProgress.snapTo(target) }
    }

    init {
        _hasSeenOnboardingHints.value = hintPrefs.getBoolean(KEY_HINT_SEEN, false)
        scope.launch {
            settingsStore.settings.collectLatest { s ->
                _settings.postValue(s)
                themeProvider.update(s)
            }
        }
        scope.launch { bootstrap() }
        // Wire cry → audio boost callback.
        cryDetection.onCryDetected = { audioPlayer.boostVolumeOnCry() }
    }

    private suspend fun bootstrap() {
        val b = repo.loadBaby()
        _baby.postValue(b)
        if (b != null) {
            _ongoingSleep.postValue(repo.ongoingSleep(b.id))
            _currentRecommendation.postValue(repo.latestRecommendation(b.id))
            refreshWakeWindow(force = true)
        }
    }

    // ─── Public API (1:1 with iOS AppState) ──────────────────────────────────

    suspend fun saveBaby(b: Baby) {
        repo.saveBaby(b)
        val firstEver = _baby.value == null
        _baby.postValue(b)
        if (firstEver) {
            _pendingWelcome.postValue(true)
            markHintsSeen()
        }
    }

    fun dismissWelcome() { _pendingWelcome.value = false }

    suspend fun saveSettings(s: UserSettings) {
        settingsStore.update(s)
        // _settings will be updated through the Flow collector.
    }

    fun markHintsSeen() {
        hintPrefs.edit { putBoolean(KEY_HINT_SEEN, true) }
        _hasSeenOnboardingHints.value = true
    }

    /** Returns NIGHT if the current local hour is inside the user's configured night window. */
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
        repo.appendSleep(session)
        _ongoingSleep.postValue(session)
        notif.scheduleSleepCheckIn(b.id, afterHours = 2.0)
        // Start foreground service + cry detection (cry is gated on settings & permission).
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

    // ─── Helpers ─────────────────────────────────────────────────────────────

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
