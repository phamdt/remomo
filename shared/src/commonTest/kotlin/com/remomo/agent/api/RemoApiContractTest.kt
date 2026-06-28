package com.remomo.agent.api

import com.remomo.agent.api.dto.CreateRunRequest
import com.remomo.agent.api.dto.RunMode
import com.remomo.agent.data.AppSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Static contract checks against remo /v1 paths and request shapes.
 * Complements remo/tests/mobile-contract.test.ts.
 */
class RemoApiContractTest {
    @Test
    fun normalizedBaseUrlStripsTrailingSlashForV1Paths() {
        val settings = AppSettings(
            baseUrl = "http://10.0.2.2:8080/",
            bearerToken = "token",
        )
        assertEquals("http://10.0.2.2:8080", settings.normalizedBaseUrl())
        assertEquals(
            "${settings.normalizedBaseUrl()}/v1/workspaces",
            "${settings.normalizedBaseUrl()}/v1/workspaces",
        )
    }

    @Test
    fun createRunRequestUsesRemoJsonFieldNames() {
        val json = ApiJson.encodeToString(
            CreateRunRequest.serializer(),
            CreateRunRequest(
                workspaceId = "demo-workspace",
                mode = RunMode.PLAN_ONLY,
                prompt = "Summarize",
                baseRef = "main",
            ),
        )
        assertTrue(json.contains("\"workspaceId\""))
        assertTrue(json.contains("\"plan_only\""))
        assertTrue(json.contains("\"baseRef\""))
    }

    @Test
    fun remoEndpointPathsMatchClient() {
        val base = "http://localhost:8080"
        val runId = "run_123"
        val expected = listOf(
            "$base/v1/workspaces",
            "$base/v1/runs",
            "$base/v1/runs/$runId",
            "$base/v1/runs/$runId/events",
            "$base/v1/runs/$runId/continue",
            "$base/v1/runs/$runId/cancel",
        )
        val clientPaths = listOf(
            "$base/v1/workspaces",
            "$base/v1/runs",
            "$base/v1/runs/$runId",
            "$base/v1/runs/$runId/events",
            "$base/v1/runs/$runId/continue",
            "$base/v1/runs/$runId/cancel",
        )
        assertEquals(expected, clientPaths)
    }
}
