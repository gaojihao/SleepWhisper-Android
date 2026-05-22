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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.unit.dp
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
 * 奶瓶喂养计量底部弹层（features/home 层）
 *
 * 职责：
 * - 让用户输入本次奶瓶喂养的毫升数，支持 -10/+10 微调按钮与 6 个快捷预设（60/90/120/150/180/210 ml）。
 * - 确认后回调 [onConfirm] 携带毫升数；跳过时回调 [onSkip]（毫升数为 null）；
 *   点击背景遮罩或调用 [onDismiss] 关闭弹层。
 * - 不持有 ViewModel，纯 UI 组件，由 HomeScreen 通过回调完成数据写入。
 *
 * 对应 iOS BottleAmountSheet.swift，用两个按钮替代 Slider（避免引入 Material 依赖）。
 */
@Composable
fun BottleAmountSheet(
    initialAmount: Int = 120,
    onConfirm: (Int) -> Unit,
    onSkip: () -> Unit,
    onDismiss: () -> Unit
) {
    val scheme = LocalSWScheme.current
    // 当前奶量，限制在 30~300 ml 范围内
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
                com.lizhi1026.sleepwhisper.core.visualkit.components.DragHandle(
                    modifier = Modifier.align(androidx.compose.ui.Alignment.CenterHorizontally)
                )
                BasicText(
                    text = stringResource(R.string.bottle_title),
                    style = SWFont.serif(22).copy(color = SWColor.textSecondary(scheme))
                )

                Row(verticalAlignment = androidx.compose.ui.Alignment.Bottom) {
                    com.lizhi1026.sleepwhisper.core.visualkit.components.RollingNumber(
                        value = amount,
                        style = SWFont.displayLGTabular().copy(color = SWColor.primary(scheme))
                    )
                    androidx.compose.foundation.layout.Spacer(Modifier.width(4.dp))
                    BasicText(
                        text = "ml",
                        style = SWFont.titleMD().copy(color = SWColor.textSecondary(scheme)),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(SWSpacing.sm)
                ) {
                    // -10 按钮：最小值 30 ml
                    SoftButton(
                        text = "-10",
                        onClick = { amount = (amount - 10).coerceAtLeast(30) },
                        style = SoftButtonStyle.GHOST,
                        modifier = Modifier.weight(1f)
                    )
                    // +10 按钮：最大值 300 ml
                    SoftButton(
                        text = "+10",
                        onClick = { amount = (amount + 10).coerceAtMost(300) },
                        style = SoftButtonStyle.GHOST,
                        modifier = Modifier.weight(1f)
                    )
                }

                // 快捷预设芯片行：选中项以 accent 渐变高亮
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
                    // 跳过：记录喂养事件但不填写毫升数
                    SoftButton(
                        text = stringResource(R.string.bottle_skip),
                        onClick = { onSkip(); onDismiss() },
                        style = SoftButtonStyle.GHOST,
                        modifier = Modifier.weight(1f)
                    )
                    // 保存：携带当前奶量回调并关闭弹层
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
