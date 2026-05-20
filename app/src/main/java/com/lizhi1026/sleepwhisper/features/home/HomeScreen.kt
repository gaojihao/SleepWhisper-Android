package com.lizhi1026.sleepwhisper.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lizhi1026.sleepwhisper.core.visualkit.components.SectionLabel
import androidx.hilt.navigation.compose.hiltViewModel
import com.lizhi1026.sleepwhisper.R
import com.lizhi1026.sleepwhisper.core.audio.PlayerState
import com.lizhi1026.sleepwhisper.core.strings.audioPresetNameKey
import com.lizhi1026.sleepwhisper.core.strings.greetingForHour
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.LocalHeroBackdropController
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor
import com.lizhi1026.sleepwhisper.core.visualkit.SWFont
import com.lizhi1026.sleepwhisper.core.visualkit.SWGradient
import com.lizhi1026.sleepwhisper.core.visualkit.SWRadius
import com.lizhi1026.sleepwhisper.core.visualkit.SWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWSpacing
import com.lizhi1026.sleepwhisper.core.visualkit.components.innerHighlight
import com.lizhi1026.sleepwhisper.core.visualkit.floatingY
import com.lizhi1026.sleepwhisper.core.visualkit.components.AudioWaveform
import com.lizhi1026.sleepwhisper.core.visualkit.components.AuroraBackdrop
import com.lizhi1026.sleepwhisper.core.visualkit.components.ChevronTrail
import com.lizhi1026.sleepwhisper.core.visualkit.components.ElevationLevel
import com.lizhi1026.sleepwhisper.core.visualkit.components.GlassCard
import com.lizhi1026.sleepwhisper.core.visualkit.components.Hairline
import com.lizhi1026.sleepwhisper.core.visualkit.components.PulseRing
import com.lizhi1026.sleepwhisper.core.visualkit.components.RollingNumber
import com.lizhi1026.sleepwhisper.core.visualkit.components.SoftButton
import com.lizhi1026.sleepwhisper.core.visualkit.components.SoftButtonStyle
import com.lizhi1026.sleepwhisper.model.DiaperEvent
import com.lizhi1026.sleepwhisper.model.FeedingEvent.FeedingMethod
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
fun HomeScreen(
    sharedScope: androidx.compose.animation.SharedTransitionScope,
    animScope: androidx.compose.animation.AnimatedVisibilityScope,
    onOpenPlayer: () -> Unit,
    vm: HomeViewModel = hiltViewModel()
) {
    val scheme = LocalSWScheme.current
    val baby by vm.baby.observeAsState(null)
    val wakeWindow by vm.cachedWakeWindow.observeAsState(null)
    val playerState by vm.playerState.observeAsState(PlayerState.Idle)
    val currentPreset by vm.currentPreset.observeAsState(null)
    val hasSeenHints by vm.hasSeenHints.observeAsState(false)

    val hour = remember { java.time.LocalTime.now().hour }

    val heroBackdrop = LocalHeroBackdropController.current
    LaunchedEffect(hour) {
        // Evening (19:00–05:59) — tint backdrop dusk-orange whisper. Otherwise clear.
        heroBackdrop.setAmbient(
            if (hour >= 19 || hour < 6) Color(0xFFFFB088) else null
        )
    }

    var showBottleSheet by remember { mutableStateOf(false) }
    var showSleepTypePicker by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        AuroraBackdrop()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SWSpacing.lg)
                .padding(top = SWSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(SWSpacing.lg)
        ) {
            GreetingSection(babyName = baby?.name, dobMs = baby?.dateOfBirth, hour = hour)

            if (!hasSeenHints) {
                OnboardingHintsCard(onDismiss = vm::onDismissHints)
            }

            wakeWindow?.let { (remaining, total) ->
                WakeWindowCard(remaining = remaining, total = total)
            }

            NowPlayingCard(
                presetIconName = currentPreset?.iconName,
                presetName = currentPreset?.let { stringResource(audioPresetNameKey(it.nameKey)) },
                isPlaying = playerState is PlayerState.Playing,
                onClick = onOpenPlayer
            )

            QuickActionsGrid(vm, onBottleTap = { showBottleSheet = true })

            SleepCTA(
                onTap = vm::onTapSleep,
                onLongPress = { showSleepTypePicker = true }
            )

            Spacer(Modifier.height(SWSpacing.huge))
        }

        if (showBottleSheet) {
            BottleAmountSheet(
                onConfirm = { ml -> vm.onRecordFeeding(FeedingMethod.BOTTLE, ml = ml) },
                onSkip = { vm.onRecordFeeding(FeedingMethod.BOTTLE, ml = null) },
                onDismiss = { showBottleSheet = false }
            )
        }
        if (showSleepTypePicker) {
            SleepTypePicker(
                suggestedType = vm.app.defaultSleepType(),
                onPick = { type -> vm.onPickSleepType(type) },
                onDismiss = { showSleepTypePicker = false }
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Section composables
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun GreetingSection(babyName: String?, dobMs: Long?, hour: Int) {
    val scheme = LocalSWScheme.current
    val daysOld = remember(dobMs) {
        dobMs?.let {
            val birth = LocalDate.ofEpochDay(it / 86_400_000L)
            val today = LocalDate.now(ZoneId.systemDefault())
            ChronoUnit.DAYS.between(birth, today).toInt().coerceAtLeast(0)
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(SWSpacing.xs)) {
        BasicText(
            text = stringResource(greetingForHour(hour)),
            style = SWFont.serifItalic(22).copy(color = SWColor.textSecondary(scheme))
        )
        babyName?.let {
            BasicText(
                text = it,
                style = SWFont.titleXL().copy(color = SWColor.textPrimary(scheme))
            )
        }
        // Short hairline flourish — reads as a book-chapter rule, not a divider.
        Hairline(modifier = Modifier.width(60.dp).padding(top = SWSpacing.xs))
        daysOld?.let {
            BasicText(
                text = stringResource(R.string.home_daycount, it).uppercase(),
                style = SWFont.labelMD().copy(color = SWColor.textTertiary(scheme))
            )
        }
    }
}

@Composable
private fun WakeWindowCard(remaining: Int, total: Int) {
    val scheme = LocalSWScheme.current
    val color = when {
        remaining > 20 -> SWColor.textPrimary(scheme)
        remaining > 0 -> SWColor.warning(scheme)
        else -> SWColor.danger(scheme)
    }
    GlassCard(
        modifier = Modifier.fillMaxWidth().floatingY(),
        contentPadding = PaddingValues(SWSpacing.xl),
        elevation = ElevationLevel.HERO,
        hero = true
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(SWSpacing.sm)) {
            SectionLabel(stringResource(R.string.home_wakewindow_title))
            Row(verticalAlignment = Alignment.Bottom) {
                RollingNumber(
                    value = remaining,
                    style = SWFont.displayLGTabular().copy(color = color)
                )
                Spacer(Modifier.width(SWSpacing.xs))
                BasicText(
                    text = stringResource(R.string.home_wakewindow_unit),
                    style = androidx.compose.ui.text.TextStyle(
                        fontSize = 28.sp,
                        color = SWColor.textSecondary(scheme)
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(SWColor.border(scheme).copy(alpha = 0.3f))
            ) {
                val frac = (remaining.toFloat() / total.coerceAtLeast(1)).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(frac)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(SWGradient.auroraGlow(scheme))
                )
            }
        }
    }
}

@Composable
private fun NowPlayingCard(
    presetIconName: String?,
    presetName: String?,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val scheme = LocalSWScheme.current
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val presetIconRes = remember(presetIconName) {
        presetIconName?.let { name ->
            ctx.resources.getIdentifier(name, "drawable", ctx.packageName).takeIf { it != 0 }
        }
    }
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        contentPadding = PaddingValues(SWSpacing.md)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SWSpacing.md)
        ) {
            // Left icon circle (52dp) — preset icon if known, else "moon" placeholder
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        if (scheme == SWScheme.DAY) SWColor.softLilac(scheme)
                        else SWColor.primary(scheme)
                    )
                    .innerHighlight(cornerRadius = 26.dp),
                contentAlignment = Alignment.Center
            ) {
                val iconRes = presetIconRes ?: R.drawable.ic_empty_moon
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = iconRes),
                    contentDescription = null,
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                        if (scheme == SWScheme.DAY) SWColor.primary(scheme) else androidx.compose.ui.graphics.Color.White
                    ),
                    modifier = Modifier.size(24.dp)
                )
            }
            // Middle: title + (waveform when playing)
            Column(modifier = Modifier.weight(1f)) {
                BasicText(
                    text = presetName ?: stringResource(R.string.home_nowplaying_placeholder),
                    style = SWFont.titleMD().copy(color = SWColor.textPrimary(scheme))
                )
                if (isPlaying) {
                    AudioWaveform(
                        modifier = Modifier.height(18.dp).padding(top = 4.dp),
                        isPlaying = true,
                        color = SWColor.accent(scheme),
                        barCount = 24
                    )
                }
            }
            // Right: ChevronTrail replaces the previous static chevron icon
            ChevronTrail()
        }
    }
}

