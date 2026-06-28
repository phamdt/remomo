package com.remomo.agent.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.remomo.agent.api.dto.CreateRunRequest
import com.remomo.agent.api.dto.RunMode
import com.remomo.agent.api.dto.WorkspaceDto
import com.remomo.agent.repository.RemoteAgentRepository
import com.remomo.agent.validation.InputValidation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NewRunUiState(
    val workspace: WorkspaceDto? = null,
    val mode: RunMode = RunMode.PLAN_ONLY,
    val prompt: String = "",
    val baseRef: String = "",
    val isSubmitting: Boolean = false,
    val error: String? = null,
)

class NewRunViewModel(
    private val repository: RemoteAgentRepository,
    workspaceId: String,
    initialMode: RunMode,
) : ViewModel() {
    private val _state = MutableStateFlow(NewRunUiState(mode = initialMode))
    val state: StateFlow<NewRunUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.listWorkspaces()
                .onSuccess { workspaces ->
                    val workspace = workspaces.firstOrNull { it.id == workspaceId }
                    _state.update { it.copy(workspace = workspace) }
                }
        }
    }

    fun updatePrompt(value: String) = _state.update { it.copy(prompt = value, error = null) }
    fun updateBaseRef(value: String) = _state.update { it.copy(baseRef = value, error = null) }
    fun updateMode(value: RunMode) = _state.update { it.copy(mode = value) }

    fun submit(onCreated: (String) -> Unit) {
        val current = _state.value
        val workspace = current.workspace
        if (workspace == null) {
            _state.update { it.copy(error = "Workspace not found") }
            return
        }

        val promptError = InputValidation.validatePrompt(current.prompt)
        val baseRefError = InputValidation.validateBaseRef(current.baseRef.ifBlank { null })
        val error = promptError ?: baseRefError
        if (error != null) {
            _state.update { it.copy(error = error) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, error = null) }
            val request = CreateRunRequest(
                workspaceId = workspace.id,
                mode = current.mode,
                prompt = current.prompt.trim(),
                baseRef = current.baseRef.trim().ifBlank { null },
            )
            repository.createRun(request)
                .onSuccess { runId ->
                    _state.update { it.copy(isSubmitting = false) }
                    onCreated(runId)
                }
                .onFailure { error ->
                    _state.update { it.copy(isSubmitting = false, error = error.message) }
                }
        }
    }
}
