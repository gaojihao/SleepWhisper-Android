package com.lizhi1026.sleepwhisper.core.visualkit.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor
import com.lizhi1026.sleepwhisper.core.visualkit.SWFont
import com.lizhi1026.sleepwhisper.core.visualkit.SWGradient
import com.lizhi1026.sleepwhisper.core.visualkit.SWMotion
import com.lizhi1026.sleepwhisper.core.visualkit.SWRadius
import com.lizhi1026.sleepwhisper.core.visualkit.SWSpacing

enum class SoftButtonStyle { PRIMARY, ACCENT, GHOST }

/**
 * Pill-shaped CTA button — port of iOS Core/VisualKit/SoftButton.swift.
 * Three styles: PRIMARY (primary gradient), ACCENT (accent gradient), GHOST (transparent + border).
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
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = SWMotion.pressInMs),
        label = "soft-btn-scale"
    )
    val shape = RoundedCornerShape(SWRadius.pill)

    val bgModifier = when (style) {
        SoftButtonStyle.PRIMARY -> Modifier.background(SWGradient.primary(scheme))
        SoftButtonStyle.ACCENT -> Modifier.background(SWGradient.accent(scheme))
        SoftButtonStyle.GHOST -> Modifier.border(1.dp, SWColor.border(scheme), shape)
    }
    val textColor: Color = when (style) {
        SoftButtonStyle.PRIMARY, SoftButtonStyle.ACCENT -> Color.White
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
                indication = null,
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
            if (leadingIconRes != null) {
                Image(
                    painter = painterResource(id = leadingIconRes),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(textColor),
                    modifier = Modifier.size(20.dp)
                )
            }
            leadingIcon?.invoke()
            BasicText(
                text = text,
                style = SWFont.titleMD().copy(color = textColor, textAlign = TextAlign.Center)
            )
        }
    }
}
