package com.remomo.agent.repository

import com.remomo.agent.api.ApiException
import com.remomo.agent.api.RemoteAgentApi
import com.remomo.agent.api.createHttpClient
import com.remomo.agent.api.dto.CreateRunRequest
import com.remomo.agent.api.dto.RunMode
import com.remomo.agent.api.dto.RunStatus
import com.remomo.agent.api.dto.RunSummaryDto
import com.remomo.agent.api.dto.SseEvent
import com.remomo.agent.api.dto.WorkspaceDto
import com.remomo.agent.api.isActive
import com.remomo.agent.api.isTerminal
import com.remomo.agent.data.AppSettings
import com.remomo.agent.data.RecentRun
import com.remomo.agent.data.RecentRunsStore
import com.remomo.agent.data.SettingsStore
import com.remomo.agent.model.RunPhase
import com.remomo.agent.model.RunScreenState
import com.remomo.agent.model.TimelineEntry
import com.remomo.agent.model.toTimelineEntry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.min

class RemoteAgentRepository(
    private val settingsStore: SettingsStore,
    private val recentRunsStore: RecentRunsStore,
    private val api: RemoteAgentApi = RemoteAgentApi(createHttpClient()),
) {
    private val settingsMutex = Mutex()
    private var cachedSettings: AppSettings? = null

    suspend fun loadSettings(): AppSettings = settingsMutex.withLock {
        cachedSettings ?: settingsStore.load().also { cachedSettings = it }
    }

    suspend fun saveSettings(settings: AppSettings) = settingsMutex.withLock {
        settingsStore.save(settings)
        cachedSettings = settings
    }

    suspend fun testConnection(settings: AppSettings): Result<List<WorkspaceDto>> =
        runCatching { api.listWorkspaces(settings) }

    suspend fun listWorkspaces(): Result<List<WorkspaceDto>> =
        runCatching { api.listWorkspaces(requireSettings()) }

    suspend fun createRun(request: CreateRunRequest): Result<String> =
        runCatching {
            val settings = requireSettings()
            val response = api.createRun(settings, request)
            recentRunsStore.upsert(
                RecentRun(
                    id = response.id,
                    workspaceId = request.workspaceId,
                    status = RunStatus.QUEUED.name.lowercase(),
                    updatedAt = "",
                ),
            )
            response.id
        }

    suspend fun getRunSummary(runId: String): Result<RunSummaryDto> =
        runCatching { api.getRunSummary(requireSettings(), runId) }

    suspend fun continueRun(
        runId: String,
        prompt: String,
        mode: RunMode,
    ): Result<Unit> = runCatching {
        api.continueRun(requireSettings(), runId, prompt, mode)
    }

    suspend fun cancelRun(runId: String): Result<Unit> = runCatching {
        api.cancelRun(requireSettings(), runId)
    }

    suspend fun loadRecentRuns(): List<RecentRun> = recentRunsStore.load()

    suspend fun observeRun(
        runId: String,
        state: MutableStateFlow<RunScreenState>,
    ) {
        var sequence = 0
        var backoffMs = 1_000L
        val seenStatus = mutableSetOf<RunStatus>()

        while (true) {
            val summary = runCatching { api.getRunSummary(requireSettings(), runId) }
                .getOrElse { error ->
                    state.update {
                        it.copy(
                            errorMessage = (error as? ApiException)?.message ?: error.message,
                            phase = RunPhase.FAILED,
                            isStreaming = false,
                        )
                    }
                    return
                }

            state.update { current ->
                current.copy(
                    summary = summary,
                    phase = phaseFor(summary),
                    errorMessage = null,
                )
            }
            recentRunsStore.upsert(
                RecentRun(
                    id = summary.id,
                    workspaceId = summary.workspaceId,
                    status = summary.status.name.lowercase(),
                    updatedAt = summary.updatedAt,
                ),
            )

            if (summary.status.isTerminal()) {
                state.update { it.copy(isStreaming = false) }
                return
            }

            state.update { it.copy(isStreaming = true, phase = RunPhase.STREAMING) }

            var streamFinished = false
            try {
                api.streamEvents(requireSettings(), runId)
                    .catch { error ->
                        if (error is CancellationException) throw error
                        state.update {
                            it.copy(errorMessage = error.message ?: "Stream disconnected")
                        }
                    }
                    .collect { event ->
                        sequence += 1
                        if (event is SseEvent.Status && event.status in seenStatus && seenStatus.size > 1) {
                            if (event.status == RunStatus.QUEUED) return@collect
                        }
                        if (event is SseEvent.Status) seenStatus.add(event.status)

                        state.update { current ->
                            val timeline = current.timeline + event.toTimelineEntry(sequence)
                            val lastResultOk = if (event is SseEvent.Result) event.ok else current.lastResultOk
                            val updatedSummary = if (event is SseEvent.Status) {
                                current.summary?.copy(status = event.status)
                            } else {
                                current.summary
                            }
                            current.copy(
                                timeline = timeline,
                                summary = updatedSummary,
                                lastResultOk = lastResultOk,
                                phase = updatedSummary?.let { phaseFor(it) } ?: current.phase,
                            )
                        }

                        if (event is SseEvent.Result || (event is SseEvent.Status && event.status.isTerminal())) {
                            val refreshed = api.getRunSummary(requireSettings(), runId)
                            state.update {
                                it.copy(
                                    summary = refreshed,
                                    isStreaming = false,
                                    phase = phaseFor(refreshed),
                                )
                            }
                            recentRunsStore.upsert(
                                RecentRun(
                                    id = refreshed.id,
                                    workspaceId = refreshed.workspaceId,
                                    status = refreshed.status.name.lowercase(),
                                    updatedAt = refreshed.updatedAt,
                                ),
                            )
                            streamFinished = true
                        }
                    }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                state.update { it.copy(errorMessage = error.message) }
            }

            if (streamFinished) {
                return
            }

            val polled = api.getRunSummary(requireSettings(), runId)
            state.update {
                it.copy(summary = polled, phase = phaseFor(polled))
            }
            if (polled.status.isTerminal()) {
                state.update { it.copy(isStreaming = false) }
                return
            }

            delay(backoffMs)
            backoffMs = min(backoffMs * 2, 30_000L)
        }
    }

    private suspend fun requireSettings(): AppSettings {
        val settings = loadSettings()
        check(settings.isConfigured) { "Configure server URL and token in Settings" }
        return settings
    }

    private fun phaseFor(summary: RunSummaryDto): RunPhase =
        when {
            summary.status == RunStatus.FAILED -> RunPhase.FAILED
            summary.status == RunStatus.CANCELLED -> RunPhase.CANCELLED
            summary.status == RunStatus.COMPLETED && summary.mode == RunMode.PLAN_ONLY ->
                RunPhase.WAITING_FOR_REVIEW
            summary.status == RunStatus.COMPLETED -> RunPhase.COMPLETED
            summary.status.isActive() -> RunPhase.STREAMING
            else -> RunPhase.LOADING
        }
}

fun runStateFlow(): MutableStateFlow<RunScreenState> =
    MutableStateFlow(RunScreenState())
