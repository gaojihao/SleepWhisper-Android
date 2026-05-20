package com.lizhi1026.sleepwhisper.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.lizhi1026.sleepwhisper.R

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_KIND = "kind"
        const val EXTRA_BABY_ID = "babyId"
        const val EXTRA_TITLE = "title"
        const val EXTRA_BODY = "body"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val kind = intent.getStringExtra(EXTRA_KIND) ?: return
        val title = intent.getStringExtra(EXTRA_TITLE) ?: return
        val body = intent.getStringExtra(EXTRA_BODY) ?: return

        val channelId = when (kind) {
            NotificationScheduler.KIND_CHECKIN -> NotificationChannels.CH_SLEEP_CHECKIN
            NotificationScheduler.KIND_REC_WINDOW -> NotificationChannels.CH_REC_WINDOW
            else -> NotificationChannels.CH_REC_WINDOW
        }

        val n = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(
                if (kind == NotificationScheduler.KIND_CHECKIN)
                    NotificationCompat.CATEGORY_ALARM
                else
                    NotificationCompat.CATEGORY_RECOMMENDATION
            )
            .build()

        val notifId = (kind.hashCode() and 0x7FFFFFFF)
        val nm = NotificationManagerCompat.from(context)
        if (nm.areNotificationsEnabled()) {
            @Suppress("MissingPermission") // checked via areNotificationsEnabled above
            nm.notify(notifId, n)
        }
    }
}
