/**
 * TrendsViewModel.kt — 趋势统计屏幕的 ViewModel（features/trends 层）
 *
 * 职责：
 * - 一次性拉取过去 7 天的睡眠、喂食、尿布三张表数据，分桶计算 weeklyBuckets
 * - 暴露今日睡眠（todaySleeps）和今日喂食（todayFeedings）供时间轴渲染
 * - 近 24 h 喂食 + 尿布合并排序，最多取 8 条（recentEvents）
 * - 通过 MediatorLiveData（hasAnyData）聚合判断是否有任何有效记录
 * - 将 AI 睡眠建议（currentRecommendation）直接透传自 AppStateContainer
 *
 * 数据流：init → refresh() → viewModelScope.launch → 协程拉取 → postValue
 */
package com.lizhi1026.sleepwhisper.features.trends

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lizhi1026.sleepwhisper.app.AppStateContainer
import com.lizhi1026.sleepwhisper.core.persistence.Repository
import com.lizhi1026.sleepwhisper.model.DiaperEvent
import com.lizhi1026.sleepwhisper.model.EventEditTarget
import com.lizhi1026.sleepwhisper.model.FeedingEvent
import com.lizhi1026.sleepwhisper.model.SleepRecommendation
import com.lizhi1026.sleepwhisper.model.SleepSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject

/**
 * 趋势统计屏幕 ViewModel，由 Hilt 注入。
 *
 * 暴露的 LiveData：
 * - [weeklyBuckets]：过去 7 日睡眠分桶，List<Pair<标签"D1"~"D7", 总秒数>>
 * - [todaySleeps]：今日（00:00 起）的睡眠记录
 * - [todayFeedings]：今日（00:00 起）的喂食记录
 * - [recentEvents]：近 24 h 喂食 + 尿布事件，按时间倒序，最多 8 条
 * - [hasAnyData]：MediatorLiveData，三路汇聚；只要有一路有数据即为 true
 * - [currentRecommendation]：AI 睡眠建议，直接来自 [AppStateContainer]
 */
