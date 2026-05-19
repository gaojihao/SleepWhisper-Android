package com.lizhi1026.sleepwhisper.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.lizhi1026.sleepwhisper.R
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor
import com.lizhi1026.sleepwhisper.core.visualkit.SWFont
import com.lizhi1026.sleepwhisper.core.visualkit.SWGradient
import com.lizhi1026.sleepwhisper.core.visualkit.SWRadius
import com.lizhi1026.sleepwhisper.core.visualkit.SWSpacing
import com.lizhi1026.sleepwhisper.core.visualkit.components.ElevationLevel
import com.lizhi1026.sleepwhisper.core.visualkit.components.GlassCard
import com.lizhi1026.sleepwhisper.core.visualkit.components.SoftButton
import com.lizhi1026.sleepwhisper.core.visualkit.components.SoftButtonStyle

/**
 * Bottom overlay for entering bottle feeding amount — port of iOS BottleAmountSheet.swift.
 * - Two adjuster buttons (-10 / +10 ml) instead of a Slider (no Material dep).
 * - 6 preset chips: 60/90/120/150/180/210 ml.
 * - Save -> onConfirm(amount); Skip -> onSkip() (no-amount feed).
 */
@Composable
fun BottleAmountSheet(
    initialAmount: Int = 120,
    onConfirm: (Int) -> Unit,
    onSkip: () -> Unit,
    onDismiss: () -> Unit
) {
    val scheme = LocalSWScheme.current
    var amount by remember { mutableIntStateOf(initialAmount.coerceIn(30, 300)) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SWSpacing.md)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                ),
            elevation = ElevationLevel.STRONG
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(SWSpacing.md)) {
                BasicText(
                    text = stringResource(R.string.bottle_title),
                    style = SWFont.titleMD().copy(color = SWColor.textPrimary(scheme))
                )

                BasicText(
                    text = "$amount ml",
                    style = SWFont.displayMD().copy(color = SWColor.primary(scheme))
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(SWSpacing.sm)
                ) {
                    SoftButton(
                        text = "-10",
                        onClick = { amount = (amount - 10).coerceAtLeast(30) },
                        style = SoftButtonStyle.GHOST,
                        modifier = Modifier.weight(1f)
                    )
                    SoftButton(
                        text = "+10",
                        onClick = { amount = (amount + 10).coerceAtMost(300) },
                        style = SoftButtonStyle.GHOST,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(SWSpacing.xs)
                ) {
                    listOf(60, 90, 120, 150, 180, 210).forEach { preset ->
                        val active = preset == amount
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(SWRadius.pill))
                                .background(
                                    if (active) SWGradient.accent(scheme)
                                    else androidx.compose.ui.graphics.Brush.linearGradient(
                                        listOf(SWColor.surfaceSunken(scheme), SWColor.surfaceSunken(scheme))
                                    )
                                )
                                .clickable { amount = preset }
                                .padding(vertical = SWSpacing.sm),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicText(
                                text = preset.toString(),
                                style = SWFont.labelMD().copy(
                                    color = if (active) Color.White else SWColor.textSecondary(scheme)
                                )
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(SWSpacing.sm)
                ) {
                    SoftButton(
                        text = stringResource(R.string.bottle_skip),
                        onClick = { onSkip(); onDismiss() },
                        style = SoftButtonStyle.GHOST,
                        modifier = Modifier.weight(1f)
                    )
                    SoftButton(
                        text = stringResource(R.string.bottle_save),
                        onClick = { onConfirm(amount); onDismiss() },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
