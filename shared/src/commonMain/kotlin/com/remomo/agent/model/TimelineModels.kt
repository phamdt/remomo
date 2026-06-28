package com.remomo.agent.model

import com.remomo.agent.api.dto.RunMode
import com.remomo.agent.api.dto.RunStatus
import com.remomo.agent.api.dto.RunSummaryDto
import com.remomo.agent.api.dto.SseEvent
import com.remomo.agent.api.isTerminal
import com.remomo.agent.util.SafeText

sealed interface TimelineEntry {
    val id: String
}

data class StatusTimelineEntry(
    override val id: String,
    val status: RunStatus,
) : TimelineEntry

data class LogTimelineEntry(
    override val id: String,
    val message: String,
) : TimelineEntry

data class ToolTimelineEntry(
    override val id: String,
    val name: String,
    val summary: String?,
) : TimelineEntry

data class ErrorTimelineEntry(
    override val id: String,
    val message: String,
) : TimelineEntry

data class ResultTimelineEntry(
    override val id: String,
    val ok: Boolean,
) : TimelineEntry

fun SseEvent.toTimelineEntry(sequence: Int): TimelineEntry {
    val id = "${type}_$sequence"
    return when (this) {
        is SseEvent.Status -> StatusTimelineEntry(id, status)
        is SseEvent.Log -> LogTimelineEntry(id, SafeText.sanitizeForDisplay(message))
        is SseEvent.Tool -> ToolTimelineEntry(id, name, summary?.let(SafeText::sanitizeForDisplay))
        is SseEvent.Error -> ErrorTimelineEntry(id, SafeText.sanitizeForDisplay(message))
        is SseEvent.Result -> ResultTimelineEntry(id, ok)
    }
}

enum class RunPhase {
    LOADING,
    STREAMING,
    WAITING_FOR_REVIEW,
    COMPLETED,
    FAILED,
    CANCELLED,
}

data class RunScreenState(
    val summary: RunSummaryDto? = null,
    val timeline: List<TimelineEntry> = emptyList(),
    val phase: RunPhase = RunPhase.LOADING,
    val isStreaming: Boolean = false,
    val errorMessage: String? = null,
    val lastResultOk: Boolean? = null,
) {
    val status: RunStatus?
        get() = summary?.status

    val canCancel: Boolean
        get() = summary?.status?.let { it == RunStatus.QUEUED || it == RunStatus.RUNNING } == true

    val canContinue: Boolean
        get() = summary?.status?.isTerminal() == true &&
            summary.mode == RunMode.PLAN_ONLY &&
            phase == RunPhase.WAITING_FOR_REVIEW

    val canStartApplyRun: Boolean
        get() = summary?.status == RunStatus.COMPLETED &&
            summary.mode == RunMode.PLAN_ONLY &&
            lastResultOk == true
}
