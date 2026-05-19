package com.lizhi1026.sleepwhisper.core.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Predefined haptic feedback wrappers — port of iOS Core/Haptics/Haptics.swift.
 *
 * Each call performs a one-shot `VibrationEffect.createPredefined(...)` matching the iOS
 * UIImpactFeedbackStyle / UINotificationFeedbackType taxonomy.
 *
 * The caller is responsible for respecting `UserSettings.hapticFeedback`; this class is
 * unconditional so it can be reused from places (tests, debug) that bypass settings.
 */
@Singleton
class SWHaptics @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun light()   = play(VibrationEffect.EFFECT_TICK)
    fun medium()  = play(VibrationEffect.EFFECT_CLICK)
    fun heavy()   = play(VibrationEffect.EFFECT_HEAVY_CLICK)
    fun soft()    = play(VibrationEffect.EFFECT_TICK)
    fun success() = play(VibrationEffect.EFFECT_DOUBLE_CLICK)
    fun warning() = play(VibrationEffect.EFFECT_HEAVY_CLICK)
    fun error()   = play(VibrationEffect.EFFECT_DOUBLE_CLICK)

    private fun play(predefined: Int) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        v.vibrate(VibrationEffect.createPredefined(predefined))
    }
}
