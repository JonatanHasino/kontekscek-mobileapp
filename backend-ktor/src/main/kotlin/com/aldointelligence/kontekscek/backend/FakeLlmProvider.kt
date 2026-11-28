package com.aldointelligence.kontekscek.backend

import kotlin.math.roundToInt

/**
 * Provider deterministik untuk demo/CI. Bukan pengganti LLM; tetap menghasilkan
 * JSON valid sesuai kontrak agar aplikasi bisa diuji tanpa API key.
 */
class FakeLlmProvider : LlmProvider {

    override suspend fun analyze(request: AnalyzeRequest): AnalyzeResponse {
        val prompt = request.prompt.trim()
        val output = request.output?.trim().orEmpty()
        val words = prompt.split(Regex("""\s+""")).filter { it.isNotBlank() }
        val lower = prompt.lowercase()

        fun has(word: String) = Regex("""(?<![\p{L}\p{N}])${Regex.escape(word)}(?![\p{L}\p{N}])""").containsMatchIn(lower)

        val hasFormat = listOf("tabel", "poin", "paragraf", "json", "markdown", "kata", "halaman", "daftar").any(::has)
        val hasAudience = listOf("untuk", "dosen", "atasan", "klien", "mahasiswa", "pembaca", "audiens").any(::has)
        val hasSource = listOf("sumber", "sitasi", "referensi", "jurnal", "url", "doi", "data").any(::has)
        val hasVerify = listOf("verifikasi", "pastikan", "tandai", "asumsi", "cek").any(::has)
        val hasConstraint = listOf("maksimal", "minimal", "jangan", "tanpa", "batas", "hanya").any(::has)

        val issues = mutableListOf<IssueDto>()
        fun issue(category: String, severity: String, title: String, why: String, fix: String, source: String = "prompt") {
            issues +=
                IssueDto(
                    id = "i${issues.size + 1}",
                    category = category,
                    severity = severity,
                    title = title,
                    why = why,
                    source = source,
                    fix = fix,
                )
        }

        if (words.size < 15) {
            issue("GOAL_UNCLEAR", "WARNING", "Prompt terlalu singkat", "AI kekurangan konteks.", "Tambahkan tujuan, konteks, dan format.")
        }
        if (!hasFormat) {
            issue("FORMAT_MISSING", "WARNING", "Format keluaran belum disebut", "Bentuk jawaban bisa tidak sesuai.", "Sebutkan format: poin, tabel, atau batas kata.")
        }
        if (!hasAudience) {
            issue("ROLE_AUDIENCE_MISSING", "INFO", "Audiens belum jelas", "Nada jawaban bisa meleset.", "Sebutkan pembaca utama.")
        }
        if (!hasSource) {
            issue("SOURCE_GROUNDING_MISSING", "WARNING", "Sumber belum diminta", "AI berisiko mengarang angka.", "Minta sumber untuk setiap angka.")
        }
        if (!hasVerify) {
            issue("VERIFICATION_MISSING", "INFO", "Verifikasi belum diminta", "Asumsi AI tidak ditandai.", "Minta AI menandai asumsi dan ketidakpastian.")
        }
        if (!hasConstraint) {
            issue("CONSTRAINTS_MISSING", "INFO", "Batasan belum ada", "Panjang/gaya bisa berubah-ubah.", "Tambahkan batas panjang dan larangan.")
        }
        if (output.isNotEmpty()) {
            val number = Regex("""\b\d+([.,]\d+)?\s*(%|persen|juta|ribu|miliar)""", RegexOption.IGNORE_CASE).find(output)
            val sourced = listOf("http://", "https://", "doi:", "bps", "kemendikbud", "who", "world bank").any { output.lowercase().contains(it) }
            if (number != null && !sourced) {
                issue(
                    "OUTPUT_UNSUPPORTED_CLAIM",
                    "WARNING",
                    "Angka tanpa sumber",
                    "Jawaban memuat angka tanpa rujukan.",
                    "Minta AI menyertakan sumber atau tandai perlu verifikasi.",
                    source = "output",
                )
            }
            if (Regex("""(?i)\bTODO\b|\[isi|lorem ipsum|as an ai""").containsMatchIn(output)) {
                issue(
                    "OUTPUT_PLACEHOLDER_LEFTOVER",
                    "WARNING",
                    "Masih ada placeholder",
                    "Jawaban memuat teks yang belum diisi.",
                    "Minta AI melengkapi placeholder.",
                    source = "output",
                )
            }
        }

        val dims =
            listOf(
                "goal" to if (has("buat") || has("tulis") || has("analisis")) 8 else 3,
                "context" to if (words.size >= 25) 8 else 4,
                "role_audience" to if (hasAudience) 8 else 3,
                "constraints" to if (hasConstraint) 8 else 4,
                "format" to if (hasFormat) 9 else 3,
                "examples" to if (has("contoh") || has("referensi")) 8 else 4,
                "grounding" to if (hasSource) 8 else 3,
                "verification" to if (hasVerify) 8 else 4,
                "ambiguity" to if (has("bagus") || has("menarik") || has("sesuai")) 4 else 9,
                "safety" to 9,
            )
        fun label(key: String) =
            when (key) {
                "goal" -> "Tujuan jelas"
                "context" -> "Konteks/latar"
                "role_audience" -> "Peran & audiens"
                "constraints" -> "Batasan"
                "format" -> "Format keluaran"
                "examples" -> "Contoh/acuan"
                "grounding" -> "Sumber & grounding"
                "verification" -> "Instruksi verifikasi"
                "ambiguity" -> "Bebas ambiguitas"
                else -> "Keamanan/privasi"
            }
        val dimensions = dims.map { (key, score) -> DimensionDto(key, score, label(key)) }
        val weights = mapOf("goal" to 2.0, "context" to 1.5, "format" to 1.5)
        var total = 0.0
        var weightSum = 0.0
        dimensions.forEach {
            val weight = weights[it.key] ?: 1.0
            total += it.score * weight
            weightSum += weight
        }
        val overall = (total / weightSum * 10).roundToInt().coerceIn(0, 100)

        val outputCheck =
            if (output.isNotEmpty()) {
                OutputCheckDto(
                    answersRequest = if (issues.any { it.source == "output" }) "partial" else "yes",
                    summary = if (issues.any { it.source == "output" }) "Jawaban menjawab sebagian; ada hal yang perlu diperbaiki." else "Jawaban tampak menjawab permintaan.",
                    mismatches = issues.filter { it.source == "output" }.map { MismatchDto(it.title) },
                    followUpPrompt = "Tolong perbaiki jawaban sebelumnya. Fokus pada: ${issues.filter { it.source == "output" }.joinToString { it.fix }}. Tandai bagian yang perlu saya verifikasi.",
                )
            } else {
                null
            }

        val detected =
            when {
                listOf("kode", "program", "api", "debug").any(::has) -> "kode"
                listOf("desain", "ui", "ux").any(::has) -> "desain"
                listOf("laporan", "makalah", "artikel").any(::has) -> "laporan"
                else -> "lainnya"
            }

        return AnalyzeResponse(
            schemaVersion = 1,
            language = if (listOf("the", "and", "for", "write", "make").count(::has) > 1) "en" else "id",
            detected = DetectedDto(detected, "generic"),
            promptScore = PromptScoreDto(overall, dimensions),
            issues = issues.take(8),
            outputCheck = outputCheck,
            requirementCoverage = emptyList(),
            rewrittenPrompt =
                """
                Tugas: $prompt

                Konteks: Gunakan hanya informasi yang saya berikan. Jika kurang, tanyakan dulu.
                Audiens: ${if (hasAudience) "Sesuai yang saya sebutkan." else "Tanyakan jika belum jelas."}
                Format keluaran: ${if (hasFormat) "Sesuai yang saya minta." else "Ringkas, terstruktur, mudah dipindai."}
                Batasan: Jangan mengarang fakta, angka, nama, atau sumber.
                Sumber: Sertakan tautan/sitasi untuk setiap angka; jika tidak ada, tandai "perlu verifikasi".
                Verifikasi: Sebutkan asumsi dan bagian yang belum pasti.
                """.trimIndent(),
            questionsToUser =
                listOf(
                    QuestionDto("q1", "Hasil akhir seperti apa yang paling kamu butuhkan?", listOf("Ringkasan", "Analisis", "Langkah praktis")),
                ).take(if (overall < 60) 1 else 0),
            confidence = if (words.size < 20) "low" else if (words.size < 60) "medium" else "high",
        )
    }
}
