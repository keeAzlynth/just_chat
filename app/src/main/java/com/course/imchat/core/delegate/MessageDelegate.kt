package com.course.imchat.core.delegate

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
        myUserId = state.value.myUserId.orEmpty(),
        nickname = state.value.nickname.trim(),
        quotingMessage = state.value.quotingMessage,
    )

    // ── Send ───────────────────────────────────────────
    fun sendMessage() {
        val text = state.value.draft.trim()
        if (text.isEmpty() || !state.value.canSend) return
        val s = state.value
        if (s.editingMessage != null) { saveEditMessage(s.editingMessage.id, text); return }

        val result = composer.composeAndSendText(
            composeContext(), text,
            groupId = s.selectedGroup?.groupId,
            receiverId = s.selectedPrivateUser?.userId,
        )
        messages.add(result.localMessage)
        state.update { it.copy(draft = "", quotingMessage = null, smartSuggestions = emptyList()) }
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
        if (!state.value.joined) return
        val result = composer.composeAndSendImage(composeContext(), uri, fileName, fileSize)
        messages.add(result.localMessage)
        state.update { it.copy(showAttachmentSheet = false) }
    }

    fun sendFile(uri: Uri, fileName: String, fileSize: Long) {
        if (!state.value.joined) return
        val result = composer.composeAndSendFile(composeContext(), uri, fileName, fileSize)
        messages.add(result.localMessage)
        state.update { it.copy(showAttachmentSheet = false) }
    }

    // ── Edit ──────────────────────────────────────────
    fun quoteMessage(msg: ChatMessage) = state.update { it.copy(quotingMessage = msg) }
    fun cancelQuote() = state.update { it.copy(quotingMessage = null) }
    fun startEdit(msg: ChatMessage) = state.update { it.copy(editingMessage = msg, draft = msg.text) }
    fun cancelEdit() = state.update { it.copy(editingMessage = null, draft = "") }
    fun saveEditMessage(messageId: String, newText: String) {
        val clean = newText.trim(); if (clean.isEmpty()) return
        val idx = messages.indexOfFirst { it.id == messageId }
        if (idx >= 0) messages[idx] = messages[idx].copy(text = clean)
        state.update { it.copy(editingMessage = null, draft = "") }
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
        s.selectedMessages.forEach { id -> if (!id.startsWith("local_")) repository.deleteMessage(id) }
        messages.removeAll { it.id in s.selectedMessages }
        state.update { it.copy(selectedMessages = emptySet(), isMultiSelectMode = false) }
    }

    // ── Forward ───────────────────────────────────────
    fun startForward(msg: ChatMessage) = state.update { it.copy(forwardingMessage = msg, showForwardDialog = true) }
    fun cancelForward() = state.update { it.copy(forwardingMessage = null, showForwardDialog = false) }
    fun forwardToUser(targetUserId: String) {
        val msg = state.value.forwardingMessage ?: return
        repository.sendPrivateMessage("↪ ${msg.nickname}: ${msg.text.take(80)}", targetUserId)
        state.update { it.copy(forwardingMessage = null, showForwardDialog = false) }
    }
    fun forwardSelectedMessages() {
        val s = state.value
        val selected = messages.filter { it.id in s.selectedMessages }
        if (selected.isEmpty()) return
        state.update {
            it.copy(
                forwardingMessage = ChatMessage(
                    id = "fwd_${java.util.UUID.randomUUID()}", userId = s.myUserId.orEmpty(),
                    nickname = s.nickname, text = selected.joinToString("\n") { "${it.nickname}: ${it.text.take(80)}" },
                    timestampSeconds = System.currentTimeMillis() / 1000, isMine = true,
                ),
                showForwardDialog = true, selectedMessages = emptySet(),
            )
        }
    }

    // ── Multi-select ──────────────────────────────────
    fun toggleMultiSelect() {
        state.update {
            it.copy(isMultiSelectMode = !it.isMultiSelectMode,
                selectedMessages = if (it.isMultiSelectMode) emptySet() else it.selectedMessages)
        }
    }
    fun toggleMessageSelection(messageId: String) {
        state.update { s ->
            val u = s.selectedMessages.toMutableSet().apply { if (messageId in this) remove(messageId) else add(messageId) }
            s.copy(selectedMessages = u)
        }
    }
    fun selectAllMessages() = state.update { s -> s.copy(selectedMessages = messages.map { it.id }.toSet()) }

    // ── Search ────────────────────────────────────────
    fun toggleSearch() = state.update { it.copy(isSearching = !it.isSearching, searchQuery = "", searchResults = emptyList()) }
    fun onSearchQueryChange(query: String) {
        val results = if (query.isBlank()) emptyList()
        else messages.filter { it.text.contains(query, ignoreCase = true) }.takeLast(50)
        state.update { it.copy(searchQuery = query, searchResults = results) }
    }

    // ── Smart reply ───────────────────────────────────
    fun computeSmartSuggestions() {
        val suggestions = SmartReplyProvider.getSuggestions(messages.takeLast(5), state.value.myUserId)
        state.update { it.copy(smartSuggestions = suggestions) }
    }

    // ── Incoming ──────────────────────────────────────
    fun handleIncomingPublicMessage(
        userId: String, nickname: String, text: String,
        timestamp: Long, fileUrl: String, fileName: String, fileSize: Long,
    ) {
        if (userId == state.value.myUserId) { markDelivery(text); return }
        if (dedup.isDuplicate("$userId|$timestamp|${text.take(40)}")) return
        messages.add(buildReceivedMessage(userId, nickname, text, timestamp, fileUrl, fileName, fileSize))
        notifyIfNeeded("public", "公共聊天", nickname, text)
        computeSmartSuggestions()
    }

    fun handleIncomingPrivateMessage(userId: String, nickname: String, text: String, timestamp: Long) {
        if (userId == state.value.myUserId) { markDelivery(text); return }
        if (dedup.isDuplicate("pm|$userId|$timestamp|${text.take(40)}")) return
        messages.add(ChatMessage(
            id = "$userId|$timestamp", userId = userId, nickname = nickname, text = text,
            timestampSeconds = timestamp, isMine = false, isPrivate = true,
            receiverId = state.value.myUserId ?: "",
        ))
        notifyIfNeeded("private_$userId", nickname, nickname, text)
        computeSmartSuggestions()
    }

    fun handleIncomingGroupMessage(groupId: String, userId: String, nickname: String, text: String, timestamp: Long) {
        if (userId == state.value.myUserId) return
        if (dedup.isDuplicate("grp|$groupId|$userId|$timestamp|${text.take(40)}")) return
        messages.add(ChatMessage(id = "$userId|$timestamp", userId = userId, nickname = nickname, text = text,
            timestampSeconds = timestamp, isMine = false, groupId = groupId))
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
        state.update { s -> if (userId == s.myUserId) s else s.copy(typingUsers = s.typingUsers + nickname) }
        scheduleTypingCleanup()
    }
    fun onDraftChange(text: String) {
        state.update { it.copy(draft = text) }
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
        state.update { it.copy(showReactionPicker = false, reactingToMessageId = null) }
    }
    fun showReactionPicker(messageId: String) = state.update { it.copy(showReactionPicker = true, reactingToMessageId = messageId) }
    fun dismissReactionPicker() = state.update { it.copy(showReactionPicker = false, reactingToMessageId = null) }

    // ── Private ───────────────────────────────────────
    private fun markDelivery(sentText: String) {
        val now = System.currentTimeMillis() / 1000
        for (i in messages.indices.reversed()) {
            val m = messages[i]
            if (m.isMine && m.id.startsWith("local_") && m.deliveryStatus == DeliveryStatus.Sending &&
                (m.text == sentText || now - m.timestampSeconds < 30)
            ) { messages[i] = m.copy(deliveryStatus = DeliveryStatus.Delivered); break }
        }
    }
    private fun buildReceivedMessage(
        userId: String, nickname: String, text: String,
        timestamp: Long, fileUrl: String, fileName: String, fileSize: Long,
    ) = ChatMessage(
        id = "$userId|$timestamp", userId = userId, nickname = nickname, text = text,
        timestampSeconds = timestamp, isMine = false,
        messageType = if (fileUrl.isNotEmpty()) MessageType.Image else MessageType.Text,
        fileUrl = fileUrl, fileName = fileName, fileSize = fileSize,
    )
    private fun scheduleTypingCleanup() {
        typingCleanupJob?.cancel()
        typingCleanupJob = scope.launch { delay(3000); state.update { it.copy(typingUsers = emptySet()) } }
    }
    private fun notifyIfNeeded(chatId: String, chatTitle: String, sender: String, text: String) {
        val ctx = appContext ?: return
        if (state.value.currentChatId == chatId) return
        NotificationHelper.showMessageNotification(ctx, chatId, chatTitle, sender, text)
    }
}
