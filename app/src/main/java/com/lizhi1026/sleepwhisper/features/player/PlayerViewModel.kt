package com.lizhi1026.sleepwhisper.features.player

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lizhi1026.sleepwhisper.app.AppStateContainer
import com.lizhi1026.sleepwhisper.core.audio.PlayerState
import com.lizhi1026.sleepwhisper.model.AudioPreset
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val app: AppStateContainer
) : ViewModel() {
    val playerState: LiveData<PlayerState> = app.audioPlayer.stateLive
    val currentPreset = app.audioPlayer.currentPresetLive
    val baby = app.baby
    val settings = app.settings

    val allPresets: List<AudioPreset> = AudioPreset.bundled

    fun recommended(forAgeMonths: Int): List<AudioPreset> =
        AudioPreset.recommended(forAgeMonths)

    fun playPreset(presetId: String, minutes: Int) {
        app.audioPlayer.play(presetId, durationSeconds = minutes * 60)
    }

    fun stop() = app.audioPlayer.stop()

    fun saveDefaultTimer(minutes: Int) {
        viewModelScope.launch {
            val cur = settings.value ?: return@launch
            app.saveSettings(cur.copy(defaultTimerMinutes = minutes))
        }
    }
}
