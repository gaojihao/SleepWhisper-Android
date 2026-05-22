/**
 * 应用唯一的 Activity。
 *
 * **职责**：
 * - 启用边缘到边缘（edge-to-edge）沉浸式布局，Compose 层自行处理系统栏内边距。
 * - 在 `onCreate` 时请求 Android 13+ 通知权限（`POST_NOTIFICATIONS`）。
 * - 将 Compose 内容树挂载到 [RootRoute]，以 [AppStateContainer] 作为唯一数据源。
 * - 监听系统配置变更（如深色/浅色模式切换），通知 [AppStateContainer.themeProvider]。
 *
 * **所在层**：app 模块 / UI 入口层。
 *
 * **与谁交互**：
 * - [AppStateContainer]（单例）— 读取全局状态、驱动主题切换。
 * - [RootRoute]（Composable）— Compose UI 树根节点。
 *
 * **关键约定**：Activity 不持有任何业务状态；所有业务操作通过 [AppStateContainer] 的公开方法完成。
 */
package com.lizhi1026.sleepwhisper.app

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var app: AppStateContainer

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* 权限结果仅在发送通知时生效；此处无需显示内联 UI 反馈 */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        // 启用边缘到边缘布局，绘制延伸至状态栏和导航栏之下；Compose 层负责处理系统栏内边距。
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()
        setContent { RootRoute(app) }
    }

    /**
     * 仅在 Android 13（TIRAMISU）及以上版本、且尚未授权时，弹出通知权限请求对话框。
     */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        // 系统深色/浅色模式切换时，同步更新主题提供者，使 Compose 层立即响应。
        app.themeProvider.onSystemConfigurationChanged()
    }
}
