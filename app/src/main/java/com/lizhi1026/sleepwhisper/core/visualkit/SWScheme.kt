package com.lizhi1026.sleepwhisper.core.visualkit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf

/**
 * visualkit 主题 scheme 定义与 CompositionLocal 注入层。
 *
 * [SWScheme] 枚举三态显示模式，与 Android 系统深色模式**正交**：
 *  - [SWScheme.DAY]   — 日间模式（浅色背景，暖橙主调）
 *  - [SWScheme.DARK]  — 夜间通用深色模式（深蓝底，对标系统 dark theme）
 *  - [SWScheme.NIGHT] — 夜间极暗态（比 DARK 更深的暗红调，专为夜间就寝护眼）
 *
 * NIGHT 不是系统深色模式的别名：它由应用根据时间或用户手动切换，
 * 可在系统浅色模式下激活，也可在系统深色模式下不激活。
 *
 * 使用方式：在 Activity/Screen 根节点调用 [SWTheme] 注入 scheme，
 * 子 Composable 通过 `LocalSWScheme.current` 读取，
 * 再传给 [SWColor] / [SWGradient] 等 token 函数取色。
 */

/**
 * visualkit 三态显示 scheme。
 * 各色 token 函数均以此枚举作为分支依据，禁止在 UI 层直接硬编码颜色值。
 */
enum class SWScheme { DAY, DARK, NIGHT }

/**
 * CompositionLocal：向下传递当前 [SWScheme]。
 * 默认值 [SWScheme.DAY]，Preview 与测试无需额外注入。
 */
val LocalSWScheme = compositionLocalOf { SWScheme.DAY }

/**
 * visualkit 主题容器——将 [SWScheme] 与 [LocalReduceMotion] 同时注入 Composition。
 *
 * 使用示例：
 * ```kotlin
 * SWTheme(scheme = SWScheme.DARK) {
 *     HomeScreen()
 * }
 * ```
 *
 * @param scheme 当前显示 scheme，通常由 ViewModel 或系统时间决定。
 * @param content 受主题约束的子 Composable 树。
 */
@Composable
fun SWTheme(scheme: SWScheme, content: @Composable () -> Unit) {
    val reduce = rememberReduceMotion()
    CompositionLocalProvider(
        LocalSWScheme provides scheme,
        LocalReduceMotion provides reduce,
        content = content
    )
}
