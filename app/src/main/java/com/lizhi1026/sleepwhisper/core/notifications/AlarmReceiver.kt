package com.lizhi1026.sleepwhisper.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val kind = intent.getStringExtra("kind") ?: return
        val titleRes = intent.getIntExtra("titleRes", 0)
        val bodyRes = intent.getIntExtra("bodyRes", 0)
        if (titleRes == 0 || bodyRes == 0) return

        val channelId = when (kind) {
            "sleep.checkin"                      -> NotificationChannels.CH_SLEEP_CHECKIN
            "rec.window",
            NotificationChannels.CH_REC_WINDOW   -> NotificationChannels.CH_REC_WINDOW
            else                                -> NotificationChannels.CH_REC_WINDOW
        }

        val n = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // TODO: replace with R.mipmap.ic_launcher
            .setContentTitle(context.getString(titleRes))
            .setContentText(context.getString(bodyRes))
            .setAutoCancel(true)
            .build()

        val notifId = kind.hashCode() and 0x7FFFFFFF
        @Suppress("MissingPermission") // POST_NOTIFICATIONS: func silently fails when not granted.
        NotificationManagerCompat.from(context).notify(notifId, n)
    }
}
