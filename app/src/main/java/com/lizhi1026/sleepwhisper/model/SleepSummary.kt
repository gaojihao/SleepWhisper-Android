package com.lizhi1026.sleepwhisper.model

import kotlinx.serialization.Serializable

// 来源：iOS SleepWhisperApp.swift 300-331 行（"醒来反馈卡"数据结构）

@Serializable
data class SleepSummary(
    val justFinishedDurationSec: Long,
    val todayTotalSec: Long,
    val diffVsYesterdayMin: Int,
    val hasYesterdayData: Boolean
) {
    /** true → 今天高于/等于昨天，趋势良好。 */
    val trendIsPositive: Boolean get() = diffVsYesterdayMin >= 0
}
