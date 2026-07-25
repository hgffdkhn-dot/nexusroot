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
import java.io.BufferedReader
import java.io.InputStreamReader

data class DiagnosticsUiState(
    val diagnosticsData: Map<String, Any> = emptyMap(),
    val loading: Boolean = false,
    val directConnectResult: String = "",
    val externalRootResult: String = ""
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
                    socket.connect(LocalSocketAddress("/data/local/tmp/nxr_daemon"))
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

    fun detectExternalRoot() {
        viewModelScope.launch {
            _uiState.update { it.copy(externalRootResult = "正在检测...") }
            val result = withContext(Dispatchers.IO) {
                try {
                    val process = Runtime.getRuntime().exec(arrayOf("which", "su"))
                    val reader = BufferedReader(InputStreamReader(process.inputStream))
                    val suPath = reader.readLine()
                    if (suPath != null && suPath.isNotEmpty()) {
                        "检测到 su: $suPath\n你可以通过 su 测试守护进程连接。"
                    } else {
                        "未检测到外部 root 管理器。"
                    }
                } catch (e: Exception) {
                    "检测失败: ${e.message}"
                }
            }
            _uiState.update { it.copy(externalRootResult = result) }
        }
    }

    fun testWithSu() {
        viewModelScope.launch {
            _uiState.update { it.copy(externalRootResult = "正在通过 su 测试连接...") }
            val result = withContext(Dispatchers.IO) {
                try {
                    // 获取 su 路径
                    val suProcess = Runtime.getRuntime().exec(arrayOf("which", "su"))
                    val suReader = BufferedReader(InputStreamReader(suProcess.inputStream))
                    val suPath = suReader.readLine()?.trim()
                    val su = if (suPath.isNullOrEmpty()) "su" else suPath

                    // 用 su 执行测试命令：确保权限并尝试连接
                    val cmd = "$su -c 'chmod 777 /data/local/tmp/nxr_daemon 2>/dev/null; " +
                            "printf \"\" | nc -U /data/local/tmp/nxr_daemon 2>&1'"
                    val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
                    val output = process.inputStream.bufferedReader().readText()
                    val error = process.errorStream.bufferedReader().readText()
                    process.waitFor()
                    if (output.contains("Connection refused") || output.contains("refused")) {
                        "❌ 通过 su 测试仍失败：Connection refused\n$output"
                    } else if (output.isNotEmpty()) {
                        "✅ 通过 su 测试成功！守护进程响应正常。\n$output"
                    } else if (error.isNotEmpty()) {
                        "⚠️ 通过 su 执行出错：$error"
                    } else {
                        "⚠️ 通过 su 执行，但无输出，可能连接成功或超时。"
                    }
                } catch (e: Exception) {
                    "❌ 通过 su 测试异常：${e.message}"
                }
            }
            _uiState.update { it.copy(externalRootResult = result) }
        }
    }
}
