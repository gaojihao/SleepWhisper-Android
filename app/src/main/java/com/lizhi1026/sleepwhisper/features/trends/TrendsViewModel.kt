package com.lizhi1026.sleepwhisper.features.trends

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lizhi1026.sleepwhisper.app.AppStateContainer
import com.lizhi1026.sleepwhisper.core.persistence.Repository
import com.lizhi1026.sleepwhisper.model.SleepRecommendation
import com.lizhi1026.sleepwhisper.model.SleepSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrendsViewModel @Inject constructor(
    private val app: AppStateContainer,
    private val repo: Repository
) : ViewModel() {

    val currentRecommendation: LiveData<SleepRecommendation?> = app.currentRecommendation

    private val _weeklyBuckets = MutableLiveData<List<Pair<String, Long>>>(emptyList())
    val weeklyBuckets: LiveData<List<Pair<String, Long>>> get() = _weeklyBuckets

    init { refresh() }

    fun refresh() {
        val b = app.baby.value ?: return
        viewModelScope.launch {
            val sevenDays = System.currentTimeMillis() - 7 * 24 * 3600_000L
            val sleeps: List<SleepSession> = repo.sleepSessions(b.id, sinceMs = sevenDays)
            val now = System.currentTimeMillis()
            val buckets = (0..6).map { offset ->
                val dayStart = now - (6 - offset) * 24 * 3600_000L
                val dayEnd = dayStart + 24 * 3600_000L
                val total = sleeps
                    .filter { it.startAt in dayStart until dayEnd && it.endAt != null }
                    .sumOf { it.durationSeconds() }
                "D${offset + 1}" to total
            }
            _weeklyBuckets.postValue(buckets)
        }
    }
}
