package com.aldointelligence.kontekscek.domain.lint

import com.aldointelligence.kontekscek.data.model.AlertSeverity
import com.aldointelligence.kontekscek.domain.guard.PrivacyGuard
import com.aldointelligence.kontekscek.domain.model.AnalysisConfidence
import com.aldointelligence.kontekscek.domain.model.AnalysisResult
import com.aldointelligence.kontekscek.domain.model.AnalysisSource
import com.aldointelligence.kontekscek.domain.model.AnswersRequest
import com.aldointelligence.kontekscek.domain.model.ClarifyingQuestion
import com.aldointelligence.kontekscek.domain.model.CoverageStatus
import com.aldointelligence.kontekscek.domain.model.Issue
import com.aldointelligence.kontekscek.domain.model.IssueCategory
import com.aldointelligence.kontekscek.domain.model.IssueSource
import com.aldointelligence.kontekscek.domain.model.Mismatch
import com.aldointelligence.kontekscek.domain.model.OutputCheck
import com.aldointelligence.kontekscek.domain.model.PromptDimension
import com.aldointelligence.kontekscek.domain.model.PromptScore
import com.aldointelligence.kontekscek.domain.model.RequirementCoverage
import kotlin.math.roundToInt

/**
 * Lint lokal instan (offline). Bukan pengganti LLM, tetapi memberi sinyal cepat
 * dan tetap berguna saat server tidak tersedia. Memakai model Issue yang sama
 * dengan hasil server.
 */
object LocalPromptLinter {

    private val taskVerbs =
        listOf(
            "buat", "buatkan", "buatin", "bikin", "bikinkan", "tulis", "tuliskan", "susun", "susunkan",
            "analisis", "rangkum", "ringkas", "jelaskan", "bandingkan", "carikan", "cari",
            "debug", "perbaiki", "refactor", "desain", "rancang", "hitung", "terjemahkan",
            "evaluasi", "review", "ulas", "edit", "generate", "validasi",
            "create", "write", "summarize", "explain", "compare", "analyze", "build",
        )

    private val formatMarkers =
        listOf(
            "tabel", "poin", "bullet", "paragraf", "json", "markdown", "kata", "halaman",
            "langkah", "daftar", "list", "format", "struktur", "bab", "subjudul",
            "kesimpulan", "pendahuluan", "slide", "section", "heading",
        )

    private val audienceMarkers =
        listOf(
            "untuk", "sebagai", "audiens", "pembaca", "dosen", "atasan", "klien", "mahasiswa",
            "pengguna", "target", "pemula", "manajer", "guru", "siswa", "investor", "tim",
            "audience", "reader", "manager", "student", "client", "user",
        )

    private val constraintMarkers =
        listOf(
            "maksimal", "minimal", "paling", "jangan", "hanya", "tanpa", "kecuali", "batas",
            "maks", "min", "tidak boleh", "wajib", "harus", "sebelum", "maksimum",
            "at most", "at least", "only", "must", "avoid", "limit",
        )

    private val exampleMarkers =
        listOf("contoh", "seperti", "referensi", "gaya", "misal", "template", "pola", "example", "style")

    private val groundingMarkers =
        listOf(
            "sumber", "sitasi", "referensi", "jurnal", "url", "link", "doi", "kutip",
            "data", "bps", "statistik", "berdasarkan", "riset", "studi", "source", "citation",
        )

    private val verificationMarkers =
        listOf(
            "verifikasi", "cek", "validasi", "pastikan", "tandai", "asumsi", "ketidakpastian",
            "jika ragu", "konfirmasi", "jangan mengarang", "verify", "check", "uncertain",
        )

    private val ambiguousWords =
        listOf(
            "bagus", "menarik", "cepat", "banyak", "sedikit", "sesuai", "tepat", "efektif",
            "optimal", "profesional", "kreatif", "mudah", "canggih", "modern", "baik",
            "luas", "bagus", "rapi", "menawan", "good", "nice", "better", "fast", "many",
        )

    private val exampleSourceMarkers =
        listOf(
            "http://", "https://", "doi:", "bps", "badan pusat statistik", "bank indonesia",
            "kemendikbud", "kemenkes", "who", "world bank", "imf", "oecd", "apjii",
            "katadata", "databoks", "pew research", "mckinsey", "deloitte",
        )

