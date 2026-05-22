package com.lizhi1026.sleepwhisper.core.visualkit.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lizhi1026.sleepwhisper.core.visualkit.LocalReduceMotion
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor
import com.lizhi1026.sleepwhisper.core.visualkit.SWFont
import com.lizhi1026.sleepwhisper.core.visualkit.SWGradient
import com.lizhi1026.sleepwhisper.core.visualkit.SWMotion
import com.lizhi1026.sleepwhisper.core.visualkit.SWRadius
import com.lizhi1026.sleepwhisper.core.visualkit.SWSpacing

/**
 * 按钮样式枚举，对应四种视觉风格。
 *
 * - [PRIMARY]：主渐变背景，用于主要操作（如"开始睡眠"）
 * - [ACCENT]：强调渐变背景，用于次级高亮操作
 * - [GHOST]：透明背景 + 边框，用于取消/辅助操作
 * - [HERO]：极光辉光 + 脉冲外发光阴影，用于首屏最核心 CTA
 */
enum class SoftButtonStyle { PRIMARY, ACCENT, GHOST, HERO }

/**
 * 胶囊形主要行动按钮（CTA），是 visualkit/components 层的核心交互组件。
 *
 * **视觉效果**：
 * - 按压时 0.97 缩放弹簧反馈（动效时长 [SWMotion.pressInMs]）
 * - HERO 样式：外发光阴影 alpha 在 0.35↔0.55 无限循环脉冲（周期 [SWMotion.breathCycleMs]）
 * - GHOST 样式：无背景，仅 1dp 边框
 *
 * **典型使用场景**：OnboardingScreen CTA、HomeScreen 主操作按钮、PlayerScreen 控制按钮
 *
 * **层级**：visualkit/components — 设计系统基础组件层
 *
 * @param text 按钮文字
 * @param onClick 点击回调
 * @param modifier 外部 Modifier
 * @param style 按钮视觉风格，见 [SoftButtonStyle]，默认 [SoftButtonStyle.PRIMARY]
 * @param enabled 是否可点击，false 时禁用但保留视觉样式
 * @param leadingIconRes 前置图标资源 ID（可选），使用 DrawableRes 染色为文字色
 * @param leadingIcon 前置自定义 Composable 图标（可选），与 [leadingIconRes] 二选一
 */
@Composable
fun SoftButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: SoftButtonStyle = SoftButtonStyle.PRIMARY,
    enabled: Boolean = true,
    @DrawableRes leadingIconRes: Int? = null,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    val scheme = LocalSWScheme.current
    val reduce = LocalReduceMotion.current
    val interaction = remember { MutableInteractionSource() }
    // 监听按压状态，用于驱动缩放动画
    val pressed by interaction.collectIsPressedAsState()
    // 按压缩放：按下 0.97f / 释放 1f，弹簧过渡时间 SWMotion.pressInMs
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = SWMotion.pressInMs),
        label = "soft-btn-scale"
    )
    val shape = RoundedCornerShape(SWRadius.pill)

    // HERO outer-glow pulse — computed at the composable scope so it can be applied
    // inside the bgModifier `when`. Falls back to a static mid-alpha under reduce-motion.
    // HERO 外发光脉冲：减弱动效模式下固定为 0.45f 中间值，避免频繁闪烁影响无障碍体验
    val heroGlowAlpha = if (style == SoftButtonStyle.HERO && !reduce) {
        val t = rememberInfiniteTransition(label = "hero-glow")
        val v by t.animateFloat(
            initialValue = 0.35f,
            targetValue = 0.55f,
            animationSpec = infiniteRepeatable(
                animation = tween(SWMotion.breathCycleMs, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha"
        )
        v
    } else 0.45f  // 减弱动效时静态外发光 alpha
    // HERO 发光颜色取当前主题 accent 色
    val heroAccent = SWColor.accent(scheme)

    // 根据样式选择背景 Modifier：PRIMARY/ACCENT 使用渐变填充，GHOST 仅边框，HERO 叠加外发光
    val bgModifier = when (style) {
        SoftButtonStyle.PRIMARY -> Modifier.background(SWGradient.primary(scheme))
        SoftButtonStyle.ACCENT -> Modifier.background(SWGradient.accent(scheme))
        SoftButtonStyle.GHOST -> Modifier.border(1.dp, SWColor.border(scheme), shape)
        SoftButtonStyle.HERO -> Modifier
            .shadow(
                elevation = 24.dp,
                shape = shape,
                clip = false,
                ambientColor = heroAccent.copy(alpha = heroGlowAlpha),
                spotColor = heroAccent.copy(alpha = heroGlowAlpha)
            )
            .background(SWGradient.auroraGlow(scheme))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    0f to Color.White.copy(alpha = 0.35f),
                    0.4f to Color.Transparent
                ),
                shape = shape
            )
    }
    // 文字颜色：实色背景样式用白色，GHOST 透明背景用主题主文字色
    val textColor: Color = when (style) {
        SoftButtonStyle.PRIMARY, SoftButtonStyle.ACCENT, SoftButtonStyle.HERO -> Color.White
        SoftButtonStyle.GHOST -> SWColor.textPrimary(scheme)
    }

    Box(
        modifier = modifier
            .scale(scale)
            .defaultMinSize(minHeight = 56.dp)
            .height(56.dp)
            .clip(shape)
            .then(bgModifier)
            .clickable(
                interactionSource = interaction,
                indication = null,  // 自定义按压效果，禁用系统默认水波纹
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = SWSpacing.xl),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SWSpacing.xs)
        ) {
            // 前置图标（DrawableRes 方式）：统一染色为按钮文字色
            if (leadingIconRes != null) {
                Image(
                    painter = painterResource(id = leadingIconRes),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(textColor),
                    modifier = Modifier.size(20.dp)
                )
            }
            // 前置自定义 Composable 图标（与 leadingIconRes 二选一）
            leadingIcon?.invoke()
            BasicText(
                text = text,
                style = SWFont.titleMD().copy(color = textColor, textAlign = TextAlign.Center)
            )
        }
    }
}
