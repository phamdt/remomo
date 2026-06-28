package com.remomo.agent

import android.app.Application
import com.remomo.agent.data.AndroidRecentRunsStore
import com.remomo.agent.data.AndroidSettingsStore
import com.remomo.agent.repository.RemoteAgentRepository

class RemoteAgentApplication : Application() {
    lateinit var repository: RemoteAgentRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = RemoteAgentRepository(
            settingsStore = AndroidSettingsStore(this),
            recentRunsStore = AndroidRecentRunsStore(this),
        )
    }
}