    fun analyze(prompt: String, output: String? = null, brief: String? = null): AnalysisResult {
        val trimmedPrompt = prompt.trim()
        val trimmedOutput = output?.trim().orEmpty()
        val words = wordsOf(trimmedPrompt)
        val lower = trimmedPrompt.lowercase()
        val issues = mutableListOf<Issue>()

        fun add(
            category: IssueCategory,
            severity: AlertSeverity,
            title: String,
            why: String,
            fix: String,
            evidence: String? = null,
            source: IssueSource = IssueSource.PROMPT,
        ) {
            issues +=
                Issue(
                    id = "l${issues.size + 1}",
                    category = category,
                    severity = severity,
                    title = title,
                    why = why,
                    evidenceQuote = evidence?.take(140),
                    source = source,
                    fix = fix,
                )
        }

        val hasTaskVerb = taskVerbs.any { containsWord(lower, it) }
        val taskVerbCount = taskVerbs.count { containsWord(lower, it) }
        val hasFormat = formatMarkers.any { containsWord(lower, it) }
        val hasAudience = audienceMarkers.any { containsWord(lower, it) }
        val hasConstraints = constraintMarkers.any { containsWord(lower, it) }
        val hasExamples = exampleMarkers.any { containsWord(lower, it) }
        val hasGrounding = groundingMarkers.any { containsWord(lower, it) }
        val hasVerification = verificationMarkers.any { containsWord(lower, it) }
        val ambiguousHits = ambiguousWords.filter { containsWord(lower, it) }.distinct()
        val hasCriteria = lower.contains(Regex("""\d""")) || lower.contains("kriteria") || lower.contains("minimal") || lower.contains("maksimal")
        val hasContext =
            words.size >= 25 ||
                lower.contains("konteks") ||
                lower.contains("latar") ||
                lower.contains("situasi") ||
                lower.contains("tugas") ||
                lower.contains("data") ||
                lower.contains("laporan")

        if (words.size < 15) {
            add(
                IssueCategory.GOAL_UNCLEAR,
                AlertSeverity.WARNING,
                "Prompt terlalu singkat",
                "Hanya ${words.size} kata. AI kekurangan konteks untuk memahami hasil yang kamu inginkan.",
                "Tambahkan tujuan, konteks singkat, dan format keluaran yang diharapkan.",
                evidence = trimmedPrompt.take(140),
            )
        }
        if (!hasTaskVerb) {
            add(
                IssueCategory.GOAL_UNCLEAR,
                AlertSeverity.WARNING,
                "Kata kerja tugas belum jelas",
                "Prompt tidak memuat kata kerja seperti buat, tulis, analisis, atau jelaskan.",
                "Mulai dengan kata kerja yang tegas: “Buatkan…”, “Analisis…”, “Ringkas…”.",
            )
        }
        if (!hasFormat) {
            add(
                IssueCategory.FORMAT_MISSING,
                AlertSeverity.WARNING,
                "Format keluaran belum disebut",
                "AI bebas memilih bentuk jawaban, sehingga hasil bisa berupa esai panjang saat kamu butuh poin singkat.",
                "Sebutkan format: “dalam 5 poin”, “tabel 3 kolom”, “maksimal 300 kata”.",
            )
        }
        if (!hasAudience) {
            add(
                IssueCategory.ROLE_AUDIENCE_MISSING,
                AlertSeverity.INFO,
                "Peran atau audiens belum ada",
                "Tanpa audiens, nada dan kedalaman jawaban bisa tidak sesuai.",
                "Tambahkan: “untuk dosen pengampu”, “untuk atasan non-teknis”, atau “untuk klien UMKM”.",
            )
        }
        if (!hasConstraints) {
            add(
                IssueCategory.CONSTRAINTS_MISSING,
                AlertSeverity.INFO,
                "Batasan belum disebut",
                "AI tidak tahu panjang, gaya, atau hal yang harus dihindari.",
                "Tambahkan batas: maksimal kata, bahasa, larangan, dan hal yang tidak boleh diubah.",
            )
        }
        if (!hasGrounding) {
            add(
                IssueCategory.SOURCE_GROUNDING_MISSING,
                AlertSeverity.WARNING,
                "Sumber belum diminta",
                "Tanpa instruksi sumber, AI cenderung mengarang angka atau referensi.",
                "Minta: “sertakan sumber/tautan untuk setiap angka; jika tidak ada, tulis perlu verifikasi”.",
            )
        }
        if (!hasVerification) {
            add(
                IssueCategory.VERIFICATION_MISSING,
                AlertSeverity.INFO,
                "Verifikasi belum diminta",
                "AI tidak diminta menandai asumsi atau ketidakpastian.",
                "Tambahkan: “sebutkan asumsi dan bagian yang belum pasti”.",
            )
        }
        if (ambiguousHits.isNotEmpty() && !hasCriteria) {
            add(
                IssueCategory.AMBIGUOUS,
                AlertSeverity.WARNING,
                "Kata kabur tanpa kriteria",
                "Kata “${ambiguousHits.first()}” bisa ditafsirkan berbeda-beda.",
                "Ganti dengan kriteria terukur, misalnya “maksimal 200 kata” atau “3 rekomendasi konkret”.",
                evidence = sentenceContaining(trimmedPrompt, ambiguousHits.first()),
            )
        }
        if (taskVerbCount >= 3) {
            add(
                IssueCategory.MULTI_TASK_OVERLOAD,
                AlertSeverity.WARNING,
                "Terlalu banyak tugas dalam satu prompt",
                "Ditemukan $taskVerbCount kata kerja tugas. AI bisa mengerjakan sebagian saja.",
                "Pecah menjadi beberapa prompt berurutan atau tetapkan prioritas tugas utama.",
            )
        }
        if (!hasContext) {
            add(
                IssueCategory.CONTEXT_MISSING,
                AlertSeverity.INFO,
                "Konteks latar minim",
                "AI tidak diberi situasi, data, atau batas ruang lingkup.",
                "Tambahkan 1–3 kalimat latar dan data yang relevan.",
            )
        }
        if (!hasExamples) {
            add(
                IssueCategory.EXAMPLES_MISSING,
                AlertSeverity.INFO,
                "Contoh/acuan tidak ada",
                "Menambahkan contoh gaya membuat hasil lebih konsisten.",
                "Opsional: beri 1 contoh format atau referensi gaya.",
            )
        }

        // ===== Pemeriksaan jawaban AI =====
        if (trimmedOutput.isNotEmpty()) {
            val outputLower = trimmedOutput.lowercase()
            val placeholder = Regex("""(?i)(\bTODO\b|\[isi[^\]]*\]|lorem ipsum|as an ai|\[placeholder\]|<[a-z_]+>)""").find(trimmedOutput)
            if (placeholder != null) {
                add(
                    IssueCategory.OUTPUT_PLACEHOLDER_LEFTOVER,
                    AlertSeverity.WARNING,
                    "Masih ada placeholder di jawaban",
                    "Jawaban memuat teks yang belum diisi atau frasa khas AI.",
                    "Minta AI mengganti placeholder dengan isi sebenarnya atau tandai bagian yang perlu kamu lengkapi.",
                    evidence = sentenceContaining(trimmedOutput, placeholder.value),
                    source = IssueSource.OUTPUT,
                )
            }

            val claimRegex = Regex("""\b\d+([.,]\d+)?\s*(%|persen|juta|ribu|miliar|triliun|responden)(?![a-zA-Z])""", RegexOption.IGNORE_CASE)
            val hasExplicitSource = exampleSourceMarkers.any { outputLower.contains(it) } || Regex("""\[\d+]""").containsMatchIn(trimmedOutput)
            val claim = claimRegex.find(trimmedOutput)
            if (claim != null && !hasExplicitSource) {
                add(
                    IssueCategory.OUTPUT_UNSUPPORTED_CLAIM,
                    AlertSeverity.WARNING,
                    "Angka/klaim tanpa sumber jelas",
                    "Jawaban menyebut angka atau persentase tanpa tautan, sitasi, atau nama sumber resmi.",
                    "Minta AI menyertakan sumber untuk setiap angka; jika tidak ada, ubah menjadi “perlu verifikasi”.",
                    evidence = sentenceContaining(trimmedOutput, claim.value),
                    source = IssueSource.OUTPUT,
                )
            }

            val promptWantsLong = Regex("""(?i)\b(laporan|makalah|artikel|esai|proposal|riset)\b""").containsMatchIn(trimmedPrompt)
            if (promptWantsLong && wordsOf(trimmedOutput).size < 80) {
                add(
                    IssueCategory.OUTPUT_MISSES_REQUIREMENT,
                    AlertSeverity.WARNING,
                    "Jawaban tampak terlalu pendek",
                    "Prompt meminta laporan/artikel, tetapi jawaban hanya ${wordsOf(trimmedOutput).size} kata.",
                    "Minta AI memperluas bagian yang kurang atau jelaskan bahwa jawaban sengaja ringkas.",
                    source = IssueSource.OUTPUT,
                )
            }
        }

        // ===== Requirement coverage dari brief =====
        val coverage = mutableListOf<RequirementCoverage>()
        if (!brief.isNullOrBlank()) {
            val requirements =
                brief.lines()
                    .map { it.trim().removePrefix("-").removePrefix("*").trim() }
                    .filter { it.length >= 8 }
                    .take(15)
            val basis = if (trimmedOutput.isNotEmpty()) trimmedOutput else trimmedPrompt
            val basisLower = basis.lowercase()
            requirements.forEach { requirement ->
                val keywords = wordsOf(requirement).filter { it.length > 4 }.distinct()
                val hits = keywords.count { basisLower.contains(it) }
                val status =
                    when {
                        keywords.isEmpty() -> CoverageStatus.PARTIAL
                        hits >= (keywords.size * 0.6).roundToInt().coerceAtLeast(1) -> CoverageStatus.MET
                        hits > 0 -> CoverageStatus.PARTIAL
                        else -> CoverageStatus.MISSING
                    }
                coverage += RequirementCoverage(requirement.take(180), status)
            }
            val missing = coverage.count { it.status == CoverageStatus.MISSING }
            if (missing > 0) {
                add(
                    IssueCategory.OUTPUT_MISSES_REQUIREMENT,
                    AlertSeverity.WARNING,
                    "$missing persyaratan brief belum terlihat",
                    "Sebagian poin brief tidak ditemukan pada jawaban/prompt.",
                    "Minta AI memenuhi poin yang belum tercakup, satu per satu.",
                    source = if (trimmedOutput.isNotEmpty()) IssueSource.FILE else IssueSource.PROMPT,
                )
            }
        }

        // ===== Keamanan lokal =====
        val guard = PrivacyGuard.sanitize("$trimmedPrompt\n$trimmedOutput")
        if (guard.hasFindings) {
            add(
                IssueCategory.SENSITIVE_DATA,
                AlertSeverity.CRITICAL,
                "Data sensitif terdeteksi",
                "Ditemukan: ${guard.findings.joinToString { "${it.type} (${it.count})" }}. Data akan disensor sebelum dikirim.",
                "Periksa pratinjau sensor, lalu kirim versi yang sudah disensor.",
            )
        }

        // ===== Skor dimensi =====
        val goalScore =
            when {
                words.isEmpty() -> 0
                !hasTaskVerb || words.size < 8 -> 2
                words.size < 15 -> 5
                words.size < 30 -> 7
                else -> 9
            }
        val contextScore = if (hasContext && words.size >= 15) 8 else 3
        val audienceScore = if (hasAudience) 8 else 2
        val constraintsScore = if (hasConstraints) 8 else 3
        val formatScore = if (hasFormat) 9 else 2
        val examplesScore = if (hasExamples) 8 else 3
        val groundingScore = if (hasGrounding) 8 else 2
        val verificationScore = if (hasVerification) 8 else 3
        val ambiguityScore =
            when {
                taskVerbCount >= 3 -> 3
                ambiguousHits.isNotEmpty() && !hasCriteria -> 4
                ambiguousHits.isNotEmpty() -> 7
                else -> 9
            }
        val safetyScore = if (guard.hasFindings) 2 else 9

        val dimensions =
            listOf(
                PromptDimension("goal", "Tujuan jelas", goalScore, if (hasTaskVerb) "Kata kerja tugas ditemukan." else "Belum ada kata kerja tugas."),
                PromptDimension("context", "Konteks/latar", contextScore),
                PromptDimension("role_audience", "Peran & audiens", audienceScore),
                PromptDimension("constraints", "Batasan", constraintsScore),
                PromptDimension("format", "Format keluaran", formatScore),
                PromptDimension("examples", "Contoh/acuan", examplesScore),
                PromptDimension("grounding", "Sumber & grounding", groundingScore),
                PromptDimension("verification", "Instruksi verifikasi", verificationScore),
                PromptDimension("ambiguity", "Bebas ambiguitas", ambiguityScore),
                PromptDimension("safety", "Keamanan/privasi", safetyScore),
            )
        val overall = weightedOverall(dimensions)

        val sortedIssues =
            issues
                .sortedBy {
                    when (it.severity) {
                        AlertSeverity.CRITICAL -> 0
                        AlertSeverity.WARNING -> 1
                        AlertSeverity.INFO -> 2
                    }
                }.take(8)
                .mapIndexed { index, issue -> issue.copy(id = "l${index + 1}") }

        val language = detectLanguage(trimmedPrompt)
        val outputCheck =
            if (trimmedOutput.isNotEmpty()) {
                val outputIssues = sortedIssues.filter { it.source == IssueSource.OUTPUT }
                val request =
                    when {
                        outputIssues.any { it.severity == AlertSeverity.CRITICAL } -> AnswersRequest.NO
                        outputIssues.isNotEmpty() -> AnswersRequest.PARTIAL
                        else -> AnswersRequest.YES
                    }
                OutputCheck(
                    answersRequest = request,
                    summary =
                        when (request) {
                            AnswersRequest.YES -> "Jawaban tampak menjawab permintaan utama."
                            AnswersRequest.PARTIAL -> "Jawaban menjawab sebagian; masih ada hal yang perlu diperbaiki."
                            AnswersRequest.NO -> "Jawaban berisiko tidak menjawab permintaan."
                        },
                    mismatches = outputIssues.map { Mismatch(it.title, it.evidenceQuote) },
                    followUpPrompt = buildFollowUp(trimmedPrompt, outputIssues),
                )
            } else {
                null
            }

        val questions = buildQuestions(goalScore, formatScore, audienceScore, groundingScore, constraintsScore)

        return AnalysisResult(
            schemaVersion = 1,
            language = language,
            taskType = detectTaskType(lower),
            targetAI = detectTargetAI(lower),
            score = PromptScore(overall, dimensions),
            issues = sortedIssues,
            outputCheck = outputCheck,
            requirementCoverage = coverage,
            rewrittenPrompt = buildRewrittenPrompt(trimmedPrompt, language),
            questionsToUser = questions,
            confidence =
                when {
                    words.size < 20 -> AnalysisConfidence.LOW
                    words.size < 60 -> AnalysisConfidence.MEDIUM
                    else -> AnalysisConfidence.HIGH
                },
            source = AnalysisSource.LOCAL,
            sourceNote = "Diperiksa dengan lint lokal di perangkat. Hubungkan server AI untuk analisis lebih dalam.",
        )
    }

