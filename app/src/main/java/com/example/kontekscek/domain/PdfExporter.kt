package com.aldointelligence.kontekscek.domain

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.aldointelligence.kontekscek.data.model.CheckStatus
import com.aldointelligence.kontekscek.data.model.ContextCardEntity
import com.aldointelligence.kontekscek.data.model.VerificationLogEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Generator PDF Jejak Verifikasi KonteksCek.
 * Desain modern, otomatis multi-halaman (A4 - 595 x 842 pt), dibuat di perangkat.
 * Dokumen mencatat proses pemeriksaan manusia; bukan jaminan kebenaran absolut.
 */
object PdfExporter {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN_LEFT = 36f
    private const val MARGIN_RIGHT = 559f
    private const val MAX_CONTENT_Y = 760f

    fun generateVerificationPdf(
        context: Context,
        log: VerificationLogEntity,
        card: ContextCardEntity?
    ): File {
        val document = PdfDocument()
        var pageNumber = 1

        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        // Paints & Styles
        val paintHeaderBg = Paint().apply { color = Color.parseColor("#0F172A") }
        val paintAccentLine = Paint().apply { color = Color.parseColor("#06B6D4"); strokeWidth = 3f }
        val paintTitle = Paint().apply {
            color = Color.WHITE
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val paintSubTitle = Paint().apply {
            color = Color.parseColor("#94A3B8")
            textSize = 9.5f
        }
        val paintSectionHeader = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 11.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val paintLabel = Paint().apply {
            color = Color.parseColor("#1E293B")
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val paintBody = Paint().apply {
            color = Color.parseColor("#334155")
            textSize = 9f
        }
        val paintCardBg = Paint().apply { color = Color.parseColor("#F8FAFC") }
        val paintCardBorder = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val paintBadgeSudah = Paint().apply { color = Color.parseColor("#059669") }
        val paintBadgeDiubah = Paint().apply { color = Color.parseColor("#D97706") }
        val paintBadgeBelum = Paint().apply { color = Color.parseColor("#DC2626") }
        val paintBadgeText = Paint().apply {
            color = Color.WHITE
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm 'WIB'", Locale("id", "ID"))
        val timestamp = dateFormat.format(Date(log.createdAt))
        val minutes = log.durationSeconds / 60
        val seconds = log.durationSeconds % 60
        val durationText = "${minutes}m ${seconds}d"

        fun drawHeader(c: Canvas, isFirstPage: Boolean) {
            c.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), if (isFirstPage) 80f else 50f, paintHeaderBg)
            c.drawLine(0f, if (isFirstPage) 80f else 50f, PAGE_WIDTH.toFloat(), if (isFirstPage) 80f else 50f, paintAccentLine)

            if (isFirstPage) {
                c.drawText("KONTEKSCEK — LAPORAN JEJAK VERIFIKASI AI", MARGIN_LEFT, 32f, paintTitle)
                c.drawText(
                    "Mode: ${log.getTaskMode().label.uppercase()}  •  Durasi Sesi: $durationText  •  Waktu: $timestamp",
                    MARGIN_LEFT, 52f, paintSubTitle
                )
                c.drawText("ID Jejak: #KC-${log.id}  •  Status Dokumen: Diverifikasi Manusia", MARGIN_LEFT, 68f, paintSubTitle)
            } else {
                c.drawText("KONTEKSCEK — JEJAK VERIFIKASI #KC-${log.id} (Lanjutan)", MARGIN_LEFT, 30f, paintTitle)
            }
        }

        fun drawFooter(c: Canvas, pageNum: Int) {
            val footerY = MAX_CONTENT_Y + 15f
            c.drawLine(MARGIN_LEFT, footerY, MARGIN_RIGHT, footerY, paintAccentLine)
            
            c.drawText(
                "Catatan proses pemeriksaan manusia, bukan jaminan kebenaran absolut.",
                MARGIN_LEFT, footerY + 18f, paintSubTitle
            )
            c.drawText(
                "Riwayat tersimpan di perangkat pengguna.",
                MARGIN_LEFT, footerY + 30f, paintSubTitle
            )
            
            c.drawText(
                "Dihasilkan oleh KonteksCek • Kelompok AI UIB • Hal $pageNum",
                MARGIN_RIGHT - 210f, footerY + 18f, paintSubTitle
            )
        }

        fun checkNewPageIfNeeded(currentY: Float, neededSpace: Float): Float {
            if (currentY + neededSpace > MAX_CONTENT_Y) {
                drawFooter(canvas, pageNumber)
                document.finishPage(page)

                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                drawHeader(canvas, false)
                return 70f
            }
            return currentY
        }

        drawHeader(canvas, true)
        var y = 100f

        // ===== SEKSI 1: RINGKASAN KARTU KONTEKS =====
        y = checkNewPageIfNeeded(y, 140f)
        canvas.drawText("1. RINGKASAN KARTU KONTEKS: ${log.cardTitle.take(40)}", MARGIN_LEFT, y, paintSectionHeader)
        y += 12f

        val contextItems = listOf(
            "Tujuan Proyek" to (card?.objective ?: "-"),
            "Audiens & Standar" to (card?.audienceStandard ?: "-"),
            "Batas Etika & Sumber" to (card?.ethicsAndSources ?: "-"),
            "Format & Bahasa" to (card?.formatAndLanguage ?: "-"),
            "Data Terlarang" to (card?.forbiddenData ?: "-")
        )

        val cardStartY = y
        var cardInnerY = y + 14f

        contextItems.forEach { (label, value) ->
            canvas.drawText("• $label:", MARGIN_LEFT + 10f, cardInnerY, paintLabel)
            cardInnerY += 14f
            cardInnerY = drawWrappedText(canvas, value, MARGIN_LEFT + 20f, cardInnerY, 500f, paintBody, 14f) + 10f
        }

        val cardEndY = cardInnerY + 6f
        val cardRect = RectF(MARGIN_LEFT, cardStartY, MARGIN_RIGHT, cardEndY)
        canvas.drawRoundRect(cardRect, 8f, 8f, paintCardBg)
        canvas.drawRoundRect(cardRect, 8f, 8f, paintCardBorder)

        // Draw text inside card box
        cardInnerY = cardStartY + 14f
        contextItems.forEach { (label, value) ->
            canvas.drawText("• $label:", MARGIN_LEFT + 10f, cardInnerY, paintLabel)
            cardInnerY += 14f
            cardInnerY = drawWrappedText(canvas, value, MARGIN_LEFT + 20f, cardInnerY, 500f, paintBody, 14f) + 10f
        }
        y = cardEndY + 28f

        // ===== SEKSI 2: DAFTAR PERIKSA 5 BUTIR =====
        y = checkNewPageIfNeeded(y, 140f)
        canvas.drawText("2. HASIL DAFTAR PERIKSA MODE ${log.getTaskMode().label.uppercase()}", MARGIN_LEFT, y, paintSectionHeader)
        y += 18f

        val checklist = log.parseChecklist()
        checklist.forEachIndexed { idx, item ->
            y = checkNewPageIfNeeded(y, 45f)
            val badgePaint = when (item.status) {
                CheckStatus.SUDAH -> paintBadgeSudah
                CheckStatus.DIUBAH -> paintBadgeDiubah
                CheckStatus.BELUM -> paintBadgeBelum
            }
            val badgeRect = RectF(MARGIN_LEFT, y - 10f, MARGIN_LEFT + 55f, y + 4f)
            canvas.drawRoundRect(badgeRect, 4f, 4f, badgePaint)
            canvas.drawText(item.status.label.uppercase(), MARGIN_LEFT + 8f, y, paintBadgeText)

            y = drawWrappedText(canvas, "${idx + 1}. ${item.title}", MARGIN_LEFT + 65f, y, 440f, paintBody, 14f) + 16f
        }
        y += 24f

        // ===== SEKSI 3: CATATAN PROSES 3 TAHAP =====
        y = checkNewPageIfNeeded(y, 160f)
        canvas.drawText("3. CATATAN PROSES 3 TAHAP (DIMINTA → DIPERIKSA → DIUBAH)", MARGIN_LEFT, y, paintSectionHeader)
        y += 18f

        val stages = listOf(
            "A. Yang Diminta ke AI (Prompt / Instruksi)" to log.stageAsked,
            "B. Yang Diperiksa / Diuji Silang" to log.stageChecked,
            "C. Yang Diubah / Disunting Manusia" to log.stageChanged,
            "D. Kesimpulan Verifikator" to log.finalConclusion
        )

        stages.forEach { (stageTitle, stageText) ->
            y = checkNewPageIfNeeded(y, 45f)
            canvas.drawText(stageTitle, MARGIN_LEFT, y, paintLabel)
            y += 16f
            y = drawWrappedText(canvas, stageText.ifBlank { "-" }, MARGIN_LEFT + 10f, y, 510f, paintBody, 14f) + 18f
        }

        drawFooter(canvas, pageNumber)
        document.finishPage(page)

        val dir = File(context.cacheDir, "pdfs").apply { mkdirs() }
        val safeTitle = log.cardTitle.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(24)
        val outFile = File(dir, "Jejak_KonteksCek_${safeTitle}_${log.id}.pdf")
        FileOutputStream(outFile).use { document.writeTo(it) }
        document.close()
        return outFile
    }

    private fun drawWrappedText(
        canvas: Canvas,
        text: String,
        x: Float,
        startY: Float,
        maxWidth: Float,
        paint: Paint,
        lineHeight: Float
    ): Float {
        var y = startY
        val lines = text.split("\n")
        for (rawLine in lines) {
            val words = rawLine.split(" ")
            var line = StringBuilder()
            for (word in words) {
                val test = if (line.isEmpty()) word else "$line $word"
                if (paint.measureText(test) <= maxWidth) {
                    line = StringBuilder(test)
                } else {
                    if (line.isNotEmpty()) {
                        canvas.drawText(line.toString(), x, y, paint)
                        y += lineHeight
                    }
                    var singleWord = word
                    while (paint.measureText(singleWord) > maxWidth && singleWord.length > 3) {
                        val sub = singleWord.take(20)
                        canvas.drawText(sub, x, y, paint)
                        y += lineHeight
                        singleWord = singleWord.drop(20)
                    }
                    line = StringBuilder(singleWord)
                }
            }
            if (line.isNotEmpty()) {
                canvas.drawText(line.toString(), x, y, paint)
                y += lineHeight
            }
        }
        return y - lineHeight
    }
}