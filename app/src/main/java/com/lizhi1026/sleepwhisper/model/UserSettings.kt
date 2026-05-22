/**
 * 用户全局偏好设置数据模型（model 领域层，纯 Kotlin，无 Android Framework 依赖）。
 *
 * **职责**：承载与具体宝宝无关的 App 级别配置，包括外观、夜间时段、哭声检测、触觉反馈等。
 *
 * **序列化**：通过 `SettingsStore` 以 `kotlinx.serialization` 序列化为单条 JSON，
 * 存储键为 `settings_json_v2`。
 * ⚠️ 新增字段**必须设默认值**，否则旧版本 JSON 反序列化时会抛 `MissingFieldException`。
 *
 * **交互方**：`SettingsStore`（持久化读写）、`AppStateContainer`（运行时持有当前实例）、
 * 各 UI ViewModel（读取偏好项决定界面行为）。
 */
package com.lizhi1026.sleepwhisper.model

import com.lizhi1026.sleepwhisper.BuildConfig
import kotlinx.serialization.Serializable

/**
 * 用户偏好设置的不可变数据类，所有字段均有默认值以兼容旧版本 JSON。
 *
 * @property activeBabyId       当前激活的宝宝 ID，null 表示尚未选择宝宝
 * @property cryDetectionEnabled 是否开启哭声检测功能
 * @property cryDetectionThresholdDb 哭声检测触发阈值，单位分贝（dB）
 * @property defaultTimerMinutes 默认计时器时长（分钟）
 * @property fadeAfterSleepMinutes 宝宝入睡后白噪音淡出等待时间（分钟）
 * @property hapticFeedback      是否启用触觉反馈（振动）
 * @property appearance          界面外观模式，见 [AppearanceMode]
 * @property nightModeStartHour  夜间时段起始小时（0–23，24 小时制）
 * @property nightModeEndHour    夜间时段结束小时（0–23，24 小时制）
 * @property locale              用户语言/地区标签，符合 BCP 47 格式（如 "zh-CN"）
 * @property lastSeenVersion     用户上次启动时的 App 版本名，用于版本升级引导判断
 * @property wakeLongPressEnabled 是否启用长按唤醒手势
 */
@Serializable
data class UserSettings(
    val activeBabyId: String? = null,
    val cryDetectionEnabled: Boolean = false,
    val cryDetectionThresholdDb: Int = 60,
    val defaultTimerMinutes: Int = 30,
    val fadeAfterSleepMinutes: Int = 5,
    val hapticFeedback: Boolean = true,
    val appearance: AppearanceMode = AppearanceMode.AUTO,
    val nightModeStartHour: Int = 22,
    val nightModeEndHour: Int = 6,
    val locale: String = java.util.Locale.getDefault().toLanguageTag(),
    val lastSeenVersion: String = BuildConfig.VERSION_NAME,
    val wakeLongPressEnabled: Boolean = true
    // iOS 有 iCloudSyncEnabled，Android 不做同步， intentionally omitted
) {
    /**
     * 界面外观模式枚举。
     *
     * @property serializedName JSON 序列化使用的字符串值，需与 iOS 端保持一致
     */
    enum class AppearanceMode(val serializedName: String) {
        /** 跟随系统深/浅色模式自动切换 */
        AUTO("auto"),
        /** 强制浅色模式 */
        LIGHT("light"),
        /** 强制深色模式 */
        DARK("dark")
    }

    companion object {
        /** 应用首次安装或重置设置时使用的默认配置实例 */
        val DEFAULT: UserSettings = UserSettings()
    }

    /**
     * 判断给定小时是否落在用户配置的夜间时段内。
     *
     * ⚠️ **支持跨午夜区间**：当 [nightModeStartHour] > [nightModeEndHour] 时（例如 22:00–07:00），
     * 采用"或"逻辑判断；否则使用普通区间 `[start, end)` 判断。
     *
     * 示例：`nightModeStartHour=22, nightModeEndHour=6`，小时 23、0、5 均返回 true，7 返回 false。
     *
     * @param hour 待判断的小时数（0–23），默认取设备当前本地时间的小时
     * @return true 表示处于夜间时段，UI 应切换夜间配色/降低亮度等
     */
    fun isInNightTime(hour: Int = java.time.LocalTime.now().hour): Boolean =
        if (nightModeStartHour > nightModeEndHour) {
            // 跨午夜：起始 ≤ hour（夜晚部分）或 hour < 结束（凌晨部分）
            hour >= nightModeStartHour || hour < nightModeEndHour
        } else {
            // 同一天内的普通区间 [start, end)
            hour in nightModeStartHour until nightModeEndHour
        }
}
