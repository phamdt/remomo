package com.remomo.agent.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AndroidSettingsStore(
    context: Context,
) : SettingsStore {
    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    override suspend fun load(): AppSettings = withContext(Dispatchers.IO) {
        AppSettings(
            baseUrl = prefs.getString(KEY_BASE_URL, "").orEmpty(),
            bearerToken = prefs.getString(KEY_BEARER_TOKEN, "").orEmpty(),
            applyToken = prefs.getString(KEY_APPLY_TOKEN, "").orEmpty(),
            saveDrafts = prefs.getBoolean(KEY_SAVE_DRAFTS, false),
        )
    }

    override suspend fun save(settings: AppSettings) = withContext(Dispatchers.IO) {
        prefs.edit()
            .putString(KEY_BASE_URL, settings.baseUrl.trim())
            .putString(KEY_BEARER_TOKEN, settings.bearerToken)
            .putString(KEY_APPLY_TOKEN, settings.applyToken)
            .putBoolean(KEY_SAVE_DRAFTS, settings.saveDrafts)
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "remote_agent_settings"
        const val KEY_BASE_URL = "base_url"
        const val KEY_BEARER_TOKEN = "bearer_token"
        const val KEY_APPLY_TOKEN = "apply_token"
        const val KEY_SAVE_DRAFTS = "save_drafts"
    }
}

class AndroidRecentRunsStore(
    context: Context,
) : RecentRunsStore {
    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun load(): List<RecentRun> = withContext(Dispatchers.IO) {
        val raw = prefs.getString(KEY_RECENTS, null) ?: return@withContext emptyList()
        runCatching { json.decodeFromString<List<RecentRun>>(raw) }.getOrDefault(emptyList())
    }

    override suspend fun upsert(run: RecentRun) = withContext(Dispatchers.IO) {
        val current = load().filterNot { it.id == run.id }
        val updated = (listOf(run) + current).take(MAX_RECENTS)
        prefs.edit().putString(KEY_RECENTS, json.encodeToString(updated)).apply()
    }

    override suspend fun remove(id: String) = withContext(Dispatchers.IO) {
        val updated = load().filterNot { it.id == id }
        prefs.edit().putString(KEY_RECENTS, json.encodeToString(updated)).apply()
    }

    private companion object {
        const val PREFS_NAME = "remote_agent_recents"
        const val KEY_RECENTS = "recents"
        const val MAX_RECENTS = 20
    }
}