@Composable
private fun QuickActionsGrid(vm: HomeViewModel, onBottleTap: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(SWSpacing.sm)) {
        Row(horizontalArrangement = Arrangement.spacedBy(SWSpacing.sm)) {
            QuickActionTile(
                label = stringResource(R.string.quickaction_breastleft),
                tint = TileTint.PEACH,
                iconRes = R.drawable.ic_action_breast_left,
                onTap = { vm.onRecordFeeding(FeedingMethod.BREAST_LEFT) },
                modifier = Modifier.weight(1f)
            )
            QuickActionTile(
                label = stringResource(R.string.quickaction_breastright),
                tint = TileTint.PEACH,
                iconRes = R.drawable.ic_action_breast_right,
                onTap = { vm.onRecordFeeding(FeedingMethod.BREAST_RIGHT) },
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(SWSpacing.sm)) {
            QuickActionTile(
                label = stringResource(R.string.quickaction_bottle),
                tint = TileTint.LILAC,
                iconRes = R.drawable.ic_action_bottle,
                onTap = onBottleTap,
                onLongPress = { vm.onRecordFeeding(FeedingMethod.BOTTLE, ml = null) },
                modifier = Modifier.weight(1f)
            )
            QuickActionTile(
                label = stringResource(R.string.quickaction_diaper),
                tint = TileTint.MINT,
                iconRes = R.drawable.ic_action_diaper,
                onTap = { vm.onRecordDiaper(DiaperEvent.DiaperType.WET) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SleepCTA(onTap: () -> Unit, onLongPress: () -> Unit) {
    val scheme = LocalSWScheme.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = SWSpacing.md),
        contentAlignment = Alignment.Center
    ) {
        PulseRing(
            color = SWColor.accent(scheme),
            radius = 110.dp,
            // STRONG on every scheme — iOS-equivalent visibility per real-device feedback;
            // SOFT was too dim against the warm-orange CTA on light backgrounds.
            intensity = com.lizhi1026.sleepwhisper.core.visualkit.components.PulseIntensity.STRONG
        )
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .fillMaxWidth()
                .pointerInput(onTap, onLongPress) {
                    detectTapGestures(
                        onLongPress = { onLongPress() },
                        onTap = { onTap() }
                    )
                }
        ) {
            SoftButton(
                text = stringResource(R.string.home_sleepcta),
                onClick = onTap,
                style = SoftButtonStyle.ACCENT,
                leadingIconRes = R.drawable.ic_moon_zzz,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
