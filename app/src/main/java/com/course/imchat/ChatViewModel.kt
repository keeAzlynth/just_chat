package com.course.imchat

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.course.imchat.core.delegate.*
import com.course.imchat.data.*
import com.course.imchat.data.cache.AppCache
import com.course.imchat.data.cache.MessageCache
import com.course.imchat.data.cache.SessionCache
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: MessageRepository = MessageRepository(),
    private val messageCache: MessageCache? = null,
    private val sessionCache: SessionCache? = null,
    private val appContext: Context? = null,
) : ViewModel() {

    // ── State flows ────────────────────────────────────────
    private val _uiState = MutableStateFlow(ChatUiState(
        serverUrl = sessionCache?.serverUrl ?: "",
        nickname = sessionCache?.nickname ?: "",
        isDarkMode = sessionCache?.isDarkMode ?: true,
    ))
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    /** High-performance message list — SnapshotStateList gives O(1) adds without full-list copy */
    val messages: SnapshotStateList<ChatMessage> = mutableStateListOf()

    // ── Delegates ─────────────────────────────────────────
    val connection = ConnectionDelegate(_uiState, repository, viewModelScope)
    val auth = AuthDelegate(_uiState, repository)
    val message = MessageDelegate(_uiState, messages, repository, viewModelScope, appContext)
    val group = GroupDelegate(_uiState, repository)
    val pin = PinDelegate(_uiState, repository)

    init {
        viewModelScope.launch {
            repository.events.collect { event -> reduce(event) }
        }
        messageCache?.let { cache ->
            viewModelScope.launch {
                cache.unreadCounts.collect { counts ->
                    val total = counts.values.sumOf { it }
                    val mapped = counts.mapValues { (chatId, count) ->
                        UnreadCount(chatId = chatId, count = count)
                    }
                    _uiState.update { it.copy(unreadCounts = mapped, totalUnreadCount = total) }
                }
            }
        }
    }

    // ── Event dispatch ────────────────────────────────────
    private fun reduce(event: IncomingEvent) {
        when (event) {
            is IncomingEvent.Connected -> {
                connection.onReconnected()
                val token = sessionCache?.sessionToken ?: ""
                if (token.isNotEmpty() && uiState.value.authStatus !is AuthStatus.Authenticated) {
                    repository.resumeSession(token)
                }
            }
            is IncomingEvent.Disconnected -> connection.onDisconnected()
            is IncomingEvent.Failure -> connection.onFailure(event.message)

            is IncomingEvent.LoggedIn -> {
                sessionCache?.saveSession(
                    url = uiState.value.serverUrl,
                    token = event.sessionToken,
                    user = event.username,
                    nick = event.nickname,
                )
                auth.onLoggedIn(event.userId)
                _uiState.update { it.copy(nickname = event.nickname, errorMessage = null) }
                join()
            }
            is IncomingEvent.ServerError -> {
                if (event.code == "INVALID_CREDENTIALS") {
                    sessionCache?.clearSession()
                    _uiState.update {
                        it.copy(errorMessage = event.message, authStatus = AuthStatus.NotAuthenticated)
                    }
                } else {
                    _uiState.update { it.copy(errorMessage = event.message) }
                }
            }
            is IncomingEvent.Joined -> {
                _uiState.update { it.copy(joined = true, errorMessage = null) }
                repository.getMessageHistory(50)
            }
            is IncomingEvent.Message -> message.handleIncomingPublicMessage(
                event.userId, event.nickname, event.text, event.timestampSeconds,
                event.fileUrl, event.fileName, event.fileSize,
            )
            is IncomingEvent.PrivateMessage -> message.handleIncomingPrivateMessage(
                event.userId, event.nickname, event.text, event.timestampSeconds,
            )
            is IncomingEvent.GroupMessage -> message.handleIncomingGroupMessage(
                event.groupId, event.userId, event.nickname, event.text, event.timestampSeconds,
            )
            is IncomingEvent.Typing -> message.handleTyping(event.userId, event.nickname)

            is IncomingEvent.MessageRecalled -> {
                val idx = messages.indexOfFirst { it.id == event.messageId.toString() }
                if (idx >= 0) messages[idx] = messages[idx].copy(isRecalled = true)
            }
            is IncomingEvent.MessageRead -> {
                val idx = messages.indexOfFirst { it.id == event.messageId.toString() }
                if (idx >= 0) messages[idx] = messages[idx].copy(
                    readBy = messages[idx].readBy + event.userId,
                    deliveryStatus = DeliveryStatus.Read,
                )
            }
            is IncomingEvent.MessageEdited -> {
                val idx = messages.indexOfFirst { it.id == event.messageId.toString() }
                if (idx >= 0) messages[idx] = messages[idx].copy(text = event.text)
            }
            is IncomingEvent.MessageDeleted -> {
                messages.removeAll { it.id == event.messageId.toString() }
            }
            is IncomingEvent.OnlineUsers -> {
                val selectedId = _uiState.value.selectedPrivateUser?.userId
                val found = event.users.find { it.userId == selectedId }
                _uiState.update {
                    it.copy(
                        onlineUsers = event.users,
                        selectedUserOnline = found != null,
                        selectedUserLastSeen = if (found == null) 0 else System.currentTimeMillis() / 1000,
                    )
                }
                AppCache.cacheUsers(event.users)
            }
            is IncomingEvent.MessageHistory -> {
                val msgs = event.messages.map { it.toChatMessage(true) }
                if (messages.isEmpty()) messages.addAll(msgs)
                AppCache.cacheMessages("public", msgs)
            }
            is IncomingEvent.PrivateMessageHistory -> {
                val msgs = event.messages.map { it.toChatMessage(true) }
                if (messages.isEmpty()) messages.addAll(msgs)
                AppCache.cacheMessages("private_${event.otherUserId}", msgs)
            }
            is IncomingEvent.GroupMessageHistory -> {
                val msgs = event.messages.map { it.toChatMessage(true) }
                if (messages.isEmpty()) messages.addAll(msgs)
                AppCache.cacheMessages("group_${event.groupId}", msgs)
                _uiState.update { it.copy(isLoadingMore = false, hasMoreMessages = event.messages.size >= 50) }
            }
            is IncomingEvent.GroupCreated -> group.onGroupCreated(event.groupId, event.name, event.ownerId).also {
                AppCache.cacheGroups(uiState.value.groups)
            }
            is IncomingEvent.GroupMembers -> group.onGroupMembers(event.groupId, event.members)
            is IncomingEvent.GroupMemberJoined -> group.onGroupMemberJoined(event.groupId, event.userId, event.nickname)

            else -> {}
        }
    }

    // ── Convenience methods ───────────────────────────────
    fun connect() {
        val wsUrl = uiState.value.serverUrl.toWsUrl()
        if (wsUrl.isEmpty()) return
        sessionCache?.serverUrl = uiState.value.serverUrl
        connection.connect(wsUrl)
    }
    fun reconnect() = connection.reconnect()
    fun dismissError() = connection.dismissError()
    fun toggleAuthMode() = auth.toggleMode()
    fun login() = auth.login()
    fun register() = auth.register()
    fun logout() { sessionCache?.clearSession(); auth.logout() }
    fun onServerUrlChange(url: String) = auth.onServerUrlChange(url)
    fun onUsernameChange(name: String) = auth.onUsernameChange(name)
    fun onPasswordChange(pw: String) = auth.onPasswordChange(pw)
    fun onNicknameChange(name: String) = auth.onNicknameChange(name)
    fun sendMessage() = message.sendMessage()
    fun sendImage(uri: Uri, name: String, size: Long = 0) = message.sendImage(uri, name, size)
    fun sendFile(uri: Uri, name: String, size: Long) = message.sendFile(uri, name, size)
    fun quoteMessage(msg: ChatMessage) = message.quoteMessage(msg)
    fun cancelQuote() = message.cancelQuote()
    fun startEditMessage(msg: ChatMessage) = message.startEdit(msg)
    fun cancelEditMessage() = message.cancelEdit()
    fun saveEditMessage() {}
    fun recallMessage(id: String) = message.recallMessage(id)
    fun deleteMessage(id: String) = message.deleteMessage(id)
    fun deleteSelectedMessages() = message.deleteSelectedMessages()
    fun startForwardMessage(msg: ChatMessage) = message.startForward(msg)
    fun cancelForward() = message.cancelForward()
    fun forwardToUser(userId: String) = message.forwardToUser(userId)
    fun forwardSelectedMessages() = message.forwardSelectedMessages()
    fun toggleMultiSelectMode() = message.toggleMultiSelect()
    fun toggleMessageSelection(id: String) = message.toggleMessageSelection(id)
    fun selectAllMessages() = message.selectAllMessages()
    fun toggleSearch() = message.toggleSearch()
    fun onSearchQueryChange(q: String) = message.onSearchQueryChange(q)
    fun onDraftChange(text: String) = message.onDraftChange(text)
    fun addReaction(msgId: String, emoji: String) = message.addReaction(msgId, emoji)
    fun showReactionPicker(msgId: String) = message.showReactionPicker(msgId)
    fun dismissReactionPicker() = message.dismissReactionPicker()
    fun toggleAttachmentSheet() { _uiState.update { it.copy(showAttachmentSheet = !it.showAttachmentSheet) } }
    fun createGroup(name: String) = group.createGroup(name)
    fun selectGroup(grp: ChatGroup?) {
        group.selectGroup(grp)
        messages.clear()
        _uiState.update { it.copy(isLoadingMore = false, hasMoreMessages = true) }
        if (grp != null) {
            // Try cache first
            val cached = AppCache.getCachedMessages("group_${grp.groupId}")
            if (!cached.isNullOrEmpty()) {
                messages.addAll(cached)
            }
            repository.getGroupMessageHistory(grp.groupId, 50)
            // Load group members for @mention
            viewModelScope.launch {
                kotlinx.coroutines.delay(300)
                repository.getGroupMembers(grp.groupId)
            }
        }
    }
    fun selectPrivateUser(user: OnlineUser?) {
        group.selectPrivateUser(user)
        messages.clear()
        _uiState.update { it.copy(isLoadingMore = false, hasMoreMessages = true) }
        if (user != null) {
            // Try cache first
            val cached = AppCache.getCachedMessages("private_${user.userId}")
            if (!cached.isNullOrEmpty()) {
                messages.addAll(cached)
            }
            repository.getPrivateMessageHistory(user.userId, 50)
        }
    }
    fun joinGroup(gid: String) = group.joinGroup(gid)
    fun leaveGroup(gid: String) = group.leaveGroup(gid)
    fun toggleGroupManagement() = group.toggleGroupManagement()
    fun showCreateGroupDialog() = group.showCreateGroupDialog()
    fun dismissCreateGroupDialog() = group.dismissCreateGroupDialog()
    // ── @Mention ──────────────────────────────────────────
    fun showMentionPicker() {
        val s = uiState.value
        val candidates = if (s.selectedGroup != null) {
            s.selectedGroup.members
        } else {
            s.onlineUsers
        }
        _uiState.update { it.copy(showMentionPicker = true, mentionCandidates = candidates) }
    }
    fun dismissMentionPicker() {
        _uiState.update { it.copy(showMentionPicker = false) }
    }
    fun selectMention(user: OnlineUser) {
        val mentionText = "@${user.nickname} "
        _uiState.update { it.copy(draft = it.draft + mentionText, showMentionPicker = false) }
    }
    // ── Voice Recording ───────────────────────────────────
    fun startVoiceRecording() {
        _uiState.update { it.copy(isVoiceRecording = true, recordingSeconds = 0f) }
    }
    fun stopVoiceRecording(cancelled: Boolean) {
        _uiState.update { it.copy(isVoiceRecording = false, recordingSeconds = 0f) }
        if (!cancelled) {
            // Placeholder: voice message would be sent here
            _uiState.update { it.copy(errorMessage = "语音消息功能开发中...") }
        }
    }
    fun updateRecordingTime(seconds: Float) {
        _uiState.update { it.copy(recordingSeconds = seconds) }
    }
    fun pinMessage(msg: ChatMessage) = pin.pinMessage(msg)
    fun unpinMessage(chatId: String) = pin.unpinMessage(chatId)
    fun unpinCurrentChat() = pin.unpinCurrentChat()
    fun saveMessage(msg: ChatMessage) = pin.saveMessage(msg)
    fun unsaveMessage(msg: ChatMessage) = pin.unsaveMessage(msg)
    fun toggleSavedMessages() = pin.toggleSavedMessages()
    fun join() { repository.join(uiState.value.nickname.trim()); repository.getOnlineUsers() }
    fun toggleOnlineUsers() {
        val s = uiState.value
        _uiState.update { it.copy(showOnlineUsers = !s.showOnlineUsers) }
        repository.getOnlineUsers()
    }
    fun toggleDarkMode() {
        val newMode = !uiState.value.isDarkMode
        sessionCache?.isDarkMode = newMode
        _uiState.update { it.copy(isDarkMode = newMode) }
    }
    fun loadMoreMessages() {
        val s = uiState.value
        if (s.isLoadingMore || !s.hasMoreMessages) return
        _uiState.update { it.copy(isLoadingMore = true) }
        val oldest = messages.firstOrNull()?.timestampSeconds
        if (oldest == null) {
            _uiState.update { it.copy(isLoadingMore = false) }
            return
        }
        val group = s.selectedGroup
        val privateUser = s.selectedPrivateUser
        when {
            group != null -> repository.getGroupMessageHistory(group.groupId, 50)
            privateUser != null -> repository.getPrivateMessageHistory(privateUser.userId, 50)
            else -> repository.getMessageHistory(50)
        }
    }
}

// ── Extensions ────────────────────────────────────────────
internal fun String.toWsUrl(): String {
    val t = trim()
    return when {
        t.startsWith("ws://") || t.startsWith("wss://") -> t
        else -> "ws://$t"
    }
}
internal fun MessageData.toChatMessage(isMine: Boolean): ChatMessage = ChatMessage(
    id = userId + "_" + timestampSeconds,
    userId = userId, nickname = nickname, text = text,
    timestampSeconds = timestampSeconds, isMine = isMine,
    messageType = when (type) { "image" -> MessageType.Image; "file" -> MessageType.File; else -> MessageType.Text },
    fileUrl = fileUrl, fileName = fileName, fileSize = fileSize,
)
internal fun IncomingEvent.Message.toMessageType() = when (messageType) {
    "image" -> MessageType.Image; "file" -> MessageType.File; else -> MessageType.Text
}
