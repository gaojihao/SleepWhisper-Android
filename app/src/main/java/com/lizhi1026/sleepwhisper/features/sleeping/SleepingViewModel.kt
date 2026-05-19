package com.lizhi1026.sleepwhisper.features.sleeping

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lizhi1026.sleepwhisper.app.AppStateContainer
import com.lizhi1026.sleepwhisper.core.audio.PlayerState
import com.lizhi1026.sleepwhisper.model.SleepSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SleepingViewModel @Inject constructor(
    private val app: AppStateContainer
) : ViewModel() {
    val ongoingSleep: LiveData<SleepSession?> = app.ongoingSleep
    val playerState: LiveData<PlayerState> = app.audioPlayer.stateLive
    val baby = app.baby

    fun onWake() = viewModelScope.launch { app.endSleep() }
    fun pausePlayer() = app.audioPlayer.pause()
    fun resumePlayer() = app.audioPlayer.resume()
}
