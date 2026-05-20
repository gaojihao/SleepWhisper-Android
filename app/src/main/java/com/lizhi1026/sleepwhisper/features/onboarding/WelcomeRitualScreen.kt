package com.lizhi1026.sleepwhisper.features.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lizhi1026.sleepwhisper.R
import com.lizhi1026.sleepwhisper.app.AppStateContainer
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor
import com.lizhi1026.sleepwhisper.core.visualkit.SWFont
import com.lizhi1026.sleepwhisper.core.visualkit.SWGradient
import com.lizhi1026.sleepwhisper.core.visualkit.SWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWSpacing
import com.lizhi1026.sleepwhisper.core.visualkit.components.AuroraBackdrop
import com.lizhi1026.sleepwhisper.core.visualkit.components.Starfield
import com.lizhi1026.sleepwhisper.model.Baby
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 4-stage welcome ritual — port of iOS WelcomeRitualView.swift.
 * Timing (in ms): 0 → ring expands 80→220, +600 greeting in, +1400 message in,
 * +2600 auto-dismiss. Tap anywhere advances to dismiss after stage>=3.
 */
@Composable
fun WelcomeRitualScreen(app: AppStateContainer) {
    val scheme = LocalSWScheme.current
    val baby by app.baby.observeAsState(null)
    val scope = rememberCoroutineScope()

    var stage by remember { mutableIntStateOf(0) }
    var finished by remember { androidx.compose.runtime.mutableStateOf(false) }
    val ringSize = remember { Animatable(80f) }
    val ringOpacity = remember { Animatable(0f) }

    fun finishOnce() {
        if (finished) return
        finished = true
        app.dismissWelcome()
    }

    LaunchedEffect(Unit) {
        scope.launch {
            stage = 1
            ringSize.animateTo(220f, tween(durationMillis = 700, easing = EaseOut))
        }
        scope.launch {
            ringOpacity.animateTo(1f, tween(durationMillis = 700, easing = EaseOut))
        }
        delay(600)
        stage = 2
        delay(800)
        stage = 3
        delay(1200)
        finishOnce()
    }

    val isPurpleHero = scheme == SWScheme.DAY
    val ringStrokeColor = if (isPurpleHero) Color(0.45f, 0.36f, 0.85f) else SWColor.accent(scheme)
    val heroSolidColor = if (isPurpleHero) SWColor.softLilac(scheme) else SWColor.accent(scheme)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { if (stage >= 3) finishOnce() }
            )
    ) {
        AuroraBackdrop()
        Starfield(modifier = Modifier.fillMaxSize(), density = 60)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = SWSpacing.xl)
                .padding(bottom = SWSpacing.huge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SWSpacing.lg)
        ) {
            Spacer(Modifier.weight(1f))

            Box(contentAlignment = Alignment.Center) {
                // expanding ring (stage >= 1)
                Box(
                    modifier = Modifier
                        .size(ringSize.value.dp)
                        .border(
                            width = 1.dp,
                            color = ringStrokeColor.copy(alpha = 0.30f * ringOpacity.value),
                            shape = CircleShape
                        )
                )
                // hero solid circle (always rendered)
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .background(heroSolidColor, CircleShape)
                )
            }

            // Stage 2: greeting + baby name
            AnimatedVisibility(visible = stage >= 2, enter = fadeIn(tween(600))) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    BasicText(
                        text = stringResource(R.string.welcome_greeting),
                        style = SWFont.serifItalic(17).copy(
                            color = SWColor.textPrimary(scheme).copy(alpha = 0.85f)
                        )
                    )
                    Spacer(Modifier.height(SWSpacing.xs))
                    BasicText(
                        text = baby?.name ?: "",
                        style = androidx.compose.ui.text.TextStyle(
                            fontSize = 44.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SWColor.primary(scheme)
                        )
                    )
                }
            }

            // Stage 3: gender-keyed message + tap hint
            AnimatedVisibility(visible = stage >= 3, enter = fadeIn(tween(600))) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    BasicText(
                        text = stringResource(messageKeyForGender(baby?.gender)),
                        style = SWFont.bodyMD().copy(
                            color = SWColor.textSecondary(scheme),
                            textAlign = TextAlign.Center
                        )
                    )
                    Spacer(Modifier.height(SWSpacing.md))
                    BasicText(
                        text = stringResource(R.string.welcome_taptocontinue),
                        style = SWFont.labelSM().copy(
                            color = SWColor.textTertiary(scheme).copy(alpha = 0.6f)
                        )
                    )
                }
            }

            Spacer(Modifier.weight(1f))
        }
    }
}

private fun messageKeyForGender(g: Baby.BabyGender?): Int = when (g) {
    Baby.BabyGender.FEMALE -> R.string.welcome_message_her
    Baby.BabyGender.MALE -> R.string.welcome_message_him
    else -> R.string.welcome_message_baby
}
