package com.example.kontekscek

import com.aldointelligence.kontekscek.data.model.ContextCardEntity
import com.aldointelligence.kontekscek.data.model.KonteksDefaults
import com.aldointelligence.kontekscek.data.model.TaskMode
import com.aldointelligence.kontekscek.domain.PdfExtractor
import com.aldointelligence.kontekscek.domain.RuleVerificationEngine
import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VerificationEngineUnitTest {

    @Test
    fun testRuleEngineDetectsForbiddenData() {
        val card = ContextCardEntity(
            title = "Test Card",
            mode = TaskMode.KULIAH.name,
            taskType = "Laporan",
            objective = "Uji Coba",
            audienceStandard = "Standard",
            ethicsAndSources = "Ethical",
            formatAndLanguage = "Format",
            forbiddenData = "NIM, Rahasia"
        )
        val textWithForbidden = "Ini adalah laporan dengan NIM 12345 dan data Rahasia."
        val result = RuleVerificationEngine.analyze(card, textWithForbidden, KonteksDefaults.checklistForMode(TaskMode.KULIAH))

        assertTrue(result.detectedIssuesCount > 0)
        assertTrue(result.readinessScore < 100)
    }

    @Test
    fun testPdfExtractorFiltersSyntaxKeywords() {
        val pdfStreamContent = """
            BT
            /Type /Font /Encoding /Identity-H
            (Analisis Laporan Kuliah Matematika) Tj
            (Diverifikasi oleh Mahasiswa) Tj
            ET
        """.trimIndent()

        val textSegments = mutableListOf<String>()
        val extractMethod = PdfExtractor::class.java.getDeclaredMethod("extractTextFromPdfContent", String::class.java, MutableList::class.java)
        extractMethod.isAccessible = true
        extractMethod.invoke(PdfExtractor, pdfStreamContent, textSegments)

        assertFalse(textSegments.any { it.contains("Identity") || it.contains("Font") })
        assertTrue(textSegments.any { it.contains("Analisis Laporan") })
    }

    @Test
    fun testDateFormatHasNoIllegalPattern() {
        val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm 'WIB'", Locale("id", "ID"))
        val formatted = dateFormat.format(Date())
        assertTrue(formatted.contains("WIB"))
    }
}