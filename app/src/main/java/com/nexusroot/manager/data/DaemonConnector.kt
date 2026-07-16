package com.nexusroot.manager.data

import com.nexusroot.manager.data.model.DaemonStatus
import com.nexusroot.manager.data.model.WhitelistItem
import com.nexusroot.manager.data.model.LogEntry
import kotlinx.coroutines.flow.Flow

interface DaemonConnector {
    val daemonStatusFlow: Flow<DaemonStatus>
    val whitelistFlow: Flow<List<WhitelistItem>>
    val logFlow: Flow<LogEntry>

    suspend fun updateWhitelist(packageName: String, allowed: Boolean)
    suspend fun refreshDiagnostics(): Map<String, Any>
}
