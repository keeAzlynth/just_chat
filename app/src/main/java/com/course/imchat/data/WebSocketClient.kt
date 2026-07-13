package com.course.imchat.data

import kotlinx.coroutines.flow.MutableSharedFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class WebSocketClient(
    private val client: OkHttpClient,
    private val serializer: MessageSerializer,
    private val events: MutableSharedFlow<IncomingEvent>,
) {
    private var socket: WebSocket? = null

    fun connect(url: String, token: String) {
        val request = Request.Builder().url(url).build()
        socket = client.newWebSocket(request, listener)
    }

    fun send(text: String): Boolean {
        return socket?.send(text) ?: false
    }

    fun close() {
        socket?.close(1000, "client closed")
        socket = null
    }

    fun isOpen(): Boolean = socket != null

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            events.tryEmit(IncomingEvent.Connected)
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            runCatching { serializer.parse(text) }
                .onSuccess { events.tryEmit(it) }
                .onFailure { events.tryEmit(IncomingEvent.ServerError("INVALID_FORMAT", "消息格式错误")) }
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            events.tryEmit(IncomingEvent.Disconnected)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            events.tryEmit(IncomingEvent.Failure(t.message ?: "连接失败"))
        }
    }
}
