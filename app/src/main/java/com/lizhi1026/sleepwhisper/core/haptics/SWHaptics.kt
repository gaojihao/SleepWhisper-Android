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
 * 触觉反馈封装类（对应 iOS Core/Haptics/Haptics.swift 的 UIImpactFeedbackStyle / UINotificationFeedbackType 分类）。
 *
 * 所在层：core/haptics — 基础设施层，依赖 Android 振动 API。
 * 交互对象：[VibratorManager]（API 31+）或 [Vibrator]（API 31 以下）；由 Hilt 注入调用方。
 *
 * 设计说明：
 *   - 本类不检查 UserSettings.hapticFeedback 开关，调用方负责判断。
 *   - 每次调用均为一次性 [VibrationEffect.createPredefined]，不维持状态。
 *   - API 31+ 通过 [VibratorManager.getDefaultVibrator] 获取振动器；
 *     之前版本使用已废弃的 [Context.VIBRATOR_SERVICE]，并以 @Suppress 压制警告。
 */
@Singleton
class SWHaptics @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * 懒加载振动器实例：
     * API 31+ 使用 VibratorManager 获取默认振动器；低版本直接获取 Vibrator 系统服务。
     */
    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // API 31+ 使用 VibratorManager 获取默认振动器
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            // API 31 以下使用已废弃的 VIBRATOR_SERVICE
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    /** 轻触反馈（对应 iOS UIImpactFeedbackStyle.light）。 */
    fun light()   = play(VibrationEffect.EFFECT_TICK)
    /** 中等反馈（对应 iOS UIImpactFeedbackStyle.medium）。 */
    fun medium()  = play(VibrationEffect.EFFECT_CLICK)
    /** 重击反馈（对应 iOS UIImpactFeedbackStyle.heavy）。 */
    fun heavy()   = play(VibrationEffect.EFFECT_HEAVY_CLICK)
    /** 柔和反馈（复用 TICK，对应 iOS UIImpactFeedbackStyle.soft）。 */
    fun soft()    = play(VibrationEffect.EFFECT_TICK)
    /** 成功通知反馈（对应 iOS UINotificationFeedbackType.success）。 */
    fun success() = play(VibrationEffect.EFFECT_DOUBLE_CLICK)
    /** 警告通知反馈（对应 iOS UINotificationFeedbackType.warning）。 */
    fun warning() = play(VibrationEffect.EFFECT_HEAVY_CLICK)
    /** 错误通知反馈（对应 iOS UINotificationFeedbackType.error）。 */
    fun error()   = play(VibrationEffect.EFFECT_DOUBLE_CLICK)

    /**
     * 实际触发振动的私有方法。
     * 若振动器不可用或硬件不支持，则静默跳过。
     *
     * @param predefined [VibrationEffect] 中定义的预设效果常量。
     */
    private fun play(predefined: Int) {
        val v = vibrator ?: return        // 振动器不可用则静默返回
        if (!v.hasVibrator()) return      // 硬件不支持振动则跳过
        v.vibrate(VibrationEffect.createPredefined(predefined))
    }
}