    private fun weightedOverall(dimensions: List<PromptDimension>): Int {
        val weights =
            mapOf(
                "goal" to 2.0,
                "context" to 1.5,
                "format" to 1.5,
            )
        var total = 0.0
        var weightSum = 0.0
        dimensions.forEach { dimension ->
            val weight = weights[dimension.key] ?: 1.0
            total += dimension.score * weight
            weightSum += weight
        }
        return if (weightSum == 0.0) 0 else (total / weightSum * 10).roundToInt().coerceIn(0, 100)
    }

    private fun buildQuestions(
        goal: Int,
        format: Int,
        audience: Int,
        grounding: Int,
        constraints: Int,
    ): List<ClarifyingQuestion> {
        val questions = mutableListOf<ClarifyingQuestion>()
        if (goal < 6) {
            questions +=
                ClarifyingQuestion(
                    "q1",
                    "Hasil akhir seperti apa yang kamu butuhkan?",
                    listOf("Ringkasan singkat", "Analisis mendalam", "Langkah praktis", "Naskah siap pakai"),
                )
        }
        if (format < 6) {
            questions +=
                ClarifyingQuestion(
                    "q2",
                    "Format keluaran mana yang paling cocok?",
                    listOf("Poin-poin", "Tabel", "Paragraf", "Langkah bernomor"),
                )
        }
        if (audience < 6) {
            questions +=
                ClarifyingQuestion(
                    "q3",
                    "Siapa pembaca utama hasil ini?",
                    listOf("Dosen", "Atasan", "Klien", "Publik umum"),
                )
        }
        if (grounding < 6) {
            questions +=
                ClarifyingQuestion(
                    "q4",
                    "Seberapa ketat kebutuhan sumber?",
                    listOf("Wajib sitasi", "Gunakan data yang saya beri", "Tidak perlu sumber"),
                )
        }
        if (constraints < 6) {
            questions +=
                ClarifyingQuestion(
                    "q5",
                    "Apa batasan yang wajib dipatuhi?",
                    listOf("Maksimal 300 kata", "Bahasa formal", "Tanpa istilah teknis", "Tanpa batasan khusus"),
                )
        }
        return questions.take(3)
    }

