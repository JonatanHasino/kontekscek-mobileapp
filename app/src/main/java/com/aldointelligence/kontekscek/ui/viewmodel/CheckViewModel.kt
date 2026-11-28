package com.aldointelligence.kontekscek.ui.viewmodel

import android.app.Application
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aldointelligence.kontekscek.BuildConfig
import com.aldointelligence.kontekscek.data.AiPreferences
import com.aldointelligence.kontekscek.data.LastAnalysis
import com.aldointelligence.kontekscek.data.remote.AnalyzeClient
import com.aldointelligence.kontekscek.data.remote.dto.AnalyzeRequest
import com.aldointelligence.kontekscek.data.remote.mapper.toDomain
import com.aldointelligence.kontekscek.domain.guard.PrivacyGuard
import com.aldointelligence.kontekscek.domain.input.ChatExportParser
import com.aldointelligence.kontekscek.domain.lint.LocalPromptLinter
import com.aldointelligence.kontekscek.domain.model.AnalysisResult
import com.aldointelligence.kontekscek.domain.model.AnalysisSource
import com.aldointelligence.kontekscek.domain.model.SUPPORTED_SCHEMA_VERSION
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class CheckDepth(val label: String) {
    FAST("Cepat"),
    DEEP("Mendalam"),
}

data class CheckPrivacyPreview(
    val sanitizedPrompt: String,
    val sanitizedOutput: String,
    val sanitizedBrief: String,
    val findings: List<PrivacyGuard.Finding>,
)

data class SharedInput(
    val rawText: String,
    val detectedPrompt: String? = null,
    val detectedOutput: String? = null,
) {
    val hasAutoSplit: Boolean get() = !detectedPrompt.isNullOrBlank() || !detectedOutput.isNullOrBlank()
}

data class CheckUiState(
    val prompt: String = "",
    val output: String = "",
    val brief: String = "",
    val depth: CheckDepth = CheckDepth.FAST,
    val loading: Boolean = false,
    val stage: String = "",
    val result: AnalysisResult? = null,
    val errorMessage: String? = null,
    val errorCode: String? = null,
    val privacyPreview: CheckPrivacyPreview? = null,
    val showConsent: Boolean = false,
    val pendingShared: SharedInput? = null,
    val serverUrl: String = "",
    val serverConfigured: Boolean = false,
    val saveTextHistory: Boolean = true,
    val consentGiven: Boolean = false,
    val lastAnalysis: LastAnalysis? = null,
    val previousScore: Int? = null,
    val showBrief: Boolean = false,
)

class CheckViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = AiPreferences(application)
    private val _state = MutableStateFlow(CheckUiState())
    val state: StateFlow<CheckUiState> = _state.asStateFlow()

    private var draftJob: Job? = null
    private var lastSanitized: SanitizedInput? = null

    private data class SanitizedInput(
        val prompt: String,
        val output: String,
        val brief: String,
    )

    init {
        val draft = prefs.loadDraft()
        _state.value =
            _state.value.copy(
                prompt = draft.prompt,
                output = draft.output,
                brief = draft.brief,
                serverUrl = effectiveServerUrl(),
                serverConfigured = effectiveServerUrl().isNotBlank(),
                saveTextHistory = prefs.saveTextHistory(),
                consentGiven = prefs.consentGiven(),
                lastAnalysis = prefs.loadLastAnalysis(),
            )
    }

    // ===== Input =====

    fun updatePrompt(value: String) {
        _state.value = _state.value.copy(prompt = value, errorMessage = null)
        persistDraft()
    }

    fun updateOutput(value: String) {
        _state.value = _state.value.copy(output = value, errorMessage = null)
        persistDraft()
    }

    fun updateBrief(value: String) {
        _state.value = _state.value.copy(brief = value)
        persistDraft()
    }

    fun toggleBrief() {
        _state.value = _state.value.copy(showBrief = !_state.value.showBrief)
    }

    fun pastePrompt(value: String) {
        updatePrompt(value)
    }

    fun pasteOutput(value: String) {
        updateOutput(value)
    }

    fun clearPrompt() = updatePrompt("")

    fun clearOutput() = updateOutput("")

    fun setDepth(depth: CheckDepth) {
        _state.value = _state.value.copy(depth = depth)
    }

    fun setSaveTextHistory(enabled: Boolean) {
        prefs.setSaveTextHistory(enabled)
        _state.value = _state.value.copy(saveTextHistory = enabled)
        if (!enabled) {
            prefs.clearAnalysisData()
            _state.value = _state.value.copy(lastAnalysis = null)
        } else {
            persistDraft()
        }
    }

    fun setServerUrl(url: String) {
        prefs.setServerUrl(url)
        val effective = effectiveServerUrl()
        _state.value =
            _state.value.copy(
                serverUrl = effective,
                serverConfigured = effective.isNotBlank(),
                errorMessage = null,
            )
    }

    // ===== Consent =====

    fun requestConsentIfNeeded(): Boolean {
        if (prefs.consentGiven()) return false
        _state.value = _state.value.copy(showConsent = true)
        return true
    }

    fun giveConsent() {
        prefs.setConsentGiven(true)
        _state.value = _state.value.copy(showConsent = false, consentGiven = true)
        startAnalyzeFlow(_state.value.prompt, _state.value.output, _state.value.brief)
    }

    fun declineConsent() {
        _state.value =
            _state.value.copy(
                showConsent = false,
                errorMessage = "Persetujuan diperlukan untuk memakai analisis AI. Kamu tetap bisa memakai lint lokal.",
            )
    }

    fun withdrawConsent() {
        prefs.setConsentGiven(false)
        _state.value = _state.value.copy(consentGiven = false)
    }

    // ===== Analisis =====

    fun requestAnalyze() {
        val state = _state.value
        if (state.prompt.trim().length < 10) {
            _state.value = state.copy(errorMessage = "Prompt minimal 10 karakter agar bisa dianalisis.")
            return
        }
        val baseUrl = effectiveServerUrl()
        if (baseUrl.isBlank() || !isOnline()) {
            val reason = if (baseUrl.isBlank()) {
                "Server AI belum diatur. Ini hasil lint lokal."
            } else {
                "Kamu sedang offline. Ini hasil lint lokal."
            }
            _state.value =
                state.copy(
                    loading = true,
                    stage = "Memeriksa secara lokal…",
                    result = null,
                    errorMessage = null,
                    previousScore = state.result?.score?.overall ?: state.lastAnalysis?.result?.score?.overall,
                )
            runLocalFallback(SanitizedInput(state.prompt, state.output, state.brief), reason)
            return
        }
        if (requestConsentIfNeeded()) return
        startAnalyzeFlow(state.prompt, state.output, state.brief)
    }

    private fun startAnalyzeFlow(prompt: String, output: String, brief: String) {
        val previous = _state.value.result?.score?.overall ?: _state.value.lastAnalysis?.result?.score?.overall
        _state.value =
            _state.value.copy(
                loading = true,
                stage = "Menyensor data sensitif…",
                result = null,
                errorMessage = null,
                previousScore = previous,
            )
        val promptGuard = PrivacyGuard.sanitize(prompt)
        val outputGuard = PrivacyGuard.sanitize(output)
        val briefGuard = PrivacyGuard.sanitize(brief)

        val findings =
            (promptGuard.findings + outputGuard.findings + briefGuard.findings)
                .groupBy { it.type }
                .map { (type, list) -> PrivacyGuard.Finding(type, list.sumOf { it.count }) }

        if (findings.isNotEmpty()) {
            _state.value =
                _state.value.copy(
                    loading = false,
                    stage = "",
                    privacyPreview =
                        CheckPrivacyPreview(
                            sanitizedPrompt = promptGuard.sanitized,
                            sanitizedOutput = outputGuard.sanitized,
                            sanitizedBrief = briefGuard.sanitized,
                            findings = findings,
                        ),
                )
            return
        }

        performAnalyze(
            SanitizedInput(
                prompt = promptGuard.sanitized,
                output = outputGuard.sanitized,
                brief = briefGuard.sanitized,
            ),
        )
    }

    fun confirmPrivacyPreviewAndAnalyze() {
        val preview = _state.value.privacyPreview ?: return
        _state.value = _state.value.copy(privacyPreview = null, loading = true, stage = "Mengirim versi tersensor…")
        performAnalyze(
            SanitizedInput(
                prompt = preview.sanitizedPrompt,
                output = preview.sanitizedOutput,
                brief = preview.sanitizedBrief,
            ),
        )
    }

    fun cancelPrivacyPreview() {
        _state.value =
            _state.value.copy(
                privacyPreview = null,
                loading = false,
                stage = "",
                errorMessage = "Dibatalkan. Tidak ada teks yang dikirim.",
            )
    }

    private fun performAnalyze(input: SanitizedInput) {
        lastSanitized = input
        val baseUrl = effectiveServerUrl()
        if (baseUrl.isBlank()) {
            runLocalFallback(input, "Server AI belum diatur. Ini hasil lint lokal.")
            return
        }
        if (!isOnline()) {
            runLocalFallback(input, "Kamu sedang offline. Ini hasil lint lokal.")
            return
        }

        _state.value = _state.value.copy(loading = true, stage = "Menganalisis prompt…", errorMessage = null)
        viewModelScope.launch {
            val client = AnalyzeClient(baseUrl)
            val request =
                AnalyzeRequest(
                    requestId = UUID.randomUUID().toString(),
                    depth = _state.value.depth.name,
                    prompt = input.prompt,
                    output = input.output.ifBlank { null },
                    brief = input.brief.ifBlank { null },
                    clientVersion = BuildConfig.VERSION_NAME,
                )
            when (val outcome = client.analyze(request)) {
                is AnalyzeClient.Outcome.Ok -> {
                    val response = outcome.body
                    if (response.schemaVersion > SUPPORTED_SCHEMA_VERSION) {
                        _state.value =
                            _state.value.copy(
                                loading = false,
                                stage = "",
                                errorCode = "SCHEMA_TOO_NEW",
                                errorMessage = "Server memakai format lebih baru. Perbarui aplikasi KonteksCek.",
                            )
                        return@launch
                    }
                    val domain = response.toDomain()
                    finishWithResult(input, domain)
                }

                is AnalyzeClient.Outcome.Err -> {
                    when (outcome.code) {
                        "QUOTA_EXCEEDED" -> {
                            val reset =
                                outcome.retryAfterSec?.let { detik ->
                                    " Coba lagi dalam ±${(detik / 60).coerceAtLeast(1)} menit."
                                } ?: ""
                            _state.value =
                                _state.value.copy(
                                    errorCode = outcome.code,
                                    errorMessage = "Kuota Cek Cepat hari ini habis.$reset",
                                )
                            runLocalFallback(input, "Kuota server habis. Ini hasil lint lokal.")
                        }

                        "RATE_LIMITED" -> {
                            _state.value =
                                _state.value.copy(
                                    errorCode = outcome.code,
                                    errorMessage = "Terlalu banyak permintaan. Tunggu sebentar lalu coba lagi.",
                                )
                        }

                        "PRO_REQUIRED" -> {
                            _state.value =
                                _state.value.copy(
                                    errorCode = outcome.code,
                                    errorMessage = "Fitur ini membutuhkan Pro. Cek Cepat tetap gratis.",
                                )
                        }

                        "INPUT_TOO_LARGE" -> {
                            _state.value =
                                _state.value.copy(
                                    errorCode = outcome.code,
                                    errorMessage = outcome.message.ifBlank { "Teks terlalu panjang. Potong dulu ya." },
                                )
                        }

                        "NETWORK", "PROVIDER_UNAVAILABLE", "ANALYSIS_FAILED", "BAD_RESPONSE" -> {
                            _state.value =
                                _state.value.copy(
                                    errorCode = outcome.code,
                                    errorMessage = outcome.message,
                                )
                            runLocalFallback(input, "Server belum bisa dihubungi. Ini hasil lint lokal.")
                        }

                        else -> {
                            _state.value =
                                _state.value.copy(
                                    errorCode = outcome.code,
                                    errorMessage = outcome.message.ifBlank { "Terjadi kesalahan." },
                                )
                            runLocalFallback(input, "Analisis AI gagal. Ini hasil lint lokal.")
                        }
                    }
                }
            }
        }
    }

    fun runLocalLintNow() {
        val state = _state.value
        if (state.prompt.trim().length < 10) {
            _state.value = state.copy(errorMessage = "Prompt minimal 10 karakter.")
            return
        }
        runLocalFallback(
            SanitizedInput(state.prompt, state.output, state.brief),
            "Hasil lint lokal di perangkat.",
        )
    }

    private fun runLocalFallback(input: SanitizedInput, reason: String) {
        val local = LocalPromptLinter.analyze(input.prompt, input.output, input.brief)
        val result = local.copy(sourceNote = reason)
        finishWithResult(input, result, keepError = true)
    }

    private fun finishWithResult(input: SanitizedInput, result: AnalysisResult, keepError: Boolean = false) {
        val state = _state.value
        val safeError = if (keepError) state.errorMessage else null
        _state.value =
            state.copy(
                loading = false,
                stage = "",
                result = result,
                errorMessage = safeError,
                errorCode = if (keepError) state.errorCode else null,
                lastAnalysis = LastAnalysis(result, input.prompt, input.output),
            )
        val saved = _state.value.lastAnalysis ?: LastAnalysis(result, input.prompt, input.output)
        prefs.saveLastAnalysis(saved)
        if (result.source == AnalysisSource.LOCAL) {
            prefs.saveDraft(AiPreferences.Draft(input.prompt, input.output, input.brief))
        }
    }

    fun retry() {
        val input = lastSanitized
        if (input != null) {
            performAnalyze(input)
        } else {
            requestAnalyze()
        }
    }

    fun reanalyzeWithAnswers(answers: Map<String, String>) {
        if (answers.isEmpty()) return
        val addition =
            answers.entries.joinToString("\n") { (question, answer) ->
                "- $question → $answer"
            }
        val augmented = "${_state.value.prompt}\n\nKlarifikasi tambahan:\n$addition"
        startAnalyzeFlow(augmented, _state.value.output, _state.value.brief)
    }

    fun useRewrittenPrompt() {
        val rewritten = _state.value.result?.rewrittenPrompt.orEmpty()
        if (rewritten.isBlank()) return
        _state.value =
            _state.value.copy(
                prompt = rewritten,
                result = null,
                errorMessage = null,
                errorCode = null,
            )
        persistDraft()
    }

    fun clearResult() {
        _state.value = _state.value.copy(result = null, errorMessage = null, errorCode = null)
    }

    fun dismissError() {
        _state.value = _state.value.copy(errorMessage = null, errorCode = null)
    }

    fun openLastAnalysis() {
        val last = _state.value.lastAnalysis ?: return
        _state.value = _state.value.copy(result = last.result, errorMessage = null, errorCode = null)
    }

    // ===== Share / Process Text =====

    fun onSharedText(text: String) {
        if (text.isBlank()) return
        val parsed = ChatExportParser.parse(text)
        _state.value =
            _state.value.copy(
                pendingShared =
                    SharedInput(
                        rawText = text,
                        detectedPrompt = parsed?.prompt,
                        detectedOutput = parsed?.output,
                    ),
            )
    }

    fun applySharedAsPrompt() {
        val pending = _state.value.pendingShared ?: return
        _state.value = _state.value.copy(prompt = pending.rawText, pendingShared = null)
        persistDraft()
    }

    fun applySharedAsOutput() {
        val pending = _state.value.pendingShared ?: return
        _state.value = _state.value.copy(output = pending.rawText, pendingShared = null)
        persistDraft()
    }

    fun applySharedAutoSplit() {
        val pending = _state.value.pendingShared ?: return
        _state.value =
            _state.value.copy(
                prompt = pending.detectedPrompt.orEmpty(),
                output = pending.detectedOutput.orEmpty(),
                pendingShared = null,
            )
        persistDraft()
    }

    fun dismissShared() {
        _state.value = _state.value.copy(pendingShared = null)
    }

    // ===== Data =====

    fun clearAnalysisData() {
        draftJob?.cancel()
        prefs.clearAnalysisData()
        _state.value =
            _state.value.copy(
                lastAnalysis = null,
                result = null,
                prompt = "",
                output = "",
                brief = "",
                errorMessage = null,
                errorCode = null,
            )
    }

    private fun persistDraft() {
        draftJob?.cancel()
        val snapshot = AiPreferences.Draft(_state.value.prompt, _state.value.output, _state.value.brief)
        draftJob =
            viewModelScope.launch {
                delay(350)
                prefs.saveDraft(snapshot)
            }
    }

    private fun effectiveServerUrl(): String =
        prefs.serverUrl().ifBlank { BuildConfig.API_BASE_URL }.trim()

    private fun isOnline(): Boolean {
        val manager = getApplication<Application>().getSystemService(ConnectivityManager::class.java)
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
