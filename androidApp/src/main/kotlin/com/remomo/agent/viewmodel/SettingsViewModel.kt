package com.remomo.agent.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.remomo.agent.api.dto.RunMode
import com.remomo.agent.data.AppSettings
import com.remomo.agent.repository.RemoteAgentRepository
import com.remomo.agent.validation.InputValidation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val baseUrl: String = "",
    val bearerToken: String = "",
    val applyToken: String = "",
    val saveDrafts: Boolean = false,
    val isSaving: Boolean = false,
    val isTesting: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

class SettingsViewModel(
    private val repository: RemoteAgentRepository,
    private val allowLocalhost: Boolean,
) : ViewModel() {
    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val settings = repository.loadSettings()
            _state.update {
                it.copy(
                    baseUrl = settings.baseUrl,
                    bearerToken = settings.bearerToken,
                    applyToken = settings.applyToken,
                    saveDrafts = settings.saveDrafts,
                )
            }
        }
    }

    fun updateBaseUrl(value: String) = _state.update { it.copy(baseUrl = value, error = null) }
    fun updateBearerToken(value: String) = _state.update { it.copy(bearerToken = value, error = null) }
    fun updateApplyToken(value: String) = _state.update { it.copy(applyToken = value, error = null) }
    fun updateSaveDrafts(value: Boolean) = _state.update { it.copy(saveDrafts = value) }

    fun save(onSaved: () -> Unit) {
        val current = _state.value
        val urlError = InputValidation.validateBaseUrl(current.baseUrl, allowLocalhost)
        val tokenError = InputValidation.validateToken(current.bearerToken)
        val error = urlError ?: tokenError
        if (error != null) {
            _state.update { it.copy(error = error) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null, message = null) }
            runCatching {
                repository.saveSettings(
                    AppSettings(
                        baseUrl = current.baseUrl,
                        bearerToken = current.bearerToken,
                        applyToken = current.applyToken,
                        saveDrafts = current.saveDrafts,
                    ),
                )
            }.onSuccess {
                _state.update { it.copy(isSaving = false, message = "Settings saved") }
                onSaved()
            }.onFailure { error ->
                _state.update { it.copy(isSaving = false, error = error.message) }
            }
        }
    }

    fun testConnection() {
        val current = _state.value
        val settings = AppSettings(
            baseUrl = current.baseUrl,
            bearerToken = current.bearerToken,
            applyToken = current.applyToken,
            saveDrafts = current.saveDrafts,
        )
        val urlError = InputValidation.validateBaseUrl(settings.baseUrl, allowLocalhost)
        val tokenError = InputValidation.validateToken(settings.bearerToken)
        val error = urlError ?: tokenError
        if (error != null) {
            _state.update { it.copy(error = error) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isTesting = true, error = null, message = null) }
            repository.testConnection(settings)
                .onSuccess { workspaces ->
                    _state.update {
                        it.copy(
                            isTesting = false,
                            message = "Connected — ${workspaces.size} workspace(s)",
                        )
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(isTesting = false, error = error.message) }
                }
        }
    }
}
