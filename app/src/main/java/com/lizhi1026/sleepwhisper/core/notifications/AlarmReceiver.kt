/**
 * # AlarmReceiver
 *
 * **职责**：接收 [NotificationScheduler] 通过 `AlarmManager` 投递的定时广播，
 * 按 Intent 中的 `kind` 字段选择对应通知渠道，并发送用户可见的提醒通知。
 *
 * **所在层**：`core/notifications`——基础设施层，无业务逻辑。
 *
 * **与谁交互**：
 * - 上游：[NotificationScheduler]（通过 `AlarmManager` PendingIntent 触发）
 * - 下游：[NotificationChannels]（选择渠道 ID）、`NotificationManagerCompat`（发送通知）
 * - 两类提醒：`KIND_CHECKIN`（睡眠签到，高优先级+震动+灯光）
 *            和 `KIND_REC_WINDOW`（推荐窗口，默认优先级）
 */
package com.lizhi1026.sleepwhisper.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.lizhi1026.sleepwhisper.R

/**
 * 定时闹钟广播接收器。
 *
 * 由 [NotificationScheduler] 通过 `AlarmManager.setExactAndAllowWhileIdle`
 * 触发；根据 [EXTRA_KIND] 字段选择通知渠道并发出提醒。
 */
class AlarmReceiver : BroadcastReceiver() {

    companion object {
        /** Intent Extra：提醒类型，取值为 [NotificationScheduler.KIND_CHECKIN] 或 [NotificationScheduler.KIND_REC_WINDOW] */
        const val EXTRA_KIND = "kind"
        /** Intent Extra：宝宝 ID，用于关联具体睡眠记录 */
        const val EXTRA_BABY_ID = "babyId"
        /** Intent Extra：通知标题文本 */
        const val EXTRA_TITLE = "title"
        /** Intent Extra：通知正文文本 */
        const val EXTRA_BODY = "body"
    }

    /**
     * 接收定时广播并发送对应类型的通知。
     *
     * @param context 系统上下文
     * @param intent  包含 kind/title/body 等 Extra 的广播 Intent
     */
    override fun onReceive(context: Context, intent: Intent) {
        // 缺少必要字段则静默丢弃，避免空指针崩溃
        val kind = intent.getStringExtra(EXTRA_KIND) ?: return
        val title = intent.getStringExtra(EXTRA_TITLE) ?: return
        val body = intent.getStringExtra(EXTRA_BODY) ?: return

        // 按 kind 选择对应渠道：睡眠签到用高优先级渠道，其余用推荐窗口渠道
        val channelId = when (kind) {
            NotificationScheduler.KIND_CHECKIN -> NotificationChannels.CH_SLEEP_CHECKIN   // 高优先级，震动+灯光
            NotificationScheduler.KIND_REC_WINDOW -> NotificationChannels.CH_REC_WINDOW   // 默认优先级
            else -> NotificationChannels.CH_REC_WINDOW                                    // 兜底：未知类型也用推荐渠道
        }

        // 构建通知：BigTextStyle 支持长文本展开显示
        val n = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)   // 用户点击后自动消除通知
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)  // 锁屏可见
            .setCategory(
                // 睡眠签到为闹钟类别（最高优先展示），推荐窗口为推荐类别
                if (kind == NotificationScheduler.KIND_CHECKIN)
                    NotificationCompat.CATEGORY_ALARM
                else
                    NotificationCompat.CATEGORY_RECOMMENDATION
            )
            .build()

        // 使用 kind 哈希值作为通知 ID，确保同类型只保留最新一条
        val notifId = (kind.hashCode() and 0x7FFFFFFF)
        val nm = NotificationManagerCompat.from(context)
        if (nm.areNotificationsEnabled()) {
            // 用户已授权通知权限，发送通知（POST_NOTIFICATIONS 已通过 areNotificationsEnabled 检查）
            @Suppress("MissingPermission") // checked via areNotificationsEnabled above
            nm.notify(notifId, n)
        }
    }
}
