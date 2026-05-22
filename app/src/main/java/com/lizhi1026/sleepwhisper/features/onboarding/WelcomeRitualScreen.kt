/**
 * WelcomeRitualScreen.kt — 首次注册后的欢迎仪式页（UI 层 / features/onboarding）
 *
 * 用途：宝宝档案保存成功后，以 4 阶段动画序列向用户呈现沉浸式欢迎体验，然后自动
 *       过渡到主界面。背景为 NightSkyCanvas（星空）+ AuroraBackdrop（极光光晕）。
 * 视觉要素：auroraGlow 渐变 hero 圆 + 88sp DM Serif 宝宝名 + 性别化欢迎语。
 * 动画时序（ms）：0 → 光环扩张 80dp→220dp（700ms EaseOut），同步淡入；
 *                 +600 → stage 2：问候语 + 宝宝名渐显；
 *                 +800 → stage 3：性别化欢迎语 + "轻触继续"提示渐显；
 *                 +1200 → 自动调用 app.dismissWelcome() 完成仪式。
 * 用户交互：stage >= 3 时点击屏幕任意位置可提前关闭仪式（调用 [finishOnce]）。
 */
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lizhi1026.sleepwhisper.R
import com.lizhi1026.sleepwhisper.app.AppStateContainer
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor
import com.lizhi1026.sleepwhisper.core.visualkit.SWFont
import com.lizhi1026.sleepwhisper.core.visualkit.SWGradient
import com.lizhi1026.sleepwhisper.core.visualkit.SWSpacing
import com.lizhi1026.sleepwhisper.core.visualkit.components.AuroraBackdrop
import com.lizhi1026.sleepwhisper.core.visualkit.components.Hairline
import com.lizhi1026.sleepwhisper.core.visualkit.components.NightSkyCanvas
import com.lizhi1026.sleepwhisper.core.visualkit.components.innerHighlight
import com.lizhi1026.sleepwhisper.model.Baby
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 欢迎仪式全屏 Composable。
 *
 * 接收 [AppStateContainer] 直接作为参数（而非 ViewModel），因为本页是过渡性仪式页，
 * 无须独立 ViewModel，所有状态（baby、dismissWelcome）均来自全局容器。
 *
 * @param app 全局状态容器，提供 baby 观察与 dismissWelcome 回调
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

    // 防重复关闭：finished 标志确保 dismissWelcome() 只调用一次
    fun finishOnce() {
        if (finished) return
        finished = true
        app.dismissWelcome()
    }

    // 仪式动画时序编排：ring 扩张与淡入并行，随后按 stage 依次展示文案
    LaunchedEffect(Unit) {
        scope.launch {
            stage = 1  // stage 1：光环开始扩张
            ringSize.animateTo(220f, tween(durationMillis = 700, easing = EaseOut))
        }
        scope.launch {
            // 光环同步淡入，与尺寸动画并行
            ringOpacity.animateTo(1f, tween(durationMillis = 700, easing = EaseOut))
        }
        delay(600)
        stage = 2   // stage 2：问候语 + 宝宝名渐显
        delay(800)
        stage = 3   // stage 3：性别化欢迎语 + 轻触提示渐显
        delay(1200)
        finishOnce()  // 自动关闭仪式，过渡至主界面
    }

    val ringStrokeColor = SWColor.accentSecondary(scheme)

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
        NightSkyCanvas(modifier = Modifier.fillMaxSize(), morphProgress = 1f)
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
                // hero solid circle (always rendered) — Aurora-glow gradient with inner highlight
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(SWGradient.auroraGlow(scheme))
                        .innerHighlight(cornerRadius = 44.dp)
                )
            }

            // Stage 2: greeting + baby name
            AnimatedVisibility(visible = stage >= 2, enter = fadeIn(tween(600))) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    BasicText(
                        text = stringResource(R.string.welcome_greeting),
                        style = SWFont.serif(22).copy(
                            color = SWColor.textPrimary(scheme).copy(alpha = 0.85f)
                        )
                    )
                    Spacer(Modifier.height(SWSpacing.xs))
                    BasicText(
                        text = baby?.name ?: "",
                        style = SWFont.cjkAware(
                            SWFont.displayXL().copy(color = SWColor.textPrimary(scheme))
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Visible
                    )
                    Spacer(Modifier.height(SWSpacing.xs))
                    Hairline(modifier = Modifier.width(60.dp))
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

/**
 * 根据宝宝性别返回对应的欢迎语字符串资源 ID。
 *
 * FEMALE → welcome_message_her；MALE → welcome_message_him；其他 → welcome_message_baby
 */
private fun messageKeyForGender(g: Baby.BabyGender?): Int = when (g) {
    Baby.BabyGender.FEMALE -> R.string.welcome_message_her
    Baby.BabyGender.MALE -> R.string.welcome_message_him
    else -> R.string.welcome_message_baby
}
