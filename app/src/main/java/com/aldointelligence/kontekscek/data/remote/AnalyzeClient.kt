package com.aldointelligence.kontekscek.data.remote

import com.aldointelligence.kontekscek.data.remote.dto.AnalyzeRequest
import com.aldointelligence.kontekscek.data.remote.dto.AnalyzeResponse
import com.aldointelligence.kontekscek.data.remote.dto.ApiErrorBody
import com.aldointelligence.kontekscek.data.remote.dto.ApiJson
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException

class AnalyzeClient(
    baseUrl: String,
    private val http: OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .callTimeout(50, TimeUnit.SECONDS)
            .build(),
) {
    private val base = baseUrl.trim().trimEnd('/')

    sealed interface Outcome {
        data class Ok(val body: AnalyzeResponse) : Outcome

        data class Err(
            val code: String,
            val message: String,
            val retryAfterSec: Int? = null,
        ) : Outcome
    }

    suspend fun analyze(request: AnalyzeRequest, token: String? = null): Outcome {
        if (base.isBlank()) {
            return Outcome.Err("NOT_CONFIGURED", "Server AI belum diatur.")
        }
        val body =
            ApiJson.encodeToString(AnalyzeRequest.serializer(), request)
                .toRequestBody("application/json; charset=utf-8".toMediaType())
        val httpRequest =
            Request.Builder()
                .url("$base/api/v1/analyze")
                .post(body)
                .apply { if (!token.isNullOrBlank()) header("Authorization", "Bearer $token") }
                .build()

        return try {
            http.newCall(httpRequest).await().use { response ->
                val text = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    runCatching { ApiJson.decodeFromString(AnalyzeResponse.serializer(), text) }
                        .fold(
                            onSuccess = { Outcome.Ok(it) },
                            onFailure = { Outcome.Err("BAD_RESPONSE", "Respons server tidak dikenali.") },
                        )
                } else {
                    val error =
                        runCatching { ApiJson.decodeFromString(ApiErrorBody.serializer(), text).error }
                            .getOrNull()
                    Outcome.Err(
                        code = error?.code ?: "HTTP_${response.code}",
                        message = error?.message?.ifBlank { "Terjadi kesalahan pada server." }
                            ?: "Terjadi kesalahan pada server.",
                        retryAfterSec = error?.retryAfterSec,
                    )
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Outcome.Err("NETWORK", "Tidak ada koneksi atau server tidak merespons.")
        } catch (e: Exception) {
            Outcome.Err("UNKNOWN", "Gagal menghubungi server.")
        }
    }
}

private suspend fun Call.await(): Response =
    suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { cancel() }
        enqueue(
            object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    continuation.resume(response)
                }
            },
        )
    }
