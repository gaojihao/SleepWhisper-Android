/**
 * 应用入口：[Application] 子类。
 *
 * **职责**：
 * - 触发 Hilt 依赖图初始化（`@HiltAndroidApp`）。
 * - 在任何组件发送通知之前，确保所有通知渠道已创建。
 *
 * **所在层**：app 模块 / 应用生命周期层。
 *
 * **与谁交互**：
 * - [NotificationChannels]（core/notifications）— 负责在系统中注册通知渠道。
 *
 * **关键约定**：此处不做任何业务初始化；状态初始化由 [AppStateContainer] 的 `init` 块完成。
 */
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
        // 在任何组件发送通知之前确保通知渠道已创建（Android 8+ 必须提前注册渠道）。
        channels.ensure()
    }
}
