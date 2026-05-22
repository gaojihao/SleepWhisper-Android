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

/**
 * 用户设置的持久化存储，基于 Jetpack DataStore（Preferences），属于 data / persistence 层。
 *
 * 所在层：core.persistence，由 Hilt 以 @Singleton 提供。
 *
 * 关键约定：
 * - ⚠️ **使用单键 `settings_json_v2` 将整个 [UserSettings] 对象序列化为 JSON 字符串**存储，
 *   而非逐字段拆成多个 Preferences 键。
 * - 新增字段时，必须在 [UserSettings] 中为新字段设置**默认值**，并保持 `ignoreUnknownKeys = true`，
 *   以确保旧版本 JSON 反序列化时不崩溃（向前兼容）。
 * - DataStore 文件名为 `sw_settings_v2`；解析失败或文件不存在时自动降级为 [UserSettings.DEFAULT]，
 *   保证 Flow 永不为空。
 * - `UserSettings` 不经过 Room，有意与 [Repository] 接口隔离，避免设置查询误入数据库路径。
 */

private val Context.sleepWhisperDataStore: DataStore<Preferences> by preferencesDataStore(name = "sw_settings_v2")

/** DataStore 中存储 [UserSettings] JSON 字符串所用的单一 Preferences 键。 */
private val SETTINGS_KEY = stringPreferencesKey("settings_json_v2")

/**
 * kotlinx.serialization JSON 实例，配置：
 * - `ignoreUnknownKeys = true`：旧版 JSON 含未知字段时不抛异常，保证向前兼容。
 * - `encodeDefaults = true`：序列化时包含具有默认值的字段，避免丢失数据。
 */
private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

@Singleton
class SettingsStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * 以 [Flow] 形式持续发射最新的 [UserSettings]。
     *
     * - DataStore IO 异常时通过 `catch` 回退到空 Preferences，随后映射为 [UserSettings.DEFAULT]。
     * - JSON 解析失败时同样回退到 [UserSettings.DEFAULT]，保证 Flow 永不终止、永不为 null。
     * - 调用方无需手动切线程，DataStore 已在内部使用 IO 调度器。
     */
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

    /**
     * 将新的 [UserSettings] 序列化为 JSON 并持久化到 DataStore。
     *
     * @param newValue 要保存的完整设置对象，已有字段与新增字段均会被写入。
     * suspend：DataStore 的 `edit` 为挂起函数，调用方需在协程中调用。
     */
    suspend fun update(newValue: UserSettings) {
        val encoded = json.encodeToString(newValue)
        context.sleepWhisperDataStore.edit { it[SETTINGS_KEY] = encoded }
    }
}
