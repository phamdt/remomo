package com.remomo.agent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.remomo.agent.api.dto.RunMode
import com.remomo.agent.repository.RemoteAgentRepository
import com.remomo.agent.viewmodel.NewRunViewModel
import com.remomo.agent.viewmodel.RunDetailViewModel
import com.remomo.agent.viewmodel.SettingsViewModel
import com.remomo.agent.viewmodel.WorkspaceListViewModel

class SettingsViewModelFactory(
    private val repository: RemoteAgentRepository,
    private val allowLocalhost: Boolean,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            return SettingsViewModel(repository, allowLocalhost) as T
        }
        error("Unknown ViewModel: ${modelClass.name}")
    }
}

class WorkspaceListViewModelFactory(
    private val repository: RemoteAgentRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WorkspaceListViewModel::class.java)) {
            return WorkspaceListViewModel(repository) as T
        }
        error("Unknown ViewModel: ${modelClass.name}")
    }
}

class NewRunViewModelFactory(
    private val repository: RemoteAgentRepository,
    private val workspaceId: String,
    private val mode: RunMode,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NewRunViewModel::class.java)) {
            return NewRunViewModel(repository, workspaceId, mode) as T
        }
        error("Unknown ViewModel: ${modelClass.name}")
    }
}

class RunDetailViewModelFactory(
    private val repository: RemoteAgentRepository,
    private val runId: String,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RunDetailViewModel::class.java)) {
            return RunDetailViewModel(repository, runId) as T
        }
        error("Unknown ViewModel: ${modelClass.name}")
    }
}
