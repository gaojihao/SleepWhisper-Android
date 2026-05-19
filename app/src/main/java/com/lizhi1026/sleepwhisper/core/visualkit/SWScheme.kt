package com.lizhi1026.sleepwhisper.core.visualkit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf

enum class SWScheme { DAY, DARK, NIGHT }

val LocalSWScheme = compositionLocalOf { SWScheme.DAY }

/** Provides [SWScheme] to descendants. Pair with a ThemeProvider observation upstream. */
@Composable
fun SWTheme(scheme: SWScheme, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalSWScheme provides scheme, content = content)
}
