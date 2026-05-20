package com.lizhi1026.sleepwhisper.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.lizhi1026.sleepwhisper.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationChannels @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun ensure() {
        val nm = context.getSystemService(NotificationManager::class.java)
        try {
            nm.createNotificationChannel(
                NotificationChannel(
                    CH_PLAYBACK,
                    context.getString(R.string.fg_channel_name),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = context.getString(R.string.fg_channel_desc)
                    setShowBadge(false)
                    enableVibration(false)
                    setSound(null, null)
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                }
            )
            nm.createNotificationChannel(
                NotificationChannel(
                    CH_SLEEP_CHECKIN,
                    context.getString(R.string.ch_sleep_checkin),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    enableVibration(true)
                    enableLights(true)
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                }
            )
            nm.createNotificationChannel(
                NotificationChannel(
                    CH_REC_WINDOW,
                    context.getString(R.string.ch_rec_window),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    enableVibration(true)
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                }
            )
        } catch (_: Throwable) {
            // createNotificationChannel can throw IllegalArgumentException if a previously
            // deleted channel id is reused with different settings; we swallow so cold start
            // is not bricked by a channel state quirk.
        }
    }

    companion object {
        const val CH_PLAYBACK = "playback"
        const val CH_SLEEP_CHECKIN = "sleep_checkin"
        const val CH_REC_WINDOW = "rec_window"
    }
}
