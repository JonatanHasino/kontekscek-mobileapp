package com.aldointelligence.kontekscek.domain.guard

/**
 * Privacy Guard lokal: menyensor data sensitif SEBELUM teks dikirim ke server.
 * Hanya jenis dan jumlah temuan yang dikembalikan — isi temuan tidak pernah keluar.
 */
object PrivacyGuard {

    data class Finding(val type: String, val count: Int)

    data class Result(
        val sanitized: String,
        val findings: List<Finding>,
    ) {
        val hasFindings: Boolean get() = findings.isNotEmpty()
    }

    private class Rule(
        val type: String,
        val regex: Regex,
        val accept: (String) -> Boolean = { true },
    )

    private val rules =
        listOf(
            Rule(
                type = "PRIVATE_KEY",
                regex = Regex("""-----BEGIN [A-Z0-9 ]*PRIVATE KEY-----[\s\S]*?-----END [A-Z0-9 ]*PRIVATE KEY-----"""),
            ),
            Rule(
                type = "API_KEY",
                regex =
                    Regex(
                        """(?<![A-Za-z0-9])(sk-[A-Za-z0-9_-]{20,}|gh[pousr]_[A-Za-z0-9]{36,}|AKIA[0-9A-Z]{16}|AIza[0-9A-Za-z_-]{35})""",
                    ),
            ),
            Rule(
                type = "JWT",
                regex = Regex("""eyJ[A-Za-z0-9_-]{8,}\.[A-Za-z0-9_-]{8,}\.[A-Za-z0-9_-]{8,}"""),
            ),
            Rule(
                type = "SECRET_ASSIGNMENT",
                regex =
                    Regex(
                        """(?i)\b(password|passwd|pwd|secret|token|api[_-]?key)\b\s*[:=]\s*(?!\[REDACTED:)\S+""",
                    ),
            ),
            Rule(
                type = "EMAIL",
                regex = Regex("""[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}"""),
            ),
            Rule(
                type = "PHONE_ID",
                regex = Regex("""(?<!\d)(?:\+62|62|0)8[1-9][0-9]{7,11}(?!\d)"""),
            ),
            Rule(
                type = "CARD",
                regex = Regex("""(?<!\d)(?:\d[ -]?){12,18}\d(?!\d)"""),
                accept = { value -> luhn(value.filter(Char::isDigit)) },
            ),
            Rule(
                type = "NIK",
                regex = Regex("""(?<!\d)\d{16}(?!\d)"""),
                accept = { value -> nikPlausible(value) },
            ),
        )

    fun sanitize(text: String, extraForbidden: List<String> = emptyList()): Result {
        var output = text
        val counts = linkedMapOf<String, Int>()

        rules.forEach { rule ->
            output =
                rule.regex.replace(output) { match ->
                    if (rule.accept(match.value)) {
                        counts.merge(rule.type, 1, Int::plus)
                        "[REDACTED:${rule.type}]"
                    } else {
                        match.value
                    }
                }
        }

        extraForbidden
            .map { it.trim() }
            .filter { it.length >= 3 }
            .distinctBy { it.lowercase() }
            .forEach { word ->
                val regex =
                    Regex(
                        """(?<![\p{L}\p{N}])${Regex.escape(word)}(?![\p{L}\p{N}])""",
                        RegexOption.IGNORE_CASE,
                    )
                output =
                    regex.replace(output) {
                        counts.merge("FORBIDDEN", 1, Int::plus)
                        "[REDACTED:FORBIDDEN]"
                    }
            }

        return Result(
            sanitized = output,
            findings = counts.map { (type, count) -> Finding(type, count) },
        )
    }

    private fun luhn(digits: String): Boolean {
        if (digits.length !in 13..19) return false
        var sum = 0
        var alternate = false
        for (i in digits.length - 1 downTo 0) {
            var n = digits[i] - '0'
            if (alternate) {
                n *= 2
                if (n > 9) n -= 9
            }
            sum += n
            alternate = !alternate
        }
        return sum % 10 == 0
    }

    private fun nikPlausible(value: String): Boolean =
        value.length == 16 && (value.substring(0, 2).toIntOrNull() in 11..94)
}
