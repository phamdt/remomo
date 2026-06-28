package com.remomo.agent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.remomo.agent.api.dto.RunMode
import com.remomo.agent.ui.navigation.Routes
import com.remomo.agent.ui.screens.NewRunScreen
import com.remomo.agent.ui.screens.RunDetailScreen
import com.remomo.agent.ui.screens.SettingsScreen
import com.remomo.agent.ui.screens.WorkspaceListScreen
import com.remomo.agent.ui.theme.MeshBackground
import com.remomo.agent.ui.theme.RemoteAgentTheme
import com.remomo.agent.viewmodel.NewRunViewModel
import com.remomo.agent.viewmodel.RunDetailViewModel
import com.remomo.agent.viewmodel.SettingsViewModel
import com.remomo.agent.viewmodel.WorkspaceListViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = (application as RemoteAgentApplication).repository
        val allowLocalhost = BuildConfig.DEBUG

        setContent {
            RemoteAgentTheme {
                RemoteAgentApp(repository = repository, allowLocalhost = allowLocalhost)
            }
        }
    }
}

@Composable
fun RemoteAgentApp(
    repository: com.remomo.agent.repository.RemoteAgentRepository,
    allowLocalhost: Boolean,
) {
    val navController = rememberNavController()
    var startDestination by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(repository) {
        val settings = repository.loadSettings()
        startDestination = if (settings.isConfigured) Routes.WORKSPACES else Routes.SETTINGS
    }

    val destination = startDestination ?: return

    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            MeshBackground(modifier = Modifier.fillMaxSize())
            NavHost(
                navController = navController,
                startDestination = destination,
                modifier = Modifier.fillMaxSize(),
            ) {
                composable(Routes.SETTINGS) {
                    val vm: SettingsViewModel = viewModel(
                        factory = SettingsViewModelFactory(repository, allowLocalhost),
                    )
                    SettingsScreen(
                        viewModel = vm,
                        onConfigured = {
                            navController.navigate(Routes.WORKSPACES) {
                                popUpTo(Routes.SETTINGS) { inclusive = true }
                            }
                        },
                    )
                }
                composable(Routes.WORKSPACES) {
                    val vm: WorkspaceListViewModel = viewModel(
                        factory = WorkspaceListViewModelFactory(repository),
                    )
                    WorkspaceListScreen(
                        viewModel = vm,
                        onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                        onSelectWorkspace = { workspace ->
                            navController.navigate(Routes.newRun(workspace.id))
                        },
                        onOpenRun = { runId ->
                            navController.navigate(Routes.runDetail(runId))
                        },
                    )
                }
                composable(
                    route = Routes.NEW_RUN,
                    arguments = listOf(
                        navArgument("workspaceId") { type = NavType.StringType },
                        navArgument("mode") {
                            type = NavType.StringType
                            defaultValue = "plan_only"
                        },
                    ),
                ) { entry ->
                    val workspaceId = entry.arguments?.getString("workspaceId").orEmpty()
                    val mode = when (entry.arguments?.getString("mode")) {
                        "apply" -> RunMode.APPLY
                        else -> RunMode.PLAN_ONLY
                    }
                    val vm: NewRunViewModel = viewModel(
                        factory = NewRunViewModelFactory(repository, workspaceId, mode),
                    )
                    NewRunScreen(
                        viewModel = vm,
                        onRunCreated = { runId ->
                            navController.navigate(Routes.runDetail(runId)) {
                                popUpTo(Routes.WORKSPACES)
                            }
                        },
                    )
                }
                composable(
                    route = Routes.RUN_DETAIL,
                    arguments = listOf(navArgument("runId") { type = NavType.StringType }),
                ) { entry ->
                    val runId = entry.arguments?.getString("runId").orEmpty()
                    val vm: RunDetailViewModel = viewModel(
                        factory = RunDetailViewModelFactory(repository, runId),
                    )
                    RunDetailScreen(
                        viewModel = vm,
                        onStartApplyRun = { workspaceId ->
                            navController.navigate(Routes.newRun(workspaceId, "apply"))
                        },
                    )
                }
            }
        }
    }
}
