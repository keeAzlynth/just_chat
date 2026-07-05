package com.course.imchat.ui.components.chat

import com.course.imchat.ChatMessage

// ── List item types for message grouping ─────────────────────
sealed class ChatListItem {
    data class MessageItem(val message: ChatMessage) : ChatListItem()
    data class DateSeparatorItem(val date: String) : ChatListItem()
}

// ── Helper: convert messages to list items with date separators ──
fun messagesToListItems(messages: List<ChatMessage>): List<ChatListItem> {
    if (messages.isEmpty()) return emptyList()
    
    val items = mutableListOf<ChatListItem>()
    var lastDate = ""
    
    for (message in messages) {
        val currentDate = message.formattedDate()
        if (currentDate != lastDate) {
            items.add(ChatListItem.DateSeparatorItem(currentDate))
            lastDate = currentDate
        }
        items.add(ChatListItem.MessageItem(message))
    }
    
    return items
}
