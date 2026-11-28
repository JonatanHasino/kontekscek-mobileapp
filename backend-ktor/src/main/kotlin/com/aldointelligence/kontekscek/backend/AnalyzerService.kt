package com.aldointelligence.kontekscek.backend

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

class AnalyzerService(
    private val provider: LlmProvider,
    private val maxPerClientPerDay: Int = envInt("MAX_DAILY_PER_CLIENT", 60),
    private val maxGlobalPerDay: Int = envInt("MAX_DAILY_GLOBAL", 300),
    private val allowProFeatures: Boolean = System.getenv("ALLOW_PRO_FEATURES")?.toBooleanStrictOrNull() ?: false,
) {
    val providerName: String = provider::class.simpleName ?: "unknown"
    private data class Cached(val at: Long, val response: AnalyzeResponse)

    private val lock = Any()
    private val cache = ConcurrentHashMap<String, Cached>()
    private val perClient = ConcurrentHashMap<String, Int>()
    private var day: LocalDate = LocalDate.now(ZoneOffset.UTC)
    private var globalCount: Int = 0

    suspend fun analyze(request: AnalyzeRequest, clientKey: String): AnalyzeResponse {
        validate(request)
        if (!allowProFeatures && request.depth.equals("DEEP", ignoreCase = true)) {
            throw AnalysisException("PRO_REQUIRED", "Cek Mendalam membutuhkan Pro.", 403)
        }
        if (!allowProFeatures && !request.brief.isNullOrBlank()) {
            throw AnalysisException("PRO_REQUIRED", "Lampiran brief membutuhkan Pro.", 403)
        }

        val cacheKey = request.requestId.takeIf { it.isNotBlank() }
        if (cacheKey != null) {
            val cached = cache[cacheKey]
            if (cached != null && System.currentTimeMillis() - cached.at < CACHE_TTL_MS) {
                return cached.response
            }
        }

        val remainingBefore = checkQuota(clientKey)
        val providerResponse =
            try {
                provider.analyze(request)
            } catch (e: AnalysisException) {
                throw e
            } catch (e: Exception) {
                throw AnalysisException("PROVIDER_UNAVAILABLE", "Penyedia AI tidak tersedia.", 503)
            }

        val normalized = normalize(providerResponse, request)
        val remaining = commitUsage(clientKey)
        val withQuota =
            normalized.copy(
                quota = QuotaDto(remainingToday = remaining, resetAt = resetAtIso()),
            )
        if (cacheKey != null) {
            cache[cacheKey] = Cached(System.currentTimeMillis(), withQuota)
            if (cache.size > 500) {
                val cutoff = System.currentTimeMillis() - CACHE_TTL_MS
                cache.entries.removeIf { it.value.at < cutoff }
            }
        }
        return withQuota
    }

    private fun validate(request: AnalyzeRequest) {
        val prompt = request.prompt.trim()
        if (prompt.length < 10) {
            throw AnalysisException("INVALID_INPUT", "Prompt minimal 10 karakter.", 400)
        }
        if (prompt.length > 6_000) {
            throw AnalysisException("INPUT_TOO_LARGE", "Prompt maksimal 6.000 karakter.", 413)
        }
        if ((request.output?.length ?: 0) > 12_000) {
            throw AnalysisException("INPUT_TOO_LARGE", "Jawaban maksimal 12.000 karakter.", 413)
        }
        if ((request.brief?.length ?: 0) > 20_000) {
            throw AnalysisException("INPUT_TOO_LARGE", "Brief maksimal 20.000 karakter.", 413)
        }
        if (request.depth.uppercase() !in setOf("FAST", "DEEP")) {
            throw AnalysisException("INVALID_INPUT", "Depth tidak dikenal.", 400)
        }
    }

    private fun checkQuota(clientKey: String): Int =
        synchronized(lock) {
            rotateDayIfNeeded()
            val used = perClient[clientKey] ?: 0
            if (globalCount >= maxGlobalPerDay) {
                throw AnalysisException(
                    "QUOTA_EXCEEDED",
                    "Kuota global server hari ini habis.",
                    429,
                    retryAfterSec = secondsUntilReset(),
                )
            }
            if (used >= maxPerClientPerDay) {
                throw AnalysisException(
                    "QUOTA_EXCEEDED",
                    "Kuota harian perangkat habis.",
                    429,
                    retryAfterSec = secondsUntilReset(),
                )
            }
            (maxPerClientPerDay - used).coerceAtLeast(0)
        }

    private fun commitUsage(clientKey: String): Int =
        synchronized(lock) {
            rotateDayIfNeeded()
            val used = (perClient[clientKey] ?: 0) + 1
            perClient[clientKey] = used
            globalCount += 1
            (maxPerClientPerDay - used).coerceAtLeast(0)
        }

    private fun rotateDayIfNeeded() {
        val today = LocalDate.now(ZoneOffset.UTC)
        if (today != day) {
            day = today
            perClient.clear()
            globalCount = 0
            cache.clear()
        }
    }

    private fun resetAtIso(): String =
        LocalDate.now(ZoneOffset.UTC).plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).toString()

    private fun secondsUntilReset(): Int {
        val now = Instant.now()
        val next = LocalDate.now(ZoneOffset.UTC).plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)
        return (next.epochSecond - now.epochSecond).coerceAtLeast(60).toInt()
    }

    private fun normalize(response: AnalyzeResponse, request: AnalyzeRequest): AnalyzeResponse {
        val dimensions =
            response.promptScore.dimensions
                .take(10)
                .map {
                    it.copy(
                        key = it.key.take(30),
                        score = it.score.coerceIn(0, 10),
                        note = it.note.take(200),
                    )
                }
        val overall =
            if (dimensions.isEmpty()) {
                response.promptScore.overall.coerceIn(0, 100)
            } else {
                weightedOverall(dimensions)
            }

        val allowedCategories =
            setOf(
                "GOAL_UNCLEAR", "CONTEXT_MISSING", "ROLE_AUDIENCE_MISSING", "CONSTRAINTS_MISSING",
                "FORMAT_MISSING", "EXAMPLES_MISSING", "SOURCE_GROUNDING_MISSING", "VERIFICATION_MISSING",
                "AMBIGUOUS", "CONFLICTING_INSTRUCTIONS", "MULTI_TASK_OVERLOAD", "SENSITIVE_DATA",
                "OUTPUT_OFF_TOPIC", "OUTPUT_MISSES_REQUIREMENT", "OUTPUT_UNSUPPORTED_CLAIM",
                "OUTPUT_FORMAT_MISMATCH", "OUTPUT_PLACEHOLDER_LEFTOVER", "OUTPUT_HALLUCINATION_RISK", "OTHER",
            )
        val allowedSeverity = setOf("CRITICAL", "WARNING", "INFO")
        val allowedSource = setOf("prompt", "output", "file")
        val issues =
            response.issues.take(8).mapIndexed { index, issue ->
                issue.copy(
                    id = issue.id.ifBlank { "i${index + 1}" }.take(30),
                    category = issue.category.uppercase().takeIf { it in allowedCategories } ?: "OTHER",
                    severity = issue.severity.uppercase().takeIf { it in allowedSeverity } ?: "INFO",
                    title = issue.title.take(70),
                    why = issue.why.take(240),
                    evidenceQuote = issue.evidenceQuote?.take(140),
                    source = issue.source.lowercase().takeIf { it in allowedSource } ?: "prompt",
                    fix = issue.fix.take(240),
                )
            }

        val outputCheck =
            if (request.output.isNullOrBlank()) {
                null
            } else {
                response.outputCheck
                    ?: OutputCheckDto(
                        answersRequest = "partial",
                        summary = "Jawaban perlu diperiksa terhadap permintaan.",
                        followUpPrompt = "Tolong cek kembali apakah jawaban memenuhi seluruh permintaan.",
                    )
            }?.copy(
                answersRequest = response.outputCheck?.answersRequest?.lowercase()?.takeIf { it in setOf("yes", "partial", "no") } ?: "partial",
                summary = response.outputCheck?.summary?.take(300) ?: "Jawaban perlu diperiksa.",
                mismatches = response.outputCheck?.mismatches?.take(10)?.map { it.copy(text = it.text.take(240), evidenceQuote = it.evidenceQuote?.take(140)) }.orEmpty(),
                followUpPrompt = response.outputCheck?.followUpPrompt?.take(2_000) ?: "Tolong perbaiki jawaban sebelumnya dan tandai bagian yang perlu verifikasi.",
            )

        return response.copy(
            schemaVersion = 1,
            language = response.language.ifBlank { "id" }.take(8),
            detected = response.detected.copy(taskType = response.detected.taskType.take(20), targetAI = response.detected.targetAI.take(20)),
            promptScore = PromptScoreDto(overall, dimensions),
            issues = issues,
            outputCheck = outputCheck,
            requirementCoverage =
                response.requirementCoverage.take(15).map {
                    it.copy(
                        requirement = it.requirement.take(200),
                        status = it.status.lowercase().takeIf { s -> s in setOf("met", "partial", "missing") } ?: "partial",
                        evidenceQuote = it.evidenceQuote?.take(140),
                    )
                },
            rewrittenPrompt = response.rewrittenPrompt.take(8_000),
            questionsToUser =
                response.questionsToUser.take(3).mapIndexed { index, question ->
                    question.copy(
                        id = question.id.ifBlank { "q${index + 1}" }.take(30),
                        question = question.question.take(180),
                        options = question.options.take(4).map { it.take(80) },
                    )
                },
            confidence = response.confidence.lowercase().takeIf { it in setOf("low", "medium", "high") } ?: "medium",
        )
    }

    private fun weightedOverall(dimensions: List<DimensionDto>): Int {
        val weights = mapOf("goal" to 2.0, "context" to 1.5, "format" to 1.5)
        var total = 0.0
        var weightSum = 0.0
        dimensions.forEach {
            val weight = weights[it.key] ?: 1.0
            total += it.score * weight
            weightSum += weight
        }
        return if (weightSum == 0.0) 0 else (total / weightSum * 10).roundToInt().coerceIn(0, 100)
    }

    private companion object {
        const val CACHE_TTL_MS = 10 * 60 * 1000L
    }
}

private fun envInt(name: String, default: Int): Int =
    System.getenv(name)?.toIntOrNull() ?: default
