package com.remomo.agent.validation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InputValidationTest {
    @Test
    fun acceptsHttpsBaseUrl() {
        assertNull(InputValidation.validateBaseUrl("https://agent.example.com", allowLocalhost = false))
    }

    @Test
    fun rejectsHttpInProduction() {
        assertEquals(
            "Use HTTPS for production server URLs",
            InputValidation.validateBaseUrl("http://agent.example.com", allowLocalhost = false),
        )
    }

    @Test
    fun allowsEmulatorHostInDebug() {
        assertNull(InputValidation.validateBaseUrl("http://10.0.2.2:8787", allowLocalhost = true))
    }

    @Test
    fun validatesBaseRefCharacters() {
        assertNull(InputValidation.validateBaseRef("main"))
        assertEquals(
            "Base ref may not contain ..",
            InputValidation.validateBaseRef("../escape"),
        )
    }

    @Test
    fun enforcesPromptLength() {
        val longPrompt = "a".repeat(InputValidation.MAX_PROMPT_LENGTH + 1)
        assertEquals(
            "Prompt must be at most ${InputValidation.MAX_PROMPT_LENGTH} characters",
            InputValidation.validatePrompt(longPrompt),
        )
    }

    @Test
    fun rejectsDataUrlsForExternalLinks() {
        assertEquals(
            "Invalid URL",
            InputValidation.validateExternalUrl("https://x.com/redirect?u=data:text/html,evil"),
        )
    }
}
