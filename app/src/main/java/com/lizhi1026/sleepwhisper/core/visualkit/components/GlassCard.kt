package com.lizhi1026.sleepwhisper.core.visualkit.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lizhi1026.sleepwhisper.core.visualkit.LocalReduceMotion
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor
import com.lizhi1026.sleepwhisper.core.visualkit.SWGradient
import com.lizhi1026.sleepwhisper.core.visualkit.SWMotion
import com.lizhi1026.sleepwhisper.core.visualkit.SWRadius
import com.lizhi1026.sleepwhisper.core.visualkit.SWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWShadow
import com.lizhi1026.sleepwhisper.core.visualkit.SWSpacing

enum class ElevationLevel { SOFT, MEDIUM, STRONG, HERO }

/**
 * Moonlit-glass card. Aurora evolution of the prior GlassCard:
 *   - DARK / NIGHT: surface + moon-halo radial top-left + aurora-glow whisper bottom-right
 *   - DAY: surface + 1dp border + subtle inner highlight gradient (top 30% slightly brighter)
 *   - All schemes: dual-layer shadow (close tight + distant wide) for cinematic depth
 *   - `hero = true` adds a continuous subtle breath scale (0.998 ↔ 1.002) at SWMotion.breathCycleMs
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = SWRadius.lg,
    contentPadding: PaddingValues = PaddingValues(SWSpacing.md),
    elevation: ElevationLevel = ElevationLevel.SOFT,
    hero: Boolean = false,
    surfaceTintOverride: Color? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val scheme = LocalSWScheme.current
    val reduce = LocalReduceMotion.current
    val shape = RoundedCornerShape(cornerRadius)

    val close = SWShadow.soft(scheme)
    val distant = when (elevation) {
        ElevationLevel.SOFT -> SWShadow.soft(scheme)
        ElevationLevel.MEDIUM -> SWShadow.medium(scheme)
        ElevationLevel.STRONG -> SWShadow.strong(scheme)
        ElevationLevel.HERO -> SWShadow.Spec(
            color = SWShadow.strong(scheme).color,
            radius = SWShadow.strong(scheme).radius + 16.dp,
            y = SWShadow.strong(scheme).y + 4.dp
        )
    }

    val breathScale = if (hero && !reduce) {
        val t = rememberInfiniteTransition(label = "card-breath")
        val v by t.animateFloat(
            initialValue = 0.998f,
            targetValue = 1.002f,
            animationSpec = infiniteRepeatable(
                animation = tween(SWMotion.breathCycleMs, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )
        v
    } else 1f

    val borderMod = if (scheme == SWScheme.DAY) {
        Modifier.border(1.dp, SWColor.border(scheme), shape)
    } else Modifier

    Box(
        modifier = modifier
            .scale(breathScale)
            // dual-layer shadow — close tight + distant wide for cinematic depth
            .shadow(close.radius, shape, clip = false, ambientColor = close.color, spotColor = close.color)
            .shadow(distant.radius, shape, clip = false, ambientColor = distant.color, spotColor = distant.color)
            .clip(shape)
            .background(surfaceTintOverride ?: SWColor.surfaceElevated(scheme))
            .then(borderMod)
    ) {
        if (scheme != SWScheme.DAY) {
            // Moon halo top-left + aurora glow whisper bottom-right.
            // The glow is dimmed via Modifier.alpha because Compose's Brush has no
            // built-in alpha multiplier — the wrapping Box scales the gradient down
            // to a whisper without changing the gradient's stop colors.
            Box(Modifier.matchParentSize().background(SWGradient.moonHalo(scheme)))
            Box(
                Modifier
                    .matchParentSize()
                    .alpha(0.06f)
                    .background(SWGradient.auroraGlow(scheme))
            )
        } else {
            // DAY: inner highlight — top 30% slightly brighter
            Box(
                Modifier.matchParentSize().background(
                    Brush.verticalGradient(
                        0f to Color.White.copy(alpha = 0.5f),
                        0.3f to Color.Transparent,
                        1f to Color.Transparent
                    )
                )
            )
        }
        Box(Modifier.padding(contentPadding), content = content)
    }
}
