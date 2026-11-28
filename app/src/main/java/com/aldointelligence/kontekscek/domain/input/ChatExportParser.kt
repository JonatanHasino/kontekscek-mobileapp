package com.aldointelligence.kontekscek.domain.input

/**
 * Memisahkan teks hasil share/ekspor chat menjadi prompt pengguna dan jawaban AI.
 * Tidak menebak bila tidak ada penanda yang dikenal.
 */
object ChatExportParser {

    data class ParsedChat(
        val prompt: String?,
        val output: String?,
    )

    private val marker =
        Regex(
            """(?im)^\s*(you said|user|human|prompt|chatgpt said|assistant|ai|gemini|claude|model)\s*:\s*""",
        )

    private val userRoles = setOf("you said", "user", "human", "prompt")

    fun parse(text: String): ParsedChat? {
        val matches = marker.findAll(text).toList()
        if (matches.isEmpty()) return null

        var prompt: String? = null
        var output: String? = null

        matches.forEachIndexed { index, match ->
            val role = match.groupValues[1].trim().lowercase()
            val contentStart = match.range.last + 1
            val contentEnd = matches.getOrNull(index + 1)?.range?.first ?: text.length
            val content = text.substring(contentStart, contentEnd).trim()
            if (content.isBlank()) return@forEachIndexed

            if (role in userRoles) {
                if (prompt.isNullOrBlank()) prompt = content
            } else {
                output = if (output.isNullOrBlank()) content else "$output\n\n$content"
            }
        }

        if (prompt.isNullOrBlank() && output.isNullOrBlank()) return null
        return ParsedChat(prompt = prompt, output = output)
    }
}
