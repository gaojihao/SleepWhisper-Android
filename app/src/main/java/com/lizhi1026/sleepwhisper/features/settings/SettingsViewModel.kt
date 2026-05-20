package com.lizhi1026.sleepwhisper.features.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lizhi1026.sleepwhisper.app.AppStateContainer
import com.lizhi1026.sleepwhisper.model.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val app: AppStateContainer
) : ViewModel() {

    val baby = app.baby
    val settings: LiveData<UserSettings> = app.settings
    val forceNightPreview: LiveData<Boolean> = app.themeProvider.forceNightPreview

    fun update(transform: (UserSettings) -> UserSettings) {
        val cur = settings.value ?: UserSettings.DEFAULT
        viewModelScope.launch { app.saveSettings(transform(cur)) }
    }

    fun setForceNightPreview(value: Boolean) {
        app.themeProvider.setForceNightPreview(value)
    }

    fun stopCryDetection() = app.cryDetection.stop()
}
