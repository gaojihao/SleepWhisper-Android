package com.lizhi1026.sleepwhisper.core.visualkit

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/**
 * visualkit 层的减少动态效果支持。
 *
 * 职责：读取系统无障碍设置中的动画时长缩放因子，判断是否需要关闭无限/过渡动画。
 * 归属层：visualkit — 被所有动画 Modifier（如 [Modifier.floatingY]）和动画 Composable 读取。
 *
 * ⚠️ 规则：当 `Settings.Global.ANIMATOR_DURATION_SCALE == 0` 时，所有无限动画和过渡动画
 * 均应静止。[LocalReduceMotion] 为 true 时，Composable 应跳过 `rememberInfiniteTransition`
 * 等动画，直接渲染静态状态。
 *
 * 使用方式：在 [SWTheme] 根节点调用 [rememberReduceMotion]，通过 [LocalReduceMotion] 注入。
 */
/**
 * ⚠️ CompositionLocal：当系统无障碍"减少动态效果"开启（动画时长缩放 = 0）时为 true。
 * Composable 应检查此值，为 true 时跳过所有无限动画与过渡动画。
 * 默认值为 false；实际值由 [rememberReduceMotion] 计算后在组合树根节点提供。
 */
val LocalReduceMotion = compositionLocalOf { false }

/**
 * 读取系统 `ANIMATOR_DURATION_SCALE` 设置，返回是否应减少动态效果。
 *
 * 结果使用 [remember] 缓存（以 Context 为 key），避免重复读取系统设置。
 * 读取失败时安全降级，返回 false（即正常播放动画）。
 *
 * @return true 表示系统动画时长缩放为 0，所有动画应静止；false 表示正常播放动画
 */
@Composable
fun rememberReduceMotion(): Boolean {
    val ctx = LocalContext.current
    return remember(ctx) {
        runCatching {
            Settings.Global.getFloat(
                ctx.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f  // 默认值 1f，即不缩放
            ) == 0f  // 等于 0 表示系统要求关闭所有动画
        }.getOrDefault(false)  // 读取异常时安全降级为 false
    }
}
