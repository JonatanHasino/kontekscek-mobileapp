package com.aldointelligence.kontekscek.domain

import com.aldointelligence.kontekscek.data.model.AlertSeverity
import com.aldointelligence.kontekscek.data.model.CheckStatus
import com.aldointelligence.kontekscek.data.model.ChecklistItemState
import com.aldointelligence.kontekscek.data.model.ContextCardEntity
import com.aldointelligence.kontekscek.data.model.RuleAlert

data class RuleAnalysisResult(
    val alerts: List<RuleAlert>,
    val readinessScore: Int, // 0..100
    val detectedIssuesCount: Int
)

/**
 * Mesin Analisis Berbasis Aturan (legacy).
 * Berjalan lokal di perangkat tanpa mengirim teks. Analisis Prompt Coach memakai
 * server AI dengan Privacy Guard lokal.
 */
object RuleVerificationEngine {

    private val numberClaimRegex = Regex("""\b\d+([.,]\d+)?\s*(%|persen|juta|ribu|miliar|responden|tahun)\b""", RegexOption.IGNORE_CASE)
    private val citationHintRegex = Regex("""(\[\d+]|https?://|sumber:|menurut|bps|apjii|kpmg|kemendikbud|doi:)""", RegexOption.IGNORE_CASE)
    private val rawPlaceholderRegex = Regex("""(\[isi|todo|lorem ipsum|placeholder|contoh_|insert here|as an ai)""", RegexOption.IGNORE_CASE)
    private val secretPatternRegex = Regex("""(sk-[a-zA-Z0-9]{12,}|api[_-]?key\s*=|password\s*=|bearer\s+[a-zA-Z0-9._-]+)""", RegexOption.IGNORE_CASE)

    fun analyze(
        card: ContextCardEntity?,
        aiOutput: String,
        checklist: List<ChecklistItemState>
    ): RuleAnalysisResult {
        val alerts = mutableListOf<RuleAlert>()
        val trimmedOutput = aiOutput.trim()

        // 1. Cek kebocoran Data Terlarang dari Kartu Konteks
        if (card != null && card.forbiddenData.isNotBlank() && trimmedOutput.isNotEmpty()) {
            val forbiddenTokens = card.forbiddenData
                .split(",", ";", "\n")
                .map { it.trim() }
                .filter { it.length >= 3 }

            val matchedForbidden = forbiddenTokens.filter { token ->
                trimmedOutput.contains(token, ignoreCase = true)
            }
            if (matchedForbidden.isNotEmpty()) {
                alerts.add(
                    RuleAlert(
                        title = "Potensi Kebocoran Data Terlarang!",
                        detail = "Ditemukan kata terlarang di teks: ${matchedForbidden.joinToString(", ")}. Hapus atau anonimkan segera.",
                        severity = AlertSeverity.CRITICAL
                    )
                )
            }
        }

        // 2. Cek pola kredensial / API Key / Password
        if (secretPatternRegex.containsMatchIn(trimmedOutput)) {
            alerts.add(
                RuleAlert(
                    title = "Terdeteksi Pola Kredensial Sensitif",
                    detail = "Teks memuat pola mirip API Key/Password. Pastikan tidak menyebarkan rahasia sistem.",
                    severity = AlertSeverity.CRITICAL
                )
            )
        }

        // 3. Cek klaim angka/statistik tanpa indikasi sumber
        if (numberClaimRegex.containsMatchIn(trimmedOutput) && !citationHintRegex.containsMatchIn(trimmedOutput)) {
            alerts.add(
                RuleAlert(
                    title = "Angka/Klaim Statistik Tanpa Sumber",
                    detail = "Output memuat angka/persentase tetapi tidak ada rujukan sumber. Cek silang ke dokumen resmi.",
                    severity = AlertSeverity.WARNING
                )
            )
        }

        // 4. Cek sisa teks mentah AI / Placeholder
        if (rawPlaceholderRegex.containsMatchIn(trimmedOutput)) {
            alerts.add(
                RuleAlert(
                    title = "Terdeteksi Teks Mentah / Placeholder AI",
                    detail = "Ditemukan frasa mentah seperti 'TODO', '[ISI]', atau 'As an AI'. Sunting sebelum diserahkan.",
                    severity = AlertSeverity.WARNING
                )
            )
        }

        // 5. Cek kesesuaian kata kunci Tujuan Kartu Konteks
        if (card != null && trimmedOutput.isNotEmpty()) {
            val keyWords = card.objective
                .split(" ", ",", ".")
                .map { it.trim().lowercase() }
                .filter { it.length > 4 }
                .distinct()

            if (keyWords.isNotEmpty()) {
                val matchCount = keyWords.count { trimmedOutput.lowercase().contains(it) }
                if (matchCount == 0) {
                    alerts.add(
                        RuleAlert(
                            title = "Indikasi Penyimpangan dari Tujuan Brief",
                            detail = "Output AI tidak mengandung kata kunci utama dari tujuan Kartu Konteks. Periksa kembali kesesuaian brief.",
                            severity = AlertSeverity.WARNING
                        )
                    )
                }
            }
        }

        // 6. Sorotan butir checklist yang belum diperiksa
        val pendingCount = checklist.count { it.status == CheckStatus.BELUM }
        if (pendingCount > 0) {
            alerts.add(
                RuleAlert(
                    title = "$pendingCount Butir Daftar Periksa Belum Tuntas",
                    detail = "Selesaikan seluruh butir atau catat bagian yang diubah sebelum mengekspor PDF jejak.",
                    severity = AlertSeverity.INFO
                )
            )
        }

        // Hitung skor kesiapan (berdasarkan checklist + penalti alert kritis/peringatan)
        val completedRatio = if (checklist.isEmpty()) 0f else {
            checklist.count { it.status != CheckStatus.BELUM }.toFloat() / checklist.size
        }
        val criticalPenalty = alerts.count { it.severity == AlertSeverity.CRITICAL } * 30
        val warningPenalty = alerts.count { it.severity == AlertSeverity.WARNING } * 12
        val score = ((completedRatio * 100).toInt() - criticalPenalty - warningPenalty).coerceIn(0, 100)

        val issueCount = alerts.count { it.severity == AlertSeverity.CRITICAL || it.severity == AlertSeverity.WARNING }
        return RuleAnalysisResult(
            alerts = alerts,
            readinessScore = score,
            detectedIssuesCount = issueCount
        )
    }
}