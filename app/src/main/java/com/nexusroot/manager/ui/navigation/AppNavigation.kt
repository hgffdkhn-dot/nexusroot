// AppNavigation.kt
package com.nexusroot.manager.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home : Screen("home", "主页", Icons.Default.Home)
    object Whitelist : Screen("whitelist", "白名单", Icons.Default.CheckCircle)
    object Logs : Screen("logs", "日志", Icons.Default.List)
    object Diagnostics : Screen("diagnostics", "诊断", Icons.Default.Build)
}

// MainActivity.kt
package com.nexusroot.manager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.nexusroot.manager.data.MockDaemonConnector
import com.nexusroot.manager.ui.diagnostics.DiagnosticsScreen
import com.nexusroot.manager.ui.diagnostics.DiagnosticsViewModel
import com.nexusroot.manager.ui.home.HomeScreen
import com.nexusroot.manager.ui.home.HomeViewModel
import com.nexusroot.manager.ui.logs.LogsScreen
import com.nexusroot.manager.ui.logs.LogsViewModel
import com.nexusroot.manager.ui.navigation.Screen
import com.nexusroot.manager.ui.theme.NexusRootTheme
import com.nexusroot.manager.ui.whitelist.WhitelistScreen
import com.nexusroot.manager.ui.whitelist.WhitelistViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NexusRootTheme {
                val navController = rememberNavController()
                val connector = remember { MockDaemonConnector() } // 注入点

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            val navBackStackEntry by navController.currentBackStackEntryAsState()
                            val currentRoute = navBackStackEntry?.destination?.route
                            Screen::class.sealedSubclasses.forEach { screenClass ->
                                val screen = screenClass.objectInstance ?: return@forEach
                                NavigationBarItem(
                                    icon = { Icon(screen.icon, contentDescription = screen.label) },
                                    label = { Text(screen.label) },
                                    selected = currentRoute == screen.route,
                                    onClick = {
                                        if (currentRoute != screen.route) {
                                            navController.navigate(screen.route) {
                                                popUpTo(Screen.Home.route) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Home.route,
                        Modifier.padding(innerPadding)
                    ) {
                        composable(Screen.Home.route) {
                            HomeScreen(viewModel = HomeViewModel(connector))
                        }
                        composable(Screen.Whitelist.route) {
                            WhitelistScreen(viewModel = WhitelistViewModel(connector))
                        }
                        composable(Screen.Logs.route) {
                            LogsScreen(viewModel = LogsViewModel(connector))
                        }
                        composable(Screen.Diagnostics.route) {
                            DiagnosticsScreen(viewModel = DiagnosticsViewModel(connector))
                        }
                    }
                }
            }
        }
    }
}