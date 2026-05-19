package com.lizhi1026.sleepwhisper.features.sleeping

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lizhi1026.sleepwhisper.R
import com.lizhi1026.sleepwhisper.core.audio.PlayerState
import com.lizhi1026.sleepwhisper.core.strings.audioPresetNameKey
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor
import com.lizhi1026.sleepwhisper.core.visualkit.SWFont
import com.lizhi1026.sleepwhisper.core.visualkit.SWGradient
import com.lizhi1026.sleepwhisper.core.visualkit.SWMotion
import com.lizhi1026.sleepwhisper.core.visualkit.SWRadius
import com.lizhi1026.sleepwhisper.core.visualkit.SWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWSpacing
import com.lizhi1026.sleepwhisper.core.visualkit.components.AudioWaveform
import com.lizhi1026.sleepwhisper.core.visualkit.components.ElevationLevel
import com.lizhi1026.sleepwhisper.core.visualkit.components.GlassCard
import com.lizhi1026.sleepwhisper.core.visualkit.components.LiquidProgressRing
import com.lizhi1026.sleepwhisper.core.visualkit.components.PulseRing
import com.lizhi1026.sleepwhisper.core.visualkit.components.Starfield
import com.lizhi1026.sleepwhisper.model.AudioPreset
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

    // Force DARK scheme regardless of system.
    CompositionLocalProvider(LocalSWScheme provides SWScheme.DARK) {
        Box(modifier = Modifier.fillMaxSize().background(SWColor.surface(SWScheme.DARK))) {
            Starfield(modifier = Modifier.fillMaxSize(), density = 80)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = SWSpacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.weight(0.5f))

                TitleSection(babyName = baby?.name ?: "")
                Spacer(Modifier.height(SWSpacing.xxl))

                CountdownSection(elapsedSec)
                Spacer(Modifier.height(SWSpacing.xxl))

                if (playerState is PlayerState.Playing) {
                    val ps = playerState as PlayerState.Playing
                    PlayerStatusCard(presetId = ps.presetId, endsAt = ps.endsAt, nowMs = nowMs)
                    Spacer(Modifier.height(SWSpacing.xxl))
                }

                WakeButton(onWake = vm::onWake)

                Spacer(Modifier.weight(1f))

                if (playerState is PlayerState.Playing) {
                    PausePill(label = stringResource(R.string.sleeping_pause), onClick = vm::pausePlayer)
                } else if (playerState is PlayerState.Paused) {
                    PausePill(label = stringResource(R.string.sleeping_resume), onClick = vm::resumePlayer)
                }
                Spacer(Modifier.height(SWSpacing.huge))
            }
        }
    }
}

@Composable
private fun TitleSection(babyName: String) {
    Column(
        verticalArrangement = Arrangement.spacedBy(SWSpacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BasicText(
            text = stringResource(R.string.sleeping_title_protecting, babyName),
            style = SWFont.serifItalic(17).copy(
                color = Color.White.copy(alpha = 0.95f),
                textAlign = TextAlign.Center,
                letterSpacing = 1.4.sp
            )
        )
        BasicText(
            text = babyName,
            style = SWFont.titleXL().copy(
                color = Color.White.copy(alpha = 0.95f),
                letterSpacing = 8.sp,
                textAlign = TextAlign.Center
            )
        )
        BasicText(
            text = stringResource(R.string.sleeping_title_dreaming),
            style = SWFont.bodyMD().copy(
                color = Color.White.copy(alpha = 0.5f),
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )
        )
    }
}

@Composable
private fun CountdownSection(elapsedSec: Long) {
    val h = (elapsedSec / 3600).toInt()
    val m = ((elapsedSec % 3600) / 60).toInt()
    val s = (elapsedSec % 60).toInt()
    val displayText = if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    val style = if (h > 0)
        SWFont.displayLargeTabular().copy(color = Color.White)   // 72pt
    else
        SWFont.displayXLTabular().copy(color = Color.White)      // 96pt
    BasicText(
        text = displayText,
        style = style,
        modifier = Modifier.padding(horizontal = 0.dp)
    )
}

@Composable
private fun PlayerStatusCard(presetId: String, endsAt: Long?, nowMs: Long) {
    val preset = remember(presetId) { AudioPreset.byId(presetId) }
    val scheme = SWScheme.DARK
    GlassCard(
        modifier = Modifier,
        cornerRadius = 18.dp,
        elevation = ElevationLevel.MEDIUM
    ) {
        androidx.compose.foundation.layout.Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SWSpacing.sm)
        ) {
            BasicText(
                text = preset?.nameKey?.let { stringResource(audioPresetNameKey(it)) } ?: "",
                style = SWFont.titleMD().copy(color = Color.White)
            )
            AudioWaveform(
                modifier = Modifier.width(60.dp).height(20.dp),
                isPlaying = true,
                color = SWColor.accent(scheme)
            )
            endsAt?.let {
                val remainingMin = ((it - nowMs) / 60_000).coerceAtLeast(0L)
                BasicText(
                    text = stringResource(R.string.sleeping_minremaining, remainingMin),
                    style = SWFont.labelMD().copy(color = Color.White.copy(alpha = 0.7f))
                )
            }
        }
    }
}

@Composable
private fun WakeButton(onWake: () -> Unit) {
    val fillProgress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val scheme = SWScheme.DARK

    Box(
        modifier = Modifier
            .size(180.dp)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitPointerEvent(PointerEventPass.Main)
                        if (down.changes.any { it.pressed }) {
                            val job = scope.launch {
                                fillProgress.animateTo(
                                    1f,
                                    tween(durationMillis = SWMotion.longPressMs.toInt())
                                )
                                if (fillProgress.value >= 0.99f) {
                                    delay(420)
                                    onWake()
                                }
                            }
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
        PulseRing(color = SWColor.accent(scheme), radius = 90.dp)
        LiquidProgressRing(
            progress = fillProgress.value,
            color = SWColor.accent(scheme),
            radius = 65.dp
        )
        Box(
            modifier = Modifier
                .size(112.dp)
                .clip(CircleShape)
                .background(SWGradient.accent(scheme)),
            contentAlignment = Alignment.Center
        ) {
            BasicText(
                text = stringResource(R.string.sleeping_wake_longpress),
                style = SWFont.labelMD().copy(
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}

@Composable
private fun PausePill(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(SWRadius.pill))
            .background(Color.White.copy(alpha = 0.06f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.18f),
                shape = RoundedCornerShape(SWRadius.pill)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = SWSpacing.lg, vertical = SWSpacing.sm)
    ) {
        BasicText(
            text = label,
            style = SWFont.bodyMD().copy(color = Color.White.copy(alpha = 0.7f))
        )
    }
}
