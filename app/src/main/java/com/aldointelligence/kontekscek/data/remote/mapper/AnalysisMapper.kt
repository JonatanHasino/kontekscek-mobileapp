package com.aldointelligence.kontekscek.data.remote.mapper

import com.aldointelligence.kontekscek.data.model.AlertSeverity
import com.aldointelligence.kontekscek.data.remote.dto.AnalyzeResponse
import com.aldointelligence.kontekscek.domain.model.AnalysisConfidence
import com.aldointelligence.kontekscek.domain.model.AnalysisResult
import com.aldointelligence.kontekscek.domain.model.AnalysisSource
import com.aldointelligence.kontekscek.domain.model.AnswersRequest
import com.aldointelligence.kontekscek.domain.model.ClarifyingQuestion
import com.aldointelligence.kontekscek.domain.model.CoverageStatus
import com.aldointelligence.kontekscek.domain.model.IssueCategory
import com.aldointelligence.kontekscek.domain.model.IssueSource
import com.aldointelligence.kontekscek.domain.model.Mismatch
import com.aldointelligence.kontekscek.domain.model.OutputCheck
import com.aldointelligence.kontekscek.domain.model.PromptDimension
import com.aldointelligence.kontekscek.domain.model.PromptScore
import com.aldointelligence.kontekscek.domain.model.QuotaInfo
import com.aldointelligence.kontekscek.domain.model.RequirementCoverage
import kotlin.math.roundToInt

/**
 * Mapper toleran: nilai enum asing / field hilang tidak boleh membuat crash.
 */
fun AnalyzeResponse.toDomain(): AnalysisResult {
    val dimensions =
        promptScore.dimensions
            .take(10)
            .map { dto ->
                PromptDimension(
                    key = dto.key.ifBlank { "unknown" },
                    label = dimensionLabel(dto.key),
                    score = dto.score.coerceIn(0, 10),
                    note = dto.note.take(180),
                )
            }
    val overall =
        if (dimensions.isNotEmpty()) {
            weightedOverall(dimensions)
        } else {
            promptScore.overall.coerceIn(0, 100)
        }

    val mappedIssues =
        issues
            .take(8)
            .mapIndexed { index, dto ->
                com.aldointelligence.kontekscek.domain.model.Issue(
                    id = dto.id.ifBlank { "i${index + 1}" },
                    category = categoryOf(dto.category),
                    severity = severityOf(dto.severity),
                    title = dto.title.ifBlank { "Temuan" }.take(140),
                    why = dto.why.take(300),
                    evidenceQuote = dto.evidenceQuote?.trim()?.take(140)?.ifBlank { null },
                    source = sourceOf(dto.source),
                    fix = dto.fix.take(300),
                )
            }

    val output =
        outputCheck?.let { dto ->
            OutputCheck(
                answersRequest = answersOf(dto.answersRequest),
                summary = dto.summary.take(300),
                mismatches = dto.mismatches.take(10).map { Mismatch(it.text.take(240), it.evidenceQuote?.take(140)) },
                followUpPrompt = dto.followUpPrompt.take(2_000),
            )
        }

    return AnalysisResult(
        schemaVersion = schemaVersion,
        language = language.ifBlank { "id" },
        taskType = detected.taskType.ifBlank { "lainnya" },
        targetAI = detected.targetAI.ifBlank { "unknown" },
        score = PromptScore(overall, dimensions),
        issues = mappedIssues,
        outputCheck = output,
        requirementCoverage =
            requirementCoverage.take(15).map {
                RequirementCoverage(
                    requirement = it.requirement.take(200),
                    status = coverageOf(it.status),
                    evidenceQuote = it.evidenceQuote?.take(140),
                )
            },
        rewrittenPrompt = rewrittenPrompt.take(8_000),
        questionsToUser =
            questionsToUser.take(3).mapIndexed { index, dto ->
                ClarifyingQuestion(
                    id = dto.id.ifBlank { "q${index + 1}" },
                    question = dto.question.take(180),
                    options = dto.options.take(4).map { option -> option.take(80) },
                )
            },
        confidence = confidenceOf(confidence),
        source = AnalysisSource.REMOTE,
        quota = quota?.let { QuotaInfo(it.remainingToday, it.resetAt) },
    )
}

private fun weightedOverall(dimensions: List<PromptDimension>): Int {
    val weights = mapOf("goal" to 2.0, "context" to 1.5, "format" to 1.5)
    var total = 0.0
    var weightSum = 0.0
    dimensions.forEach { dimension ->
        val weight = weights[dimension.key] ?: 1.0
        total += dimension.score * weight
        weightSum += weight
    }
    return if (weightSum == 0.0) 0 else (total / weightSum * 10).roundToInt().coerceIn(0, 100)
}

private fun dimensionLabel(key: String): String =
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
        "safety" -> "Keamanan/privasi"
        else -> key.replace('_', ' ').replaceFirstChar { it.uppercase() }
    }

private fun categoryOf(value: String): IssueCategory =
    runCatching { IssueCategory.valueOf(value.trim().uppercase()) }.getOrDefault(IssueCategory.OTHER)

private fun severityOf(value: String): AlertSeverity =
    runCatching { AlertSeverity.valueOf(value.trim().uppercase()) }.getOrDefault(AlertSeverity.INFO)

private fun sourceOf(value: String): IssueSource =
    when (value.trim().lowercase()) {
        "output" -> IssueSource.OUTPUT
        "file" -> IssueSource.FILE
        "local" -> IssueSource.LOCAL
        else -> IssueSource.PROMPT
    }

private fun answersOf(value: String): AnswersRequest =
    when (value.trim().lowercase()) {
        "yes" -> AnswersRequest.YES
        "no" -> AnswersRequest.NO
        else -> AnswersRequest.PARTIAL
    }

private fun coverageOf(value: String): CoverageStatus =
    when (value.trim().lowercase()) {
        "met" -> CoverageStatus.MET
        "missing" -> CoverageStatus.MISSING
        else -> CoverageStatus.PARTIAL
    }

private fun confidenceOf(value: String): AnalysisConfidence =
    when (value.trim().lowercase()) {
        "low" -> AnalysisConfidence.LOW
        "high" -> AnalysisConfidence.HIGH
        else -> AnalysisConfidence.MEDIUM
    }
