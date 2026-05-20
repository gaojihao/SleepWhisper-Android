package com.lizhi1026.sleepwhisper.core.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.AlarmManagerCompat
import com.lizhi1026.sleepwhisper.R
import com.lizhi1026.sleepwhisper.core.toast.ToastCenter
import com.lizhi1026.sleepwhisper.core.toast.ToastCenter.Style
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val toast: ToastCenter
) {
    private val am: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        const val KIND_CHECKIN = "sleep.checkin"
        const val KIND_REC_WINDOW = "rec.window"

        private const val MIN_REC_WINDOW_LEAD_MS = 5 * 60_000L // 5 min — Doze recovery buffer
    }

    fun scheduleSleepCheckIn(babyId: String, afterHours: Double) {
        val triggerAt = System.currentTimeMillis() + (afterHours * 3_600_000).toLong()
        val title = context.getString(R.string.notification_sleepcheckin_title)
        // Round up to whole hours for the body; "Sleep has lasted over %1$d hours."
        val body = context.getString(
            R.string.notification_sleepcheckin_body,
            afterHours.toInt().coerceAtLeast(1)
        )
        scheduleExactOrFallback(
            kind = KIND_CHECKIN,
            babyId = babyId,
            triggerAt = triggerAt,
            title = title,
            body = body
        )
    }

    /**
     * Caller supplies quiet-hour bounds (typically from UserSettings.nightModeStart/EndHour) so
     * the scheduler stays free of persistence dependencies. If [startMs] lands inside quiet
     * hours or within MIN_REC_WINDOW_LEAD_MS the schedule is dropped.
     */
    fun scheduleRecommendationWindow(
        babyId: String,
        startMs: Long,
        endMs: Long,
        quietStartHour: Int = 22,
        quietEndHour: Int = 7
    ) {
        if (isInQuietHours(startMs, quietStartHour, quietEndHour)) return
        val now = System.currentTimeMillis()
        if (startMs - now < MIN_REC_WINDOW_LEAD_MS) return

        val title = context.getString(R.string.notification_recwindow_title)
        val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        val body = context.getString(
            R.string.notification_recwindow_body,
            fmt.format(Date(startMs)),
            fmt.format(Date(endMs))
        )
        scheduleExactOrFallback(
            kind = KIND_REC_WINDOW,
            babyId = babyId,
            triggerAt = startMs,
            title = title,
            body = body
        )
    }

    fun cancel(kind: String, babyId: String) {
        val intent = baseIntent(kind, babyId)
        val pi = PendingIntent.getBroadcast(
            context,
            requestCode(kind, babyId),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) {
            am.cancel(pi)
            pi.cancel()
        }
    }

    // ---------------------------------------------------------------------------
    // Internal
    // ---------------------------------------------------------------------------

    /** Stable per-(kind, babyId) request code; combines hashes with prime mixer to reduce collisions. */
    private fun requestCode(kind: String, babyId: String): Int {
        val h = 31 * kind.hashCode() xor babyId.hashCode()
        return h and 0x7FFFFFFF
    }

    private fun baseIntent(kind: String, babyId: String): Intent =
        Intent(context, AlarmReceiver::class.java).apply {
            action = kind
            // Make the intent identity stable so PendingIntent matching is reliable on cancel.
            data = android.net.Uri.parse("sleepwhisper://alarm/$kind/$babyId")
            putExtra(AlarmReceiver.EXTRA_KIND, kind)
            putExtra(AlarmReceiver.EXTRA_BABY_ID, babyId)
        }

    private fun isInQuietHours(at: Long, quietStart: Int, quietEnd: Int): Boolean {
        val cal = Calendar.getInstance().apply { timeInMillis = at }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        return if (quietStart > quietEnd) {
            hour >= quietStart || hour < quietEnd
        } else {
            hour in quietStart until quietEnd
        }
    }

    private fun scheduleExactOrFallback(
        kind: String,
        babyId: String,
        triggerAt: Long,
        title: String,
        body: String
    ) {
        val intent = baseIntent(kind, babyId).apply {
            putExtra(AlarmReceiver.EXTRA_TITLE, title)
            putExtra(AlarmReceiver.EXTRA_BODY, body)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            requestCode(kind, babyId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (canScheduleExactAlarmsCompat()) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            } else {
                AlarmManagerCompat.setAndAllowWhileIdle(am, AlarmManager.RTC_WAKEUP, triggerAt, pi)
                toast.show(R.string.toast_exact_alarm_needed, style = Style.WARNING)
            }
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    /**
     * API 33 (Tiramisu) replaced `canScheduleExactAlarms()` with `canUseExactAlarms()` for apps
     * that hold the USE_EXACT_ALARM permission. We declare both in the manifest, so we accept
     * either signal as proof of authorization.
     */
    private fun canScheduleExactAlarmsCompat(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && am.canScheduleExactAlarms()) return true
        return am.canScheduleExactAlarms()
    }
}