    private fun buildFollowUp(prompt: String, issues: List<Issue>): String {
        if (issues.isEmpty()) {
            return "Jawaban sudah cukup baik. Minta AI menambahkan sumber untuk setiap angka sebelum dipakai."
        }
        val fixes = issues.take(5).joinToString("\n") { "- ${it.fix.ifBlank { it.title }}" }
        return "Tolong perbaiki jawaban sebelumnya untuk permintaan berikut:\n\"${prompt.take(220)}\"\n\nPerbaiki poin ini:\n$fixes\n\nTetap gunakan bahasa dan data yang sama; tandai bagian yang perlu saya verifikasi."
    }

    private fun buildRewrittenPrompt(prompt: String, language: String): String {
        if (language == "en") {
            return """
                Task: $prompt

                Context: Use only the information I provide below. If important details are missing, ask me before answering.
                Audience: Ask me if the target reader is unclear.
                Output format: Clear, structured, easy to scan. State the format if I did not specify one.
                Constraints: Do not invent facts, numbers, names, or sources.
                Sources: For every number or claim, include a link/citation; if unavailable, mark it "needs verification".
                Verification: List your assumptions and anything that is uncertain.
            """.trimIndent()
        }
        return """
            Tugas: $prompt

            Konteks: Gunakan hanya informasi yang saya berikan. Jika ada detail penting yang kurang, tanyakan dulu sebelum menjawab.
            Audiens: Tanyakan jika pembaca utama belum jelas.
            Format keluaran: Ringkas, terstruktur, dan mudah dipindai. Sebutkan format yang dipakai.
            Batasan: Jangan mengarang fakta, angka, nama, atau sumber.
            Sumber: Untuk setiap angka/klaim, sertakan tautan atau sitasi. Jika tidak ada, tandai "perlu verifikasi".
            Verifikasi: Sebutkan asumsi dan bagian yang belum pasti.
        """.trimIndent()
    }

