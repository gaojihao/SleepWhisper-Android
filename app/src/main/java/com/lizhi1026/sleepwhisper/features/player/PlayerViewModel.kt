/**
 * PlayerViewModel — 播放器功能的 ViewModel（UI 层 / features/player）
 *
 * 职责：
 *   - 向 UI 暴露响应式数据：[playerState]（播放状态）、[currentPreset]（当前预设）、
 *     [allPresets]（全量预设列表）、[baby]（宝宝信息）、[settings]（用户设置）。
 *   - 不直接绑定 AudioPlayerService；所有播放控制均通过 [app.audioPlayer] 代理调用，
 *     解耦 UI 与底层服务生命周期。
 *   - 提供 [recommended] 按月龄筛选推荐预设的纯函数查询。
 *   - 通过 [saveDefaultTimer] 将用户选择的计时时长持久化到 [UserSettings]。
 *
 * 数据流：
 *   AppStateContainer.audioPlayer → LiveData → observeAsState → Composable UI
 */
package com.lizhi1026.sleepwhisper.features.player

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lizhi1026.sleepwhisper.app.AppStateContainer
import com.lizhi1026.sleepwhisper.core.audio.PlayerState
import com.lizhi1026.sleepwhisper.model.AudioPreset
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 播放器 ViewModel，由 Hilt 注入 [AppStateContainer]。
 *
 * 暴露的 LiveData（只读）：
 * - [playerState]    — 播放器当前状态（Loading / Playing / FadingOut / Paused 等）。
 * - [currentPreset]  — 当前激活的音频预设；未播放时为 null。
 * - [allPresets]     — 等同于 [AudioPreset.bundled]，全量内置预设列表（不可变）。
 * - [baby]           — 宝宝档案，来自 AppStateContainer。
 * - [settings]       — 用户偏好设置，来自 AppStateContainer。
 */
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val app: AppStateContainer
) : ViewModel() {
    // 播放器实时状态，由 app.audioPlayer 代理持有，ViewModel 仅做透传。
    val playerState: LiveData<PlayerState> = app.audioPlayer.stateLive
    // 当前激活预设；停止播放后变为 null，驱动 UI 退出高亮状态。
    val currentPreset = app.audioPlayer.currentPresetLive
    // 宝宝档案 LiveData，来自全局 AppStateContainer。
    val baby = app.baby
    // 用户设置 LiveData，包含 defaultTimerMinutes 等持久化字段。
    val settings = app.settings

    // 全量内置预设，等同于 AudioPreset.bundled，运行时不可变。
    val allPresets: List<AudioPreset> = AudioPreset.bundled

    /**
     * 根据宝宝月龄返回推荐预设列表（纯函数，无副作用）。
     *
     * @param forAgeMonths 宝宝当前月龄。
     * @return 按推荐月龄范围过滤后的预设列表。
     */
    fun recommended(forAgeMonths: Int): List<AudioPreset> =
        AudioPreset.recommended(forAgeMonths)

    /**
     * 启动指定预设的播放。
     * 不直接绑定 AudioPlayerService，通过 [app.audioPlayer] 代理调用。
     *
     * @param presetId 要播放的预设 ID。
     * @param minutes  播放时长（分钟），内部转换为秒后传入底层播放器。
     */
    fun playPreset(presetId: String, minutes: Int) {
        // 将分钟数转换为秒，通过 audioPlayer 代理发起播放请求。
        app.audioPlayer.play(presetId, durationSeconds = minutes * 60)
    }

    /** 停止当前播放，通过 audioPlayer 代理调用。 */
    fun stop() = app.audioPlayer.stop()

    /**
     * 将用户选择的默认计时时长持久化到 [UserSettings.defaultTimerMinutes]。
     * 在 viewModelScope 中异步写入，避免阻塞主线程。
     *
     * @param minutes 用户选中的时长（分钟）。
     */
    fun saveDefaultTimer(minutes: Int) {
        viewModelScope.launch {
            // 读取当前设置快照；若 settings 尚未初始化则跳过写入。
            val cur = settings.value ?: return@launch
            app.saveSettings(cur.copy(defaultTimerMinutes = minutes))
        }
    }
}
