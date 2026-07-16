package com.nexusroot.manager.data.model

data class WhitelistItem(
    val packageName: String,
    val appName: String,
    val uid: Int,
    val allowed: Boolean
)
