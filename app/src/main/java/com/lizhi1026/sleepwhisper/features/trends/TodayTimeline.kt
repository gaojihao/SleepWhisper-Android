/**
 * TodayTimeline.kt — 今日睡眠与喂食水平时间轴（UI 层 / features/trends）
 *
 * 职责：
 * - 以 24 小时为横轴，在 Canvas 中绘制背景轨道、睡眠色块（胶囊形）、喂食圆点
 * - 跨日睡眠（startAt < 00:00）会自动裁剪到今日 00:00 起点
 * - 正在进行的睡眠（endAt == null）以当前时间为临时终点
 * - 底部 HourLegend 显示 0 / 6 / 12 / 18 / 24 刻度标签
 */
package com.lizhi1026.sleepwhisper.features.trends

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor
import com.lizhi1026.sleepwhisper.core.visualkit.SWFont
import com.lizhi1026.sleepwhisper.model.FeedingEvent
import com.lizhi1026.sleepwhisper.model.SleepSession
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * 今日 24 小时水平时间轴 Composable。
 *
 * Canvas 绘制层次（从底到顶）：
 * 1. 背景轨道（surfaceSunken 圆角矩形）
 * 2. 睡眠色块（primary 颜色胶囊，跨日自动裁剪）
 * 3. 喂食圆点（accent 颜色小圆，半径 4dp）
 *
 * 底部由 [HourLegend] 提供 0/6/12/18/24 文字刻度。
 *
 * @param sleeps 今日睡眠记录（可含跨日和进行中记录）
 * @param feedings 今日喂食记录
 * @param modifier 外部传入的 Modifier
 */
@Composable
fun TodayTimeline(
    sleeps: List<SleepSession>,
    feedings: List<FeedingEvent>,
    modifier: Modifier = Modifier
) {
    val scheme = LocalSWScheme.current
    // 缓存今日 00:00 时间戳，避免重组时重复计算
    val todayStartMs = remember24hStart()
    val dayMs = 24 * 3600_000L
    val now = System.currentTimeMillis()

    Box(modifier = modifier.fillMaxWidth()) {
        Canvas(modifier = Modifier.fillMaxWidth().height(64.dp)) {
            // Track background（背景轨道，占 Canvas 高度的 60%，垂直居中）
            val trackHeight = size.height * 0.6f
            val trackY = (size.height - trackHeight) / 2f
            drawRoundRect(
                color = SWColor.surfaceSunken(scheme),
                topLeft = Offset(0f, trackY),
                size = Size(size.width, trackHeight),
                cornerRadius = CornerRadius(trackHeight / 2f)
            )

            // Sleep capsules（睡眠色块：跨日裁剪到今日 00:00，进行中取当前时间为终点）
            sleeps.forEach { s ->
                val rawStart = s.startAt.coerceAtLeast(todayStartMs)
                val rawEnd = (s.endAt ?: now).coerceAtMost(todayStartMs + dayMs)
                // 跳过有效区间为零或负值的记录（如跨日但已超出今日范围）
                if (rawEnd <= rawStart) return@forEach
                // 将时间戳映射到 Canvas 横轴像素坐标
                val xStart = (rawStart - todayStartMs).toFloat() / dayMs * size.width
                val xEnd = (rawEnd - todayStartMs).toFloat() / dayMs * size.width
                drawRoundRect(
                    color = SWColor.primary(scheme).copy(alpha = 0.85f),
                    topLeft = Offset(xStart, trackY),
                    // 最小宽度 2dp，保证极短睡眠也可见
                    size = Size((xEnd - xStart).coerceAtLeast(2.dp.toPx()), trackHeight),
                    cornerRadius = CornerRadius(trackHeight / 2f)
                )
            }

            // Feeding dots（喂食圆点：accent 颜色，半径 4dp，垂直居中）
            feedings.forEach { f ->
                val x = (f.startedAt - todayStartMs).toFloat() / dayMs * size.width
                drawCircle(
                    color = SWColor.accent(scheme),
                    radius = 4.dp.toPx(),
                    center = Offset(x, size.height / 2f)
                )
            }
        }
        // 时间刻度图例，紧贴 Canvas 下方
        HourLegend(modifier = Modifier.fillMaxWidth())
    }
}

/**
 * 时间轴底部小时刻度图例（0 / 6 / 12 / 18 / 24），
 * 使用 Row SpaceBetween 均匀分布，字体为 labelSM 三级文字色。
 */
@Composable
private fun HourLegend(modifier: Modifier = Modifier) {
    val scheme = LocalSWScheme.current
    Row(modifier = modifier.padding(top = 70.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
        listOf("0", "6", "12", "18", "24").forEach { h ->
            BasicText(
                text = h,
                style = SWFont.labelSM().copy(
                    color = SWColor.textTertiary(scheme),
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}

/**
 * 用 [remember] 缓存今日 00:00:00 的毫秒时间戳。
 * 避免每次重组时重新构造 ZonedDateTime，保持引用稳定性。
 */
@Composable
private fun remember24hStart(): Long = remember {
    ZonedDateTime.now(ZoneId.systemDefault())
        .with(LocalTime.MIDNIGHT)
        .toInstant().toEpochMilli()
}

// 工具函数：将毫秒时间戳格式化为 "HH:mm" 字符串（调试/扩展用，当前未在 UI 中直接使用）
@Suppress("unused")
private fun nowFormatted(ms: Long): String =
    java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(ms))

// 工具函数：从毫秒时间戳提取本地时区的小时数（调试/扩展用，当前未在 UI 中直接使用）
@Suppress("unused")
private fun instantHour(ms: Long): Int =
    Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).hour
