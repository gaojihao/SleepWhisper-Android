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
import java.util.Calendar
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

        private const val QUIET_HOURS_START = 22
        private const val QUIET_HOURS_END = 7
        private const val MIN_REC_WINDOW_LEAD_MS = 60_000L
    }

    fun scheduleSleepCheckIn(babyId: String, afterHours: Double) {
        val triggerAt = System.currentTimeMillis() + (afterHours * 3600_000).toLong()
        scheduleExactOrFallback(
            kind = KIND_CHECKIN,
            babyId = babyId,
            triggerAt = triggerAt,
            titleRes = R.string.app_name,
            bodyRes = R.string.app_name  // TODO: replace with localized strings in Phase 7
        )
    }

    fun scheduleRecommendationWindow(babyId: String, startMs: Long, endMs: Long) {
        if (isInQuietHours(startMs)) return
        val now = System.currentTimeMillis()
        if (startMs - now < MIN_REC_WINDOW_LEAD_MS) return

        scheduleExactOrFallback(
            kind = KIND_REC_WINDOW,
            babyId = babyId,
            triggerAt = startMs,
            titleRes = R.string.app_name,
            bodyRes = R.string.app_name  // TODO: replace with localized strings in Phase 7
        )
    }

    fun cancel(kind: String, babyId: String) {
        val requestCode = requestCode(kind, babyId)
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("kind", kind)
            // put dummy extras so FLAG_NO_CREATE matches the same intent shape.
            putExtra("titleRes", 0)
            putExtra("bodyRes", 0)
            putExtra("babyId", babyId)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            requestCode,
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

    private fun requestCode(kind: String, babyId: String): Int =
        ("$kind:$babyId").hashCode() and 0x7FFFFFFF

    private fun isInQuietHours(at: Long): Boolean {
        val cal = Calendar.getInstance()
        cal.timeInMillis = at
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        return if (QUIET_HOURS_START > QUIET_HOURS_END) {
            hour >= QUIET_HOURS_START || hour < QUIET_HOURS_END
        } else {
            hour >= QUIET_HOURS_START && hour < QUIET_HOURS_END
        }
    }

    private fun scheduleExactOrFallback(
        kind: String,
        babyId: String,
        triggerAt: Long,
        titleRes: Int,
        bodyRes: Int
    ) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = kind
            putExtra("kind", kind)
            putExtra("titleRes", titleRes)
            putExtra("bodyRes", bodyRes)
            putExtra("babyId", babyId)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            requestCode(kind, babyId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (am.canScheduleExactAlarms()) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            } else {
                AlarmManagerCompat.setAndAllowWhileIdle(am, AlarmManager.RTC_WAKEUP, triggerAt, pi)
                toast.show(
                    R.string.toast_exact_alarm_needed,
                    style = Style.WARNING
                )
            }
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }
}