    private fun detectLanguage(prompt: String): String {
        val lower = prompt.lowercase()
        val indonesian =
            listOf("yang", "dan", "untuk", "dengan", "saya", "buat", "tolong", "tentang", "dari", "ini", "itu", "adalah", "akan", "tidak", "agar", "supaya")
                .count { containsWord(lower, it) }
        val english =
            listOf("the", "and", "for", "with", "my", "make", "create", "about", "from", "this", "that", "is", "will", "not", "please", "write")
                .count { containsWord(lower, it) }
        return if (english > indonesian) "en" else "id"
    }

    private fun detectTaskType(lower: String): String =
        when {
            listOf("kode", "program", "fungsi", "api", "debug", "error", "code").any { containsWord(lower, it) } -> "kode"
            listOf("desain", "ui", "ux", "wireframe", "logo", "layout").any { containsWord(lower, it) } -> "desain"
            listOf("caption", "konten", "posting", "video", "copywriting", "iklan").any { containsWord(lower, it) } -> "konten"
            listOf("riset", "jurnal", "penelitian", "survei").any { containsWord(lower, it) } -> "riset"
            listOf("laporan", "makalah", "esai", "artikel").any { containsWord(lower, it) } -> "laporan"
            else -> "lainnya"
        }

    private fun detectTargetAI(lower: String): String =
        when {
            lower.contains("chatgpt") -> "chatgpt"
            lower.contains("gemini") -> "gemini"
            lower.contains("claude") -> "claude"
            else -> "generic"
        }

    private fun wordsOf(text: String): List<String> =
        text.lowercase()
            .split(Regex("""[^\p{L}\p{N}%]+"""))
            .filter { it.isNotBlank() }

    private fun containsWord(haystackLower: String, word: String): Boolean =
        Regex("""(?<![\p{L}\p{N}])${Regex.escape(word.lowercase())}(?![\p{L}\p{N}])""")
            .containsMatchIn(haystackLower)

    private fun sentenceContaining(text: String, needle: String): String {
        if (needle.isBlank()) return text.take(140)
        val sentences = text.split(Regex("""(?<=[.!?\n])\s+"""))
        val sentence = sentences.firstOrNull { it.contains(needle, ignoreCase = true) } ?: text
        return sentence.trim().take(140)
    }
}
