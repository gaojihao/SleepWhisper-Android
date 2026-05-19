package com.lizhi1026.sleepwhisper.core.visualkit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor
import com.lizhi1026.sleepwhisper.core.visualkit.SWGradient
import com.lizhi1026.sleepwhisper.core.visualkit.SWRadius
import com.lizhi1026.sleepwhisper.core.visualkit.SWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWShadow
import com.lizhi1026.sleepwhisper.core.visualkit.SWSpacing

enum class ElevationLevel { SOFT, MEDIUM, STRONG }

/**
 * Glass-effect card — port of iOS Core/VisualKit/GlassCard.swift.
 * DAY: solid white surface + 1.dp light border.
 * DARK/NIGHT: surface + cardOverlay gradient as inner highlight.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = SWRadius.lg,
    contentPadding: PaddingValues = PaddingValues(SWSpacing.md),
    elevation: ElevationLevel = ElevationLevel.SOFT,
    content: @Composable BoxScope.() -> Unit
) {
    val scheme = LocalSWScheme.current
    val spec = when (elevation) {
        ElevationLevel.SOFT -> SWShadow.soft(scheme)
        ElevationLevel.MEDIUM -> SWShadow.medium(scheme)
        ElevationLevel.STRONG -> SWShadow.strong(scheme)
    }
    val shape = RoundedCornerShape(cornerRadius)
    val borderMod = if (scheme == SWScheme.DAY) {
        Modifier.border(1.dp, SWColor.border(scheme), shape)
    } else Modifier
    Box(
        modifier = modifier
            .shadow(spec.radius, shape = shape, clip = false, ambientColor = spec.color, spotColor = spec.color)
            .clip(shape)
            .background(SWColor.surfaceElevated(scheme))
            .then(borderMod)
    ) {
        if (scheme != SWScheme.DAY) {
            Box(Modifier.fillMaxSize().background(SWGradient.cardOverlay(scheme)))
        }
        Box(Modifier.padding(contentPadding), content = content)
    }
}
