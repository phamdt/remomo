package com.remomo.agent.audit

import com.remomo.agent.api.dto.SseEvent
import com.remomo.agent.model.toTimelineEntry
import com.remomo.agent.util.SafeText
import com.remomo.agent.validation.InputValidation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Regression tests documenting KMP security audit findings.
 */
class KmpSecurityAuditTest {
    @Test
    fun externalUrlRejectsNonHttps() {
        assertEquals(
            "Only HTTPS links can be opened",
            InputValidation.validateExternalUrl("http://github.com/org/repo/pull/1"),
        )
    }

    @Test
    fun externalUrlRejectsJavascriptScheme() {
        assertEquals(
            "Invalid URL",
            InputValidation.validateExternalUrl("https://evil.com/javascript:alert(1)"),
        )
    }

    @Test
    fun externalUrlAcceptsHttpsGithubPr() {
        assertNull(
            InputValidation.validateExternalUrl("https://github.com/org/repo/pull/42"),
        )
    }

    @Test
    fun safeTextStripsHtmlTagsFromSseLogs() {
        val sanitized = SafeText.sanitizeForDisplay("<script>alert('x')</script>hello")
        assertEquals("alert('x')hello", sanitized)
        assertTrue(!sanitized.contains("<script>"))
    }

    @Test
    fun toolTimelineNameIsNotSanitized() {
        val entry = SseEvent.Tool(name = "<b>grep</b>", summary = "ok").toTimelineEntry(1)
        val tool = entry as com.remomo.agent.model.ToolTimelineEntry
        assertEquals("<b>grep</b>", tool.name)
        assertNotEquals(tool.name, SafeText.sanitizeForDisplay(tool.name))
    }
}
