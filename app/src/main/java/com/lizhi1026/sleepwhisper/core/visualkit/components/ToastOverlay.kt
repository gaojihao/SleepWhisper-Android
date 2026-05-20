package com.lizhi1026.sleepwhisper.core.visualkit.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lizhi1026.sleepwhisper.core.toast.ToastCenter
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor
import com.lizhi1026.sleepwhisper.core.visualkit.SWFont
import com.lizhi1026.sleepwhisper.core.visualkit.SWMotion
import com.lizhi1026.sleepwhisper.core.visualkit.SWRadius
import com.lizhi1026.sleepwhisper.core.visualkit.SWSpacing
import kotlinx.coroutines.delay

/**
 * Bottom-anchored transient toast overlay — port of iOS Toast.swift.
 * Auto-dismisses after [SWMotion.toastMs] * 15 ≈ 3 seconds.
 */
@Composable
fun ToastOverlay(
    item: ToastCenter.ToastItem?,
    onDismiss: () -> Unit,
    onUndo: (() -> Unit)? = null,
    onEditAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scheme = LocalSWScheme.current

    LaunchedEffect(item) {
        if (item != null) {
            delay(3000)
            onDismiss()
        }
    }

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(
            visible = item != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            val safeItem = item ?: return@AnimatedVisibility
            val text = if (safeItem.args.isNotEmpty())
                stringResource(safeItem.messageRes, *safeItem.args.toTypedArray())
            else stringResource(safeItem.messageRes)
            val tint: Color = when (safeItem.style) {
                ToastCenter.Style.INFO -> SWColor.primary(scheme)
                ToastCenter.Style.SUCCESS -> SWColor.success(scheme)
                ToastCenter.Style.WARNING -> SWColor.warning(scheme)
                ToastCenter.Style.ERROR -> SWColor.danger(scheme)
            }
            Box(
                modifier = Modifier
                    .padding(SWSpacing.md)
                    .clip(RoundedCornerShape(SWRadius.pill))
                    .background(SWColor.surfaceElevated(scheme))
                    .padding(horizontal = SWSpacing.md, vertical = SWSpacing.sm)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SWSpacing.sm)) {
                    Box(Modifier.size(8.dp).background(tint, RoundedCornerShape(4.dp)))
                    BasicText(
                        text = text,
                        style = SWFont.bodyMD().copy(color = SWColor.textPrimary(scheme))
                    )
                    if (safeItem.undo != null && onUndo != null) {
                        Spacer(Modifier.size(SWSpacing.xs))
                        ToastAction(label = "Undo", color = tint, onClick = onUndo)
                    }
                    if (safeItem.editAction != null && onEditAction != null) {
                        Spacer(Modifier.size(SWSpacing.xs))
                        ToastAction(label = "Edit", color = tint, onClick = onEditAction)
                    }
                }
            }
        }
    }
}

@Composable
private fun ToastAction(label: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(SWRadius.pill))
            .background(color.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(horizontal = SWSpacing.sm, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        BasicText(text = label, style = SWFont.labelMD().copy(color = color))
    }
}
