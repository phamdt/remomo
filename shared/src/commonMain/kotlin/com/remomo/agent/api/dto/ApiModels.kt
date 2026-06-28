package com.remomo.agent.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WorkspacesResponse(
    val workspaces: List<WorkspaceDto>,
)

@Serializable
data class WorkspaceDto(
    val id: String,
    val name: String,
    val repos: List<WorkspaceRepoDto>,
    val defaultPromptContext: String? = null,
)

@Serializable
data class WorkspaceRepoDto(
    val repoId: String,
    val role: String,
    val path: String,
)

@Serializable
enum class RunMode {
    @SerialName("plan_only")
    PLAN_ONLY,

    @SerialName("apply")
    APPLY,
}

@Serializable
enum class RunStatus {
    @SerialName("queued")
    QUEUED,

    @SerialName("running")
    RUNNING,

    @SerialName("completed")
    COMPLETED,

    @SerialName("failed")
    FAILED,

    @SerialName("cancelled")
    CANCELLED,
}

@Serializable
data class CreateRunRequest(
    val workspaceId: String,
    val mode: RunMode,
    val prompt: String,
    val baseRef: String? = null,
)

@Serializable
data class CreateRunResponse(
    val id: String,
)

@Serializable
data class ContinueRunRequest(
    val prompt: String,
)

@Serializable
data class RunRepoDto(
    val repoId: String,
    val role: String,
    val path: String,
    val branch: String? = null,
    val prUrl: String? = null,
)

@Serializable
data class RunSummaryDto(
    val id: String,
    val workspaceId: String,
    val status: RunStatus,
    val mode: RunMode,
    val repos: List<RunRepoDto> = emptyList(),
    val resultPath: String? = null,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class ApiErrorResponse(
    val error: String,
)

@Serializable
data class OkResponse(
    val ok: Boolean = true,
)
