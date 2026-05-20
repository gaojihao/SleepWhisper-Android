package com.lizhi1026.sleepwhisper.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lizhi1026.sleepwhisper.core.persistence.Repository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Rebuilds time-sensitive alarms after device boot or app upgrade. Exact alarms scheduled
 * via AlarmManager do NOT survive reboot, so without this receiver every sleep check-in
 * silently disappears when the user reboots overnight.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var repo: Repository
    @Inject lateinit var scheduler: NotificationScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != "android.intent.action.QUICKBOOT_POWERON" &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) return

        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val baby = repo.loadBaby() ?: return@launch
                val ongoing = repo.ongoingSleep(baby.id) ?: return@launch

                // Original schedule was startAt + 2h. After reboot, schedule the remainder.
                val elapsedHours = (System.currentTimeMillis() - ongoing.startAt) / 3_600_000.0
                val remainingHours = (2.0 - elapsedHours).coerceAtLeast(0.05)
                scheduler.scheduleSleepCheckIn(baby.id, afterHours = remainingHours)
            } finally {
                pending.finish()
            }
        }
    }
}
