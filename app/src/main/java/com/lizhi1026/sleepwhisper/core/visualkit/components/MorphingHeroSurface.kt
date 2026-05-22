/**
 * MorphingHeroSurface — Hero #1 共享元素动画入口点。
 *
 * 视觉效果：Home 屏的"入睡 CTA 按钮"与 SleepingScreen 的计时光晕圆圈之间，
 * 通过 Jetpack Compose SharedTransitionScope 实现无缝 Bounds 形变过渡。
 * 过渡时长由 [SWMotion.heroMorphMs] 控制，缓动为 LinearEasing。
 *
 * 所在层：core/visualkit — 设计令牌层（可视组件工具包）
 *
 * 典型使用场景：
 *   - Home 屏 SleepCTA 按钮：应用 `Modifier.sleepHeroOrigin(...)`
 *   - SleepingScreen 计时光晕：应用 `Modifier.sleepHeroDestination(...)`
 *
 * ⚠️ 联动提醒：修改 HomeScreen 或 SleepingScreen 任意一端的布局/尺寸时，
 * 务必同步检查另一端，确保 sharedBounds 的 key 与 ResizeMode 匹配。
 * 入睡/退出状态由 `AppStateContainer.beginSleepMorph()` / `endSleepMorph()` 驱动。
 */
package com.lizhi1026.sleepwhisper.core.visualkit.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lizhi1026.sleepwhisper.core.visualkit.SWMotion

/**
 * Hero 形变共享元素 Key——两端必须使用完全相同的字符串，
 * Compose 才能在路由切换时将它们识别为同一元素对。
 */
private const val SLEEP_HERO_KEY = "sleep-hero"

/**
 * 将 Modifier 标记为 Sleep Hero 动画的**起始端**（Home 屏 SleepCTA 按钮）。
 *
 * 路由切换时，Compose 会将此 Bounds 动画过渡到目标端（[sleepHeroDestination]）
 * 的位置与尺寸，实现视觉上"按钮扩展为计时光晕"的形变效果。
 *
 * @param sharedScope 由 `SharedTransitionLayout` 提供的共享过渡作用域
 * @param animScope   由 `AnimatedVisibility` / `AnimatedContent` 提供的可见性作用域
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sleepHeroOrigin(
    sharedScope: SharedTransitionScope,
    animScope: AnimatedVisibilityScope
): Modifier = with(sharedScope) {
    this@sleepHeroOrigin.sharedBounds(
        sharedContentState = rememberSharedContentState(key = SLEEP_HERO_KEY),
        animatedVisibilityScope = animScope,
        // 使用线性缓动 + heroMorphMs 时长，与 SleepingScreen 动效节奏保持一致
        boundsTransform = { _, _ -> tween(SWMotion.heroMorphMs, easing = LinearEasing) },
        // RemeasureToBounds：在过渡中持续重测量内容，确保圆→圆形变尺寸正确
        resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds
    )
}

/**
 * 将 Modifier 标记为 Sleep Hero 动画的**目标端**（SleepingScreen 计时光晕）。
 *
 * 与 [sleepHeroOrigin] 使用相同的 [SLEEP_HERO_KEY]，
 * Compose SharedTransition 引擎据此在路由切换时配对并驱动形变。
 *
 * @param sharedScope 由 `SharedTransitionLayout` 提供的共享过渡作用域
 * @param animScope   由 `AnimatedVisibility` / `AnimatedContent` 提供的可见性作用域
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sleepHeroDestination(
    sharedScope: SharedTransitionScope,
    animScope: AnimatedVisibilityScope
): Modifier = with(sharedScope) {
    this@sleepHeroDestination.sharedBounds(
        sharedContentState = rememberSharedContentState(key = SLEEP_HERO_KEY),
        animatedVisibilityScope = animScope,
        // 与起始端保持相同的过渡规格，确保两端动画曲线对称
        boundsTransform = { _, _ -> tween(SWMotion.heroMorphMs, easing = LinearEasing) },
        resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds
    )
}
