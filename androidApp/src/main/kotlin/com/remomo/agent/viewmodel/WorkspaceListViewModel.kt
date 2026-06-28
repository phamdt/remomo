package com.remomo.agent.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.remomo.agent.api.dto.WorkspaceDto
import com.remomo.agent.data.RecentRun
import com.remomo.agent.repository.RemoteAgentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WorkspaceListUiState(
    val isLoading: Boolean = true,
    val workspaces: List<WorkspaceDto> = emptyList(),
    val recentRuns: List<RecentRun> = emptyList(),
    val error: String? = null,
)

class WorkspaceListViewModel(
    private val repository: RemoteAgentRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(WorkspaceListUiState())
    val state: StateFlow<WorkspaceListUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val recents = repository.loadRecentRuns()
            repository.listWorkspaces()
                .onSuccess { workspaces ->
                    _state.update {
                        it.copy(isLoading = false, workspaces = workspaces, recentRuns = recents)
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(isLoading = false, recentRuns = recents, error = error.message)
                    }
                }
        }
    }
}
