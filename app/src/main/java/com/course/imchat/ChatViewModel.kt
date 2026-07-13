package com.course.imchat

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.course.imchat.feature.auth.AuthDelegate
import com.course.imchat.feature.chat.ConnectionDelegate
import com.course.imchat.feature.chat.MessageDelegate
import com.course.imchat.feature.groups.GroupDelegate
import com.course.imchat.feature.saved.PinDelegate
import com.course.imchat.data.*
import com.course.imchat.data.cache.AppCache
import com.course.imchat.data.cache.ChatCacheKey
import com.course.imchat.data.cache.MessageCache
import com.course.imchat.data.cache.PersistentCache
import com.course.imchat.data.cache.SessionCache
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    // ── Infrastructure ─────────────────────────────────────
    private val appContext: Context = application.applicationContext
    private val sessionCache = SessionCache(appContext)
    private val messageCache = MessageCache(appContext)

    // ── Persistent cache init ──────────────────────────────
    init {
        PersistentCache.ensureInit(appContext)
    }

    // ── State flows ────────────────────────────────────────
    private val _uiState = MutableStateFlow(ChatUiState(
        auth = AuthState(
            serverUrl = sessionCache.serverUrl,
            nickname = sessionCache.nickname,
            username = sessionCache.username,
        ),
        ui = UiState(isDarkMode = sessionCache.isDarkMode),
    ))
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    /** High-performance message list (Compose snapshot-backed) */
    val messages: SnapshotStateList<ChatMessage> = mutableStateListOf()

    // ── Repository (must precede delegates) ─────────────────
    private val repository = MessageRepository()

    // ── Delegates — each scoped to one domain ───────────────
    val connection = ConnectionDelegate(_uiState, repository, viewModelScope)
    val auth = AuthDelegate(_uiState, repository)
    val message = MessageDelegate(_uiState, messages, repository, viewModelScope, appContext)
    val group = GroupDelegate(_uiState, repository)
    val pin = PinDelegate(_uiState, repository)

    // ── Startup ────────────────────────────────────────────
    init {
        val token = sessionCache.sessionToken
        val url = sessionCache.serverUrl
        if (token.isNotEmpty() && url.isNotEmpty()) {
            connection.connect(url.toWsUrl())
        }

        // Event pipeline — with error recovery
        viewModelScope.launch {
            try {
                repository.events.collect { event -> reduce(event) }
            } catch (e: Exception) {
                _uiState.update { it.copy(auth = it.auth.copy(
                    errorMessage = "Event pipeline crashed: ${e.localizedMessage}")) }
                // Re-launch the collector
                restartEventCollector()
            }
        }

        // Unread counts
        viewModelScope.launch {
            try {
                messageCache.unreadCounts.collect { counts ->
                    val total = counts.values.sumOf { it }
                    val mapped = counts.mapValues { (chatId, count) ->
                        UnreadCount(chatId = chatId, count = count)
                    }
                    _uiState.update { it.copy(chat = it.chat.copy(unreadCounts = mapped, totalUnreadCount = total)) }
                }
            } catch (_: Exception) {
                // Unread counter is best-effort
            }
        }
    }

    /** Restart the event collector if it dies. */
    private fun restartEventCollector() {
        viewModelScope.launch {
            try {
                repository.events.collect { event -> reduce(event) }
            } catch (_: Exception) { /* Give up after one retry */ }
        }
    }

    // ── Event dispatch (single reduce point) ────────────────
    private fun reduce(event: IncomingEvent) {
        try {
            when (event) {
            is IncomingEvent.Connected -> {
                connection.onReconnected()
                // Always resume session on (re)connect — server needs to
                // associate this WebSocket with the existing user account.
                val token = sessionCache.sessionToken
                if (token.isNotEmpty()) {
                    repository.resumeSession(token)
                }
            }
                is IncomingEvent.Disconnected -> connection.onDisconnected()
                is IncomingEvent.Failure -> connection.onFailure(event.message)

                is IncomingEvent.LoggedIn -> {
                    sessionCache.saveSession(
                        url = uiState.value.serverUrl,
                        token = event.sessionToken,
                        user = event.username,
                        nick = event.nickname,
                    )
                    auth.onLoggedIn(event.userId)
                    _uiState.update { it.copy(auth = it.auth.copy(nickname = event.nickname, errorMessage = null)) }
                    join()
                }
                is IncomingEvent.ServerError -> {
                    if (event.code == "INVALID_CREDENTIALS") {
                        sessionCache.clearSession()
                        _uiState.update {
                            it.copy(auth = it.auth.copy(errorMessage = event.message, authStatus = AuthStatus.NotAuthenticated))
                        }
                    } else {
                        _uiState.update { it.copy(auth = it.auth.copy(errorMessage = event.message)) }
                    }
                }
                is IncomingEvent.Joined -> {
                    _uiState.update { it.copy(chat = it.chat.copy(joined = true), auth = it.auth.copy(errorMessage = null)) }
                    repository.getMessageHistory(50)
                }
                is IncomingEvent.Message -> message.handleIncomingPublicMessage(
                    event.userId, event.nickname, event.text, event.timestampSeconds,
                    event.fileUrl, event.fileName, event.fileSize, event.messageId,
                )
                is IncomingEvent.PrivateMessage -> message.handleIncomingPrivateMessage(
                    event.userId, event.nickname, event.text, event.timestampSeconds, event.messageId,
                )
                is IncomingEvent.GroupMessage -> message.handleIncomingGroupMessage(
                    event.groupId, event.userId, event.nickname, event.text, event.timestampSeconds, event.messageId,
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
                        it.copy(chat = it.chat.copy(
                            onlineUsers = event.users.associateBy { u -> u.userId },
                            selectedUserOnline = found != null,
                            selectedUserLastSeen = if (found == null) 0 else System.currentTimeMillis() / 1000,
                        ))
                    }
                    AppCache.cacheUsers(event.users)
                }
                is IncomingEvent.MessageHistory -> {
                    val msgs = event.messages.map { it.toChatMessage(true) }
                    if (messages.isEmpty()) messages.addAll(msgs)
                    AppCache.cacheMessages(ChatCacheKey.PUBLIC, msgs)
                    PersistentCache.saveMessages(ChatCacheKey.PUBLIC, messages.takeLast(50))
                }
                is IncomingEvent.PrivateMessageHistory -> {
                    val msgs = event.messages.map { it.toChatMessage(true) }
                    if (messages.isEmpty()) messages.addAll(msgs)
                    AppCache.cacheMessages(ChatCacheKey.privateChat(event.otherUserId), msgs)
                    PersistentCache.saveMessages(ChatCacheKey.privateChat(event.otherUserId), messages.takeLast(50))
                }
                is IncomingEvent.GroupMessageHistory -> {
                    val msgs = event.messages.map { it.toChatMessage(true) }
                    if (messages.isEmpty()) messages.addAll(msgs)
                    AppCache.cacheMessages(ChatCacheKey.groupChat(event.groupId), msgs)
                    PersistentCache.saveMessages(ChatCacheKey.groupChat(event.groupId), messages.takeLast(50))
                    _uiState.update { it.copy(chat = it.chat.copy(isLoadingMore = false, hasMoreMessages = event.messages.size >= 50)) }
                }
                is IncomingEvent.GroupCreated -> group.onGroupCreated(event.groupId, event.name, event.ownerId).also {
                    AppCache.cacheGroups(uiState.value.groups)
                }
                is IncomingEvent.GroupMembers -> group.onGroupMembers(event.groupId, event.members)
                is IncomingEvent.GroupMemberJoined -> group.onGroupMemberJoined(event.groupId, event.userId, event.nickname)

                else -> {}
            }
        } catch (e: Exception) {
            // Per-event error: log and continue (do NOT crash the collector)
            _uiState.update { it.copy(auth = it.auth.copy(errorMessage = "Event error: ${e.localizedMessage?.take(80)}")) }
        }
    }

    // ── Convenience methods (View→Delegate bridge) ──────────
    fun connect() {
        val wsUrl = uiState.value.serverUrl.toWsUrl()
        if (wsUrl.isEmpty()) return
        sessionCache.serverUrl = uiState.value.serverUrl
        connection.connect(wsUrl)
    }

    /**
     * Called when app returns to foreground.
     * If authenticated but disconnected, auto-reconnect silently.
     */
    fun onAppForeground() {
        val s = uiState.value
        val isAuth = s.authStatus is AuthStatus.Authenticated
        val isDisconnected = s.connectionStatus is ConnectionStatus.Disconnected
                || s.connectionStatus is ConnectionStatus.Error
                || s.connectionStatus is ConnectionStatus.Idle
        if (isAuth && isDisconnected) {
            val wsUrl = s.serverUrl.toWsUrl()
            if (wsUrl.isNotEmpty()) {
                connection.connect(wsUrl)
            }
        }
    }

    /**
     * Called when app enters background.
     * Keep the connection alive — the OkHttp WebSocket survives
     * background via its pingInterval keepalive.
     */
    fun onAppBackground() {
        // Intentionally no-op: WebSocket stays open with ping keepalive.
        // OkHttp's pingInterval(25s) sends pings even in background,
        // preventing the server's 120s idle timeout from killing the connection.
    }

    fun reconnect() = connection.reconnect()
    fun dismissError() = connection.dismissError()
    fun toggleAuthMode() = auth.toggleMode()
    fun login() { sessionCache.username = uiState.value.username; auth.login() }
    fun register() { sessionCache.username = uiState.value.username; auth.register() }
    fun logout() { sessionCache.clearSession(); auth.logout() }
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
    fun onDraftChange(text: String) { message.onDraftChange(text); saveDraftForCurrentChat(text) }
    private fun saveDraftForCurrentChat(text: String) {
        PersistentCache.saveDraft(ChatCacheKey.forChat(
            _uiState.value.selectedGroup?.groupId, _uiState.value.selectedPrivateUser?.userId), text)
    }
    fun addReaction(msgId: String, emoji: String) = message.addReaction(msgId, emoji)
    fun showReactionPicker(msgId: String) = message.showReactionPicker(msgId)
    fun dismissReactionPicker() = message.dismissReactionPicker()
    fun toggleAttachmentSheet() { _uiState.update { it.copy(ui = it.ui.copy(showAttachmentSheet = !it.ui.showAttachmentSheet)) } }
    fun createGroup(name: String) = group.createGroup(name)
    fun selectGroup(grp: ChatGroup?) {
        saveCurrentMessagesToCache()
        group.selectGroup(grp)
        messages.clear()
        _uiState.update { it.copy(chat = it.chat.copy(isLoadingMore = false, hasMoreMessages = true, draft = "")) }
        if (grp != null) {
            restoreMessages(ChatCacheKey.groupChat(grp.groupId))
            repository.getGroupMessageHistory(grp.groupId, 50)
            viewModelScope.launch { repository.getGroupMembers(grp.groupId) }
        } else {
            restoreMessages(ChatCacheKey.PUBLIC)
            if (messages.isEmpty()) repository.getMessageHistory(50)
        }
    }
    fun selectPrivateUser(user: OnlineUser?) {
        saveCurrentMessagesToCache()
        group.selectPrivateUser(user)
        messages.clear()
        _uiState.update { it.copy(chat = it.chat.copy(isLoadingMore = false, hasMoreMessages = true, draft = "")) }
        if (user != null) {
            restoreMessages(ChatCacheKey.privateChat(user.userId))
            if (messages.isEmpty()) repository.getPrivateMessageHistory(user.userId, 50)
        } else {
            restoreMessages(ChatCacheKey.PUBLIC)
            if (messages.isEmpty()) repository.getMessageHistory(50)
        }
    }
    private fun saveCurrentMessagesToCache() {
        if (messages.isEmpty()) return
        PersistentCache.saveMessages(currentChatKey(), messages.takeLast(50))
    }
    private fun restoreMessages(key: String) {
        val draft = PersistentCache.loadDraft(key)
        if (!draft.isNullOrBlank()) _uiState.update { it.copy(chat = it.chat.copy(draft = draft)) }
        val diskMsgs = PersistentCache.loadMessages(key)
        if (!diskMsgs.isNullOrEmpty()) { messages.addAll(diskMsgs); return }
        AppCache.getCachedMessages(key)?.let { messages.addAll(it) }
    }
    private fun currentChatKey() = when {
        _uiState.value.selectedGroup != null -> ChatCacheKey.groupChat(_uiState.value.selectedGroup!!.groupId)
        _uiState.value.selectedPrivateUser != null -> ChatCacheKey.privateChat(_uiState.value.selectedPrivateUser!!.userId)
        else -> ChatCacheKey.PUBLIC
    }
    fun joinGroup(gid: String) = group.joinGroup(gid)
    fun leaveGroup(gid: String) = group.leaveGroup(gid)
    fun toggleGroupManagement() = group.toggleGroupManagement()
    fun showCreateGroupDialog() = group.showCreateGroupDialog()
    fun dismissCreateGroupDialog() = group.dismissCreateGroupDialog()
    fun showMentionPicker() {
        val s = uiState.value
        val group = s.nav.selectedGroup
        val candidates = group?.members ?: s.onlineUsers.values.toList()
        _uiState.update { it.copy(ui = it.ui.copy(showMentionPicker = true, mentionCandidates = candidates)) }
    }
    fun dismissMentionPicker() { _uiState.update { it.copy(ui = it.ui.copy(showMentionPicker = false)) } }
    fun selectMention(user: OnlineUser) {
        _uiState.update { it.copy(
            chat = it.chat.copy(draft = it.chat.draft + "@${user.nickname} "),
            ui = it.ui.copy(showMentionPicker = false)) }
    }
    fun showCreatePollDialog() = _uiState.update { it.copy(poll = it.poll.copy(showCreatePollDialog = true, pollTitle = "", pollOptions = "")) }
    fun dismissCreatePollDialog() = _uiState.update { it.copy(poll = it.poll.copy(showCreatePollDialog = false)) }
    fun onPollTitleChange(t: String) = _uiState.update { it.copy(poll = it.poll.copy(pollTitle = t)) }
    fun onPollOptionsChange(o: String) = _uiState.update { it.copy(poll = it.poll.copy(pollOptions = o)) }
    fun createPoll() {
        val s = _uiState.value
        val title = s.poll.pollTitle.trim()
        val options = s.poll.pollOptions.split("\n").filter { it.isNotBlank() }
        if (title.isEmpty() || options.size < 2) return
        _uiState.update { it.copy(
            chat = it.chat.copy(draft = "\uD83D\uDCCA $title\n" + options.joinToString("\n")),
            poll = it.poll.copy(showCreatePollDialog = false, pollTitle = "", pollOptions = "")) }
    }
    fun votePoll(messageId: String, optionId: String) {
        val myId = _uiState.value.myUserId ?: return
        _uiState.update { s ->
            val poll = s.poll.activePolls[messageId] ?: PollData()
            val newVotes = poll.votes.toMutableMap()
            val existing = newVotes[optionId]?.toMutableSet() ?: mutableSetOf()
            if (existing.contains(myId)) { existing.remove(myId); if (existing.isEmpty()) newVotes.remove(optionId) }
            else existing.add(myId).also { newVotes[optionId] = existing }
            s.copy(poll = s.poll.copy(activePolls = s.poll.activePolls + (messageId to PollData(newVotes))))
        }
    }
    fun startVoiceRecording() { _uiState.update { it.copy(ui = it.ui.copy(isVoiceRecording = true, recordingSeconds = 0f)) } }
    fun stopVoiceRecording(cancelled: Boolean) {
        _uiState.update { it.copy(ui = it.ui.copy(isVoiceRecording = false, recordingSeconds = 0f)) }
        if (!cancelled) _uiState.update { it.copy(auth = it.auth.copy(errorMessage = "Voice message not yet supported")) }
    }
    fun updateRecordingTime(seconds: Float) { _uiState.update { it.copy(ui = it.ui.copy(recordingSeconds = seconds)) } }
    fun pinMessage(msg: ChatMessage) = pin.pinMessage(msg)
    fun unpinMessage(chatId: String) = pin.unpinMessage(chatId)
    fun unpinCurrentChat() = pin.unpinCurrentChat()
    fun saveMessage(msg: ChatMessage) = pin.saveMessage(msg)
    fun unsaveMessage(msg: ChatMessage) = pin.unsaveMessage(msg)
    fun toggleSavedMessages() = pin.toggleSavedMessages()
    fun join() { repository.join(uiState.value.nickname.trim()); repository.getOnlineUsers() }
    fun toggleOnlineUsers() {
        _uiState.update { it.copy(ui = it.ui.copy(showOnlineUsers = !it.ui.showOnlineUsers)) }
        repository.getOnlineUsers()
    }
    fun toggleDarkMode() {
        val newMode = !uiState.value.isDarkMode
        sessionCache.isDarkMode = newMode
        _uiState.update { it.copy(ui = it.ui.copy(isDarkMode = newMode)) }
    }
    fun toggleScreenshotProtection() {
        _uiState.update { it.copy(ui = it.ui.copy(screenshotProtection = !it.ui.screenshotProtection)) }
    }
    fun loadMoreMessages() {
        val s = uiState.value
        if (s.isLoadingMore || !s.hasMoreMessages) return
        _uiState.update { it.copy(chat = it.chat.copy(isLoadingMore = true)) }
        if (messages.isEmpty()) { _uiState.update { it.copy(chat = it.chat.copy(isLoadingMore = false)) }; return }
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
