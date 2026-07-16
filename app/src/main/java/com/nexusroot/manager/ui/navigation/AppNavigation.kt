package com.nexusroot.manager.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home : Screen("home", "主页", Icons.Default.Home)
    object Whitelist : Screen("whitelist", "白名单", Icons.Default.CheckCircle)
    object Logs : Screen("logs", "日志", Icons.Default.List)
    object Diagnostics : Screen("diagnostics", "诊断", Icons.Default.Build)

    companion object {
        val allScreens = listOf(Home, Whitelist, Logs, Diagnostics)
    }
}
