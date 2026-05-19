package com.lizhi1026.sleepwhisper.core.persistence

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lizhi1026.sleepwhisper.model.UserSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.sleepWhisperDataStore: DataStore<Preferences> by preferencesDataStore(name = "sw_settings_v2")

private val SETTINGS_KEY = stringPreferencesKey("settings_json_v2")

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

@Singleton
class SettingsStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val settings: Flow<UserSettings> = context.sleepWhisperDataStore.data
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { prefs ->
            val raw = prefs[SETTINGS_KEY]
            if (raw.isNullOrBlank()) {
                UserSettings.DEFAULT
            } else {
                try {
                    json.decodeFromString<UserSettings>(raw)
                } catch (t: Throwable) {
                    UserSettings.DEFAULT
                }
            }
        }

    suspend fun update(newValue: UserSettings) {
        val encoded = json.encodeToString(newValue)
        context.sleepWhisperDataStore.edit { it[SETTINGS_KEY] = encoded }
    }
}
