package com.lizhi1026.sleepwhisper.core.theme

import android.content.Context
import android.content.res.Configuration
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.lizhi1026.sleepwhisper.core.visualkit.SWScheme
import com.lizhi1026.sleepwhisper.model.UserSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 主题 Scheme 决策中心（@Singleton，全局唯一实例）。
 *
 * 所在层：core/theme — 基础设施层，依赖 Android Configuration 与 Jetpack LiveData。
 * 交互对象：[UserSettings]（appearance / nightModeStart / nightModeEnd 字段）；
 *           [Configuration.uiMode]（系统深色模式状态）；[SWScheme]（输出的配色方案枚举）。
 *
 * 决策优先级（从高到低）：
 *   1. forceNightPreview == true → SWScheme.NIGHT（设置页夜间预览专用）
 *   2. 当前时刻在 nightModeStart..nightModeEnd 范围内 → SWScheme.NIGHT
 *   3. appearance == AUTO → 跟随系统 uiMode（深色 → DARK，浅色 → DAY）
 *   4. appearance == DARK → SWScheme.DARK
 *   5. appearance == LIGHT → SWScheme.DAY
 *
 * 自动刷新：每 60 秒触发一次 refresh()，确保跨越夜间时段边界时 scheme 自动切换。
 * 主动刷新：[update]（用户修改设置）、[onSystemConfigurationChanged]（系统深色模式切换）。
 */
@Singleton
class ThemeProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // 当前活跃配色方案，初始为白天模式
    private val _scheme = MutableLiveData(SWScheme.DAY)

    /** 对外暴露的只读配色方案 LiveData，UI 层订阅此字段响应主题变化。 */
    val scheme: LiveData<SWScheme> get() = _scheme

    // 夜间预览强制开关（设置页专用）
    private val _forceNightPreview = MutableLiveData(false)

    /** 夜间预览开关 LiveData，设置页可订阅以同步 UI 状态。 */
    val forceNightPreview: LiveData<Boolean> get() = _forceNightPreview

    // 当前生效的用户设置，默认为默认值
    private var currentSettings: UserSettings = UserSettings.DEFAULT

    // 协程作用域：SupervisorJob 确保子协程异常不影响父作用域
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var ticker: Job? = null // 60 秒定时刷新协程句柄

    init {
        refresh()      // 初始化时立即计算一次 scheme
        startTicker()  // 启动 60 秒定时自动刷新
    }

    /**
     * 用户修改设置后调用，更新缓存的 settings 并立即重算 scheme。
     *
     * @param settings 最新的用户设置对象。
     */
    fun update(settings: UserSettings) {
        currentSettings = settings
        refresh()
    }

    /**
     * 设置夜间预览强制开关，用于设置页实时预览夜间配色效果。
     *
     * @param value true 强制使用 NIGHT scheme；false 恢复正常决策逻辑。
     */
    fun setForceNightPreview(value: Boolean) {
        _forceNightPreview.value = value
        refresh() // 立即重算并更新 scheme
    }

    /**
     * 系统深色模式发生变化时由 MainActivity.onConfigurationChanged 调用，触发 scheme 重算。
     */
    fun onSystemConfigurationChanged() {
        refresh()
    }

    /**
     * 启动 60 秒定时刷新协程。
     * 取消已有 ticker 后重新创建，避免重复定时器。
     */
    private fun startTicker() {
        ticker?.cancel()
        // 每 60 秒自动重算 scheme，保证跨越夜间时段边界时及时切换
        ticker = scope.launch {
            while (true) {
                delay(60_000L) // 等待 60 秒
                refresh()
            }
        }
    }

    /**
     * 核心决策函数：根据 forceNightPreview、夜间时段、外观设置与系统 uiMode 计算目标 scheme，
     * 仅在值发生变化时才更新 LiveData（避免无意义重组）。
     */
    private fun refresh() {
        val previewing = _forceNightPreview.value == true
        val newScheme: SWScheme = when {
            // 最高优先级：设置页强制夜间预览
            previewing -> SWScheme.NIGHT
            // 当前时刻在用户配置的夜间时段内
            currentSettings.isInNightTime(LocalTime.now().hour) -> SWScheme.NIGHT
            // 其他情况：按外观设置决策
            else -> when (currentSettings.appearance) {
                UserSettings.AppearanceMode.AUTO  -> if (isSystemDark()) SWScheme.DARK else SWScheme.DAY
                UserSettings.AppearanceMode.DARK  -> SWScheme.DARK
                UserSettings.AppearanceMode.LIGHT -> SWScheme.DAY
            }
        }
        // 仅在 scheme 发生变化时更新，避免触发不必要的 UI 重组
        if (_scheme.value != newScheme) {
            _scheme.value = newScheme
        }
    }

    /**
     * 读取系统当前 uiMode，判断是否为深色模式。
     *
     * @return true 表示系统当前处于深色（夜间）模式。
     */
    private fun isSystemDark(): Boolean {
        val cfg = context.resources.configuration
        // 通过位掩码提取 UI_MODE_NIGHT_MASK 并与 UI_MODE_NIGHT_YES 比对
        return (cfg.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }
}
