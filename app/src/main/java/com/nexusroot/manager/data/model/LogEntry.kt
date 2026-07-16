package com.nexusroot.manager.data.model

data class LogEntry(
    val timestamp: Long,
    val type: LogType,
    val message: String,
    val tag: String = ""
)

enum class LogType { SU, MANAGER }
