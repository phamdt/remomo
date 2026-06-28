package com.remomo.agent.data

import kotlinx.serialization.Serializable

data class AppSettings(
    val baseUrl: String = "",
    val bearerToken: String = "",
    val applyToken: String = "",
    val saveDrafts: Boolean = false,
) {
    val isConfigured: Boolean
        get() = baseUrl.isNotBlank() && bearerToken.isNotBlank()

    fun normalizedBaseUrl(): String = baseUrl.trim().removeSuffix("/")
}

@Serializable
data class RecentRun(
    val id: String,
    val workspaceId: String,
    val status: String,
    val updatedAt: String,
)

interface SettingsStore {
    suspend fun load(): AppSettings
    suspend fun save(settings: AppSettings)
}

interface RecentRunsStore {
    suspend fun load(): List<RecentRun>
    suspend fun upsert(run: RecentRun)
    suspend fun remove(id: String)
}
