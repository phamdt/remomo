package com.remomo.agent.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.remomo.agent.ui.theme.GlassPanel
import com.remomo.agent.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onConfigured: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Server settings", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Connect to your Remote Cursor Agent API. Tokens are stored encrypted on device.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        GlassPanel(modifier = Modifier.fillMaxWidth(), useBlur = false) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.baseUrl,
                    onValueChange = viewModel::updateBaseUrl,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("API base URL") },
                    placeholder = { Text("https://agent.example.com") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.bearerToken,
                    onValueChange = viewModel::updateBearerToken,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Bearer token") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.applyToken,
                    onValueChange = viewModel::updateApplyToken,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Apply token (optional)") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Save prompt drafts")
                    Switch(checked = state.saveDrafts, onCheckedChange = viewModel::updateSaveDrafts)
                }
            }
        }

        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        state.message?.let {
            Text(it, color = MaterialTheme.colorScheme.primary)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = viewModel::testConnection,
                enabled = !state.isTesting,
                modifier = Modifier.semantics { contentDescription = "Test connection" },
            ) {
                if (state.isTesting) {
                    CircularProgressIndicator(modifier = Modifier.height(18.dp))
                } else {
                    Text("Test")
                }
            }
            Button(
                onClick = { viewModel.save(onConfigured) },
                enabled = !state.isSaving,
                modifier = Modifier.semantics { contentDescription = "Save settings" },
            ) {
                Text(if (state.isSaving) "Saving…" else "Save & continue")
            }
        }
    }
}
