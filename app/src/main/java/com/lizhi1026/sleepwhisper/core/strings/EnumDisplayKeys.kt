package com.lizhi1026.sleepwhisper.core.strings

import androidx.annotation.StringRes
import com.lizhi1026.sleepwhisper.R
import com.lizhi1026.sleepwhisper.model.Baby
import com.lizhi1026.sleepwhisper.model.DiaperEvent
import com.lizhi1026.sleepwhisper.model.FeedingEvent
import com.lizhi1026.sleepwhisper.model.SleepSession
import com.lizhi1026.sleepwhisper.model.UserSettings

/**
 * 枚举 → 本地化字符串资源 ID 的映射集合（顶层扩展函数 + 普通函数）。
 *
 * 所在层：core/strings — 表现辅助层，连接领域模型与 Android 资源系统。
 * 交互对象：[Baby]、[FeedingEvent]、[DiaperEvent]、[SleepSession]、[UserSettings]（领域枚举）；
 *           R.string.*（Android 资源 ID）。
 *
 * 设计原因：将 Android 依赖（@StringRes / R 类）集中在 core/ 层，
 * 避免 model/ 包直接引入 Android Framework，保持领域模型的可测试性。
 *
 * 包含映射：
 *   - BabyGender.displayKey()       → 性别显示名称
 *   - FeedingMethod.displayKey()    → 喂养方式显示名称
 *   - DiaperType.displayKey()       → 尿布类型显示名称
 *   - SleepType.displayKey()        → 睡眠类型显示名称
 *   - AppearanceMode.displayKey()   → 外观模式显示名称
 *   - audioPresetNameKey(nameKey)   → 音频预设显示名称
 *   - greetingForHour(hour)         → 时段问候语
 */

/** 宝宝性别枚举 → 对应本地化字符串资源 ID。 */
@StringRes
fun Baby.BabyGender.displayKey(): Int = when (this) {
    Baby.BabyGender.FEMALE -> R.string.onboarding_gender_female
    Baby.BabyGender.MALE -> R.string.onboarding_gender_male
    Baby.BabyGender.UNKNOWN -> R.string.onboarding_gender_unknown
}

/** 喂养方式枚举 → 对应本地化字符串资源 ID。 */
@StringRes
fun FeedingEvent.FeedingMethod.displayKey(): Int = when (this) {
    FeedingEvent.FeedingMethod.BREAST_LEFT -> R.string.feeding_breastleft
    FeedingEvent.FeedingMethod.BREAST_RIGHT -> R.string.feeding_breastright
    FeedingEvent.FeedingMethod.BOTTLE -> R.string.feeding_bottle
    FeedingEvent.FeedingMethod.SOLID -> R.string.feeding_solid
}

/** 尿布类型枚举 → 对应本地化字符串资源 ID。 */
@StringRes
fun DiaperEvent.DiaperType.displayKey(): Int = when (this) {
    DiaperEvent.DiaperType.WET -> R.string.diaper_wet
    DiaperEvent.DiaperType.DIRTY -> R.string.diaper_dirty
    DiaperEvent.DiaperType.MIXED -> R.string.diaper_mixed
    DiaperEvent.DiaperType.DRY -> R.string.diaper_dry
}

/** 睡眠类型枚举 → 对应本地化字符串资源 ID。 */
@StringRes
fun SleepSession.SleepType.displayKey(): Int = when (this) {
    SleepSession.SleepType.NAP -> R.string.sleeptype_nap_title
    SleepSession.SleepType.NIGHT -> R.string.sleeptype_night_title
    SleepSession.SleepType.CONTACT_NAP -> R.string.sleeptype_contact_title
}

/** 外观模式枚举 → 对应本地化字符串资源 ID（设置页展示用）。 */
@StringRes
fun UserSettings.AppearanceMode.displayKey(): Int = when (this) {
    UserSettings.AppearanceMode.AUTO -> R.string.settings_appearance_auto
    UserSettings.AppearanceMode.LIGHT -> R.string.settings_appearance_light
    UserSettings.AppearanceMode.DARK -> R.string.settings_appearance_dark
}

/**
 * 音频预设名称 ID → 本地化字符串资源 ID。
 *
 * 将 iOS 风格的点分 nameKey（如 "audio.lullaby1"）映射到 Android R.string 资源。
 * 调用方直接传入 `AudioPreset.nameKey`。
 *
 * @param nameKey iOS 风格音频预设标识符（点分格式）。
 * @return 对应的 @StringRes ID；未匹配时回退到 R.string.app_name。
 */
@StringRes
fun audioPresetNameKey(nameKey: String): Int = when (nameKey) {
    "audio.womb"      -> R.string.audio_womb       // 子宫音
    "audio.heartbeat" -> R.string.audio_heartbeat  // 心跳音
    "audio.dryer"     -> R.string.audio_dryer      // 烘干机噪音
    "audio.vacuum"    -> R.string.audio_vacuum     // 吸尘器噪音
    "audio.white"     -> R.string.audio_white      // 白噪音
    "audio.brown"     -> R.string.audio_brown      // 棕色噪音
    "audio.pink"      -> R.string.audio_pink       // 粉色噪音
    "audio.rain"      -> R.string.audio_rain       // 雨声
    "audio.ocean"     -> R.string.audio_ocean      // 海浪声
    "audio.fan"       -> R.string.audio_fan        // 风扇噪音
    "audio.lullaby1"  -> R.string.audio_lullaby1   // 摇篮曲 1
    "audio.lullaby2"  -> R.string.audio_lullaby2   // 摇篮曲 2
    else -> R.string.app_name                      // 未知预设回退到应用名
}

/**
 * 按小时（0..23）返回对应时段的问候语字符串资源 ID，对应 iOS Home greetingKey 阶梯逻辑。
 *
 * @param hour 当前小时（24 小时制，0..23）。
 * @return 对应时段的 @StringRes ID。
 */
@StringRes
fun greetingForHour(hour: Int): Int = when (hour) {
    in 5..9   -> R.string.home_greeting_morning    // 早上好（5:00–9:59）
    in 10..12 -> R.string.home_greeting_noon       // 中午好（10:00–12:59）
    in 13..16 -> R.string.home_greeting_afternoon  // 下午好（13:00–16:59）
    in 17..20 -> R.string.home_greeting_evening    // 晚上好（17:00–20:59）
    else      -> R.string.home_greeting_night      // 深夜/凌晨（21:00–4:59）
}
