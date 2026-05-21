package com.lizhi1026.sleepwhisper.features.home

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.lizhi1026.sleepwhisper.R
import com.lizhi1026.sleepwhisper.core.strings.displayKey
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor
import com.lizhi1026.sleepwhisper.core.visualkit.SWFont
import com.lizhi1026.sleepwhisper.core.visualkit.SWSpacing
import com.lizhi1026.sleepwhisper.core.visualkit.components.ElevationLevel
import com.lizhi1026.sleepwhisper.core.visualkit.components.GlassCard
import com.lizhi1026.sleepwhisper.core.visualkit.components.SoftButton
import com.lizhi1026.sleepwhisper.core.visualkit.components.SoftButtonStyle
import com.lizhi1026.sleepwhisper.model.DiaperEvent
import com.lizhi1026.sleepwhisper.model.EventEditTarget
import com.lizhi1026.sleepwhisper.model.FeedingEvent
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Edit sheet for an existing Feeding / Diaper event — port of iOS EventEditSheet.swift.
 * Picks a new occurredAt timestamp and (for bottle feeds) a new amount in ml.
 *
 * Caller observes [com.lizhi1026.sleepwhisper.app.AppStateContainer.pendingEditTarget] and
 * dismisses by invoking [onDismiss] when save / cancel completes.
 */
@Composable
fun EventEditSheet(
    target: EventEditTarget,
    onDismiss: () -> Unit,
    onSaveFeeding: (FeedingEvent) -> Unit,
    onSaveDiaper: (DiaperEvent) -> Unit
) {
    val scheme = LocalSWScheme.current
    val ctx = LocalContext.current

    val initialTime: Long = when (target) {
        is EventEditTarget.Feeding -> target.event.startedAt
        is EventEditTarget.Diaper -> target.event.occurredAt
    }
    val initialAmount: Int = when (target) {
        is EventEditTarget.Feeding -> target.event.amountMl ?: 0
        else -> 0
    }

    var time by remember(target) { mutableLongStateOf(initialTime) }
    var amount by remember(target) { mutableIntStateOf(initialAmount) }

    val timeFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

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
                    onClick = {} // swallow taps on card body
                ),
            elevation = ElevationLevel.STRONG
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(SWSpacing.md)) {
                com.lizhi1026.sleepwhisper.core.visualkit.components.DragHandle(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                BasicText(
                    text = when (target) {
                        is EventEditTarget.Feeding -> stringResource(
                            R.string.event_edit_feeding,
                            stringResource(target.event.method.displayKey())
                        )
                        is EventEditTarget.Diaper -> stringResource(
                            R.string.event_edit_diaper,
                            stringResource(target.event.type.displayKey())
                        )
                    },
                    style = com.lizhi1026.sleepwhisper.core.visualkit.SWFont.serif(22).copy(
                        color = SWColor.textPrimary(scheme)
                    )
                )

                Column {
                    BasicText(
                        text = stringResource(R.string.event_edit_timelabel),
                        style = SWFont.labelMD().copy(color = SWColor.textSecondary(scheme))
                    )
                    SoftButton(
                        text = timeFormat.format(Date(time)),
                        style = SoftButtonStyle.GHOST,
                        onClick = { pickDateTime(ctx, time) { time = it } }
                    )
                }

                if (target is EventEditTarget.Feeding && target.event.method == FeedingEvent.FeedingMethod.BOTTLE) {
                    Column {
                        BasicText(
                            text = stringResource(R.string.event_edit_bottlestepper, amount),
                            style = SWFont.labelMD().copy(color = SWColor.textSecondary(scheme))
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(SWSpacing.sm)
                        ) {
                            SoftButton(
                                text = "-10",
                                onClick = { amount = (amount - 10).coerceAtLeast(0) },
                                style = SoftButtonStyle.GHOST,
                                modifier = Modifier.weight(1f)
                            )
                            SoftButton(
                                text = "+10",
                                onClick = { amount = (amount + 10).coerceAtMost(500) },
                                style = SoftButtonStyle.GHOST,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(SWSpacing.sm)
                ) {
                    SoftButton(
                        text = stringResource(R.string.common_cancel),
                        onClick = onDismiss,
                        style = SoftButtonStyle.GHOST,
                        modifier = Modifier.weight(1f)
                    )
                    SoftButton(
                        text = stringResource(R.string.common_save),
                        onClick = {
                            when (target) {
                                is EventEditTarget.Feeding -> onSaveFeeding(
                                    target.event.copy(
                                        startedAt = time,
                                        amountMl = if (target.event.method == FeedingEvent.FeedingMethod.BOTTLE) amount.takeIf { it > 0 } else target.event.amountMl,
                                        isEdited = true
                                    )
                                )
                                is EventEditTarget.Diaper -> onSaveDiaper(
                                    target.event.copy(occurredAt = time)
                                )
                            }
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

private fun pickDateTime(
    ctx: android.content.Context,
    initialMs: Long,
    onPicked: (Long) -> Unit
) {
    val cal = Calendar.getInstance().apply { timeInMillis = initialMs }
    DatePickerDialog(
        ctx,
        { _, y, m, d ->
            val date = Calendar.getInstance().apply {
                clear()
                set(y, m, d, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
            }
            TimePickerDialog(
                ctx,
                { _, hour, minute ->
                    date.set(Calendar.HOUR_OF_DAY, hour)
                    date.set(Calendar.MINUTE, minute)
                    onPicked(date.timeInMillis)
                },
                date.get(Calendar.HOUR_OF_DAY),
                date.get(Calendar.MINUTE),
                true
            ).show()
        },
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH),
        cal.get(Calendar.DAY_OF_MONTH)
    ).apply {
        datePicker.maxDate = System.currentTimeMillis()
    }.show()
}
