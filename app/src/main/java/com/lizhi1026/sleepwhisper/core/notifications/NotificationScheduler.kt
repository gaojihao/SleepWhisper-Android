/**
 * # NotificationScheduler
 *
 * **职责**：封装 `AlarmManager` 精确闹钟的调度与取消，为上层业务提供两类定时提醒：
 * 1. **睡眠签到（KIND_CHECKIN）**：睡眠开始约 2 小时后触发，提醒家长记录宝宝状态。
 * 2. **推荐睡眠窗口（KIND_REC_WINDOW）**：在算法预测的下一个睡眠窗口起点前触发。
 *
 * **所在层**：`core/notifications`——基础设施层，依赖 [AlarmReceiver] 接收广播。
 *
 * **与谁交互**：
 * - 调用方：业务层（`AppStateContainer`、`SleepViewModel` 等）
 * - 下游：[AlarmReceiver]（通过 `AlarmManager` PendingIntent 唤醒）
 * - 静默时段（Quiet Hours）默认 22:00–07:00，调度时自动过滤；
 *   距触发时间不足 5 分钟的也会被丢弃，防止立即弹出干扰
 */
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

/**
 * 精确闹钟调度器（单例）。
 *
 * 优先使用 `setExactAndAllowWhileIdle` 在 Doze 模式下也能准时触发；
 * Android 12（S）及以上需要用户额外授予精确闹钟权限，无权限时降级为非精确闹钟并弹出提示。
 */
