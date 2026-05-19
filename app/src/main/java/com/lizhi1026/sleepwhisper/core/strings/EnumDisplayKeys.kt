package com.lizhi1026.sleepwhisper.core.strings

import androidx.annotation.StringRes
import com.lizhi1026.sleepwhisper.R
import com.lizhi1026.sleepwhisper.model.Baby
import com.lizhi1026.sleepwhisper.model.DiaperEvent
import com.lizhi1026.sleepwhisper.model.FeedingEvent
import com.lizhi1026.sleepwhisper.model.SleepSession
import com.lizhi1026.sleepwhisper.model.UserSettings

/**
 * Maps domain enums to localized `@StringRes` ids. Lives in core/ so it can be reused by
 * ViewModels and Composables without polluting model/ with Android-only dependencies.
 */

@StringRes
fun Baby.BabyGender.displayKey(): Int = when (this) {
    Baby.BabyGender.FEMALE -> R.string.onboarding_gender_female
    Baby.BabyGender.MALE -> R.string.onboarding_gender_male
    Baby.BabyGender.UNKNOWN -> R.string.onboarding_gender_unknown
}

@StringRes
fun FeedingEvent.FeedingMethod.displayKey(): Int = when (this) {
    FeedingEvent.FeedingMethod.BREAST_LEFT -> R.string.feeding_breastleft
    FeedingEvent.FeedingMethod.BREAST_RIGHT -> R.string.feeding_breastright
    FeedingEvent.FeedingMethod.BOTTLE -> R.string.feeding_bottle
    FeedingEvent.FeedingMethod.SOLID -> R.string.feeding_solid
}

@StringRes
fun DiaperEvent.DiaperType.displayKey(): Int = when (this) {
    DiaperEvent.DiaperType.WET -> R.string.diaper_wet
    DiaperEvent.DiaperType.DIRTY -> R.string.diaper_dirty
    DiaperEvent.DiaperType.MIXED -> R.string.diaper_mixed
    DiaperEvent.DiaperType.DRY -> R.string.diaper_dry
}

@StringRes
fun SleepSession.SleepType.displayKey(): Int = when (this) {
    SleepSession.SleepType.NAP -> R.string.sleeptype_nap_title
    SleepSession.SleepType.NIGHT -> R.string.sleeptype_night_title
    SleepSession.SleepType.CONTACT_NAP -> R.string.sleeptype_contact_title
}

@StringRes
fun UserSettings.AppearanceMode.displayKey(): Int = when (this) {
    UserSettings.AppearanceMode.AUTO -> R.string.settings_appearance_auto
    UserSettings.AppearanceMode.LIGHT -> R.string.settings_appearance_light
    UserSettings.AppearanceMode.DARK -> R.string.settings_appearance_dark
}

/**
 * Audio preset name. Maps `nameKey` (the iOS-style id like "audio.lullaby1") to the Android
 * resource id by replacing the dot with an underscore.
 *
 * Caller passes `AudioPreset.nameKey` directly. Returns 0 if no matching resource exists.
 */
@StringRes
fun audioPresetNameKey(nameKey: String): Int = when (nameKey) {
    "audio.womb" -> R.string.audio_womb
    "audio.heartbeat" -> R.string.audio_heartbeat
    "audio.dryer" -> R.string.audio_dryer
    "audio.vacuum" -> R.string.audio_vacuum
    "audio.white" -> R.string.audio_white
    "audio.brown" -> R.string.audio_brown
    "audio.pink" -> R.string.audio_pink
    "audio.rain" -> R.string.audio_rain
    "audio.ocean" -> R.string.audio_ocean
    "audio.fan" -> R.string.audio_fan
    "audio.lullaby1" -> R.string.audio_lullaby1
    "audio.lullaby2" -> R.string.audio_lullaby2
    else -> R.string.app_name
}

/**
 * Greeting based on hour-of-day (0..23). Matches iOS Home greetingKey ladder.
 */
@StringRes
fun greetingForHour(hour: Int): Int = when (hour) {
    in 5..9 -> R.string.home_greeting_morning
    in 10..12 -> R.string.home_greeting_noon
    in 13..16 -> R.string.home_greeting_afternoon
    in 17..20 -> R.string.home_greeting_evening
    else -> R.string.home_greeting_night
}
