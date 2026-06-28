package com.remomo.agent.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.remomo.agent.api.dto.WorkspaceDto
import com.remomo.agent.data.RecentRun
import com.remomo.agent.ui.theme.GlassPanel
import com.remomo.agent.viewmodel.WorkspaceListViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WorkspaceListScreen(
    viewModel: WorkspaceListViewModel,
    onOpenSettings: () -> Unit,
    onSelectWorkspace: (WorkspaceDto) -> Unit,
    onOpenRun: (String) -> Unit,
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workspaces") },
                actions = {
                    IconButton(
                        onClick = viewModel::refresh,
                        modifier = Modifier.semantics { contentDescription = "Refresh workspaces" },
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                    }
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.semantics { contentDescription = "Open settings" },
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        if (state.isLoading) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.padding(24.dp))
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.error?.let { error ->
                item {
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
            }

            if (state.recentRuns.isNotEmpty()) {
                item { Text("Recent runs", style = MaterialTheme.typography.titleMedium) }
                items(state.recentRuns, key = RecentRun::id) { run ->
                    GlassPanel(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenRun(run.id) },
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(run.id, style = MaterialTheme.typography.titleSmall)
                            Text("Status: ${run.status}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                item { Text("Workspaces", style = MaterialTheme.typography.titleMedium) }
            }

            if (state.workspaces.isEmpty()) {
                item {
                    Text(
                        "No workspaces returned. Check server config.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            items(state.workspaces, key = WorkspaceDto::id) { workspace ->
                GlassPanel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectWorkspace(workspace) },
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(workspace.name, style = MaterialTheme.typography.titleMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            workspace.repos.forEach { repo ->
                                AssistChip(
                                    onClick = {},
                                    label = { Text("${repo.role} · ${repo.path}") },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
