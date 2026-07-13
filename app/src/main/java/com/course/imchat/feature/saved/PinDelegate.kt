package com.course.imchat.feature.saved

import com.course.imchat.ChatMessage
import com.course.imchat.ChatUiState
import com.course.imchat.PinnedMessage
import com.course.imchat.SavedMessage
import com.course.imchat.data.MessageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * Handles pinned messages and saved messages (bookmarks).
 * v2.1: Moved to feature/saved module.
 */
class PinDelegate(
    private val state: MutableStateFlow<ChatUiState>,
    private val repository: MessageRepository,
) {
    fun pinMessage(msg: ChatMessage) {
        val chatId = state.value.chat.currentChatId
        val pinned = PinnedMessage(
            messageId = msg.id,
            chatId = chatId,
            message = msg,
            pinnedBy = state.value.auth.myUserId ?: "",
            pinnedAt = System.currentTimeMillis() / 1000,
        )
        state.update {
            it.copy(chat = it.chat.copy(pinnedMessages = it.chat.pinnedMessages + (chatId to pinned)))
        }
        repository.pinMessage(msg.id, chatId)
    }

    fun unpinMessage(chatId: String) {
        val current = state.value.chat.pinnedMessages[chatId]
        if (current != null) {
            repository.unpinMessage(current.messageId, chatId)
            state.update { it.copy(chat = it.chat.copy(pinnedMessages = it.chat.pinnedMessages - chatId)) }
        }
    }

    fun unpinCurrentChat() {
        unpinMessage(state.value.chat.currentChatId)
    }

    fun saveMessage(msg: ChatMessage) {
        val saved = SavedMessage(
            messageId = msg.id,
            chatId = state.value.chat.currentChatId,
            message = msg,
            savedAt = System.currentTimeMillis() / 1000,
        )
        state.update { it.copy(nav = it.nav.copy(savedMessages = it.nav.savedMessages + saved)) }
        repository.saveMessageToCollection(msg.id, state.value.chat.currentChatId)
    }

    fun unsaveMessage(msg: ChatMessage) {
        state.update { it.copy(nav = it.nav.copy(savedMessages = it.nav.savedMessages.filter { sm -> sm.messageId != msg.id })) }
        repository.unsaveMessageFromCollection(msg.id)
    }

    fun toggleSavedMessages() {
        state.update { it.copy(ui = it.ui.copy(showSavedMessages = !it.ui.showSavedMessages)) }
    }
}
