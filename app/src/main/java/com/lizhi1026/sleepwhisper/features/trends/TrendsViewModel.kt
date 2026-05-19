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

@HiltViewModel
class TrendsViewModel @Inject constructor(
    val app: AppStateContainer,
    private val repo: Repository
) : ViewModel() {

    val currentRecommendation: LiveData<SleepRecommendation?> = app.currentRecommendation

    private val _weeklyBuckets = MutableLiveData<List<Pair<String, Long>>>(emptyList())
    val weeklyBuckets: LiveData<List<Pair<String, Long>>> get() = _weeklyBuckets

    private val _todaySleeps = MutableLiveData<List<SleepSession>>(emptyList())
    val todaySleeps: LiveData<List<SleepSession>> get() = _todaySleeps

    private val _todayFeedings = MutableLiveData<List<FeedingEvent>>(emptyList())
    val todayFeedings: LiveData<List<FeedingEvent>> get() = _todayFeedings

    private val _recentEvents = MutableLiveData<List<RecentEvent>>(emptyList())
    val recentEvents: LiveData<List<RecentEvent>> get() = _recentEvents

    val hasAnyData: LiveData<Boolean> = MediatorLiveData<Boolean>().apply {
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

    init { refresh() }

    fun refresh() {
        val b = app.baby.value ?: return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val sevenDaysAgo = now - 7 * 24 * 3600_000L
            val todayStartMs = ZonedDateTime.now(ZoneId.systemDefault())
                .with(LocalTime.MIDNIGHT)
                .toInstant().toEpochMilli()
            val last24h = now - 24 * 3600_000L

            val sleeps = repo.sleepSessions(b.id, sinceMs = sevenDaysAgo)
            val feedings = repo.feedings(b.id, sinceMs = sevenDaysAgo)
            val diapers = repo.diapers(b.id, sinceMs = sevenDaysAgo)

            // Weekly buckets (7 days incl. today).
            val buckets = (0..6).map { offset ->
                val dayStart = todayStartMs - (6 - offset) * 24 * 3600_000L
                val dayEnd = dayStart + 24 * 3600_000L
                val total = sleeps
                    .filter { it.startAt in dayStart until dayEnd && it.endAt != null }
                    .sumOf { it.durationSeconds(now) }
                "D${offset + 1}" to total
            }

            val todaySleeps = sleeps.filter { it.startAt >= todayStartMs }
            val todayFeedings = feedings.filter { it.startedAt >= todayStartMs }

            // Recent 24h events (feeding + diaper), capped 8, newest first.
            val recent = buildList<RecentEvent> {
                feedings.filter { it.startedAt >= last24h }.forEach {
                    add(RecentEvent.OfFeeding(it))
                }
                diapers.filter { it.occurredAt >= last24h }.forEach {
                    add(RecentEvent.OfDiaper(it))
                }
            }.sortedByDescending { it.timestamp }.take(8)

            _weeklyBuckets.postValue(buckets)
            _todaySleeps.postValue(todaySleeps)
            _todayFeedings.postValue(todayFeedings)
            _recentEvents.postValue(recent)
        }
    }

    fun beginEdit(event: RecentEvent) {
        when (event) {
            is RecentEvent.OfFeeding -> app.requestEdit(EventEditTarget.Feeding(event.event))
            is RecentEvent.OfDiaper -> app.requestEdit(EventEditTarget.Diaper(event.event))
        }
    }
}

/** Light wrapper around either a [FeedingEvent] or [DiaperEvent] for the recent-events list. */
sealed class RecentEvent {
    abstract val timestamp: Long
    data class OfFeeding(val event: FeedingEvent) : RecentEvent() {
        override val timestamp: Long get() = event.startedAt
    }
    data class OfDiaper(val event: DiaperEvent) : RecentEvent() {
        override val timestamp: Long get() = event.occurredAt
    }
}
