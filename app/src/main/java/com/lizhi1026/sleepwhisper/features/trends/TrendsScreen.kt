/**
 * TrendsScreen.kt — 趋势统计主屏幕（UI 层 / features/trends）
 *
 * 职责：
 * - 聚合展示过去 7 天的睡眠柱状图（WeeklyCard / WeeklyBars）
 * - 展示今日睡眠与喂食时间轴（TodayCard → TodayTimeline）
 * - 展示近 24 h 喂食 + 尿布事件列表（RecentEventsList）
 * - 无数据时显示空状态占位卡（EmptyStateCard）
 * - 若 AI 睡眠建议存在则在底部展示；否则显示"正在生成建议"占位卡
 *
 * 典型用户操作：
 * 1. 进入"趋势"Tab → 屏幕自动刷新，展示 7 日睡眠柱图和今日时间轴
 * 2. 长按近期事件行 → 触发 ViewModel.beginEdit() → 弹出 EventEditSheet
 */
package com.lizhi1026.sleepwhisper.features.trends

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lizhi1026.sleepwhisper.R
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor
import com.lizhi1026.sleepwhisper.core.visualkit.SWFont
import com.lizhi1026.sleepwhisper.core.visualkit.SWSpacing
import com.lizhi1026.sleepwhisper.core.visualkit.floatingY
import com.lizhi1026.sleepwhisper.core.visualkit.components.AuroraBackdrop
import com.lizhi1026.sleepwhisper.core.visualkit.components.ElevationLevel
import com.lizhi1026.sleepwhisper.core.visualkit.components.EmptyStateCard
import com.lizhi1026.sleepwhisper.core.visualkit.components.GlassCard
import com.lizhi1026.sleepwhisper.core.visualkit.components.Hairline
import com.lizhi1026.sleepwhisper.core.visualkit.components.SectionLabel

/**
 * 趋势统计主屏幕入口 Composable。
 *
 * 通过 [TrendsViewModel] 观察以下 LiveData：
 * - [TrendsViewModel.weeklyBuckets]：过去 7 日睡眠分桶数据
 * - [TrendsViewModel.currentRecommendation]：AI 睡眠建议（可为 null）
 * - [TrendsViewModel.todaySleeps] / [TrendsViewModel.todayFeedings]：今日记录
 * - [TrendsViewModel.recentEvents]：近 24 h 合并事件列表（最多 8 条）
 * - [TrendsViewModel.hasAnyData]：MediatorLiveData，决定是否显示空状态
 */
@Composable
fun TrendsScreen(vm: TrendsViewModel = hiltViewModel()) {
    val scheme = LocalSWScheme.current
    val buckets by vm.weeklyBuckets.observeAsState(emptyList())
    val rec by vm.currentRecommendation.observeAsState(null)
    val todaySleeps by vm.todaySleeps.observeAsState(emptyList())
    val todayFeedings by vm.todayFeedings.observeAsState(emptyList())
    val recent by vm.recentEvents.observeAsState(emptyList())
    val hasAnyData by vm.hasAnyData.observeAsState(false)
    val baby by vm.app.baby.observeAsState(null)

    Box(modifier = Modifier.fillMaxSize()) {
        AuroraBackdrop()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SWSpacing.lg)
                .padding(top = SWSpacing.md),
            verticalArrangement = Arrangement.spacedBy(SWSpacing.lg)
        ) {
            // 顶部标题区：屏幕标题 + 婴儿姓名副标题
            Header(babyName = baby?.name)

            if (!hasAnyData) {
                // 无任何睡眠/喂食记录时显示空状态引导卡
                EmptyStateCard(
                    titleRes = R.string.trends_empty_title,
                    subtitleRes = R.string.trends_empty_subtitle,
                    iconRes = R.drawable.ic_empty_moon
                )
            } else {
                // 今日时间轴卡片：睡眠色块 + 喂食点
                TodayCard(todaySleeps, todayFeedings)
                if (recent.isNotEmpty()) {
                    // 近 24 h 事件列表：长按可进入编辑
                    RecentEventsList(events = recent, onLongPress = vm::beginEdit)
                }
                // 过去 7 日睡眠柱状图卡片
                WeeklyCard(buckets)
            }

            if (hasAnyData && rec == null) {
                // 有数据但 AI 建议尚未生成时，显示"建议生成中"占位卡
                EmptyStateCard(
                    titleRes = R.string.trends_aiasleep_title,
                    subtitleRes = R.string.trends_aiasleep_subtitle
                )
            }
            Spacer(Modifier.height(SWSpacing.huge))
        }
    }
}

/**
 * 屏幕顶部标题区域。
 *
 * 显示"趋势"大标题、装饰分割线，以及婴儿姓名副标题（全大写字母间距）。
 * 若 [babyName] 为 null（未绑定宝宝），副标题显示为空字符串。
 */
@Composable
private fun Header(babyName: String?) {
    val scheme = LocalSWScheme.current
    Column(verticalArrangement = Arrangement.spacedBy(SWSpacing.xs)) {
        BasicText(
            text = stringResource(R.string.trends_title),
            style = SWFont.serif(28).copy(color = SWColor.textPrimary(scheme))
        )
        Hairline(modifier = Modifier.width(60.dp).padding(top = SWSpacing.xs))
        BasicText(
            text = stringResource(R.string.trends_subtitle, babyName ?: "").uppercase(),
            style = SWFont.labelMD().copy(
                color = SWColor.textSecondary(scheme),
                letterSpacing = 1.2.sp
            )
        )
    }
}

