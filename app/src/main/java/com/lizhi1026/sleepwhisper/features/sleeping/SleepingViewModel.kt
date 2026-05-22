/**
 * SleepingViewModel.kt — 睡眠进行中界面的 ViewModel（features/sleeping）
 *
 * 用途：为 [SleepingScreen] 提供可观测状态，并将用户操作委托给 [AppStateContainer]。
 * 架构说明：本 ViewModel **无内部状态机**；开始、暂停、结束睡眠的所有状态完全由
 *           AppStateContainer 集中管理，ViewModel 仅做轻量透传（thin wrapper）。
 * 用户交互流程：
 *   - [ongoingSleep] 驱动计时器显示（startAt 由 Screen 内 LaunchedEffect 读取）；
 *   - [onWake] 触发 app.endSleep()，之后 AppStateContainer 更新 ongoingSleep → null，
 *     导航由上层观察 baby / ongoingSleep 变化完成；
 *   - [pausePlayer] / [resumePlayer] 直接转发到 AudioPlayer，无需协程。
 */
package com.lizhi1026.sleepwhisper.features.sleeping

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lizhi1026.sleepwhisper.app.AppStateContainer
import com.lizhi1026.sleepwhisper.core.audio.PlayerState
import com.lizhi1026.sleepwhisper.model.SleepSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 睡眠进行中界面的 ViewModel。
 *
 * 无内部状态机：所有睡眠状态均由 [AppStateContainer] 集中管理，ViewModel 仅做轻量透传。
 *
 * @param app 全局应用状态容器（Hilt 注入）
 */
@HiltViewModel
class SleepingViewModel @Inject constructor(
    val app: AppStateContainer
) : ViewModel() {
    /** 当前进行中的睡眠会话，null 表示无活跃会话 */
    val ongoingSleep: LiveData<SleepSession?> = app.ongoingSleep
    /** 音频播放器状态（Idle / Playing / Paused） */
    val playerState: LiveData<PlayerState> = app.audioPlayer.stateLive
    /** 当前激活的宝宝档案 */
    val baby = app.baby

    /** 结束睡眠：在 viewModelScope 中委托 AppStateContainer 完成会话收尾与摘要生成 */
    fun onWake() = viewModelScope.launch { app.endSleep() }
    /** 暂停白噪音播放（直接转发，无需协程） */
    fun pausePlayer() = app.audioPlayer.pause()
    /** 恢复白噪音播放（直接转发，无需协程） */
    fun resumePlayer() = app.audioPlayer.resume()
}
