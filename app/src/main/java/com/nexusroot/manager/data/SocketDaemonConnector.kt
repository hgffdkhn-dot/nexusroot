package com.nexusroot.manager.data

import android.net.LocalSocket
import android.net.LocalSocketAddress
import com.nexusroot.manager.data.model.DaemonStatus
import com.nexusroot.manager.data.model.LogEntry
import com.nexusroot.manager.data.model.WhitelistItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import nexusroot.Nexusroot   // 导入生成的 protobuf 类
import java.io.IOException

class SocketDaemonConnector : DaemonConnector {

    private var socket: LocalSocket? = null
    private var inputStream: java.io.InputStream? = null
    private var outputStream: java.io.OutputStream? = null

    private suspend fun ensureConnected(): Boolean {
        if (socket?.isConnected != true) {
            try {
                withContext(Dispatchers.IO) {
                    socket = LocalSocket().apply {
                        connect(LocalSocketAddress("/dev/socket/nxr_daemon"))
                    }
                    inputStream = socket!!.inputStream
                    outputStream = socket!!.outputStream
                }
                return true
            } catch (e: IOException) {
                // daemon 未运行或无法连接
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
            socket?.close()
            socket = null
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
            // 连接失败，返回未运行状态
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
                    appName = "",
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
        // 暂不实现
    }

    override suspend fun updateWhitelist(packageName: String, allowed: Boolean) {
        val item = Nexusroot.WhitelistItem.newBuilder()
            .setUid(0) // TODO: 替换为真实 UID
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
        val resp = sendRequest(
            Nexusroot.Request.newBuilder()
                .setStatus(Nexusroot.StatusRequest.getDefaultInstance())
                .build()
        )
        return mapOf(
            "daemon_pid" to 0,
            "socket_connected" to (socket?.isConnected ?: false),
            "sepolicy_loaded" to true
        )
    }
}
