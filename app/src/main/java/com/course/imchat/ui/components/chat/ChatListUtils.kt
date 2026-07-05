package com.course.imchat.ui.components.chat

import com.course.imchat.ChatMessage

// ── List item types for message grouping ─────────────────────
sealed class ChatListItem {
    data class MessageItem(
        val message: ChatMessage,
        val isFirstInGroup: Boolean = true,   // show avatar + name
        val isLastInGroup: Boolean = true,     // show timestamp + bubble tail
    ) : ChatListItem()
    data class DateSeparatorItem(val date: String) : ChatListItem()
}

// ── Helper: convert messages to list items with date separators + same-sender grouping ──
fun messagesToListItems(messages: List<ChatMessage>): List<ChatListItem> {
    if (messages.isEmpty()) return emptyList()

    val items = mutableListOf<ChatListItem>()
    var lastDate = ""
    val groupingWindow = 5 * 60  // 5 minutes — same sender within window is grouped

    for (i in messages.indices) {
        val msg = messages[i]
        val currentDate = msg.formattedDate()

        if (currentDate != lastDate) {
            items.add(ChatListItem.DateSeparatorItem(currentDate))
            lastDate = currentDate
        }

        val isFirst = i == 0 ||
            messages[i - 1].userId != msg.userId ||
            messages[i - 1].isMine != msg.isMine ||
            (msg.timestampSeconds - messages[i - 1].timestampSeconds) > groupingWindow

        val isLast = i == messages.lastIndex ||
            messages[i + 1].userId != msg.userId ||
            messages[i + 1].isMine != msg.isMine ||
            (messages[i + 1].timestampSeconds - msg.timestampSeconds) > groupingWindow

        items.add(ChatListItem.MessageItem(message = msg, isFirstInGroup = isFirst, isLastInGroup = isLast))
    }

    return items
}
