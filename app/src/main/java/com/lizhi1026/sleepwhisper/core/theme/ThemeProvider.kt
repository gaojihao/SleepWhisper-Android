package com.lizhi1026.sleepwhisper.core.theme

import android.content.Context
import android.content.res.Configuration
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.lizhi1026.sleepwhisper.core.visualkit.SWScheme
import com.lizhi1026.sleepwhisper.model.UserSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Decides the active [SWScheme] based on user settings, time-of-day, and system uiMode.
 *
 * Priority: forceNightPreview → settings.isInNightTime → appearance(AUTO uses system,
 * DARK/LIGHT explicit). Refreshes every 60 s; also reacts to update(settings) and
 * onSystemConfigurationChanged calls from the activity.
 */
@Singleton
class ThemeProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _scheme = MutableLiveData(SWScheme.DAY)
    val scheme: LiveData<SWScheme> get() = _scheme

    private val _forceNightPreview = MutableLiveData(false)
    val forceNightPreview: LiveData<Boolean> get() = _forceNightPreview

    private var currentSettings: UserSettings = UserSettings.DEFAULT

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var ticker: Job? = null

    init {
        refresh()
        startTicker()
    }

    fun update(settings: UserSettings) {
        currentSettings = settings
        refresh()
    }

    fun setForceNightPreview(value: Boolean) {
        _forceNightPreview.value = value
        refresh()
    }

    /** MainActivity must call this from onConfigurationChanged when uiMode changes. */
    fun onSystemConfigurationChanged() {
        refresh()
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = scope.launch {
            while (true) {
                delay(60_000L)
                refresh()
            }
        }
    }

    private fun refresh() {
        val previewing = _forceNightPreview.value == true
        val newScheme: SWScheme = when {
            previewing -> SWScheme.NIGHT
            currentSettings.isInNightTime(LocalTime.now().hour) -> SWScheme.NIGHT
            else -> when (currentSettings.appearance) {
                UserSettings.AppearanceMode.AUTO -> if (isSystemDark()) SWScheme.DARK else SWScheme.DAY
                UserSettings.AppearanceMode.DARK -> SWScheme.DARK
                UserSettings.AppearanceMode.LIGHT -> SWScheme.DAY
            }
        }
        if (_scheme.value != newScheme) {
            _scheme.value = newScheme
        }
    }

    private fun isSystemDark(): Boolean {
        val cfg = context.resources.configuration
        return (cfg.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }
}
