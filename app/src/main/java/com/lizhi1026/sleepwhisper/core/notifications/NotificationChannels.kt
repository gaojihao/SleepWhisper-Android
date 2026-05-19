package com.lizhi1026.sleepwhisper.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.annotation.StringRes
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
        // playback: persistent foreground, silent channel — created by the App once at cold start.
        nm.createNotificationChannel(
            NotificationChannel(
                CH_PLAYBACK,
                context.getString(R.string.fg_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.fg_channel_desc)
                setShowBadge(false)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CH_SLEEP_CHECKIN,
                context.getString(R.string.ch_sleep_checkin),
                NotificationManager.IMPORTANCE_HIGH
            )
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CH_REC_WINDOW,
                context.getString(R.string.ch_rec_window),
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
    }

    companion object {
        const val CH_PLAYBACK = "playback"
        const val CH_SLEEP_CHECKIN = "sleep_checkin"
        const val CH_REC_WINDOW = "rec_window"
    }
}
