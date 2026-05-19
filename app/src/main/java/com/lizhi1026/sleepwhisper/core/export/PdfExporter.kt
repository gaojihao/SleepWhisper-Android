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
 * Builds a single-page A4 PDF report — port of iOS Core/Export/PdfExporter.swift.
 *
 * Output is a temporary file in cacheDir; caller shares it via [shareIntent] or directly
 * by passing the FileProvider URI to ACTION_SEND.
 */
@Singleton
class PdfExporter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /** A4 at 72dpi = 595 × 842 pt. */
    private val pageWidthPt = 595
    private val pageHeightPt = 842
    private val leftMarginPt = 40f
    private val topMarginPt = 56f

    fun buildReport(
        baby: Baby,
        sleeps: List<SleepSession>,
        feeds: List<FeedingEvent>,
        diapers: List<DiaperEvent>,
        now: Long = System.currentTimeMillis()
    ): File {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidthPt, pageHeightPt, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply {
            color = 0xFF0E1116.toInt()
            textSize = 26f
            isFakeBoldText = true
        }
        val subPaint = Paint().apply {
            color = 0xFF5F6470.toInt()
            textSize = 14f
        }
        val bodyPaint = Paint().apply {
            color = 0xFF0E1116.toInt()
            textSize = 12f
        }

        val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())

        var y = topMarginPt
        canvas.drawText("SleepWhisper Daily Report", leftMarginPt, y, titlePaint)
        y += 28
        canvas.drawText(
            "${baby.name} · ${baby.ageInMonths(now)} mo · ${dateFmt.format(Date(now))}",
            leftMarginPt, y, subPaint
        )
        y += 32

        val totalSleepSec = sleeps.sumOf { it.durationSeconds(now) }
        val totalSleepDisplay = formatHhMm(totalSleepSec)
        val totalMl = feeds.filter { it.method == FeedingMethod.BOTTLE }.sumOf { it.amountMl ?: 0 }

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

        canvas.drawText("Recent sleep sessions (latest 40):", leftMarginPt, y, subPaint)
        y += 20
        sleeps.sortedByDescending { it.startAt }.take(40).forEach { s ->
            val end = s.endAt
            val line = if (end != null) {
                "${timeFmt.format(Date(s.startAt))} – ${timeFmt.format(Date(end))}  (${formatHhMm(s.durationSeconds(now))})"
            } else {
                "${timeFmt.format(Date(s.startAt))} – ongoing"
            }
            canvas.drawText(line, leftMarginPt, y, bodyPaint)
            y += 14
            if (y > pageHeightPt - 40f) return@forEach
        }

        doc.finishPage(page)

        val outFile = File(
            context.cacheDir,
            "SleepWhisper-${sanitizeFileName(baby.name)}-${now}.pdf"
        )
        FileOutputStream(outFile).use { doc.writeTo(it) }
        doc.close()
        return outFile
    }

    fun fileProviderUri(file: File) =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    private fun formatHhMm(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        return "%d:%02d".format(h, m)
    }

    private fun sanitizeFileName(s: String): String =
        s.replace(Regex("[^A-Za-z0-9_-]"), "_")
}
