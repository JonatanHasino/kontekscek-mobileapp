package com.aldointelligence.kontekscek.domain.lint

import com.aldointelligence.kontekscek.domain.model.AnswersRequest
import com.aldointelligence.kontekscek.domain.model.CoverageStatus
import com.aldointelligence.kontekscek.domain.model.IssueCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalPromptLinterTest {

    @Test
    fun `prompt buruk menghasilkan isu utama dan skor rendah`() {
        val result = LocalPromptLinter.analyze("buatin laporan tentang AI")
        assertTrue(result.score.overall < 40)
        assertTrue(result.issues.any { it.category == IssueCategory.FORMAT_MISSING })
        assertTrue(result.issues.any { it.category == IssueCategory.GOAL_UNCLEAR })
        assertTrue(result.rewrittenPrompt.contains("Tugas:"))
    }

    @Test
    fun `prompt bagus punya skor tinggi dan sedikit isu`() {
        val prompt =
            """
            Buatkan laporan analisis 800 kata untuk dosen pengampu tentang dampak AI generatif
            pada produktivitas mahasiswa. Konteks: data survei internal 120 responden yang saya lampirkan.
            Format: pendahuluan, 3 temuan utama dalam poin, tabel ringkasan, kesimpulan.
            Batasan: bahasa Indonesia formal, maksimal 800 kata, jangan mengarang data.
            Sertakan sitasi untuk setiap angka dan tandai asumsi yang belum pasti.
            """.trimIndent()
        val result = LocalPromptLinter.analyze(prompt)
        assertTrue("skor=${result.score.overall}", result.score.overall >= 70)
        assertTrue(result.issues.size <= 4)
    }

    @Test
    fun `dua tahun bukan klaim statistik`() {
        val result =
            LocalPromptLinter.analyze(
                "Tulis ringkasan kebijakan dua tahun terakhir dalam bentuk poin.",
                "Kebijakan ini berlaku selama 2 tahun dan dievaluasi setiap tahun.",
            )
        assertFalse(result.issues.any { it.category == IssueCategory.OUTPUT_UNSUPPORTED_CLAIM })
    }

    @Test
    fun `persen tanpa sumber tetap diberi peringatan meski ada kata menurut`() {
        val result =
            LocalPromptLinter.analyze(
                "Buat ringkasan dalam 3 poin tentang adopsi AI.",
                "Menurut saya, 40% mahasiswa sudah memakai AI setiap hari.",
            )
        assertTrue(result.issues.any { it.category == IssueCategory.OUTPUT_UNSUPPORTED_CLAIM })
    }

    @Test
    fun `persen dengan sumber resmi tidak diberi peringatan`() {
        val result =
            LocalPromptLinter.analyze(
                "Buat ringkasan dalam 3 poin tentang adopsi AI.",
                "Menurut laporan BPS 2025, 40% mahasiswa memakai AI setiap hari (https://bps.go.id).",
            )
        assertFalse(result.issues.any { it.category == IssueCategory.OUTPUT_UNSUPPORTED_CLAIM })
    }

    @Test
    fun `placeholder TODO terdeteksi`() {
        val result =
            LocalPromptLinter.analyze(
                "Buatkan ringkasan laporan dalam 5 poin.",
                "Berikut ringkasannya: TODO isi bagian pendahuluan.",
            )
        assertTrue(result.issues.any { it.category == IssueCategory.OUTPUT_PLACEHOLDER_LEFTOVER })
    }

    @Test
    fun `kata placeholder biasa tidak memicu false positive`() {
        val result =
            LocalPromptLinter.analyze(
                "Jelaskan dalam 3 poin cara mengisi placeholder pada formulir pendaftaran online.",
                "Isi placeholder dengan nama lengkap dan email.",
            )
        assertFalse(result.issues.any { it.category == IssueCategory.OUTPUT_PLACEHOLDER_LEFTOVER })
    }

    @Test
    fun `output yang terlalu pendek untuk laporan terdeteksi`() {
        val result =
            LocalPromptLinter.analyze(
                "Buat laporan lengkap tentang evaluasi sistem dalam 5 bagian.",
                "Sistem sudah baik.",
            )
        assertTrue(result.issues.any { it.category == IssueCategory.OUTPUT_MISSES_REQUIREMENT })
    }

    @Test
    fun `brief menghasikan requirement coverage`() {
        val result =
            LocalPromptLinter.analyze(
                "Buat ringkasan dalam poin.",
                "Berikut ringkasan: tabel harga sudah diperbarui dan sumber resmi dicantumkan.",
                "Wajib menyertakan tabel harga.\nGunakan sumber resmi untuk setiap angka.",
            )
        assertTrue(result.requirementCoverage.isNotEmpty())
        assertTrue(result.requirementCoverage.any { it.status == CoverageStatus.MET })
    }

    @Test
    fun `pertanyaan klarifikasi maksimal tiga`() {
        val result = LocalPromptLinter.analyze("bikin sesuatu")
        assertTrue(result.questionsToUser.size <= 3)
        assertTrue(result.questionsToUser.all { it.options.isNotEmpty() })
    }

    @Test
    fun `prompt bahasa Inggris memakai template Inggris`() {
        val result =
            LocalPromptLinter.analyze("Write a short summary about the impact of AI for my manager in 5 bullet points.")
        assertEquals("en", result.language)
        assertTrue(result.rewrittenPrompt.startsWith("Task:"))
    }

    @Test
    fun `data sensitif menjadi isu kritikal`() {
        val result =
            LocalPromptLinter.analyze(
                "Buat ringkasan laporan untuk dosen dalam 5 poin. Kirim ke jonatan@example.com",
            )
        assertTrue(result.issues.any { it.category == IssueCategory.SENSITIVE_DATA })
    }

    @Test
    fun `beberapa kata kerja memicu multi task`() {
        val result =
            LocalPromptLinter.analyze("Buat laporan, analisis data, rangkum temuan, dan jelaskan kesimpulan untuk atasan.")
        assertTrue(result.issues.any { it.category == IssueCategory.MULTI_TASK_OVERLOAD })
    }

    @Test
    fun `kata kabur tanpa kriteria memicu ambiguity`() {
        val result = LocalPromptLinter.analyze("Buatkan desain yang bagus dan menarik untuk klien.")
        assertTrue(result.issues.any { it.category == IssueCategory.AMBIGUOUS })
    }

    @Test
    fun `tanpa output outputCheck null`() {
        val result = LocalPromptLinter.analyze("Buatkan ringkasan dalam 3 poin untuk atasan.")
        assertNull(result.outputCheck)
    }

    @Test
    fun `dengan output outputCheck terisi`() {
        val result =
            LocalPromptLinter.analyze(
                "Buatkan ringkasan dalam 3 poin untuk atasan.",
                "1. Proyek selesai.\n2. Biaya sesuai.\n3. Risiko rendah.",
            )
        assertNotNull(result.outputCheck)
        assertTrue(result.outputCheck!!.answersRequest in listOf(AnswersRequest.YES, AnswersRequest.PARTIAL, AnswersRequest.NO))
    }

    @Test
    fun `selalu ada sepuluh dimensi skor`() {
        val result = LocalPromptLinter.analyze("Buatkan ringkasan laporan dalam 5 poin untuk dosen.")
        assertEquals(10, result.score.dimensions.size)
        assertTrue(result.score.overall in 0..100)
    }

    @Test
    fun `hasil lokal ditandai source local`() {
        val result = LocalPromptLinter.analyze("Buatkan ringkasan laporan dalam 5 poin untuk dosen.")
        assertTrue(result.sourceNote != null)
    }
}
