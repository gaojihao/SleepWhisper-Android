package com.lizhi1026.sleepwhisper.core.export

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.lizhi1026.sleepwhisper.model.Baby
import com.lizhi1026.sleepwhisper.model.DiaperEvent
import com.lizhi1026.sleepwhisper.model.FeedingEvent
import com.lizhi1026.sleepwhisper.model.FeedingEvent.FeedingMethod
import com.lizhi1026.sleepwhisper.model.SleepSession
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 日报 PDF 生成器（单页 A4 格式）。
 *
 * 所在层：core/export — 基础设施层，依赖 Android Framework（android.graphics.pdf.PdfDocument）。
 * 交互对象：[Baby]、[SleepSession]、[FeedingEvent]、[DiaperEvent]（输入数据）；
 *           [FileProvider]（将 cacheDir 内的临时文件转为可分享的 content:// URI）。
 *
 * 使用流程：
 *   1. 调用 [buildReport] 生成 PDF 文件至 cacheDir，文件名含宝宝名称与时间戳。
 *   2. 调用 [fileProviderUri] 获取 FileProvider URI。
 *   3. 将 URI 挂载到 ACTION_SEND Intent 实现分享，或直接传给下载管理器。
 *
 * 注意：输出文件为临时文件，系统在必要时可能清理 cacheDir；如需持久保留，请复制到外部存储。
 */
@Singleton
class PdfExporter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /** A4 页面尺寸（72 dpi）：宽 595pt × 高 842pt。 */
    private val pageWidthPt = 595
    private val pageHeightPt = 842
    private val leftMarginPt = 40f   // 左边距（pt）
    private val topMarginPt = 56f    // 顶部边距（pt）

    /**
     * 生成单页 A4 日报 PDF 文件并写入 cacheDir。
     *
     * @param baby    宝宝档案，用于页眉展示名称与月龄。
     * @param sleeps  当日睡眠记录列表。
     * @param feeds   当日喂养记录列表。
     * @param diapers 当日尿布记录列表。
     * @param now     报告基准时间戳（毫秒），默认为系统当前时间。
     * @return 写入完成的 PDF 临时文件，路径位于 cacheDir 下。
     */
    fun buildReport(
        baby: Baby,
        sleeps: List<SleepSession>,
        feeds: List<FeedingEvent>,
        diapers: List<DiaperEvent>,
        now: Long = System.currentTimeMillis()
    ): File {
        val doc = PdfDocument()
        // 创建 A4 单页
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidthPt, pageHeightPt, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas

        // 标题画笔：深色、粗体
        val titlePaint = Paint().apply {
            color = 0xFF0E1116.toInt()
            textSize = 26f
            isFakeBoldText = true
        }
        // 副标题画笔：灰色、中号
        val subPaint = Paint().apply {
            color = 0xFF5F6470.toInt()
            textSize = 14f
        }
        // 正文画笔：深色、小号
        val bodyPaint = Paint().apply {
            color = 0xFF0E1116.toInt()
            textSize = 12f
        }

        val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) // 日期格式
        val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())      // 时间格式（小时:分钟）

        var y = topMarginPt
        // 绘制报告标题
        canvas.drawText("SleepWhisper Daily Report", leftMarginPt, y, titlePaint)
        y += 28
        // 绘制宝宝信息副标题：姓名 · 月龄 · 日期
        canvas.drawText(
            "${baby.name} · ${baby.ageInMonths(now)} mo · ${dateFmt.format(Date(now))}",
            leftMarginPt, y, subPaint
        )
        y += 32

        // 统计总睡眠时长（秒）
        val totalSleepSec = sleeps.sumOf { it.durationSeconds(now) }
        val totalSleepDisplay = formatHhMm(totalSleepSec)
        // 统计奶瓶喂养总量（ml），仅统计 BOTTLE 方式
        val totalMl = feeds.filter { it.method == FeedingMethod.BOTTLE }.sumOf { it.amountMl ?: 0 }

        // 绘制汇总统计行
        listOf(
            "Total sleep: $totalSleepDisplay",
            "Sleep sessions: ${sleeps.size}",
            "Feeding count: ${feeds.size}  ·  Bottle total: ${totalMl} ml",
            "Diaper count: ${diapers.size}"
        ).forEach { line ->
            canvas.drawText(line, leftMarginPt, y, bodyPaint)
            y += 18
        }
        y += 12

        // 绘制睡眠明细列表标题
        canvas.drawText("Recent sleep sessions (latest 40):", leftMarginPt, y, subPaint)
        y += 20
        // 按开始时间倒序，最多展示 40 条睡眠记录
        sleeps.sortedByDescending { it.startAt }.take(40).forEach { s ->
            val end = s.endAt
            // 已结束记录显示时间段和时长；进行中记录显示 "ongoing"
            val line = if (end != null) {
                "${timeFmt.format(Date(s.startAt))} – ${timeFmt.format(Date(end))}  (${formatHhMm(s.durationSeconds(now))})"
            } else {
                "${timeFmt.format(Date(s.startAt))} – ongoing"
            }
            canvas.drawText(line, leftMarginPt, y, bodyPaint)
            y += 14
            if (y > pageHeightPt - 40f) return@forEach // 超出页面底部安全区则停止绘制
        }

        doc.finishPage(page)

        // 将 PDF 写入 cacheDir 临时文件
        val outFile = File(
            context.cacheDir,
            "SleepWhisper-${sanitizeFileName(baby.name)}-${now}.pdf"
        )
        FileOutputStream(outFile).use { doc.writeTo(it) }
        doc.close()
        return outFile
    }

    /**
     * 将 cacheDir 内的 PDF 文件转换为 FileProvider content:// URI，用于 Intent 分享。
     *
     * 要求 AndroidManifest.xml 中已声明对应的 `<provider>` 节点，authority 为 `${packageName}.fileprovider`。
     */
    fun fileProviderUri(file: File) =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    /**
     * 将秒数格式化为 "H:MM" 形式的时长字符串（例如 "1:05"）。
     */
    private fun formatHhMm(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        return "%d:%02d".format(h, m)
    }

    /**
     * 将宝宝名称中的非法文件名字符替换为下划线，保证文件名安全。
     * 允许字符：字母、数字、下划线、连字符。
     */
    private fun sanitizeFileName(s: String): String =
        s.replace(Regex("[^A-Za-z0-9_-]"), "_")
}
