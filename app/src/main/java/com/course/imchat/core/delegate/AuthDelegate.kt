package com.course.imchat.core.delegate

import com.course.imchat.AuthStatus
import com.course.imchat.ChatUiState
import com.course.imchat.data.MessageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * Handles authentication: login, register, logout.
 */
class AuthDelegate(
    private val state: MutableStateFlow<ChatUiState>,
    private val repository: MessageRepository,
) {
    fun toggleMode() {
        state.update { it.copy(isLoginMode = !it.isLoginMode) }
    }

    fun login() {
        val s = state.value
        val username = s.username.trim()
        val password = s.password.trim()
        if (username.isEmpty() || password.isEmpty()) return

        state.update { it.copy(authStatus = AuthStatus.Authenticating, errorMessage = null) }
        repository.login(username, password)
    }

    fun register() {
        val s = state.value
        val username = s.username.trim()
        val password = s.password.trim()
        val nickname = s.nickname.trim()
        if (username.isEmpty() || password.isEmpty() || nickname.isEmpty()) return

        state.update { it.copy(authStatus = AuthStatus.Authenticating, errorMessage = null) }
        repository.register(username, password, nickname)
    }

    fun logout() {
        repository.logout()
        state.update {
            it.copy(
                authStatus = AuthStatus.NotAuthenticated,
                joined = false,
                myUserId = null,
                onlineUsers = emptyList(),
            )
        }
    }

    fun onLoggedIn(userId: String) {
        state.update {
            it.copy(
                authStatus = AuthStatus.Authenticated,
                myUserId = userId,
                errorMessage = null,
            )
        }
    }

    fun onAuthError(message: String) {
        state.update {
            it.copy(
                authStatus = AuthStatus.Error(message),
                errorMessage = message,
            )
        }
    }

    fun onServerUrlChange(url: String) {
        state.update { it.copy(serverUrl = url) }
    }

    fun onUsernameChange(name: String) {
        state.update { it.copy(username = name.take(20)) }
    }

    fun onPasswordChange(pw: String) {
        state.update { it.copy(password = pw.take(50)) }
    }

    fun onNicknameChange(name: String) {
        state.update { it.copy(nickname = name.take(20)) }
    }
}
