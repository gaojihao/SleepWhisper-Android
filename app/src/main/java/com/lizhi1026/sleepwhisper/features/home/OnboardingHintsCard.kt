package com.lizhi1026.sleepwhisper.features.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.lizhi1026.sleepwhisper.R
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor
import com.lizhi1026.sleepwhisper.core.visualkit.SWFont
import com.lizhi1026.sleepwhisper.core.visualkit.SWSpacing
import com.lizhi1026.sleepwhisper.core.visualkit.components.GlassCard
import com.lizhi1026.sleepwhisper.core.visualkit.components.SoftButton
import com.lizhi1026.sleepwhisper.core.visualkit.components.SoftButtonStyle

/** 提示条目：标题与正文均使用字符串资源 ID，支持多语言。 */
private data class Hint(val titleRes: Int, val bodyRes: Int)

/** 三条新手提示：启动睡眠、清醒窗口、AI 助手。 */
private val HINTS = listOf(
    Hint(R.string.hint_startsleep_title, R.string.hint_startsleep_body),
    Hint(R.string.hint_window_title, R.string.hint_window_body),
    Hint(R.string.hint_ai_title, R.string.hint_ai_body)
)

/**
 * 首页嵌入式新手引导卡片（features/home 层）
 *
 * 职责：
 * - 以内联卡片形式展示 3 页新手提示（启动睡眠、清醒窗口、AI 助手），用户可逐页翻看。
 * - 最后一页按钮文案变为"我准备好了"，点击后回调 [onDismiss]；
 *   HomeViewModel 接收后调用 AppStateContainer.markHintsSeen() 永久隐藏。
 * - 对应 iOS OnboardingHintsCard.swift。
 */
@Composable
fun OnboardingHintsCard(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalSWScheme.current
    // 当前页索引，从 0 开始
    var page by remember { mutableIntStateOf(0) }
    val hint = HINTS[page]
    val isLast = page == HINTS.lastIndex

    GlassCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(SWSpacing.sm)) {
            BasicText(
                text = stringResource(hint.titleRes),
                style = SWFont.titleMD().copy(color = SWColor.textPrimary(scheme))
            )
            BasicText(
                text = stringResource(hint.bodyRes),
                style = SWFont.bodyMD().copy(color = SWColor.textSecondary(scheme))
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicText(
                    text = "${page + 1} / ${HINTS.size}",
                    style = SWFont.labelMD().copy(color = SWColor.textTertiary(scheme))
                )
                Box(modifier = Modifier.weight(1f))
                SoftButton(
                    text = if (isLast) stringResource(R.string.common_imready)
                           else stringResource(R.string.common_next),
                    // 最后一页：触发 onDismiss 并标记已读；其他页：翻到下一页
                    onClick = { if (isLast) onDismiss() else page += 1 },
                    style = if (isLast) SoftButtonStyle.PRIMARY else SoftButtonStyle.GHOST
                )
            }
        }
    }
}
