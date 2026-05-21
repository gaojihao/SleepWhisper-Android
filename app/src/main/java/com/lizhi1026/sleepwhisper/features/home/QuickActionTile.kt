package com.lizhi1026.sleepwhisper.features.home

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor
import com.lizhi1026.sleepwhisper.core.visualkit.SWFont
import com.lizhi1026.sleepwhisper.core.visualkit.SWMotion
import com.lizhi1026.sleepwhisper.core.visualkit.SWSpacing
import com.lizhi1026.sleepwhisper.core.visualkit.components.ElevationLevel
import com.lizhi1026.sleepwhisper.core.visualkit.components.GlassCard
import com.lizhi1026.sleepwhisper.core.visualkit.components.innerHighlight

/** Tile tint — port of iOS QuickActionTile tint cases. */
enum class TileTint { PEACH, MINT, LILAC }

/**
 * Square quick-action tile — port of iOS Features/Home/QuickActionTile.swift.
 *
 * Layout: 40dp tinted icon circle + labelMD, vertically stacked inside a GlassCard.
 * Wraps content height so the label is never clipped (was previously fixed at 70dp +
 * 16dp padding, which only left 38dp for the 44dp icon and pushed the label off-screen).
 */
@Composable
fun QuickActionTile(
    label: String,
    tint: TileTint,
    @DrawableRes iconRes: Int,
    onTap: () -> Unit,
    onLongPress: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scheme = LocalSWScheme.current
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(SWMotion.pressInMs),
        label = "tile-scale"
    )
    val tintBg = when (tint) {
        TileTint.PEACH -> SWColor.softPeach(scheme)
        TileTint.MINT -> SWColor.softMint(scheme)
        TileTint.LILAC -> SWColor.softLilac(scheme)
    }
    val iconTint = when (tint) {
        TileTint.PEACH -> SWColor.danger(scheme)    // warm coral, reads well over peach
        TileTint.MINT -> SWColor.success(scheme)
        TileTint.LILAC -> SWColor.primary(scheme)
    }

    GlassCard(
        modifier = modifier
            .scale(scale)
            .semantics(mergeDescendants = true) {
                contentDescription = label
                role = Role.Button
                onClick(label = null) { onTap(); true }
                if (onLongPress != null) {
                    onLongClick(label = null) { onLongPress(); true }
                }
            }
            .pointerInput(onLongPress, onTap) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    },
                    onLongPress = if (onLongPress != null) { _ -> onLongPress() } else null,
                    onTap = { onTap() }
                )
            },
        contentPadding = PaddingValues(SWSpacing.xs),
        elevation = ElevationLevel.SOFT
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SWSpacing.xxs)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(tintBg)
                    .innerHighlight(cornerRadius = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(iconTint),
                    modifier = Modifier.size(18.dp)
                )
            }
            BasicText(
                text = label,
                style = SWFont.labelMD().copy(
                    color = SWColor.textPrimary(scheme),
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}
