package com.nexusroot.manager.ui.diagnostics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DiagnosticsScreen(viewModel: DiagnosticsViewModel) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("系统诊断") },
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Default.Refresh, "刷新")
                    }
                }
            )
        }
    ) { padding ->
        if (state.loading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 组件状态卡片网格
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusCard("Daemon", state.diagnosticsData["daemon_pid"] != null, Modifier.weight(1f))
                    StatusCard("注入库", true, Modifier.weight(1f)) // 示例
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusCard("SELinux", state.diagnosticsData["sepolicy_loaded"] == true, Modifier.weight(1f))
                    StatusCard("内核", true, Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("详细信息", style = MaterialTheme.typography.titleMedium)
                Divider()
                state.diagnosticsData.forEach { (key, value) ->
                    Text("$key: $value", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun StatusCard(title: String, ok: Boolean, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.labelSmall)
            val color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            Surface(Modifier.size(12.dp), shape = MaterialTheme.shapes.small, color = color) {}
            Text(if (ok) "正常" else "异常", style = MaterialTheme.typography.bodySmall)
        }
    }
}