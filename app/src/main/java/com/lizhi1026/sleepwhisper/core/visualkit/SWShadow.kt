package com.lizhi1026.sleepwhisper.core.visualkit

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Shadow tokens — direct port of iOS Core/VisualKit/Gradients.swift SWShadow. */
object SWShadow {
    data class Spec(val color: Color, val radius: Dp, val y: Dp)

    fun soft(scheme: SWScheme): Spec =
        Spec(Color.Black.copy(alpha = if (scheme == SWScheme.DAY) 0.04f else 0.25f), 8.dp, 2.dp)

    fun medium(scheme: SWScheme): Spec =
        Spec(Color.Black.copy(alpha = if (scheme == SWScheme.DAY) 0.05f else 0.30f), 16.dp, 6.dp)

    fun strong(scheme: SWScheme): Spec =
        Spec(Color.Black.copy(alpha = if (scheme == SWScheme.DAY) 0.07f else 0.40f), 32.dp, 12.dp)

    fun glow(tint: Color): Spec =
        Spec(tint.copy(alpha = 0.35f), 40.dp, 0.dp)
}
