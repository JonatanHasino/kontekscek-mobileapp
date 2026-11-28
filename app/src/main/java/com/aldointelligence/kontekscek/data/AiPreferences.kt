package com.aldointelligence.kontekscek.data

import android.content.Context
import com.aldointelligence.kontekscek.data.remote.dto.ApiJson
import com.aldointelligence.kontekscek.domain.model.AnalysisResult
import kotlinx.serialization.Serializable

@Serializable
data class LastAnalysis(
    val result: AnalysisResult,
    val prompt: String? = null,
    val output: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

/**
 * Preferensi khusus fitur Cek AI. Memakai file SharedPreferences yang sama
 * dengan pengaturan lama, tanpa mengubah key lama.
 */
class AiPreferences(context: Context) {
    private val preferences =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun consentGiven(): Boolean = preferences.getBoolean(KEY_CONSENT, false)

    fun setConsentGiven(value: Boolean) {
        preferences.edit().putBoolean(KEY_CONSENT, value).apply()
    }

    fun serverUrl(): String = preferences.getString(KEY_SERVER_URL, null).orEmpty()

    fun setServerUrl(value: String) {
        preferences.edit().putString(KEY_SERVER_URL, value.trim()).apply()
    }

    fun saveTextHistory(): Boolean = preferences.getBoolean(KEY_SAVE_TEXT, true)

    fun setSaveTextHistory(value: Boolean) {
        preferences.edit().putBoolean(KEY_SAVE_TEXT, value).apply()
        if (!value) clearDraft()
    }

    fun loadDraft(): Draft =
        Draft(
            prompt = preferences.getString(KEY_DRAFT_PROMPT, null).orEmpty(),
            output = preferences.getString(KEY_DRAFT_OUTPUT, null).orEmpty(),
            brief = preferences.getString(KEY_DRAFT_BRIEF, null).orEmpty(),
        )

    fun saveDraft(draft: Draft) {
        if (!saveTextHistory()) return
        preferences
            .edit()
            .putString(KEY_DRAFT_PROMPT, draft.prompt)
            .putString(KEY_DRAFT_OUTPUT, draft.output)
            .putString(KEY_DRAFT_BRIEF, draft.brief)
            .apply()
    }

    fun clearDraft() {
        preferences
            .edit()
            .remove(KEY_DRAFT_PROMPT)
            .remove(KEY_DRAFT_OUTPUT)
            .remove(KEY_DRAFT_BRIEF)
            .apply()
    }

    fun loadLastAnalysis(): LastAnalysis? {
        val raw = preferences.getString(KEY_LAST_ANALYSIS, null) ?: return null
        return runCatching { ApiJson.decodeFromString(LastAnalysis.serializer(), raw) }
            .onFailure { preferences.edit().remove(KEY_LAST_ANALYSIS).apply() }
            .getOrNull()
    }

    fun saveLastAnalysis(value: LastAnalysis) {
        val safe =
            if (saveTextHistory()) {
                value
            } else {
                value.copy(prompt = null, output = null)
            }
        val raw = runCatching { ApiJson.encodeToString(LastAnalysis.serializer(), safe) }.getOrNull() ?: return
        preferences.edit().putString(KEY_LAST_ANALYSIS, raw).apply()
    }

    fun clearAnalysisData() {
        preferences
            .edit()
            .remove(KEY_LAST_ANALYSIS)
            .remove(KEY_DRAFT_PROMPT)
            .remove(KEY_DRAFT_OUTPUT)
            .remove(KEY_DRAFT_BRIEF)
            .apply()
    }

    data class Draft(
        val prompt: String = "",
        val output: String = "",
        val brief: String = "",
    )

    private companion object {
        const val PREFERENCES_NAME = "kontekscek_preferences"
        const val KEY_CONSENT = "ai_consent_v1"
        const val KEY_SERVER_URL = "ai_server_url"
        const val KEY_SAVE_TEXT = "ai_save_text"
        const val KEY_DRAFT_PROMPT = "ai_draft_prompt"
        const val KEY_DRAFT_OUTPUT = "ai_draft_output"
        const val KEY_DRAFT_BRIEF = "ai_draft_brief"
        const val KEY_LAST_ANALYSIS = "last_analysis_json"
    }
}
