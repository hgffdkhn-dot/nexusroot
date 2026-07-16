package com.nexusroot.manager.ui.whitelist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhitelistScreen(viewModel: WhitelistViewModel) {
    val filteredItems by viewModel.filteredItems.collectAsState()
    val query by viewModel.uiState.map { it.query }.collectAsState(initial = "")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("白名单管理") },
                actions = {
                    var expanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Default.Search, contentDescription = "搜索")
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        TextField(
                            value = query,
                            onValueChange = viewModel::onSearchQueryChanged,
                            placeholder = { Text("搜索应用...") },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(filteredItems, key = { it.packageName }) { item ->
                ListItem(
                    headlineContent = { Text(item.appName) },
                    supportingContent = { Text(item.packageName) },
                    trailingContent = {
                        Switch(
                            checked = item.allowed,
                            onCheckedChange = { checked ->
                                viewModel.toggleApp(item.packageName, checked)
                            }
                        )
                    }
                )
                Divider()
            }
        }
    }
}