package com.lizhi1026.sleepwhisper.core.visualkit

import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color

/**
 * visualkit 层的全局 ambient 色控制器（单例）。
 *
 * 职责：持有当前应向背景晕染的"环境色"状态，供 AuroraBackdrop 等背景层读取。
 * 归属层：visualkit — 被 Home、SleepingScreen、Player 等页面写入，被所有背景 Composable 读取。
 *
 * 颜色来源优先级（由高到低）：
 *   1. 播放器当前音频预设的中间 aura 色（Player Hero #3）
 *   2. 时间段提示色（Home / SleepingScreen）
 *   3. null — 背景层使用默认 Aurora 渐变色板
 *
 * ⚠️ 这是单例控制器：Player 选择音频预设时，把 `AuraColors` 的中间色通过
 * [setAmbient] 传染到全局背景，实现沉浸感色彩联动。
 *
 * 调用 `setAmbient(null)` 可清除 ambient 色，让背景回到默认 Aurora 色板。
 */
@Stable
class HeroBackdropController {
    // 内部可变状态，Compose 可观察
    private val _ambient = mutableStateOf<Color?>(null)
    // 对外只读，背景层通过此属性读取当前 ambient 色
    val ambient: Color? get() = _ambient.value

    /**
     * 设置当前 ambient 色。
     * @param color 新的环境色；传 null 表示清除，背景回到默认 Aurora 色板。
     */
    fun setAmbient(color: Color?) { _ambient.value = color }
}

/**
 * ⚠️ visualkit 层的 CompositionLocal，为组合树下游提供 [HeroBackdropController] 实例。
 * 默认值为新建实例；通常在 App 根节点由 SWTheme 或专用 Provider 注入共享单例。
 */
val LocalHeroBackdropController = compositionLocalOf { HeroBackdropController() }

/**
 * 将 [base] 色在线性 RGB 空间中向 [ambient] 色混合 [fraction] 比例。
 *
 * 用于背景层将 ambient 色柔和地叠加到默认 Aurora 底色上，营造音频预设的沉浸感。
 *
 * @param base      底色（Aurora 默认色或当前背景色）
 * @param ambient   目标 ambient 色；为 null 时直接返回 [base]，不做任何混合
 * @param fraction  混合比例，自动 coerce 到 [0, 1]；0 = 完全使用 base，1 = 完全使用 ambient
 * @return          混合结果，alpha 始终保持 [base] 的 alpha 不变
 */
fun blendAmbient(base: Color, ambient: Color?, fraction: Float): Color {
    if (ambient == null) return base
    val f = fraction.coerceIn(0f, 1f)
    return base.copy(
        red   = base.red   * (1f - f) + ambient.red   * f,
        green = base.green * (1f - f) + ambient.green * f,
        blue  = base.blue  * (1f - f) + ambient.blue  * f,
        alpha = base.alpha,
    )
}
