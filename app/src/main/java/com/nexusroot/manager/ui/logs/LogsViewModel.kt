package com.nexusroot.manager.ui.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexusroot.manager.data.DaemonConnector
import com.nexusroot.manager.data.model.LogEntry
import com.nexusroot.manager.data.model.LogType
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class LogsUiState(
    val logs: List<LogEntry> = emptyList(),
    val selectedTab: LogType = LogType.SU
)

class LogsViewModel(private val connector: DaemonConnector) : ViewModel() {
    private val _uiState = MutableStateFlow(LogsUiState())
    val uiState: StateFlow<LogsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            connector.logFlow.collect { entry ->
                _uiState.update { state ->
                    state.copy(logs = (listOf(entry) + state.logs).take(100)) // 限制数量
                }
            }
        }
    }

    fun selectTab(type: LogType) {
        _uiState.value = _uiState.value.copy(selectedTab = type)
    }

    val filteredLogs: StateFlow<List<LogEntry>> = _uiState.map { state ->
        state.logs.filter { it.type == state.selectedTab }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
}