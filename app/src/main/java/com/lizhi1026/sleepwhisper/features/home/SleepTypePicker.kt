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
import androidx.compose.runtime.remember
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
import com.lizhi1026.sleepwhisper.model.SleepSession.SleepType

/**
 * 睡眠类型选择底部弹层（features/home 层）
 *
 * 职责：
 * - 展示三种睡眠类型（小睡 NAP、夜睡 NIGHT、接觉 CONTACT_NAP）供用户选择。
 * - [suggestedType] 由 `AppStateContainer.defaultSleepType()` 根据当前时间推断，
 *   推荐项以 accent 渐变高亮并附"推荐"徽章。
 * - 用户选择后回调 [onPick]，由 HomeViewModel.onPickSleepType() 委托给 AppStateContainer.startSleep(type)。
 * - 点击背景遮罩触发 [onDismiss]，不做任何睡眠记录。
 * - 对应 iOS SleepTypePicker.swift。
 */
@Composable
fun SleepTypePicker(
    suggestedType: SleepType,
    onPick: (SleepType) -> Unit,
    onDismiss: () -> Unit
) {
    val scheme = LocalSWScheme.current

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
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                BasicText(
                    text = stringResource(R.string.sleeptype_picker_title),
                    style = com.lizhi1026.sleepwhisper.core.visualkit.SWFont.serif(22).copy(
                        color = SWColor.textPrimary(scheme)
                    )
                )
                BasicText(
                    text = stringResource(R.string.sleeptype_picker_subtitle),
                    style = SWFont.labelMD().copy(color = SWColor.textSecondary(scheme))
                )
                // 三种睡眠类型行，推荐项高亮；选择后立即回调并关闭弹层
                SleepTypeRow(
                    SleepType.NAP, R.string.sleeptype_nap_title, R.string.sleeptype_nap_sub,
                    suggested = SleepType.NAP == suggestedType,
                    onPick = { onPick(it); onDismiss() }
                )
                SleepTypeRow(
                    SleepType.NIGHT, R.string.sleeptype_night_title, R.string.sleeptype_night_sub,
                    suggested = SleepType.NIGHT == suggestedType,
                    onPick = { onPick(it); onDismiss() }
                )
                SleepTypeRow(
                    SleepType.CONTACT_NAP, R.string.sleeptype_contact_title, R.string.sleeptype_contact_sub,
                    suggested = SleepType.CONTACT_NAP == suggestedType,
                    onPick = { onPick(it); onDismiss() }
                )
            }
        }
    }
}

/**
 * 睡眠类型选项行。
 *
 * @param type 该行对应的 SleepType 枚举值。
 * @param titleRes 主标题字符串资源 ID。
 * @param subRes 副标题字符串资源 ID。
 * @param suggested 是否为推荐类型，true 时以 accent 渐变高亮并显示"推荐"徽章。
 * @param onPick 用户点击该行后回调，携带对应的 [SleepType]。
 */
@Composable
private fun SleepTypeRow(
    type: SleepType,
    titleRes: Int,
    subRes: Int,
    suggested: Boolean,
    onPick: (SleepType) -> Unit
) {
    val scheme = LocalSWScheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SWRadius.md))
            // 推荐项：accent 渐变背景；普通项：下沉表面纯色背景
            .background(if (suggested) SWGradient.accent(scheme) else androidx.compose.ui.graphics.Brush.linearGradient(listOf(SWColor.surfaceSunken(scheme), SWColor.surfaceSunken(scheme))))
            .clickable { onPick(type) }
            .padding(SWSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                text = stringResource(titleRes),
                style = SWFont.titleMD().copy(color = if (suggested) Color.White else SWColor.textPrimary(scheme))
            )
            BasicText(
                text = stringResource(subRes),
                style = SWFont.labelMD().copy(color = if (suggested) Color.White.copy(alpha = 0.85f) else SWColor.textSecondary(scheme))
            )
        }
        // 仅推荐项展示"推荐"徽章
        if (suggested) {
            BasicText(
                text = stringResource(R.string.sleeptype_badge_suggested),
                style = SWFont.labelSM().copy(color = Color.White),
                modifier = Modifier
                    .clip(RoundedCornerShape(SWRadius.pill))
                    .background(Color.White.copy(alpha = 0.25f))
                    .padding(horizontal = SWSpacing.sm, vertical = SWSpacing.xxs)
            )
        }
    }
}
