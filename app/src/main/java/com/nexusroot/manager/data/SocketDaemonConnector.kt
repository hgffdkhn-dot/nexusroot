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
import nexusroot.Nexusroot    // 导入生成的 protobuf 类

class SocketDaemonConnector : DaemonConnector {

    private var socket: LocalSocket? = null
    private var inputStream: java.io.InputStream? = null
    private var outputStream: java.io.OutputStream? = null

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
        outputStream?.write(data, 0, data.size) // 避免重载歧义
        outputStream?.flush()
        val buf = ByteArray(4096)
        val len = inputStream?.read(buf) ?: 0
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
            val list = resp.whitelist.itemsList.map { item ->
                WhitelistItem(
                    packageName = item.packageName,
                    appName = "", // 后续通过 PackageManager 填充
                    uid = item.uid,
                    allowed = item.allowed
                )
            }
            emit(list)
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
