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

@OptIn(ExperimentalMaterial3Api::class)
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
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusCard("Daemon", state.diagnosticsData["daemon_pid"] != null, Modifier.weight(1f))
                    StatusCard("注入库", true, Modifier.weight(1f))
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

                Spacer(modifier = Modifier.height(24.dp))
                Text("连接诊断", style = MaterialTheme.typography.titleMedium)
                Button(onClick = viewModel::testDirectConnect, modifier = Modifier.fillMaxWidth()) {
                    Text("测试直接连接 /data/local/tmp/nxr_daemon")
                }
                if (state.directConnectResult.isNotEmpty()) {
                    Card(colors = CardDefaults.cardColors(
                        containerColor = if (state.directConnectResult.contains("✅")) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.errorContainer
                    )) {
                        Text(state.directConnectResult, modifier = Modifier.padding(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("外部 Root 利用", style = MaterialTheme.typography.titleMedium)
                Button(onClick = viewModel::detectExternalRoot, modifier = Modifier.fillMaxWidth()) {
                    Text("检测外部 su")
                }
                Button(onClick = viewModel::testWithSu, modifier = Modifier.fillMaxWidth()) {
                    Text("通过 su 测试守护进程连接")
                }
                if (state.externalRootResult.isNotEmpty()) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Text(state.externalRootResult, modifier = Modifier.padding(16.dp))
                    }
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
