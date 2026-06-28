package com.remomo.agent.ui.navigation

object Routes {
    const val SETTINGS = "settings"
    const val WORKSPACES = "workspaces"
    const val NEW_RUN = "new_run/{workspaceId}?mode={mode}"
    const val RUN_DETAIL = "run/{runId}"

    fun newRun(workspaceId: String, mode: String = "plan_only"): String =
        "new_run/$workspaceId?mode=$mode"

    fun runDetail(runId: String): String = "run/$runId"
}
