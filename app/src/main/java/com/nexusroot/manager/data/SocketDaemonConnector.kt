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
import java.io.InputStream
import java.io.OutputStream

class SocketDaemonConnector : DaemonConnector {

    private var socket: LocalSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    private suspend fun ensureConnected() {
        if (socket?.isConnected != true) {
            withContext(Dispatchers.IO) {
                socket = LocalSocket().apply {
                    connect(LocalSocketAddress("/dev/socket/nxr_daemon"))
                }
                inputStream = socket!!.inputStream
                outputStream = socket!!.outputStream
            }
        }
    }

    private suspend fun sendRequest(request: Nexusroot.Request): Nexusroot.Response {
        ensureConnected()
        val data = request.toByteArray()
        outputStream!!.write(data)
        outputStream!!.flush()
        // 读取响应（简化处理，假设一次读完）
        val buf = ByteArray(4096)
        val len = inputStream!!.read(buf)
        return Nexusroot.Response.parseFrom(buf, 0, len)
    }

    override val daemonStatusFlow: Flow<DaemonStatus> = flow {
        val req = Nexusroot.Request.newBuilder()
            .setStatus(Nexusroot.StatusRequest.getDefaultInstance())
            .build()
        val resp = sendRequest(req)
        if (resp.success && resp.hasStatus()) {
            val s = resp.status
            emit(DaemonStatus(s.daemonAlive, s.suVersion, s.suPath, s.seContext))
        }
    }

    override val whitelistFlow: Flow<List<WhitelistItem>> = flow {
        val req = Nexusroot.Request.newBuilder()
            .setWhitelist(
                Nexusroot.WhitelistRequest.newBuilder()
                    .setAction(Nexusroot.WhitelistRequest.Action.LIST)
            ).build()
        val resp = sendRequest(req)
        if (resp.success && resp.hasWhitelist()) {
            val list = resp.whitelist.itemsList.map {
                WhitelistItem(it.packageName, "", it.uid, it.allowed) // appName 从 PM 获取，暂空
            }
            emit(list)
        }
    }

    override val logFlow: Flow<LogEntry> = flow {
        // daemon 暂未实现实时日志推送，可在后续扩展
    }

    override suspend fun updateWhitelist(packageName: String, allowed: Boolean) {
        // 需要先解析 packageName -> uid（可通过 PackageManager，这里简化）
        // 暂时发送一个假 uid=0，实际应通过 APK 的 PackageManager 获取
        val item = Nexusroot.WhitelistItem.newBuilder()
            .setUid(0)
            .setPackageName(packageName)
            .setAllowed(allowed)
            .build()
        val req = Nexusroot.Request.newBuilder()
            .setWhitelist(
                Nexusroot.WhitelistRequest.newBuilder()
                    .setAction(if (allowed) Nexusroot.WhitelistRequest.Action.ADD
                               else Nexusroot.WhitelistRequest.Action.REMOVE)
                    .addItems(item)
            ).build()
        sendRequest(req)
    }

    override suspend fun refreshDiagnostics(): Map<String, Any> {
        // 暂通过 status 推断
        val req = Nexusroot.Request.newBuilder()
            .setStatus(Nexusroot.StatusRequest.getDefaultInstance())
            .build()
        val resp = sendRequest(req)
        return mapOf(
            "daemon_pid" to 0,
            "socket_connected" to (socket?.isConnected ?: false),
            "sepolicy_loaded" to true
        )
    }
}
