package com.remomo.agent.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.remomo.agent.api.dto.RunMode
import com.remomo.agent.model.RunScreenState
import com.remomo.agent.repository.RemoteAgentRepository
import com.remomo.agent.repository.runStateFlow
import com.remomo.agent.validation.InputValidation
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RunDetailUiState(
    val runState: RunScreenState = RunScreenState(),
    val continuePrompt: String = "",
    val isContinuing: Boolean = false,
    val isCancelling: Boolean = false,
    val actionError: String? = null,
)

class RunDetailViewModel(
    private val repository: RemoteAgentRepository,
    private val runId: String,
) : ViewModel() {
    private val runState = runStateFlow()
    private val _uiState = MutableStateFlow(RunDetailUiState())
    val uiState: StateFlow<RunDetailUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null

    init {
        startObserving()
    }

    fun startObserving() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            repository.observeRun(runId, runState)
        }
        viewModelScope.launch {
            runState.collect { runScreen ->
                _uiState.update { it.copy(runState = runScreen) }
            }
        }
    }

    fun updateContinuePrompt(value: String) =
        _uiState.update { it.copy(continuePrompt = value, actionError = null) }

    fun cancelRun() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCancelling = true, actionError = null) }
            repository.cancelRun(runId)
                .onSuccess { _uiState.update { it.copy(isCancelling = false) } }
                .onFailure { error ->
                    _uiState.update { it.copy(isCancelling = false, actionError = error.message) }
                }
        }
    }

    fun continueRun() {
        val prompt = _uiState.value.continuePrompt
        val mode = _uiState.value.runState.summary?.mode ?: RunMode.PLAN_ONLY
        val promptError = InputValidation.validatePrompt(prompt)
        if (promptError != null) {
            _uiState.update { it.copy(actionError = promptError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isContinuing = true, actionError = null) }
            repository.continueRun(runId, prompt.trim(), mode)
                .onSuccess {
                    _uiState.update { it.copy(isContinuing = false, continuePrompt = "") }
                    startObserving()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isContinuing = false, actionError = error.message) }
                }
        }
    }

    override fun onCleared() {
        observeJob?.cancel()
        super.onCleared()
    }
}
