package com.nexusroot.manager.ui.diagnostics

import android.net.LocalSocket
import android.net.LocalSocketAddress
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexusroot.manager.data.DaemonConnector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DiagnosticsUiState(
    val diagnosticsData: Map<String, Any> = emptyMap(),
    val loading: Boolean = false,
    val directConnectResult: String = ""
)

class DiagnosticsViewModel(private val connector: DaemonConnector) : ViewModel() {

    private val _uiState = MutableStateFlow(DiagnosticsUiState())
    val uiState: StateFlow<DiagnosticsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            val data = connector.refreshDiagnostics()
            _uiState.update { it.copy(diagnosticsData = data, loading = false) }
        }
    }

    fun testDirectConnect() {
        viewModelScope.launch {
            _uiState.update { it.copy(directConnectResult = "正在测试...") }
            val result = withContext(Dispatchers.IO) {
                try {
                    val socket = LocalSocket()
                    socket.connect(LocalSocketAddress("/dev/socket/nxr_daemon"))
                    val connected = socket.isConnected
                    socket.close()
                    if (connected) "✅ 直接连接成功！守护进程可连通。"
                    else "❌ 连接失败：socket 未连接。"
                } catch (e: Exception) {
                    "❌ 异常：${e.message}"
                }
            }
            _uiState.update { it.copy(directConnectResult = result) }
        }
    }
}
