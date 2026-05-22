/**
 * # BootReceiver
 *
 * **职责**：监听设备开机与应用更新事件，重建因重启而丢失的睡眠签到闹钟。
 * `AlarmManager` 的精确闹钟在设备重启后会全部清空，本接收器负责在开机时
 * 查找仍在进行中的睡眠记录，并按剩余时间重新调度 check-in 提醒。
 *
 * **所在层**：`core/notifications`——基础设施层。
 *
 * **与谁交互**：
 * - 监听广播：`BOOT_COMPLETED`、`LOCKED_BOOT_COMPLETED`、
 *             `QUICKBOOT_POWERON`（小米/华为兼容）、`MY_PACKAGE_REPLACED`（应用更新）
 * - 依赖：[Repository]（查询进行中的睡眠）、[NotificationScheduler]（重建闹钟）
 */
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
 * 开机/升级广播接收器。
 *
 * Rebuilds time-sensitive alarms after device boot or app upgrade. Exact alarms scheduled
 * via AlarmManager do NOT survive reboot, so without this receiver every sleep check-in
 * silently disappears when the user reboots overnight.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    /** Hilt 注入：数据仓库，用于查询宝宝信息和进行中的睡眠记录 */
    @Inject lateinit var repo: Repository
    /** Hilt 注入：通知调度器，用于重建精确闹钟 */
    @Inject lateinit var scheduler: NotificationScheduler

    /**
     * 接收系统广播，过滤有效 action 后异步重建睡眠签到闹钟。
     *
     * @param context 系统上下文
     * @param intent  开机或应用更新广播 Intent
     */
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        // 过滤：仅处理开机完成、加密存储开机、小米快速开机、应用包替换四种广播
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != "android.intent.action.QUICKBOOT_POWERON" &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) return

        // 使用 goAsync 延长广播接收器生命周期，确保异步 IO 任务完成后再通知系统
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                // 查询当前宝宝信息，若未设置宝宝则无需调度
                val baby = repo.loadBaby() ?: return@launch
                // 查询该宝宝是否有进行中的睡眠，若已结束则无需重建闹钟
                val ongoing = repo.ongoingSleep(baby.id) ?: return@launch

                // Original schedule was startAt + 2h. After reboot, schedule the remainder.
                // 计算已经过去的小时数，剩余时间 = 2h - 已过时间，最少保留 0.05h（约 3 分钟）防止立即触发
                val elapsedHours = (System.currentTimeMillis() - ongoing.startAt) / 3_600_000.0
                val remainingHours = (2.0 - elapsedHours).coerceAtLeast(0.05)
                // 用剩余时间重新注册精确闹钟
                scheduler.scheduleSleepCheckIn(baby.id, afterHours = remainingHours)
            } finally {
                // 无论成功或异常，都必须调用 finish() 通知系统广播已处理完毕
                pending.finish()
            }
        }
    }
}
