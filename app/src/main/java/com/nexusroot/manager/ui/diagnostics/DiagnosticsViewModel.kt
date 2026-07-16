package com.nexusroot.manager.ui.diagnostics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexusroot.manager.data.DaemonConnector
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DiagnosticsUiState(
    val diagnosticsData: Map<String, Any> = emptyMap(),
    val loading: Boolean = false
)

class DiagnosticsViewModel(private val connector: DaemonConnector) : ViewModel() {
    private val _uiState = MutableStateFlow(DiagnosticsUiState())
    val uiState: StateFlow<DiagnosticsUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true)
            val data = connector.refreshDiagnostics()
            _uiState.value = DiagnosticsUiState(diagnosticsData = data, loading = false)
        }
    }
}