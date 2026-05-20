package com.lizhi1026.sleepwhisper.features.sleeping

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
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
import com.lizhi1026.sleepwhisper.core.visualkit.floatingY
import com.lizhi1026.sleepwhisper.core.visualkit.components.AudioWaveform
import com.lizhi1026.sleepwhisper.core.visualkit.components.ElevationLevel
import com.lizhi1026.sleepwhisper.core.visualkit.components.GlassCard
import com.lizhi1026.sleepwhisper.core.visualkit.components.LiquidProgressRing
import com.lizhi1026.sleepwhisper.core.visualkit.components.PulseRing
import com.lizhi1026.sleepwhisper.core.visualkit.components.NightSkyCanvas
import com.lizhi1026.sleepwhisper.core.visualkit.components.sleepHeroDestination
import com.lizhi1026.sleepwhisper.model.AudioPreset
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
fun SleepingScreen(
    sharedScope: androidx.compose.animation.SharedTransitionScope,
    animScope: androidx.compose.animation.AnimatedVisibilityScope,
    vm: SleepingViewModel = hiltViewModel()
) {
    val ongoing by vm.ongoingSleep.observeAsState(null)
    val playerState by vm.playerState.observeAsState(PlayerState.Idle)
    val baby by vm.baby.observeAsState(null)

    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    // Tick only while the screen is at least STARTED — when the user navigates away or
    // the system backgrounds the app, the coroutine is cancelled and the per-second
    // recomposition stops. repeatOnLifecycle re-launches it when we resume.
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                nowMs = System.currentTimeMillis()
                delay(1000)
            }
        }
    }

    val elapsedSec = ongoing?.let { (nowMs - it.startAt) / 1000 } ?: 0L

    // Force DARK scheme regardless of system. Sleeping is an immersive cinematic surface
    // designed against the dark palette (deep blue starfield, cool glows) — overriding it
    // with the NIGHT red-light scheme changes the visual identity completely.
    CompositionLocalProvider(LocalSWScheme provides SWScheme.DARK) {
        val morphProgress by remember(vm.app.morphProgress) {
            derivedStateOf { vm.app.morphProgress.value }
        }
        val morphScope = rememberCoroutineScope()
        Box(modifier = Modifier.fillMaxSize().background(SWColor.surface(SWScheme.DARK))) {
            // Star density ramps with morph progress on entry. After the morph settles
            // (morphProgress = 1), density stays at the full 80. Phase 2 replaces this
            // with NightSkyCanvas.
            NightSkyCanvas(
                modifier = Modifier.fillMaxSize(),
                morphProgress = morphProgress
            )

            // Invisible 600dp halo placeholder — anchors the shared element to its
            // destination position so the Sleep CTA's morph has somewhere to fly to.
            // Drawn first (under the timer column) so the timer text remains visible.
            Box(
                modifier = Modifier
                    .size(600.dp)
                    .align(Alignment.Center)
                    .sleepHeroDestination(sharedScope, animScope)
            )

            // Anchor the whole stack to the vertical center of the usable area. Earlier
            // revisions used Spacer(weight 0.5f)/weight(1f) to position the WakeButton at
            // roughly 1/3 from top — but when the player is Playing the layout adds a
            // PlayerStatusCard (~64dp) + a 32dp spacer above the button, and on smaller
            // heights (landscape, multi-window, large font scale) the weighted spacers
            // collapse to 0 and the 180dp WakeButton was getting pushed off the bottom of
            // the screen. The only visible remnant was the PulseRing's outward-expanding
            // arcs (drawn up to 1.8x the canvas size, so they leak beyond the WakeButton
            // box), producing the "wake button is gone, ripples at the bottom" symptom.
            // CenterVertically keeps the WakeButton anchored near the screen center no
            // matter how tall the content above grows; windowInsetsPadding ensures the
            // edge-to-edge background still applies while keeping content out from under
            // the status and navigation bars.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .padding(horizontal = SWSpacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(SWSpacing.xxl, Alignment.CenterVertically)
            ) {
                TitleSection(babyName = baby?.name ?: "")

                CountdownSection(elapsedSec)

                if (playerState is PlayerState.Playing) {
                    val ps = playerState as PlayerState.Playing
                    PlayerStatusCard(presetId = ps.presetId, endsAt = ps.endsAt, nowMs = nowMs)
                }

                WakeButton(
                    onWake = {
                        morphScope.launch { vm.app.endSleepMorph() }
                        vm.onWake()
                    }
                )

                if (playerState is PlayerState.Playing) {
                    PausePill(label = stringResource(R.string.sleeping_pause), onClick = vm::pausePlayer)
                } else if (playerState is PlayerState.Paused) {
                    PausePill(label = stringResource(R.string.sleeping_resume), onClick = vm::resumePlayer)
                }
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
    val moonlitSilver = Color(0xFFECF2F8)
    val style = if (h > 0)
        SWFont.displayLGTabular().copy(color = moonlitSilver)   // 64sp (fits in 280dp ring)
    else
        SWFont.displayXLTabular().copy(color = moonlitSilver)   // 96sp (only used briefly for MM:SS)
    BasicText(
        text = displayText,
        style = style,
        modifier = Modifier
            .padding(horizontal = 0.dp)
            .floatingY(amplitude = 2.dp, durationMillis = 8000)
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
                .background(SWGradient.auroraGlow(scheme)),
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
