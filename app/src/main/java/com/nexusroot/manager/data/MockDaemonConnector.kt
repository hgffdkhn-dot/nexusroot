// DaemonConnector.kt (接口)
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
    suspend fun refreshDiagnostics(): Map<String, Any> // 返回诊断数据
}

// MockDaemonConnector.kt
package com.nexusroot.manager.data

import com.nexusroot.manager.data.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow

class MockDaemonConnector : DaemonConnector {
    private val _status = MutableStateFlow(
        DaemonStatus(
            daemonAlive = true,
            suVersion = "NexusRoot v1.0.0",
            suPath = "/data/adb/nxr/bin/nr-su",
            seContext = "u:r:nxr_daemon:s0"
        )
    )
    override val daemonStatusFlow: Flow<DaemonStatus> = _status

    private val _whitelist = MutableStateFlow(
        listOf(
            WhitelistItem("com.example.app1", "Test App 1", 10001, true),
            WhitelistItem("com.example.app2", "Test App 2", 10002, false),
            WhitelistItem("com.termux", "Termux", 10086, true)
        )
    )
    override val whitelistFlow: Flow<List<WhitelistItem>> = _whitelist

    override val logFlow: Flow<LogEntry> = flow {
        while (true) {
            emit(
                LogEntry(
                    System.currentTimeMillis(),
                    LogType.SU,
                    "com.termux: exec /system/bin/sh",
                    "su_request"
                )
            )
            delay(5000)
        }
    }

    override suspend fun updateWhitelist(packageName: String, allowed: Boolean) {
        _whitelist.value = _whitelist.value.map {
            if (it.packageName == packageName) it.copy(allowed = allowed) else it
        }
    }

    override suspend fun refreshDiagnostics(): Map<String, Any> {
        delay(500) // 模拟网络延迟
        return mapOf(
            "daemon_pid" to 1234,
            "socket_connected" to true,
            "sepolicy_loaded" to true,
            "connections" to 2
        )
    }
}