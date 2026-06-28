package com.remomo.agent.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.remomo.agent.api.dto.RunMode
import com.remomo.agent.ui.theme.GlassPanel
import com.remomo.agent.viewmodel.NewRunViewModel

@Composable
fun NewRunScreen(
    viewModel: NewRunViewModel,
    onRunCreated: (String) -> Unit,
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            state.workspace?.name ?: "New run",
            style = MaterialTheme.typography.headlineMedium,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.mode == RunMode.PLAN_ONLY,
                onClick = { viewModel.updateMode(RunMode.PLAN_ONLY) },
                label = { Text("Plan") },
            )
            FilterChip(
                selected = state.mode == RunMode.APPLY,
                onClick = { viewModel.updateMode(RunMode.APPLY) },
                label = { Text("Apply") },
            )
        }

        GlassPanel(modifier = Modifier.fillMaxWidth(), useBlur = false) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.prompt,
                    onValueChange = viewModel::updatePrompt,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Prompt") },
                    minLines = 6,
                )
                OutlinedTextField(
                    value = state.baseRef,
                    onValueChange = viewModel::updateBaseRef,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Base ref (optional)") },
                    singleLine = true,
                )
            }
        }

        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Button(
            onClick = { viewModel.submit(onRunCreated) },
            enabled = !state.isSubmitting,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.isSubmitting) {
                CircularProgressIndicator()
            } else {
                Text("Start run")
            }
        }
    }
}
