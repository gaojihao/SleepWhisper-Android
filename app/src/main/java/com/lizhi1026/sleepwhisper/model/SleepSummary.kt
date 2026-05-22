/**
 * 单次睡眠结束后的摘要数据模型（model 领域层，纯 Kotlin，无 Android Framework 依赖）。
 *
 * **职责**：封装"醒来反馈卡"所需的统计数据——刚结束的本次睡眠时长、
 * 今日累计睡眠时长、与昨日的差值，以及是否存在昨日对比数据。
 *
 * **数据来源**：对标 iOS SleepWhisperApp.swift 300–331 行中同名结构体。
 *
 * **交互方**：`SleepRepository`（计算并构造此对象）、`WakeUpFeedbackViewModel`
 * / `WakeUpFeedbackCard`（读取并渲染反馈卡 UI）。
 */
package com.lizhi1026.sleepwhisper.model

import kotlinx.serialization.Serializable

// 来源：iOS SleepWhisperApp.swift 300-331 行（"醒来反馈卡"数据结构）

/**
 * 一次睡眠会话结束后生成的摘要快照，不可变数据类。
 *
 * @property justFinishedDurationSec 刚刚结束的本次睡眠时长（秒）
 * @property todayTotalSec           今日（自然日 00:00 起）累计总睡眠时长（秒）
 * @property diffVsYesterdayMin      今日总睡眠与昨日总睡眠的差值（分钟，正值表示今天更多）；
 *                                   当 [hasYesterdayData] 为 false 时此字段无意义，不应展示
 * @property hasYesterdayData        是否存在昨日的有效睡眠数据，用于控制趋势对比区域的显隐
 */
@Serializable
data class SleepSummary(
    val justFinishedDurationSec: Long,
    val todayTotalSec: Long,
    val diffVsYesterdayMin: Int,
    val hasYesterdayData: Boolean
) {
    /**
     * 今日睡眠趋势是否为正向（今日总睡眠 ≥ 昨日总睡眠）。
     *
     * ⚠️ 仅当 [hasYesterdayData] 为 true 时此属性才有意义；
     * 调用方应先判断 [hasYesterdayData] 再读取此值。
     *
     * 实现：直接比较 [diffVsYesterdayMin]，≥ 0 即视为正向趋势。
     */
    val trendIsPositive: Boolean get() = diffVsYesterdayMin >= 0
}
