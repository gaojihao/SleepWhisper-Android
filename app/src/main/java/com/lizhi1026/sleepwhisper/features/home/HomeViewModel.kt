package com.lizhi1026.sleepwhisper.features.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lizhi1026.sleepwhisper.app.AppStateContainer
import com.lizhi1026.sleepwhisper.core.audio.PlayerState
import com.lizhi1026.sleepwhisper.model.Baby
import com.lizhi1026.sleepwhisper.model.DiaperEvent
import com.lizhi1026.sleepwhisper.model.FeedingEvent.FeedingMethod
import com.lizhi1026.sleepwhisper.model.SleepSession.SleepType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 首页 ViewModel（features/home 层）
 *
 * 职责：
 * - 通过构造注入 [AppStateContainer]（单一门面），将其 LiveData 直接暴露给 HomeScreen，
 *   不持有独立的 UiState 数据类。
 * - 暴露五条 LiveData：[baby]、[cachedWakeWindow]、[playerState]、[currentPreset]、[hasSeenHints]。
 * - init 块启动协程，每 60 秒强制刷新清醒窗口（`refreshWakeWindow(force=true)`）。
 * - 所有写操作通过 `app.xxx()` 委托给 AppStateContainer，不直接调用 Repository。
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    val app: AppStateContainer
) : ViewModel() {

    /** 当前宝宝档案，来自 AppStateContainer.baby。 */
    val baby: LiveData<Baby?> = app.baby

    /** 清醒窗口剩余/总分钟数对，由 AppStateContainer 根据最近睡眠记录计算。 */
    val cachedWakeWindow = app.cachedWakeWindow

    /** 音频播放器状态（Idle / Playing / Paused），来自 AudioPlayer.stateLive。 */
    val playerState: LiveData<PlayerState> = app.audioPlayer.stateLive

    /** 当前播放的音效预设，无预设时为 null。 */
    val currentPreset = app.audioPlayer.currentPresetLive

    /** 用户是否已读过新手引导提示，持久化存储于 AppStateContainer。 */
    val hasSeenHints = app.hasSeenOnboardingHints

    init {
        // 每 60 秒强制刷新清醒窗口，确保用户长时间停留首页时数据保持最新
        viewModelScope.launch {
            while (true) {
                app.refreshWakeWindow(force = true)
                delay(60_000)
            }
        }
    }

    /**
     * 记录哺乳事件，委托给 AppStateContainer.recordFeeding。
     *
     * @param method 哺乳方式（BREAST_LEFT / BREAST_RIGHT / BOTTLE）。
     * @param ml 奶瓶喂养毫升数，母乳或跳过填量时为 null。
     */
    fun onRecordFeeding(method: FeedingMethod, ml: Int? = null) =
        viewModelScope.launch { app.recordFeeding(method, ml) }

    /**
     * 记录换尿片事件，委托给 AppStateContainer.recordDiaper。
     *
     * @param type 尿片类型（WET / SOILED 等）。
     */
    fun onRecordDiaper(type: DiaperEvent.DiaperType) =
        viewModelScope.launch { app.recordDiaper(type) }

    /**
     * 用户点击 SleepCTA 后调用，启动默认睡眠类型的睡眠会话。
     * Hero 动画由 HomeScreen 侧负责触发，此处仅调用 app.startSleep()。
     */
    fun onTapSleep() = viewModelScope.launch { app.startSleep() }

    /**
     * 用户从 SleepTypePicker 选择睡眠类型后调用。
     *
     * @param type 用户选择的睡眠类型（NAP / NIGHT / CONTACT_NAP）。
     */
    fun onPickSleepType(type: SleepType) = viewModelScope.launch { app.startSleep(type) }

    /**
     * 用户点击"我准备好了"或完成所有引导页后调用，将已读状态持久化。
     * 调用后 [hasSeenHints] 变为 true，OnboardingHintsCard 从首页消失。
     */
    fun onDismissHints() = app.markHintsSeen()
}
