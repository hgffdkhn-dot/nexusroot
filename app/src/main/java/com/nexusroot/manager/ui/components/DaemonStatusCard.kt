package com.nexusroot.manager.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DaemonStatusCard(alive: Boolean, suVersion: String, seContext: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(12.dp),
                // 模拟圆点
                contentAlignment = Alignment.Center
            ) {
                val color = if (alive) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.error
                Surface(
                    modifier = Modifier.size(12.dp),
                    shape = MaterialTheme.shapes.small,
                    color = color
                ) {}
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("守护进程状态", style = MaterialTheme.typography.labelSmall)
                Text(
                    text = if (alive) "运行中" else "已停止",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text("SU 版本: $suVersion", style = MaterialTheme.typography.bodyMedium)
                Text("SELinux 上下文: $seContext", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}