/**
 * 今日睡眠与喂食卡片，内嵌 [TodayTimeline] 时间轴。
 *
 * 带浮动动画（floatingY）让卡片轻微上下漂浮，增强视觉层次感。
 *
 * @param sleeps 今日（00:00 起）的睡眠记录列表
 * @param feedings 今日（00:00 起）的喂食记录列表
 */
@Composable
private fun TodayCard(
    sleeps: List<com.lizhi1026.sleepwhisper.model.SleepSession>,
    feedings: List<com.lizhi1026.sleepwhisper.model.FeedingEvent>
) {
    val scheme = LocalSWScheme.current
    GlassCard(
        modifier = Modifier.fillMaxWidth().floatingY(amplitude = 2.dp, durationMillis = 9000),
        cornerRadius = 22.dp,
        contentPadding = PaddingValues(SWSpacing.lg),
        elevation = ElevationLevel.MEDIUM
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(SWSpacing.sm)) {
            SectionLabel(stringResource(R.string.trends_today))
            TodayTimeline(sleeps = sleeps, feedings = feedings)
        }
    }
}

/**
 * 过去 7 日睡眠统计卡片。
 *
 * 顶部展示段标题与 7 日平均睡眠小时数（avgHours），
 * 下方嵌套 [WeeklyBars] Canvas 柱状图。
 *
 * @param buckets ViewModel 计算好的 7 个分桶，每项为 Pair(标签"D1"~"D7", 总秒数)
 */
@Composable
private fun WeeklyCard(buckets: List<Pair<String, Long>>) {
    val scheme = LocalSWScheme.current
    // 7 日总秒数，用于计算日均睡眠小时
    val totalSec = buckets.sumOf { it.second }
    // 日均睡眠小时 = 总秒 / 3600 / 7
    val avgHours = totalSec.toDouble() / 3600.0 / 7.0
    val avgHoursFormatted = "%.1f".format(avgHours)
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        contentPadding = PaddingValues(SWSpacing.lg),
        elevation = ElevationLevel.MEDIUM
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(SWSpacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                SectionLabel(stringResource(R.string.trends_week_title))
                BasicText(
                    text = stringResource(R.string.trends_week_avg, avgHoursFormatted),
                    style = SWFont.titleMD().copy(color = SWColor.textPrimary(scheme))
                )
            }
            Hairline()
            WeeklyBars(buckets)
        }
    }
}

/**
 * 7 日睡眠 Canvas 柱状图。
 *
 * 每列对应一个分桶（D1~D7）：
 * - 柱高按当日总睡眠秒数与 7 日最大值的比例换算
 * - 最后一列（今日）使用 aurora 双色渐变突出显示
 * - 最短柱高不低于 8dp，避免零值时完全不可见
 *
 * @param buckets ViewModel 计算好的 7 个分桶列表
 */
@Composable
private fun WeeklyBars(buckets: List<Pair<String, Long>>) {
    val scheme = LocalSWScheme.current
    // 取 7 日最大秒数作为柱高基准，至少为 1 防止除零
    val maxSec = buckets.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
    // 最后一条为"今日"，用于高亮判断
    val today = buckets.lastOrNull()
    Row(
        modifier = Modifier.fillMaxWidth().height(200.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        buckets.forEach { (label, secs) ->
            val isToday = (label == today?.first)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(SWSpacing.xs)
            ) {
                Box(modifier = Modifier.height(180.dp), contentAlignment = Alignment.BottomCenter) {
                    // 根据是否为今日选择高亮渐变还是低调单色
                    val brush = chartBarBrush(isHighlighted = isToday)
                    Canvas(modifier = Modifier.width(22.dp).height(180.dp)) {
                        // 按比例计算柱高，最低 8dp 保证可见性
                        val h = (secs.toFloat() / maxSec) * size.height
                        val capsuleH = h.coerceAtLeast(8.dp.toPx())
                        drawRoundRect(
                            brush = brush,
                            topLeft = Offset(0f, size.height - capsuleH),
                            size = Size(size.width, capsuleH),
                            cornerRadius = CornerRadius(size.width / 2f)
                        )
                    }
                }
                // 列底部日期标签（D1~D7）
                BasicText(
                    text = label,
                    style = SWFont.labelSM().copy(color = SWColor.textSecondary(scheme))
                )
            }
        }
    }
}

/**
 * Vertical gradient brush for the WeeklyBars chart. Today's column uses a
 * dual-color aurora gradient that visually pops; non-today columns use accent
 * at 70% alpha for a quieter row that doesn't compete.
 */
@Composable
private fun chartBarBrush(isHighlighted: Boolean): Brush {
    val scheme = LocalSWScheme.current
    return if (isHighlighted) {
        Brush.verticalGradient(
            colors = listOf(
                SWColor.accent(scheme),
                SWColor.accentSecondary(scheme)
            )
        )
    } else {
        val base = SWColor.accent(scheme).copy(alpha = 0.7f)
        Brush.verticalGradient(colors = listOf(base, base))
    }
}
