package com.aldointelligence.kontekscek.domain.model

import com.aldointelligence.kontekscek.data.model.AlertSeverity
import kotlinx.serialization.Serializable

@Serializable
enum class IssueCategory {
    GOAL_UNCLEAR,
    CONTEXT_MISSING,
    ROLE_AUDIENCE_MISSING,
    CONSTRAINTS_MISSING,
    FORMAT_MISSING,
    EXAMPLES_MISSING,
    SOURCE_GROUNDING_MISSING,
    VERIFICATION_MISSING,
    AMBIGUOUS,
    CONFLICTING_INSTRUCTIONS,
    MULTI_TASK_OVERLOAD,
    SENSITIVE_DATA,
    OUTPUT_OFF_TOPIC,
    OUTPUT_MISSES_REQUIREMENT,
    OUTPUT_UNSUPPORTED_CLAIM,
    OUTPUT_FORMAT_MISMATCH,
    OUTPUT_PLACEHOLDER_LEFTOVER,
    OUTPUT_HALLUCINATION_RISK,
    OTHER,
    ;

    val label: String
        get() =
            when (this) {
                GOAL_UNCLEAR -> "Tujuan belum jelas"
                CONTEXT_MISSING -> "Konteks kurang"
                ROLE_AUDIENCE_MISSING -> "Peran/audiens belum ada"
                CONSTRAINTS_MISSING -> "Batasan belum ada"
                FORMAT_MISSING -> "Format keluaran belum ada"
                EXAMPLES_MISSING -> "Contoh/acuan belum ada"
                SOURCE_GROUNDING_MISSING -> "Sumber belum diminta"
                VERIFICATION_MISSING -> "Verifikasi belum diminta"
                AMBIGUOUS -> "Instruksi ambigu"
                CONFLICTING_INSTRUCTIONS -> "Instruksi bertentangan"
                MULTI_TASK_OVERLOAD -> "Terlalu banyak tugas"
                SENSITIVE_DATA -> "Data sensitif"
                OUTPUT_OFF_TOPIC -> "Jawaban melenceng"
                OUTPUT_MISSES_REQUIREMENT -> "Persyaratan belum dipenuhi"
                OUTPUT_UNSUPPORTED_CLAIM -> "Klaim tanpa sumber"
                OUTPUT_FORMAT_MISMATCH -> "Format jawaban tidak sesuai"
                OUTPUT_PLACEHOLDER_LEFTOVER -> "Sisa placeholder"
                OUTPUT_HALLUCINATION_RISK -> "Risiko halusinasi"
                OTHER -> "Lainnya"
            }
}

@Serializable
enum class IssueSource { PROMPT, OUTPUT, FILE, LOCAL }

@Serializable
enum class AnswersRequest { YES, PARTIAL, NO }

@Serializable
enum class CoverageStatus { MET, PARTIAL, MISSING }

@Serializable
enum class AnalysisConfidence { LOW, MEDIUM, HIGH }

@Serializable
enum class AnalysisSource { REMOTE, LOCAL }

@Serializable
data class PromptDimension(
    val key: String,
    val label: String,
    val score: Int,
    val note: String = "",
)

@Serializable
data class PromptScore(
    val overall: Int,
    val dimensions: List<PromptDimension>,
)

@Serializable
data class Issue(
    val id: String,
    val category: IssueCategory,
    val severity: AlertSeverity,
    val title: String,
    val why: String,
    val evidenceQuote: String? = null,
    val source: IssueSource = IssueSource.PROMPT,
    val fix: String = "",
)

@Serializable
data class Mismatch(
    val text: String,
    val evidenceQuote: String? = null,
)

@Serializable
data class OutputCheck(
    val answersRequest: AnswersRequest,
    val summary: String,
    val mismatches: List<Mismatch>,
    val followUpPrompt: String,
)

@Serializable
data class RequirementCoverage(
    val requirement: String,
    val status: CoverageStatus,
    val evidenceQuote: String? = null,
)

@Serializable
data class ClarifyingQuestion(
    val id: String,
    val question: String,
    val options: List<String>,
)

@Serializable
data class QuotaInfo(
    val remainingToday: Int,
    val resetAt: String? = null,
)

@Serializable
data class AnalysisResult(
    val schemaVersion: Int = 1,
    val language: String = "id",
    val taskType: String = "lainnya",
    val targetAI: String = "unknown",
    val score: PromptScore = PromptScore(0, emptyList()),
    val issues: List<Issue> = emptyList(),
    val outputCheck: OutputCheck? = null,
    val requirementCoverage: List<RequirementCoverage> = emptyList(),
    val rewrittenPrompt: String = "",
    val questionsToUser: List<ClarifyingQuestion> = emptyList(),
    val confidence: AnalysisConfidence = AnalysisConfidence.MEDIUM,
    val source: AnalysisSource = AnalysisSource.REMOTE,
    val sourceNote: String? = null,
    val quota: QuotaInfo? = null,
) {
    val scoreLabel: String
        get() =
            when {
                score.overall >= 80 -> "Sangat baik"
                score.overall >= 60 -> "Baik"
                score.overall >= 40 -> "Cukup"
                else -> "Kurang"
            }
}

const val SUPPORTED_SCHEMA_VERSION = 1
