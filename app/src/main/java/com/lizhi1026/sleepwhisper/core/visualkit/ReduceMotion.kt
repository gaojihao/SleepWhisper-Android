package com.lizhi1026.sleepwhisper.core.visualkit

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/**
 * True when the user has reduced animation duration to 0 in system Accessibility settings.
 * Composables and decorators should bypass infinite/transition animations when this is true.
 *
 * Default value is false; the actual value is computed once by [rememberReduceMotion]
 * and provided via [LocalReduceMotion] at the root of the composition tree.
 */
val LocalReduceMotion = compositionLocalOf { false }

@Composable
fun rememberReduceMotion(): Boolean {
    val ctx = LocalContext.current
    return remember(ctx) {
        runCatching {
            Settings.Global.getFloat(
                ctx.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        }.getOrDefault(false)
    }
}
