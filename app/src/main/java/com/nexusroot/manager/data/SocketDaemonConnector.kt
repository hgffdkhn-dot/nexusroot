package com.nexusroot.manager.data

import android.net.LocalSocket
import android.net.LocalSocketAddress
import android.util.Log
import com.nexusroot.manager.data.model.DaemonStatus
import com.nexusroot.manager.data.model.LogEntry
import com.nexusroot.manager.data.model.WhitelistItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import nexusroot.Nexusroot
import java.io.IOException

class SocketDaemonConnector : DaemonConnector {

    companion object {
        private const val TAG = "NexusRootConn"
    }

    private var socket: LocalSocket? = null
    private var inputStream: java.io.InputStream? = null
    private var outputStream: java.io.OutputStream? = null
    private var lastError: String = ""

    private suspend fun ensureConnected(): Boolean {
        if (socket?.isConnected != true) {
            try {
                withContext(Dispatchers.IO) {
                    Log.d(TAG, "Attempting to connect to @nxr_daemon")
                    socket = LocalSocket().apply {
                        connect(LocalSocketAddress("/dev/socket/nxr_daemon"))
                    }
                    inputStream = socket!!.inputStream
                    outputStream = socket!!.outputStream
                    Log.d(TAG, "Successfully connected to daemon")
                }
                lastError = ""
                return true
            } catch (e: IOException) {
                Log.e(TAG, "Failed to connect to daemon: ${e.message}", e)
                lastError = e.message ?: "Unknown error"
                socket = null
                inputStream = null
                outputStream = null
                return false
            }
        }
        return true
    }

    private suspend fun sendRequest(request: Nexusroot.Request): Nexusroot.Response? {
        if (!ensureConnected()) return null
        try {
            val data = request.toByteArray()
            outputStream?.write(data, 0, data.size)
            outputStream?.flush()
            val buf = ByteArray(4096)
            val len = inputStream?.read(buf) ?: 0
            val responseBytes = buf.copyOf(len)
            return Nexusroot.Response.parseFrom(responseBytes)
        } catch (e: Exception) {
            Log.e(TAG, "Error during request/response", e)
            lastError = e.message ?: "IO error"
            socket?.close()
            socket = null
            inputStream = null
            outputStream = null
            return null
        }
    }

    override val daemonStatusFlow: Flow<DaemonStatus> = flow {
        val req = Nexusroot.Request.newBuilder()
            .setStatus(Nexusroot.StatusRequest.getDefaultInstance())
            .build()
        val resp = sendRequest(req)
        if (resp != null && resp.success && resp.hasStatus()) {
            val s = resp.status
            emit(DaemonStatus(s.daemonAlive, s.suVersion, s.suPath, s.seContext))
        } else {
            emit(DaemonStatus(false, "未知", "/data/adb/nxr/bin/nr-su", ""))
        }
    }

    override val whitelistFlow: Flow<List<WhitelistItem>> = flow {
        val req = Nexusroot.Request.newBuilder()
            .setWhitelist(
                Nexusroot.WhitelistRequest.newBuilder()
                    .setAction(Nexusroot.WhitelistRequest.Action.LIST)
            ).build()
        val resp = sendRequest(req)
        if (resp != null && resp.success && resp.hasWhitelist()) {
            val list = resp.whitelist.itemsList.map { item ->
                WhitelistItem(
                    packageName = item.packageName,
                    appName = "", // 后续通过 PackageManager 填充
                    uid = item.uid,
                    allowed = item.allowed
                )
            }
            emit(list)
        } else {
            emit(emptyList())
        }
    }

    override val logFlow: Flow<LogEntry> = flow {
        // 日志实时推送暂未实现，可留空
    }

    override suspend fun updateWhitelist(packageName: String, allowed: Boolean) {
        // TODO: 需要从 PackageManager 获取正确的 UID，这里暂时写死 0 测试
        val item = Nexusroot.WhitelistItem.newBuilder()
            .setUid(0)
            .setPackageName(packageName)
            .setAllowed(allowed)
            .build()
        val req = Nexusroot.Request.newBuilder()
            .setWhitelist(
                Nexusroot.WhitelistRequest.newBuilder()
                    .setAction(
                        if (allowed) Nexusroot.WhitelistRequest.Action.ADD
                        else Nexusroot.WhitelistRequest.Action.REMOVE
                    )
                    .addItems(item)
            ).build()
        sendRequest(req)
    }

    override suspend fun refreshDiagnostics(): Map<String, Any> {
        // 先尝试连接，更新 lastError
        ensureConnected()
        return mapOf(
            "daemon_pid" to "unknown",
            "socket_connected" to (socket?.isConnected ?: false),
            "last_error" to lastError,
            "daemon_address" to "@nxr_daemon"
        )
    }
}
