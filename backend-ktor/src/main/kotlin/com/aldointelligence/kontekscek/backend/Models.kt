package com.aldointelligence.kontekscek.backend

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

val ApiJson =
    Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
        encodeDefaults = true
        prettyPrint = false
    }

@Serializable
data class ContextPresetDto(
    val objective: String? = null,
    val audience: String? = null,
    val forbidden: List<String> = emptyList(),
)

@Serializable
data class AnalyzeRequest(
    val requestId: String = "",
    val depth: String = "FAST",
    val prompt: String = "",
    val output: String? = null,
    val brief: String? = null,
    val briefName: String? = null,
    val contextPreset: ContextPresetDto? = null,
    val clientVersion: String = "unknown",
    val locale: String = "id-ID",
)

@Serializable
data class DetectedDto(
    val taskType: String = "lainnya",
    val targetAI: String = "unknown",
)

@Serializable
data class DimensionDto(
    val key: String = "",
    val score: Int = 0,
    val note: String = "",
)

@Serializable
data class PromptScoreDto(
    val overall: Int = 0,
    val dimensions: List<DimensionDto> = emptyList(),
)

@Serializable
data class IssueDto(
    val id: String = "",
    val category: String = "OTHER",
    val severity: String = "INFO",
    val title: String = "",
    val why: String = "",
    val evidenceQuote: String? = null,
    val source: String = "prompt",
    val fix: String = "",
)

@Serializable
data class MismatchDto(
    val text: String = "",
    val evidenceQuote: String? = null,
)

@Serializable
data class OutputCheckDto(
    val answersRequest: String = "partial",
    val summary: String = "",
    val mismatches: List<MismatchDto> = emptyList(),
    val followUpPrompt: String = "",
)

@Serializable
data class RequirementDto(
    val requirement: String = "",
    val status: String = "partial",
    val evidenceQuote: String? = null,
)

@Serializable
data class QuestionDto(
    val id: String = "",
    val question: String = "",
    val options: List<String> = emptyList(),
)

@Serializable
data class QuotaDto(
    val remainingToday: Int = 0,
    val resetAt: String? = null,
)

@Serializable
data class AnalyzeResponse(
    val schemaVersion: Int = 1,
    val language: String = "id",
    val detected: DetectedDto = DetectedDto(),
    val promptScore: PromptScoreDto = PromptScoreDto(),
    val issues: List<IssueDto> = emptyList(),
    val outputCheck: OutputCheckDto? = null,
    val requirementCoverage: List<RequirementDto> = emptyList(),
    val rewrittenPrompt: String = "",
    val questionsToUser: List<QuestionDto> = emptyList(),
    val confidence: String = "medium",
    val quota: QuotaDto? = null,
)

@Serializable
data class ApiErrorDto(
    val code: String,
    val message: String,
    val retryAfterSec: Int? = null,
)

@Serializable
data class ApiErrorBody(
    val error: ApiErrorDto,
)

@Serializable
data class HealthDto(
    val status: String,
    val service: String,
    val provider: String,
)

class AnalysisException(
    val code: String,
    override val message: String,
    val httpStatus: Int = 502,
    val retryAfterSec: Int? = null,
) : Exception(message)
