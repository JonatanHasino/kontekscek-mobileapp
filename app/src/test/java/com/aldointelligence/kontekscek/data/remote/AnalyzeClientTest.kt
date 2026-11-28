package com.aldointelligence.kontekscek.data.remote

import com.aldointelligence.kontekscek.data.remote.dto.AnalyzeRequest
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AnalyzeClientTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun request() =
        AnalyzeRequest(
            requestId = "test-request",
            prompt = "Buat ringkasan laporan dalam 5 poin.",
            clientVersion = "2.0.0-beta1",
        )

    private fun client(readTimeoutMs: Long = 5_000) =
        AnalyzeClient(
            baseUrl = server.url("/").toString(),
            http =
                OkHttpClient.Builder()
                    .connectTimeout(1, TimeUnit.SECONDS)
                    .readTimeout(readTimeoutMs, TimeUnit.MILLISECONDS)
                    .callTimeout(readTimeoutMs + 500, TimeUnit.MILLISECONDS)
                    .build(),
        )

    @Test
    fun `200 valid menghasilkan Ok`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "schemaVersion": 1,
                      "language": "id",
                      "promptScore": { "overall": 72, "dimensions": [{"key":"goal","score":8,"note":"ok"}] },
                      "issues": [{"id":"i1","category":"FORMAT_MISSING","severity":"WARNING","title":"Format","why":"Kenapa","source":"prompt","fix":"Sebutkan format"}],
                      "rewrittenPrompt": "Tugas: ...",
                      "confidence": "high"
                    }
                    """.trimIndent(),
                ),
        )

        val result = runBlocking { client().analyze(request()) }
        assertTrue(result is AnalyzeClient.Outcome.Ok)
        val body = (result as AnalyzeClient.Outcome.Ok).body
        assertEquals(1, body.schemaVersion)
        assertEquals("FORMAT_MISSING", body.issues.first().category)
        assertEquals(72, body.promptScore.overall)
    }

    @Test
    fun `200 dengan field hilang dan enum asing tetap parse`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"issues":[{"category":"WHAT","severity":"SUPER"}]}"""),
        )
        val result = runBlocking { client().analyze(request()) }
        assertTrue(result is AnalyzeClient.Outcome.Ok)
        val body = (result as AnalyzeClient.Outcome.Ok).body
        assertEquals("WHAT", body.issues.first().category)
    }

    @Test
    fun `429 quota dipetakan`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(429)
                .setBody("""{"error":{"code":"QUOTA_EXCEEDED","message":"Kuota habis","retryAfterSec":3600}}"""),
        )
        val result = runBlocking { client().analyze(request()) }
        assertTrue(result is AnalyzeClient.Outcome.Err)
        val err = result as AnalyzeClient.Outcome.Err
        assertEquals("QUOTA_EXCEEDED", err.code)
        assertEquals(3600, err.retryAfterSec)
    }

    @Test
    fun `500 bukan JSON memberi kode HTTP`() {
        server.enqueue(MockResponse().setResponseCode(500).setBody("<html>oops</html>"))
        val result = runBlocking { client().analyze(request()) }
        assertTrue(result is AnalyzeClient.Outcome.Err)
        assertEquals("HTTP_500", (result as AnalyzeClient.Outcome.Err).code)
    }

    @Test
    fun `200 bukan JSON memberi BAD_RESPONSE`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("bukan json"))
        val result = runBlocking { client().analyze(request()) }
        assertTrue(result is AnalyzeClient.Outcome.Err)
        assertEquals("BAD_RESPONSE", (result as AnalyzeClient.Outcome.Err).code)
    }

    @Test
    fun `timeout memberi NETWORK`() {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        val result = runBlocking { client(readTimeoutMs = 300).analyze(request()) }
        assertTrue(result is AnalyzeClient.Outcome.Err)
        assertEquals("NETWORK", (result as AnalyzeClient.Outcome.Err).code)
    }

    @Test
    fun `base url kosong memberi NOT_CONFIGURED`() {
        val result = runBlocking { AnalyzeClient("").analyze(request()) }
        assertTrue(result is AnalyzeClient.Outcome.Err)
        assertEquals("NOT_CONFIGURED", (result as AnalyzeClient.Outcome.Err).code)
    }

    @Test
    fun `schema version lebih baru tetap dikembalikan untuk dicek viewmodel`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"schemaVersion":2}"""))
        val result = runBlocking { client().analyze(request()) }
        assertTrue(result is AnalyzeClient.Outcome.Ok)
        assertEquals(2, (result as AnalyzeClient.Outcome.Ok).body.schemaVersion)
    }
}
