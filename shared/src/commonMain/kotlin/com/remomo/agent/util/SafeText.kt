package com.remomo.agent.util

object SafeText {
    fun sanitizeForDisplay(raw: String): String =
        raw.replace(Regex("<[^>]*>"), "")
            .replace('\u0000', ' ')
            .trim()
}
