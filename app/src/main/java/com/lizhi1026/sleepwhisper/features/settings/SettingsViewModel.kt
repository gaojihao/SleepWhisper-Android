/**
 * SettingsViewModel — 设置功能的 ViewModel（UI 层 / features/settings）
 *
 * 职责：
 *   - 向 UI 暴露响应式数据：[baby]（宝宝档案）、[settings]（用户设置）、
 *     [forceNightPreview]（夜间预览临时开关，不持久化）。
 *   - 提供 [update] 统一入口：接收 UserSettings 变换函数，协程异步持久化，
 *     调用方无需关心 IO 调度。
 *   - [setForceNightPreview] 直接操作 themeProvider，仅影响当前进程运行时状态。
 *   - [stopCryDetection] 委托 AppStateContainer.cryDetection 停止后台检测服务。
 */
package com.lizhi1026.sleepwhisper.features.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lizhi1026.sleepwhisper.app.AppStateContainer
import com.lizhi1026.sleepwhisper.model.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 设置页 ViewModel，由 Hilt 注入 [AppStateContainer]。
 *
 * 暴露的 LiveData（只读）：
 * - [baby]             — 宝宝档案，来自 AppStateContainer。
 * - [settings]         — 用户持久化设置，来自 AppStateContainer。
 * - [forceNightPreview] — 夜间预览临时开关状态，来自 themeProvider（不写 DB）。
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val app: AppStateContainer
) : ViewModel() {

    // 宝宝档案 LiveData，来自全局 AppStateContainer。
    val baby = app.baby
    // 用户持久化设置 LiveData。
    val settings: LiveData<UserSettings> = app.settings
    // 夜间预览开关状态；由 themeProvider 持有，进程重启后重置。
    val forceNightPreview: LiveData<Boolean> = app.themeProvider.forceNightPreview

    /**
     * 以函数式变换统一更新 UserSettings 并异步持久化。
     * 若 settings 尚未初始化则使用 DEFAULT 兜底，保证写入不丢失。
     *
     * @param transform 接收当前 UserSettings，返回修改后副本的纯函数。
     */
    fun update(transform: (UserSettings) -> UserSettings) {
        val cur = settings.value ?: UserSettings.DEFAULT
        // 在 viewModelScope 中异步写入，避免阻塞主线程。
        viewModelScope.launch { app.saveSettings(transform(cur)) }
    }

    /**
     * 切换夜间预览状态，直接操作 themeProvider，不写入 UserSettings。
     * 用于 SettingsScreen 外观分区的"夜间预览"开关。
     *
     * @param value 目标开关状态。
     */
    fun setForceNightPreview(value: Boolean) {
        app.themeProvider.setForceNightPreview(value)
    }

    /** 停止哭声检测后台服务，通常在用户关闭检测开关后调用。 */
    fun stopCryDetection() = app.cryDetection.stop()
}
