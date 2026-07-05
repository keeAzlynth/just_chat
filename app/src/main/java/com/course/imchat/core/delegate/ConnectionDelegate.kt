package com.course.imchat.core.delegate

import com.course.imchat.ConnectionStatus
import com.course.imchat.data.MessageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Handles WebSocket connection lifecycle and auto-reconnection.
 * Decoupled from ChatViewModel — single responsibility.
 */
class ConnectionDelegate(
    private val state: MutableStateFlow<com.course.imchat.ChatUiState>,
    private val repository: MessageRepository,
    private val scope: CoroutineScope,
) {
    private var reconnectAttempt = 0
    private var reconnectJob: Job? = null

    fun connect(wsUrl: String) {
        state.update {
            it.copy(
                connectionStatus = ConnectionStatus.Connecting,
                authStatus = com.course.imchat.AuthStatus.NotAuthenticated,
                joined = false,
                errorMessage = null,
            )
        }
        repository.connect(wsUrl)
        reconnectAttempt = 0
    }

    fun reconnect() {
        val wsUrl = state.value.serverUrl.toWsUrl()
        if (wsUrl.isEmpty()) return

        state.update {
            it.copy(connectionStatus = ConnectionStatus.Reconnecting, errorMessage = null)
        }
        repository.disconnect()
        repository.connect(wsUrl)
        reconnectAttempt = 0
    }

    fun onConnected() {
        state.update { it.copy(connectionStatus = ConnectionStatus.Connected) }
    }

    fun onDisconnected() {
        // Preserve auth status — the user is still authenticated, just disconnected.
        // Auto-reconnect will restore the session transparently.
        state.update {
            it.copy(
                connectionStatus = ConnectionStatus.Reconnecting,
                joined = false,
                // Keep authStatus as-is — do NOT reset to NotAuthenticated
            )
        }
        scheduleAutoReconnect()
    }

    fun onFailure(message: String) {
        state.update {
            it.copy(
                connectionStatus = ConnectionStatus.Error(message),
                joined = false,
                errorMessage = message,
                // Keep authStatus — transient failures shouldn't force re-login
            )
        }
        scheduleAutoReconnect()
    }

    fun onReconnected() {
        state.update {
            it.copy(connectionStatus = ConnectionStatus.Connected, errorMessage = null)
        }
    }

    private fun scheduleAutoReconnect() {
        reconnectJob?.cancel()
        val wsUrl = state.value.serverUrl.toWsUrl()
        if (wsUrl.isEmpty()) return

        reconnectAttempt++
        val delayMs = minOf(1000L * (1 shl minOf(reconnectAttempt, 5)), 30_000L)

        state.update { it.copy(connectionStatus = ConnectionStatus.Reconnecting) }
        reconnectJob = scope.launch {
            delay(delayMs)
            repository.connect(wsUrl)
        }
    }

    fun dismissError() {
        state.update { it.copy(errorMessage = null) }
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
