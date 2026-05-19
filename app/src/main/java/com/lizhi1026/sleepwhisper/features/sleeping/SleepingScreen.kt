package com.lizhi1026.sleepwhisper.features.sleeping

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lizhi1026.sleepwhisper.R
import com.lizhi1026.sleepwhisper.core.audio.PlayerState
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor
import com.lizhi1026.sleepwhisper.core.visualkit.SWFont
import com.lizhi1026.sleepwhisper.core.visualkit.SWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWSpacing
import com.lizhi1026.sleepwhisper.core.visualkit.components.PulseRing
import com.lizhi1026.sleepwhisper.core.visualkit.components.SoftButton
import com.lizhi1026.sleepwhisper.core.visualkit.components.SoftButtonStyle
import kotlinx.coroutines.delay

@Composable
fun SleepingScreen(vm: SleepingViewModel = hiltViewModel()) {
    val ongoing by vm.ongoingSleep.observeAsState(null)
    val playerState by vm.playerState.observeAsState(PlayerState.Idle)
    val baby by vm.baby.observeAsState(null)

    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) { nowMs = System.currentTimeMillis(); delay(1000) }
    }

    val elapsedSec = ongoing?.let { (nowMs - it.startAt) / 1000 } ?: 0

    Box(modifier = Modifier.fillMaxSize().background(SWColor.surface(SWScheme.DARK))) {
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
            Box(
                modifier = Modifier.pointerInput(Unit) {
                    detectTapGestures(onLongPress = { vm.onWake() })
                },
                contentAlignment = Alignment.Center
            ) {
                PulseRing(color = SWColor.accent(SWScheme.DARK), radius = 100.dp)
                BasicText(
                    text = stringResource(R.string.sleeping_wake_longpress),
                    style = SWFont.bodyMD().copy(color = SWColor.textSecondary(SWScheme.DARK))
                )
            }
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

private fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
