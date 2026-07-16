// DaemonStatus.kt
package com.nexusroot.manager.data.model

data class DaemonStatus(
    val daemonAlive: Boolean,
    val suVersion: String,
    val suPath: String,
    val seContext: String
)

// WhitelistItem.kt
data class WhitelistItem(
    val packageName: String,
    val appName: String,
    val uid: Int,
    val allowed: Boolean
)

// LogEntry.kt
data class LogEntry(
    val timestamp: Long,
    val type: LogType,
    val message: String,
    val tag: String = ""
)

enum class LogType { SU, MANAGER }