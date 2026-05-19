package com.lizhi1026.sleepwhisper.app

import android.app.Application
import com.lizhi1026.sleepwhisper.core.notifications.NotificationChannels
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SleepWhisperApp : Application() {

    @Inject lateinit var channels: NotificationChannels

    override fun onCreate() {
        super.onCreate()
        // Create notification channels once before any component posts a notification.
        channels.ensure()
    }
}
