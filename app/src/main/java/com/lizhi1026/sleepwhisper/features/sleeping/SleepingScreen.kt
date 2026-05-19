package com.lizhi1026.sleepwhisper.features.sleeping

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lizhi1026.sleepwhisper.R
import com.lizhi1026.sleepwhisper.core.audio.PlayerState
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor
import com.lizhi1026.sleepwhisper.core.visualkit.SWFont
import com.lizhi1026.sleepwhisper.core.visualkit.SWMotion
import com.lizhi1026.sleepwhisper.core.visualkit.SWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWSpacing
import com.lizhi1026.sleepwhisper.core.visualkit.components.LiquidProgressRing
import com.lizhi1026.sleepwhisper.core.visualkit.components.PulseRing
import com.lizhi1026.sleepwhisper.core.visualkit.components.SoftButton
import com.lizhi1026.sleepwhisper.core.visualkit.components.SoftButtonStyle
import com.lizhi1026.sleepwhisper.core.visualkit.components.Starfield
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SleepingScreen(vm: SleepingViewModel = hiltViewModel()) {
    val ongoing by vm.ongoingSleep.observeAsState(null)
    val playerState by vm.playerState.observeAsState(PlayerState.Idle)
    val baby by vm.baby.observeAsState(null)

    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) { nowMs = System.currentTimeMillis(); delay(1000) }
    }

    val elapsedSec = ongoing?.let { (nowMs - it.startAt) / 1000 } ?: 0L

    // Force DARK scheme for the immersive sleeping view, regardless of system.
    CompositionLocalProvider(LocalSWScheme provides SWScheme.DARK) {
        Box(modifier = Modifier.fillMaxSize().background(SWColor.surface(SWScheme.DARK))) {
            Starfield(modifier = Modifier.fillMaxSize(), density = 80)
            Column(
                modifier = Modifier.fillMaxSize().padding(SWSpacing.xl),
                verticalArrangement = Arrangement.spacedBy(SWSpacing.xxl, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BasicText(
                    text = stringResource(R.string.sleeping_title_protecting, baby?.name ?: ""),
                    style = SWFont.titleMD().copy(color = SWColor.textSecondary(SWScheme.DARK))
                )
                BasicText(
                    text = formatDuration(elapsedSec),
                    style = SWFont.displayXLTabular().copy(color = Color.White)
                )

                WakeButton(onWake = vm::onWake)

                if (playerState is PlayerState.Playing) {
                    SoftButton(
                        text = stringResource(R.string.sleeping_pause),
                        onClick = vm::pausePlayer,
                        style = SoftButtonStyle.GHOST
                    )
                } else if (playerState is PlayerState.Paused) {
                    SoftButton(
                        text = stringResource(R.string.sleeping_resume),
                        onClick = vm::resumePlayer,
                        style = SoftButtonStyle.GHOST
                    )
                }
            }
        }
    }
}

@Composable
private fun WakeButton(onWake: () -> Unit) {
    val fillProgress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitPointerEvent(PointerEventPass.Main)
                        if (down.changes.any { it.pressed }) {
                            // Begin filling
                            val job = scope.launch {
                                fillProgress.animateTo(
                                    targetValue = 1f,
                                    animationSpec = tween(durationMillis = SWMotion.longPressMs.toInt())
                                )
                                if (fillProgress.value >= 0.99f) onWake()
                            }
                            // Wait for release
                            while (true) {
                                val ev = awaitPointerEvent(PointerEventPass.Main)
                                if (ev.changes.all { !it.pressed }) {
                                    if (fillProgress.value < 0.99f) {
                                        job.cancel()
                                        scope.launch { fillProgress.animateTo(0f, tween(200)) }
                                    }
                                    break
                                }
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        PulseRing(color = SWColor.accent(SWScheme.DARK), radius = 100.dp)
        LiquidProgressRing(
            progress = fillProgress.value,
            color = SWColor.accent(SWScheme.DARK),
            radius = 80.dp
        )
        BasicText(
            text = stringResource(R.string.sleeping_wake_longpress),
            style = SWFont.bodyMD().copy(color = Color.White.copy(alpha = 0.85f))
        )
    }
}

private fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
