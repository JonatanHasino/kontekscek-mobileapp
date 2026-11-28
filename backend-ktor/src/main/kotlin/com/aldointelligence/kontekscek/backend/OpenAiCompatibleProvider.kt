package com.aldointelligence.kontekscek.backend

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Provider OpenAI-compatible (OpenAI, OpenRouter, Groq, dsb).
 * API key HANYA dari environment, tidak pernah ditulis ke log/APK.
 */
class OpenAiCompatibleProvider(
    private val baseUrl: String,
    private val apiKey: String,
    private val modelFast: String,
    private val modelDeep: String,
    private val jsonMode: Boolean = true,
) : LlmProvider {

    private val http =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build()

    override suspend fun analyze(request: AnalyzeRequest): AnalyzeResponse {
        val model = if (request.depth.equals("DEEP", ignoreCase = true)) modelDeep else modelFast
        val first = callModel(request, model, extraInstruction = null)
        decode(first)?.let { return it }

        val retry = callModel(request, model, extraInstruction = "Return ONLY valid JSON matching the schema. No markdown.")
        return decode(retry)
            ?: throw AnalysisException("ANALYSIS_FAILED", "Model tidak mengembalikan JSON yang valid.", 502)
    }

    private fun callModel(request: AnalyzeRequest, model: String, extraInstruction: String?): String {
        val system = loadSystemPrompt() + if (extraInstruction != null) "\n\n$extraInstruction" else ""
        val userContent = buildString {
            append("<user_prompt>").append(request.prompt.escapeTags()).append("</user_prompt>")
            request.output?.takeIf { it.isNotBlank() }?.let {
                append("\n<ai_output>").append(it.escapeTags()).append("</ai_output>")
            }
            request.brief?.takeIf { it.isNotBlank() }?.let {
                append("\n<attached_brief>").append(it.escapeTags()).append("</attached_brief>")
            }
        }

        val messages =
            buildJsonArray {
                add(
                    buildJsonObject {
                        put("role", "system")
                        put("content", system)
                    },
                )
                add(
                    buildJsonObject {
                        put("role", "user")
                        put("content", userContent)
                    },
                )
            }
        val payload =
            buildJsonObject {
                put("model", model)
                put("temperature", 0.2)
                put("max_tokens", 1800)
                if (jsonMode) {
                    put("response_format", buildJsonObject { put("type", "json_object") })
                }
                put("messages", messages)
            }

        val httpRequest =
            HttpRequest.newBuilder()
                .uri(URI.create("${baseUrl.trimEnd('/')}/chat/completions"))
                .timeout(Duration.ofSeconds(45))
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build()

        val response =
            runCatching { http.send(httpRequest, HttpResponse.BodyHandlers.ofString()) }
                .getOrElse { throw AnalysisException("PROVIDER_UNAVAILABLE", "Penyedia AI tidak dapat dihubungi.", 503) }

        if (response.statusCode() !in 200..299) {
            throw AnalysisException(
                "PROVIDER_UNAVAILABLE",
                if (response.statusCode() == 429) "Penyedia AI sedang sibuk." else "Penyedia AI menolak permintaan.",
                503,
            )
        }

        val root = runCatching { ApiJson.parseToJsonElement(response.body()).jsonObject }.getOrNull()
            ?: throw AnalysisException("ANALYSIS_FAILED", "Respons penyedia tidak valid.", 502)
        val content =
            root["choices"]?.jsonArray?.firstOrNull()?.jsonObject
                ?.get("message")?.jsonObject
                ?.get("content")?.jsonPrimitive?.contentOrNull
                ?: throw AnalysisException("ANALYSIS_FAILED", "Respons penyedia kosong.", 502)
        return content
    }

    private fun decode(raw: String): AnalyzeResponse? {
        val cleaned = raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        return runCatching { ApiJson.decodeFromString(AnalyzeResponse.serializer(), cleaned) }.getOrNull()
    }

    private fun loadSystemPrompt(): String =
        javaClass.classLoader.getResource("analyzer_system_prompt_v1.txt")?.readText()
            ?: "You are Prompt Coach. Return only JSON per schema."

    private fun String.escapeTags(): String = replace("<", "&lt;").replace(">", "&gt;")

    companion object {
        fun fromEnv(): LlmProvider? {
            val key = System.getenv("LLM_API_KEY")?.trim().orEmpty()
            if (key.isBlank()) return null
            val base = System.getenv("LLM_BASE_URL")?.trim().takeUnless { it.isNullOrBlank() } ?: "https://api.openai.com/v1"
            val fast = System.getenv("LLM_MODEL_FAST")?.trim().takeUnless { it.isNullOrBlank() } ?: "gpt-4o-mini"
            val deep = System.getenv("LLM_MODEL_DEEP")?.trim().takeUnless { it.isNullOrBlank() } ?: fast
            val jsonMode = System.getenv("LLM_JSON_MODE")?.toBooleanStrictOrNull() ?: true
            return OpenAiCompatibleProvider(base, key, fast, deep, jsonMode)
        }
    }
}
