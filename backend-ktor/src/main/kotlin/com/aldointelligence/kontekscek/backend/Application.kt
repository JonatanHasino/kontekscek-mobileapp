package com.aldointelligence.kontekscek.backend

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.plugins.statuspages.exception
import io.ktor.server.request.receiveText
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    val providerMode = System.getenv("LLM_PROVIDER")?.trim()?.lowercase() ?: "auto"
    val provider =
        when (providerMode) {
            "fake" -> FakeLlmProvider()
            "openai", "openai-compatible", "groq", "openrouter" ->
                OpenAiCompatibleProvider.fromEnv()
                    ?: error("LLM_PROVIDER=$providerMode but LLM_API_KEY is empty")
            else ->
                OpenAiCompatibleProvider.fromEnv() ?: FakeLlmProvider()
        }
    val service = AnalyzerService(provider)
    println("KonteksCek backend starting with provider: ${service.providerName}")

    embeddedServer(Netty, port = port, host = "0.0.0.0") {
        module(service)
    }.start(wait = true)
}

fun Application.module(service: AnalyzerService) {
    install(ContentNegotiation) {
        json(ApiJson)
    }
    install(StatusPages) {
        exception<AnalysisException> { call, cause ->
            cause.retryAfterSec?.let { retry -> call.response.header(HttpHeaders.RetryAfter, retry.toString()) }
            call.respond(
                HttpStatusCode.fromValue(cause.httpStatus),
                ApiErrorBody(ApiErrorDto(cause.code, cause.message, cause.retryAfterSec)),
            )
        }
        exception<Throwable> { call, cause ->
            call.application.log.error("Unhandled error: ${cause::class.simpleName}")
            call.respond(
                HttpStatusCode.InternalServerError,
                ApiErrorBody(ApiErrorDto("INTERNAL", "Terjadi kesalahan internal.")),
            )
        }
    }

    routing {
        get("/health") {
            call.respond(HealthDto("OK", "KonteksCek Analyse API", service.providerName))
        }

        post("/api/v1/analyze") {
            val raw = call.receiveText()
            if (raw.length > MAX_BODY_CHARS) {
                throw AnalysisException("INPUT_TOO_LARGE", "Body permintaan terlalu besar.", 413)
            }
            val request =
                runCatching { ApiJson.decodeFromString(AnalyzeRequest.serializer(), raw) }
                    .getOrElse { throw AnalysisException("INVALID_INPUT", "Body bukan JSON sesuai kontrak.", 400) }
            if (request.requestId.isBlank()) {
                throw AnalysisException("INVALID_INPUT", "requestId wajib diisi.", 400)
            }
            val clientKey =
                call.request.headers["X-Forwarded-For"]?.substringBefore(',').orEmpty().trim()
                    .ifBlank { call.request.headers["X-Real-IP"]?.trim().orEmpty() }
                    .ifBlank { "local" }
            // Catatan privasi: isi prompt/jawaban tidak pernah ditulis ke log.
            val response = service.analyze(request, clientKey)
            call.respond(response)
        }
    }
}

private const val MAX_BODY_CHARS = 300_000
