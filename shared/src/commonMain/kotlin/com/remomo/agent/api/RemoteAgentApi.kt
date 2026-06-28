package com.remomo.agent.api

import com.remomo.agent.api.dto.ApiErrorResponse
import com.remomo.agent.api.dto.ContinueRunRequest
import com.remomo.agent.api.dto.CreateRunRequest
import com.remomo.agent.api.dto.CreateRunResponse
import com.remomo.agent.api.dto.OkResponse
import com.remomo.agent.api.dto.RunSummaryDto
import com.remomo.agent.api.dto.SseEvent
import com.remomo.agent.api.dto.WorkspaceDto
import com.remomo.agent.api.dto.WorkspacesResponse
import com.remomo.agent.data.AppSettings
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json

class RemoteAgentApi(
    private val client: HttpClient,
    private val json: Json = ApiJson,
) {
    suspend fun listWorkspaces(settings: AppSettings): List<WorkspaceDto> =
        authorizedGet(settings, "/v1/workspaces").body<WorkspacesResponse>().workspaces

    suspend fun createRun(
        settings: AppSettings,
        request: CreateRunRequest,
    ): CreateRunResponse {
        val token = authTokenForMode(settings, request.mode)
        return authorizedPost(settings, "/v1/runs", token) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun getRunSummary(settings: AppSettings, runId: String): RunSummaryDto =
        authorizedGet(settings, "/v1/runs/$runId").body()

    suspend fun continueRun(
        settings: AppSettings,
        runId: String,
        prompt: String,
        mode: com.remomo.agent.api.dto.RunMode,
    ): OkResponse {
        val token = authTokenForMode(settings, mode)
        return authorizedPost(settings, "/v1/runs/$runId/continue", token) {
            contentType(ContentType.Application.Json)
            setBody(ContinueRunRequest(prompt))
        }.body()
    }

    suspend fun cancelRun(settings: AppSettings, runId: String): OkResponse =
        authorizedPost(settings, "/v1/runs/$runId/cancel", settings.bearerToken) {
            contentType(ContentType.Application.Json)
        }.body()

    fun streamEvents(settings: AppSettings, runId: String): Flow<SseEvent> = flow {
        val response = client.get("${settings.normalizedBaseUrl()}/v1/runs/$runId/events") {
            applyAuth(settings.bearerToken)
            header(HttpHeaders.Accept, "text/event-stream")
        }
        ensureSuccess(response)
        val channel = response.bodyAsChannel()
        val dataLines = mutableListOf<String>()
        while (!channel.isClosedForRead) {
            val line = channel.readUTF8Line() ?: break
            when {
                line.isEmpty() -> {
                    if (dataLines.isNotEmpty()) {
                        emit(SseParser.parseEvent(dataLines.joinToString("\n"), json))
                        dataLines.clear()
                    }
                }
                line.startsWith("data:") -> dataLines.add(line.removePrefix("data:").trim())
                line.startsWith(":") -> Unit
            }
        }
        if (dataLines.isNotEmpty()) {
            emit(SseParser.parseEvent(dataLines.joinToString("\n"), json))
        }
    }.flowOn(Dispatchers.Default)

    private suspend fun authorizedGet(
        settings: AppSettings,
        path: String,
    ): HttpResponse = client.get("${settings.normalizedBaseUrl()}$path") {
        applyAuth(settings.bearerToken)
    }.also { ensureSuccess(it) }

    private suspend fun authorizedPost(
        settings: AppSettings,
        path: String,
        token: String,
        block: HttpRequestBuilder.() -> Unit = {},
    ): HttpResponse = client.post("${settings.normalizedBaseUrl()}$path") {
        applyAuth(token)
        block()
    }.also { ensureSuccess(it) }

    private fun HttpRequestBuilder.applyAuth(token: String) {
        header(HttpHeaders.Authorization, "Bearer $token")
    }

    private suspend fun ensureSuccess(response: HttpResponse) {
        if (response.status.isSuccess()) return
        val message = runCatching { response.body<ApiErrorResponse>().error }
            .getOrDefault("Request failed (${response.status.value})")
        throw ApiException(response.status.value, message)
    }

    private fun authTokenForMode(
        settings: AppSettings,
        mode: com.remomo.agent.api.dto.RunMode,
    ): String {
        if (mode == com.remomo.agent.api.dto.RunMode.APPLY && settings.applyToken.isNotBlank()) {
            return settings.applyToken
        }
        return settings.bearerToken
    }
}
