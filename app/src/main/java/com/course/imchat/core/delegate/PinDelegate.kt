package com.course.imchat.core.delegate

import com.course.imchat.ChatMessage
import com.course.imchat.ChatUiState
import com.course.imchat.PinnedMessage
import com.course.imchat.SavedMessage
import com.course.imchat.data.MessageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * Handles pinned messages and saved messages (bookmarks).
 */
class PinDelegate(
    private val state: MutableStateFlow<ChatUiState>,
    private val repository: MessageRepository,
) {
    fun pinMessage(msg: ChatMessage) {
        val chatId = state.value.currentChatId
        val pinned = PinnedMessage(
            messageId = msg.id,
            chatId = chatId,
            message = msg,
            pinnedBy = state.value.myUserId ?: "",
            pinnedAt = System.currentTimeMillis() / 1000,
        )
        state.update {
            it.copy(pinnedMessages = it.pinnedMessages + (chatId to pinned))
        }
        repository.pinMessage(msg.id, chatId)
    }

    fun unpinMessage(chatId: String) {
        val current = state.value.pinnedMessages[chatId]
        if (current != null) {
            repository.unpinMessage(current.messageId, chatId)
            state.update { it.copy(pinnedMessages = it.pinnedMessages - chatId) }
        }
    }

    fun unpinCurrentChat() {
        unpinMessage(state.value.currentChatId)
    }

    fun saveMessage(msg: ChatMessage) {
        val saved = SavedMessage(
            messageId = msg.id,
            chatId = state.value.currentChatId,
            message = msg,
            savedAt = System.currentTimeMillis() / 1000,
        )
        state.update { it.copy(savedMessages = it.savedMessages + saved) }
        repository.saveMessageToCollection(msg.id, state.value.currentChatId)
    }

    fun unsaveMessage(msg: ChatMessage) {
        state.update { it.copy(savedMessages = it.savedMessages.filter { sm -> sm.messageId != msg.id }) }
        repository.unsaveMessageFromCollection(msg.id)
    }

    fun toggleSavedMessages() {
        state.update { it.copy(showSavedMessages = !it.showSavedMessages) }
    }
}
