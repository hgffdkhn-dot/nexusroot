package com.nexusroot.manager.ui.logs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.nexusroot.manager.data.model.LogType
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(viewModel: LogsViewModel) {
    val state by viewModel.uiState.collectAsState()
    val logs by viewModel.filteredLogs.collectAsState()

    Scaffold(
        topBar = {
            Column {
                TopAppBar(title = { Text("日志") })
                TabRow(selectedTabIndex = if (state.selectedTab == LogType.SU) 0 else 1) {
                    Tab(
                        selected = state.selectedTab == LogType.SU,
                        onClick = { viewModel.selectTab(LogType.SU) },
                        text = { Text("超级用户") }
                    )
                    Tab(
                        selected = state.selectedTab == LogType.MANAGER,
                        onClick = { viewModel.selectTab(LogType.MANAGER) },
                        text = { Text("管理器") }
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(logs) { log ->
                val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                ListItem(
                    headlineContent = { Text(log.message) },
                    supportingContent = {
                        Text("${log.tag} | ${sdf.format(Date(log.timestamp))}")
                    }
                )
                Divider()
            }
        }
    }
}