@HiltViewModel
class TrendsViewModel @Inject constructor(
    val app: AppStateContainer,
    private val repo: Repository
) : ViewModel() {

    // AI 睡眠建议直接透传自全局状态容器，不在本 ViewModel 中计算
    val currentRecommendation: LiveData<SleepRecommendation?> = app.currentRecommendation

    // 过去 7 日睡眠分桶，每项 Pair(日期标签, 总秒数)
    private val _weeklyBuckets = MutableLiveData<List<Pair<String, Long>>>(emptyList())
    val weeklyBuckets: LiveData<List<Pair<String, Long>>> get() = _weeklyBuckets

    // 今日睡眠记录（从当日 00:00 起）
    private val _todaySleeps = MutableLiveData<List<SleepSession>>(emptyList())
    val todaySleeps: LiveData<List<SleepSession>> get() = _todaySleeps

    // 今日喂食记录（从当日 00:00 起）
    private val _todayFeedings = MutableLiveData<List<FeedingEvent>>(emptyList())
    val todayFeedings: LiveData<List<FeedingEvent>> get() = _todayFeedings

    // 近 24 h 喂食 + 尿布事件合并列表（最多 8 条）
    private val _recentEvents = MutableLiveData<List<RecentEvent>>(emptyList())
    val recentEvents: LiveData<List<RecentEvent>> get() = _recentEvents

    /**
     * 聚合数据可用性判断：只要 weeklyBuckets / todaySleeps / todayFeedings
     * 其中一路有有效数据，即返回 true，UI 据此决定是否显示空状态卡。
     */
    val hasAnyData: LiveData<Boolean> = MediatorLiveData<Boolean>().apply {
        // 三路数据变化时均重新计算
        val refresh = {
            value = _weeklyBuckets.value?.any { it.second > 0 } == true ||
                    _todaySleeps.value?.isNotEmpty() == true ||
                    _todayFeedings.value?.isNotEmpty() == true
        }
        addSource(_weeklyBuckets) { refresh() }
        addSource(_todaySleeps) { refresh() }
        addSource(_todayFeedings) { refresh() }
        refresh()
    }

    // ViewModel 创建时立即触发首次数据加载
    init { refresh() }

    /**
     * 单次数据刷新：在协程内一次性从 Repository 拉取过去 7 天三张表，
     * 计算 weeklyBuckets 分桶、today 过滤、recent 事件合并，并通过 postValue 推送结果。
     *
     * 若当前未绑定宝宝（[AppStateContainer.baby] 为 null）则直接返回不执行。
     */
    fun refresh() {
        val b = app.baby.value ?: return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            // 7 天前时间戳，作为三张表查询的起始边界
            val sevenDaysAgo = now - 7 * 24 * 3600_000L
            // 今日 00:00 时间戳，用于过滤今日记录和分桶计算
            val todayStartMs = ZonedDateTime.now(ZoneId.systemDefault())
                .with(LocalTime.MIDNIGHT)
                .toInstant().toEpochMilli()
            // 近 24h 起始时间戳，用于筛选 recentEvents
            val last24h = now - 24 * 3600_000L

            // 一次性拉取三张表（均为挂起函数，顺序执行）
            val sleeps = repo.sleepSessions(b.id, sinceMs = sevenDaysAgo)
            val feedings = repo.feedings(b.id, sinceMs = sevenDaysAgo)
            val diapers = repo.diapers(b.id, sinceMs = sevenDaysAgo)

            // Weekly buckets (7 days incl. today).
            // offset=0 对应 7 天前（D1），offset=6 对应今天（D7）
            val buckets = (0..6).map { offset ->
                val dayStart = todayStartMs - (6 - offset) * 24 * 3600_000L
                val dayEnd = dayStart + 24 * 3600_000L
                // 统计该天内所有已结束睡眠的总时长（秒）
                val total = sleeps
                    .filter { it.startAt in dayStart until dayEnd && it.endAt != null }
                    .sumOf { it.durationSeconds(now) }
                "D${offset + 1}" to total
            }

            // 过滤出今日（从 00:00 起）的睡眠和喂食
            val todaySleeps = sleeps.filter { it.startAt >= todayStartMs }
            val todayFeedings = feedings.filter { it.startedAt >= todayStartMs }

            // Recent 24h events (feeding + diaper), capped 8, newest first.
            val recent = buildList<RecentEvent> {
                // 将近 24h 喂食包装为 RecentEvent.OfFeeding
                feedings.filter { it.startedAt >= last24h }.forEach {
                    add(RecentEvent.OfFeeding(it))
                }
                // 将近 24h 尿布包装为 RecentEvent.OfDiaper
                diapers.filter { it.occurredAt >= last24h }.forEach {
                    add(RecentEvent.OfDiaper(it))
                }
            }.sortedByDescending { it.timestamp }.take(8) // 按时间戳倒序后截取最多 8 条

            // 通过 postValue 在主线程安全地更新 LiveData
            _weeklyBuckets.postValue(buckets)
            _todaySleeps.postValue(todaySleeps)
            _todayFeedings.postValue(todayFeedings)
            _recentEvents.postValue(recent)
        }
    }

    /**
     * 触发编辑指定近期事件，通过 [AppStateContainer.requestEdit] 弹出 EventEditSheet。
     *
     * @param event 要编辑的事件，[RecentEvent.OfFeeding] 或 [RecentEvent.OfDiaper]
     */
    fun beginEdit(event: RecentEvent) {
        when (event) {
            // 喂食事件 → 包装为 EventEditTarget.Feeding
            is RecentEvent.OfFeeding -> app.requestEdit(EventEditTarget.Feeding(event.event))
            // 尿布事件 → 包装为 EventEditTarget.Diaper
            is RecentEvent.OfDiaper -> app.requestEdit(EventEditTarget.Diaper(event.event))
        }
    }
}

/**
 * 近期事件的轻量 sealed 包装类，统一喂食与尿布两种事件类型，
 * 便于 [RecentEventsList] 统一渲染和排序。
 *
 * 子类：
 * - [OfFeeding]：包装 [FeedingEvent]，timestamp 取 startedAt
 * - [OfDiaper]：包装 [DiaperEvent]，timestamp 取 occurredAt
 */
sealed class RecentEvent {
    abstract val timestamp: Long
    data class OfFeeding(val event: FeedingEvent) : RecentEvent() {
        override val timestamp: Long get() = event.startedAt
    }
    data class OfDiaper(val event: DiaperEvent) : RecentEvent() {
        override val timestamp: Long get() = event.occurredAt
    }
}
