package com.remomo.agent.ui.screens

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.remomo.agent.api.dto.RunRepoDto
import com.remomo.agent.api.dto.RunStatus
import com.remomo.agent.model.ErrorTimelineEntry
import com.remomo.agent.model.LogTimelineEntry
import com.remomo.agent.model.ResultTimelineEntry
import com.remomo.agent.model.StatusTimelineEntry
import com.remomo.agent.model.TimelineEntry
import com.remomo.agent.model.ToolTimelineEntry
import com.remomo.agent.ui.theme.GlassPanel
import com.remomo.agent.validation.InputValidation
import com.remomo.agent.viewmodel.RunDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunDetailScreen(
    viewModel: RunDetailViewModel,
    onStartApplyRun: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val run = state.runState
    val summary = run.summary
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(summary?.id ?: "Run") },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                StatusHeader(
                    status = summary?.status,
                    mode = summary?.mode?.name?.lowercase(),
                    isStreaming = run.isStreaming,
                )
            }

            run.errorMessage?.let { message ->
                item { Text(message, color = MaterialTheme.colorScheme.error) }
            }
            state.actionError?.let { message ->
                item { Text(message, color = MaterialTheme.colorScheme.error) }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (run.canCancel) {
                        OutlinedButton(
                            onClick = viewModel::cancelRun,
                            enabled = !state.isCancelling,
                        ) {
                            Text(if (state.isCancelling) "Cancelling…" else "Cancel")
                        }
                    }
                }
            }

            if (run.canContinue || run.canStartApplyRun) {
                item {
                    GlassPanel(modifier = Modifier.fillMaxWidth(), useBlur = false) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (run.canContinue) {
                                OutlinedTextField(
                                    value = state.continuePrompt,
                                    onValueChange = viewModel::updateContinuePrompt,
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Follow-up prompt") },
                                    minLines = 3,
                                )
                                Button(
                                    onClick = viewModel::continueRun,
                                    enabled = !state.isContinuing,
                                ) {
                                    Text(if (state.isContinuing) "Sending…" else "Continue")
                                }
                            }
                            if (run.canStartApplyRun && summary != null) {
                                Button(onClick = { onStartApplyRun(summary.workspaceId) }) {
                                    Text("Start apply run")
                                }
                            }
                        }
                    }
                }
            }

            if (summary?.repos?.isNotEmpty() == true) {
                item { Text("Results", style = MaterialTheme.typography.titleMedium) }
                items(summary.repos, key = RunRepoDto::repoId) { repo ->
                    ResultRepoRow(repo = repo, onOpenUrl = { url ->
                        if (InputValidation.validateExternalUrl(url) == null) {
                            CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(url))
                        }
                    })
                }
            }

            item { Text("Timeline", style = MaterialTheme.typography.titleMedium) }
            if (run.timeline.isEmpty() && run.isStreaming) {
                item { CircularProgressIndicator(modifier = Modifier.padding(8.dp)) }
            }
            items(run.timeline, key = TimelineEntry::id) { entry ->
                TimelineRow(entry)
            }
        }
    }
}

@Composable
private fun StatusHeader(
    status: RunStatus?,
    mode: String?,
    isStreaming: Boolean,
) {
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (isStreaming) {
                CircularProgressIndicator(modifier = Modifier.padding(end = 4.dp))
            }
            Column {
                Text(
                    text = status?.name?.lowercase() ?: "loading",
                    style = MaterialTheme.typography.titleMedium,
                )
                mode?.let {
                    Text("Mode: $it", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun ResultRepoRow(
    repo: RunRepoDto,
    onOpenUrl: (String) -> Unit,
) {
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${repo.role} · ${repo.path}", style = MaterialTheme.typography.titleSmall)
            repo.branch?.let { Text("Branch: $it", fontFamily = FontFamily.Monospace) }
            if (repo.prUrl != null) {
                AssistChip(
                    onClick = { onOpenUrl(repo.prUrl) },
                    label = { Text("Open PR") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open pull request") },
                )
            } else {
                Text("No PR link", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun TimelineRow(entry: TimelineEntry) {
    when (entry) {
        is LogTimelineEntry -> GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Text(entry.message, modifier = Modifier.padding(14.dp))
        }
        is ToolTimelineEntry -> GlassPanel(modifier = Modifier.fillMaxWidth(), useBlur = false) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(entry.name, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelLarge)
                entry.summary?.let {
                    Text(it, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        is StatusTimelineEntry -> Text(
            "Status → ${entry.status.name.lowercase()}",
            color = MaterialTheme.colorScheme.primary,
        )
        is ErrorTimelineEntry -> Text(entry.message, color = MaterialTheme.colorScheme.error)
        is ResultTimelineEntry -> Text(
            if (entry.ok) "Run finished successfully" else "Run finished with errors",
            color = if (entry.ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )
    }
}
