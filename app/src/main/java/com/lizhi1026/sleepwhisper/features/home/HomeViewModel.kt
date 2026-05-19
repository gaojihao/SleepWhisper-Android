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

@HiltViewModel
class HomeViewModel @Inject constructor(
    val app: AppStateContainer
) : ViewModel() {

    val baby: LiveData<Baby?> = app.baby
    val cachedWakeWindow = app.cachedWakeWindow
    val playerState: LiveData<PlayerState> = app.audioPlayer.stateLive
    val currentPreset = app.audioPlayer.currentPresetLive
    val hasSeenHints = app.hasSeenOnboardingHints

    init {
        viewModelScope.launch {
            while (true) {
                app.refreshWakeWindow(force = true)
                delay(60_000)
            }
        }
    }

    fun onRecordFeeding(method: FeedingMethod, ml: Int? = null) =
        viewModelScope.launch { app.recordFeeding(method, ml) }

    fun onRecordDiaper(type: DiaperEvent.DiaperType) =
        viewModelScope.launch { app.recordDiaper(type) }

    fun onTapSleep() = viewModelScope.launch { app.startSleep() }

    fun onPickSleepType(type: SleepType) = viewModelScope.launch { app.startSleep(type) }

    fun onDismissHints() = app.markHintsSeen()
}
