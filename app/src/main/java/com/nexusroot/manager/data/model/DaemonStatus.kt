package com.nexusroot.manager.data.model

data class DaemonStatus(
    val daemonAlive: Boolean,
    val suVersion: String,
    val suPath: String,
    val seContext: String
)
