package com.course.imchat.feature.chat

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.course.imchat.*
import com.course.imchat.core.notification.NotificationHelper
import com.course.imchat.data.MessageDeduplicator
import com.course.imchat.data.MessageRepository
import com.course.imchat.data.SmartReplyProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Handles message sending, editing, recall, delete, forward, multi-select, search.
 * v2.1: Moved to feature/chat module.
 */
class MessageDelegate(
    private val state: MutableStateFlow<ChatUiState>,
    private val messages: SnapshotStateList<ChatMessage>,
    private val repository: MessageRepository,
    private val scope: CoroutineScope,
    private val appContext: Context?,
) {
    private val composer = MessageComposer(repository)
    private val dedup = MessageDeduplicator(500)
    private var typingCleanupJob: Job? = null
    private var lastTypingSentMs = 0L

    private fun composeContext() = MessageComposer.Context(
        myUserId = state.value.auth.myUserId.orEmpty(),
        nickname = state.value.auth.nickname.trim(),
        quotingMessage = state.value.chat.quotingMessage,
    )

    // ── Send ───────────────────────────────────────────
    fun sendMessage() {
        val text = state.value.chat.draft.trim()
        if (text.isEmpty() || !state.value.canSend) return
        val s = state.value
        if (s.chat.editingMessage != null) { saveEditMessage(s.chat.editingMessage.id, text); return }

        val result = composer.composeAndSendText(
            composeContext(), text,
            groupId = s.nav.selectedGroup?.groupId,
            receiverId = s.chat.selectedPrivateUser?.userId,
        )
        messages.add(result.localMessage)
        state.update {
            it.copy(
                chat = it.chat.copy(draft = "", quotingMessage = null),
                ui = it.ui.copy(smartSuggestions = emptyList()),
            )
        }
        triggerHaptic()
    }

    private fun triggerHaptic() {
        val ctx = appContext ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        } catch (_: Exception) {}
    }

    fun sendImage(uri: Uri, fileName: String, fileSize: Long) {
        if (!state.value.chat.joined) return
        val result = composer.composeAndSendImage(composeContext(), uri, fileName, fileSize)
        messages.add(result.localMessage)
        state.update { it.copy(ui = it.ui.copy(showAttachmentSheet = false)) }
    }

    fun sendFile(uri: Uri, fileName: String, fileSize: Long) {
        if (!state.value.chat.joined) return
        val result = composer.composeAndSendFile(composeContext(), uri, fileName, fileSize)
        messages.add(result.localMessage)
        state.update { it.copy(ui = it.ui.copy(showAttachmentSheet = false)) }
    }

    // ── Edit ──────────────────────────────────────────
    fun quoteMessage(msg: ChatMessage) = state.update { it.copy(chat = it.chat.copy(quotingMessage = msg)) }
    fun cancelQuote() = state.update { it.copy(chat = it.chat.copy(quotingMessage = null)) }
    fun startEdit(msg: ChatMessage) = state.update { it.copy(chat = it.chat.copy(editingMessage = msg, draft = msg.text)) }
    fun cancelEdit() = state.update { it.copy(chat = it.chat.copy(editingMessage = null, draft = "")) }
    fun saveEditMessage(messageId: String, newText: String) {
        val clean = newText.trim(); if (clean.isEmpty()) return
        val idx = messages.indexOfFirst { it.id == messageId }
        if (idx >= 0) messages[idx] = messages[idx].copy(text = clean)
        state.update { it.copy(chat = it.chat.copy(editingMessage = null, draft = "")) }
        repository.editMessage(messageId, clean)
    }

    // ── Recall / Delete ───────────────────────────────
    fun recallMessage(messageId: String) = repository.recallMessage(messageId)
    fun deleteMessage(messageId: String) {
        messages.removeAll { it.id == messageId }
        repository.deleteMessage(messageId)
    }
    fun deleteSelectedMessages() {
        val s = state.value
        s.ui.selectedMessages.forEach { id -> if (!id.startsWith("local_")) repository.deleteMessage(id) }
        messages.removeAll { it.id in s.ui.selectedMessages }
        state.update { it.copy(ui = it.ui.copy(selectedMessages = emptySet(), isMultiSelectMode = false)) }
    }

    // ── Forward ───────────────────────────────────────
    fun startForward(msg: ChatMessage) = state.update { it.copy(ui = it.ui.copy(forwardingMessage = msg, showForwardDialog = true)) }
    fun cancelForward() = state.update { it.copy(ui = it.ui.copy(forwardingMessage = null, showForwardDialog = false)) }
    fun forwardToUser(targetUserId: String) {
        val msg = state.value.ui.forwardingMessage ?: return
        repository.sendPrivateMessage("\u21A9 ${msg.nickname}: ${msg.text.take(80)}", targetUserId)
        state.update { it.copy(ui = it.ui.copy(forwardingMessage = null, showForwardDialog = false)) }
    }
    fun forwardSelectedMessages() {
        val s = state.value
        val selected = messages.filter { it.id in s.ui.selectedMessages }
        if (selected.isEmpty()) return
        state.update {
            it.copy(ui = it.ui.copy(
                forwardingMessage = ChatMessage(
                    id = "fwd_${java.util.UUID.randomUUID()}", userId = s.auth.myUserId.orEmpty(),
                    nickname = s.auth.nickname, text = selected.joinToString("\n") { "${it.nickname}: ${it.text.take(80)}" },
                    timestampSeconds = System.currentTimeMillis() / 1000, isMine = true,
                ),
                showForwardDialog = true, selectedMessages = emptySet(),
            ))
        }
    }

    // ── Multi-select ──────────────────────────────────
    fun toggleMultiSelect() {
        state.update {
            it.copy(ui = it.ui.copy(
                isMultiSelectMode = !it.ui.isMultiSelectMode,
                selectedMessages = if (it.ui.isMultiSelectMode) emptySet() else it.ui.selectedMessages,
            ))
        }
    }
    fun toggleMessageSelection(messageId: String) {
        state.update { s ->
            val u = s.ui.selectedMessages.toMutableSet().apply {
                if (messageId in this) remove(messageId) else add(messageId)
            }
            s.copy(ui = s.ui.copy(selectedMessages = u))
        }
    }
    fun selectAllMessages() = state.update { s -> s.copy(ui = s.ui.copy(selectedMessages = messages.map { it.id }.toSet())) }

    // ── Search ────────────────────────────────────────
    fun toggleSearch() = state.update { it.copy(ui = it.ui.copy(isSearching = !it.ui.isSearching, searchQuery = "", searchResults = emptyList())) }
    fun onSearchQueryChange(query: String) {
        val results = if (query.isBlank()) emptyList()
        else messages.filter { it.text.contains(query, ignoreCase = true) }.takeLast(50)
        state.update { it.copy(ui = it.ui.copy(searchQuery = query, searchResults = results)) }
    }

    // ── Smart reply ───────────────────────────────────
    fun computeSmartSuggestions() {
        val suggestions = SmartReplyProvider.getSuggestions(messages.takeLast(5), state.value.auth.myUserId)
        state.update { it.copy(ui = it.ui.copy(smartSuggestions = suggestions)) }
    }

    // ── Incoming ──────────────────────────────────────
    fun handleIncomingPublicMessage(
        userId: String, nickname: String, text: String,
        timestamp: Long, fileUrl: String, fileName: String, fileSize: Long,
        serverMessageId: Long = 0,
    ) {
        val myId = state.value.auth.myUserId
        if (userId == myId && myId != null) {
            markDelivery(text, timestamp, serverMessageId)
            return
        }
        val msgId = if (serverMessageId > 0) serverMessageId.toString() else "$userId|$timestamp"
        if (dedup.isDuplicate("$msgId|${text.take(40)}")) return
        messages.add(buildReceivedMessage(userId, nickname, text, timestamp, fileUrl, fileName, fileSize, msgId))
        notifyIfNeeded("public", "\u516C\u5171\u804A\u5929", nickname, text)
        computeSmartSuggestions()
    }

    fun handleIncomingPrivateMessage(userId: String, nickname: String, text: String, timestamp: Long, serverMessageId: Long = 0) {
        val myId = state.value.auth.myUserId
        if (userId == myId && myId != null) { markDelivery(text, timestamp, serverMessageId); return }
        val msgId = if (serverMessageId > 0) serverMessageId.toString() else "$userId|$timestamp"
        if (dedup.isDuplicate("pm|$msgId|${text.take(40)}")) return
        messages.add(ChatMessage(
            id = msgId, userId = userId, nickname = nickname, text = text,
            timestampSeconds = timestamp, isMine = false, isPrivate = true,
            receiverId = myId ?: "",
        ))
        notifyIfNeeded("private_$userId", nickname, nickname, text)
        computeSmartSuggestions()
    }

    fun handleIncomingGroupMessage(groupId: String, userId: String, nickname: String, text: String, timestamp: Long, serverMessageId: Long = 0) {
        val myId = state.value.auth.myUserId
        if (userId == myId && myId != null) { markDelivery(text, timestamp, serverMessageId); return }
        val msgId = if (serverMessageId > 0) serverMessageId.toString() else "$userId|$timestamp"
        if (dedup.isDuplicate("grp|$groupId|$msgId|${text.take(40)}")) return
        messages.add(ChatMessage(
            id = msgId, userId = userId, nickname = nickname, text = text,
            timestampSeconds = timestamp, isMine = false, groupId = groupId
        ))
        notifyIfNeeded("group_$groupId", groupId, nickname, text)
        computeSmartSuggestions()
    }

    // ── Typing ────────────────────────────────────────
    fun sendTyping() {
        val now = System.currentTimeMillis()
        if (now - lastTypingSentMs < 3000L) return
        lastTypingSentMs = now; repository.sendTyping()
    }
    fun handleTyping(userId: String, nickname: String) {
        state.update { s -> if (userId == s.auth.myUserId) s else s.copy(chat = s.chat.copy(typingUsers = s.chat.typingUsers + nickname)) }
        scheduleTypingCleanup()
    }
    fun onDraftChange(text: String) {
        state.update { it.copy(chat = it.chat.copy(draft = text)) }
        if (text.isNotEmpty()) sendTyping()
    }

    // ── Reactions ─────────────────────────────────────
    fun addReaction(messageId: String, emoji: String) {
        val idx = messages.indexOfFirst { it.id == messageId }
        if (idx >= 0) {
            val m = messages[idx]
            val r = m.reactions.toMutableMap().apply { this[emoji] = (this[emoji] ?: 0) + 1 }
            messages[idx] = m.copy(reactions = r, myReaction = emoji)
        }
        state.update { it.copy(ui = it.ui.copy(showReactionPicker = false, reactingToMessageId = null)) }
    }
    fun showReactionPicker(messageId: String) = state.update { it.copy(ui = it.ui.copy(showReactionPicker = true, reactingToMessageId = messageId)) }
    fun dismissReactionPicker() = state.update { it.copy(ui = it.ui.copy(showReactionPicker = false, reactingToMessageId = null)) }

    // ── Private ───────────────────────────────────────
    private fun markDelivery(sentText: String, serverTimestamp: Long = 0L, serverMessageId: Long = 0L) {
        val now = System.currentTimeMillis() / 1000
        // Strategy 1: Match by exact text — most reliable
        for (i in messages.indices.reversed()) {
            val m = messages[i]
            if (m.isMine && m.id.startsWith("local_") && m.deliveryStatus == DeliveryStatus.Sending && m.text == sentText) {
                val newId = if (serverMessageId > 0) serverMessageId.toString() else "${m.userId}_$serverTimestamp"
                messages[i] = m.copy(deliveryStatus = DeliveryStatus.Delivered, id = newId)
                return
            }
        }
        // Strategy 2: Match by time window — fallback
        for (i in messages.indices.reversed()) {
            val m = messages[i]
            if (m.isMine && m.id.startsWith("local_") && m.deliveryStatus == DeliveryStatus.Sending &&
                now - m.timestampSeconds < 30
            ) {
                val newId = if (serverMessageId > 0) serverMessageId.toString() else "${m.userId}_$serverTimestamp"
                messages[i] = m.copy(deliveryStatus = DeliveryStatus.Delivered, id = newId)
                return
            }
        }
    }
    private fun buildReceivedMessage(
        userId: String, nickname: String, text: String,
        timestamp: Long, fileUrl: String, fileName: String, fileSize: Long,
        id: String,
    ) = ChatMessage(
        id = id, userId = userId, nickname = nickname, text = text,
        timestampSeconds = timestamp, isMine = false,
        messageType = if (fileUrl.isNotEmpty()) MessageType.Image else MessageType.Text,
        fileUrl = fileUrl, fileName = fileName, fileSize = fileSize,
    )
    private fun scheduleTypingCleanup() {
        typingCleanupJob?.cancel()
        typingCleanupJob = scope.launch { delay(3000); state.update { it.copy(chat = it.chat.copy(typingUsers = emptySet())) } }
    }
    private fun notifyIfNeeded(chatId: String, chatTitle: String, sender: String, text: String) {
        val ctx = appContext ?: return
        if (state.value.chat.currentChatId == chatId) return
        NotificationHelper.showMessageNotification(ctx, chatId, chatTitle, sender, text)
    }
}