@Singleton
class NotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val toast: ToastCenter
) {
    /** 系统闹钟服务 */
    private val am: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        /** 提醒类型：睡眠签到（高优先级渠道，约睡眠开始 2 小时后触发） */
        const val KIND_CHECKIN = "sleep.checkin"
        /** 提醒类型：推荐睡眠窗口（默认优先级渠道，在窗口起点触发） */
        const val KIND_REC_WINDOW = "rec.window"

        /** 推荐窗口最小提前量：5 分钟，为 Doze 唤醒恢复预留缓冲 */
        private const val MIN_REC_WINDOW_LEAD_MS = 5 * 60_000L // 5 min — Doze recovery buffer
    }

    /**
     * 调度睡眠签到提醒。
     *
     * @param babyId     宝宝 ID，用于构造稳定的 PendingIntent 请求码
     * @param afterHours 距现在的触发延迟（小时），通常为 2.0；开机重建时传入剩余小时数
     */
    fun scheduleSleepCheckIn(babyId: String, afterHours: Double) {
        // 将小时数转换为毫秒并叠加当前时间，得到精确触发时刻
        val triggerAt = System.currentTimeMillis() + (afterHours * 3_600_000).toLong()
        val title = context.getString(R.string.notification_sleepcheckin_title)
        // Round up to whole hours for the body; "Sleep has lasted over %1$d hours."
        // 正文中使用不低于 1 的整数小时数，避免显示"超过 0 小时"
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
     * 调度推荐睡眠窗口提醒。
     *
     * Caller supplies quiet-hour bounds (typically from UserSettings.nightModeStart/EndHour) so
     * the scheduler stays free of persistence dependencies. If [startMs] lands inside quiet
     * hours or within MIN_REC_WINDOW_LEAD_MS the schedule is dropped.
     *
     * @param babyId         宝宝 ID
     * @param startMs        推荐窗口开始时间（Unix 毫秒）
     * @param endMs          推荐窗口结束时间（Unix 毫秒），仅用于通知正文展示
     * @param quietStartHour 静默时段开始小时（默认 22 点）
     * @param quietEndHour   静默时段结束小时（默认 7 点）
     */
    fun scheduleRecommendationWindow(
        babyId: String,
        startMs: Long,
        endMs: Long,
        quietStartHour: Int = 22,
        quietEndHour: Int = 7
    ) {
        // 静默时段内不发推荐窗口通知，避免打扰用户睡眠
        if (isInQuietHours(startMs, quietStartHour, quietEndHour)) return
        val now = System.currentTimeMillis()
        // 距窗口起点不足 5 分钟则丢弃，防止立即触发扰乱用户
        if (startMs - now < MIN_REC_WINDOW_LEAD_MS) return

        val title = context.getString(R.string.notification_recwindow_title)
        // 格式化开始/结束时间为 HH:mm 字符串用于通知正文
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

    /**
     * 取消指定类型和宝宝的已调度闹钟。
     *
     * @param kind   提醒类型（[KIND_CHECKIN] 或 [KIND_REC_WINDOW]）
     * @param babyId 宝宝 ID
     */
    fun cancel(kind: String, babyId: String) {
        val intent = baseIntent(kind, babyId)
        // FLAG_NO_CREATE：若 PendingIntent 不存在则返回 null，避免无谓创建
        val pi = PendingIntent.getBroadcast(
            context,
            requestCode(kind, babyId),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) {
            // 先取消闹钟，再取消 PendingIntent 本身，防止内存泄漏
            am.cancel(pi)
            pi.cancel()
        }
    }

    // ---------------------------------------------------------------------------
    // Internal
    // ---------------------------------------------------------------------------

    /**
     * 基于 (kind, babyId) 生成稳定的 PendingIntent 请求码。
     *
     * Stable per-(kind, babyId) request code; combines hashes with prime mixer to reduce collisions.
     * 使用质数 31 混合哈希，降低不同 kind+babyId 组合的碰撞概率。
     */
    private fun requestCode(kind: String, babyId: String): Int {
        val h = 31 * kind.hashCode() xor babyId.hashCode()
        return h and 0x7FFFFFFF  // 保证为非负整数，兼容 PendingIntent 要求
    }

    /**
     * 构建发给 [AlarmReceiver] 的基础 Intent。
     *
     * 通过 `data` URI 使 PendingIntent 的 identity 与 (kind, babyId) 绑定，
     * 确保取消时能精确匹配。
     */
    private fun baseIntent(kind: String, babyId: String): Intent =
        Intent(context, AlarmReceiver::class.java).apply {
            action = kind
            // Make the intent identity stable so PendingIntent matching is reliable on cancel.
            // URI 格式：sleepwhisper://alarm/{kind}/{babyId}，保证 PendingIntent 的唯一匹配
            data = android.net.Uri.parse("sleepwhisper://alarm/$kind/$babyId")
            putExtra(AlarmReceiver.EXTRA_KIND, kind)
            putExtra(AlarmReceiver.EXTRA_BABY_ID, babyId)
        }

    /**
     * 判断给定时刻是否落在静默时段内。
     *
     * 支持跨午夜的静默区间（如 22:00–07:00）和普通区间（如 09:00–18:00）。
     *
     * @param at         待判断的时间戳（Unix 毫秒）
     * @param quietStart 静默时段起始小时（0–23）
     * @param quietEnd   静默时段结束小时（0–23）
     */
    private fun isInQuietHours(at: Long, quietStart: Int, quietEnd: Int): Boolean {
        val cal = Calendar.getInstance().apply { timeInMillis = at }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        return if (quietStart > quietEnd) {
            // 跨午夜区间，例如 22:00–07:00：hour >= 22 或 hour < 7
            hour >= quietStart || hour < quietEnd
        } else {
            // 普通区间，例如 09:00–18:00
            hour in quietStart until quietEnd
        }
    }

    /**
     * 优先使用精确闹钟调度；Android 12+ 无权限时降级为非精确闹钟并弹出警告 Toast。
     *
     * @param kind      提醒类型
     * @param babyId    宝宝 ID
     * @param triggerAt 触发时刻（Unix 毫秒）
     * @param title     通知标题
     * @param body      通知正文
     */
    private fun scheduleExactOrFallback(
        kind: String,
        babyId: String,
        triggerAt: Long,
        title: String,
        body: String
    ) {
        // 将标题和正文写入 Intent，供 AlarmReceiver 发送通知时使用
        val intent = baseIntent(kind, babyId).apply {
            putExtra(AlarmReceiver.EXTRA_TITLE, title)
            putExtra(AlarmReceiver.EXTRA_BODY, body)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            requestCode(kind, babyId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE  // 已有则更新内容
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ 需要检查用户是否授予精确闹钟权限
            if (canScheduleExactAlarmsCompat()) {
                // 有权限：使用精确闹钟，Doze 下也能准时触发
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            } else {
                // 无精确闹钟权限：降级为非精确闹钟，并提示用户授权
                AlarmManagerCompat.setAndAllowWhileIdle(am, AlarmManager.RTC_WAKEUP, triggerAt, pi)
                toast.show(R.string.toast_exact_alarm_needed, style = Style.WARNING)
            }
        } else {
            // Android 11 及以下：直接使用精确闹钟，无需额外权限检查
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    /**
     * 兼容性精确闹钟权限检查。
     *
     * API 33 (Tiramisu) replaced `canScheduleExactAlarms()` with `canUseExactAlarms()` for apps
     * that hold the USE_EXACT_ALARM permission. We declare both in the manifest, so we accept
     * either signal as proof of authorization.
     *
     * @return 当前是否可使用精确闹钟
     */
    private fun canScheduleExactAlarmsCompat(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true  // Android 11 及以下无需检查
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && am.canScheduleExactAlarms()) return true  // Android 13+ canUseExactAlarms
        return am.canScheduleExactAlarms()  // Android 12 canScheduleExactAlarms
    }
}
