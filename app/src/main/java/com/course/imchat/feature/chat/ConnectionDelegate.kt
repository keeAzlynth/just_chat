package com.course.imchat.feature.chat

import com.course.imchat.AuthStatus
import com.course.imchat.ChatUiState
import com.course.imchat.ConnectionStatus
import com.course.imchat.data.MessageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * WebSocket connection lifecycle + auto-reconnection.
 *
 * Key design decisions:
 * - Exponential backoff: 1s → 2s → 4s → 8s → 15s (max), capped at 10 retries
 * - onConnected cancels any pending reconnect job
 * - Reconnection only triggers when authenticated (session token available)
 * - OkHttp's pingInterval(25s) handles keepalive, so background disconnects are rare
 */
class ConnectionDelegate(
    private val state: MutableStateFlow<ChatUiState>,
    private val repository: MessageRepository,
    private val scope: CoroutineScope,
) {
    private var reconnectAttempt = 0
    private var reconnectJob: Job? = null
    private val maxRetries = 10

    fun connect(wsUrl: String) {
        reconnectJob?.cancel()
        state.update {
            it.copy(
                auth = it.auth.copy(
                    connectionStatus = ConnectionStatus.Connecting,
                    authStatus = if (it.auth.authStatus is AuthStatus.Authenticated) it.auth.authStatus else AuthStatus.NotAuthenticated,
                    errorMessage = null,
                ),
                chat = it.chat.copy(joined = false),
            )
        }
        repository.connect(wsUrl)
        reconnectAttempt = 0
    }

    fun reconnect() {
        val wsUrl = state.value.auth.serverUrl.toWsUrl()
        if (wsUrl.isEmpty()) return
        reconnectJob?.cancel()
        state.update {
            it.copy(auth = it.auth.copy(connectionStatus = ConnectionStatus.Reconnecting, errorMessage = null))
        }
        repository.disconnect()
        repository.connect(wsUrl)
        reconnectAttempt = 0
    }

    fun onConnected() {
        reconnectJob?.cancel()
        reconnectAttempt = 0
        state.update { it.copy(auth = it.auth.copy(connectionStatus = ConnectionStatus.Connected)) }
    }

    fun onDisconnected() {
        val s = state.value
        // Only reconnect if authenticated (we have a session to resume)
        val shouldReconnect = s.authStatus is AuthStatus.Authenticated || s.auth.myUserId != null
        if (shouldReconnect) {
            state.update {
                it.copy(
                    auth = it.auth.copy(connectionStatus = ConnectionStatus.Reconnecting),
                    chat = it.chat.copy(joined = false),
                )
            }
            scheduleAutoReconnect()
        } else {
            state.update {
                it.copy(auth = it.auth.copy(connectionStatus = ConnectionStatus.Disconnected))
            }
        }
    }

    fun onFailure(message: String) {
        if (reconnectAttempt >= maxRetries) {
            state.update {
                it.copy(
                    auth = it.auth.copy(
                        connectionStatus = ConnectionStatus.Error(message),
                        errorMessage = "连接失败(已重试${maxRetries}次): $message",
                    ),
                    chat = it.chat.copy(joined = false),
                )
            }
            return
        }
        state.update {
            it.copy(
                auth = it.auth.copy(connectionStatus = ConnectionStatus.Error(message), errorMessage = message),
                chat = it.chat.copy(joined = false),
            )
        }
        scheduleAutoReconnect()
    }

    fun onReconnected() {
        reconnectJob?.cancel()
        reconnectAttempt = 0
        state.update {
            it.copy(auth = it.auth.copy(connectionStatus = ConnectionStatus.Connected, errorMessage = null))
        }
    }

    private fun scheduleAutoReconnect() {
        reconnectJob?.cancel()
        val wsUrl = state.value.auth.serverUrl.toWsUrl()
        if (wsUrl.isEmpty()) return
        if (reconnectAttempt >= maxRetries) return

        reconnectAttempt++
        // Exponential backoff: 1s, 2s, 4s, 8s, 15s (capped)
        val delayMs = minOf(1000L * (1L shl minOf(reconnectAttempt - 1, 4)), 15_000L)

        state.update { it.copy(auth = it.auth.copy(connectionStatus = ConnectionStatus.Reconnecting)) }
        reconnectJob = scope.launch {
            delay(delayMs)
            repository.connect(wsUrl)
        }
    }

    fun dismissError() {
        state.update { it.copy(auth = it.auth.copy(errorMessage = null)) }
    }
}

private fun String.toWsUrl(): String {
    if (isEmpty()) return ""
    val trimmed = trim()
    return when {
        trimmed.startsWith("ws://") || trimmed.startsWith("wss://") -> trimmed
        trimmed.contains("://") -> trimmed
        else -> "ws://$trimmed"
    }
}
