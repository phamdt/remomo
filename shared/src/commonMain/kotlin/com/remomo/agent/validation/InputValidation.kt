package com.remomo.agent.validation

object InputValidation {
    private val baseRefRegex = Regex("^[a-zA-Z0-9/._-]+$")
    const val MAX_PROMPT_LENGTH = 100_000

    fun validateBaseUrl(url: String, allowLocalhost: Boolean): String? {
        val trimmed = url.trim().removeSuffix("/")
        if (trimmed.isBlank()) return "Server URL is required"
        val lower = trimmed.lowercase()
        if (allowLocalhost && (lower.startsWith("http://10.0.2.2") || lower.startsWith("http://127.0.0.1") || lower.startsWith("http://localhost"))) {
            return null
        }
        if (!lower.startsWith("https://")) return "Use HTTPS for production server URLs"
        return null
    }

    fun validateToken(token: String): String? {
        if (token.isBlank()) return "Bearer token is required"
        return null
    }

    fun validatePrompt(prompt: String): String? {
        if (prompt.isBlank()) return "Prompt is required"
        if (prompt.length > MAX_PROMPT_LENGTH) {
            return "Prompt must be at most $MAX_PROMPT_LENGTH characters"
        }
        return null
    }

    fun validateBaseRef(baseRef: String?): String? {
        if (baseRef.isNullOrBlank()) return null
        if (baseRef.contains("..")) return "Base ref may not contain .."
        if (!baseRefRegex.matches(baseRef)) {
            return "Base ref may only contain letters, numbers, / . _ -"
        }
        return null
    }
}
