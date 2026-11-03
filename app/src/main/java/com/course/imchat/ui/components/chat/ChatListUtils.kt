package com.course.imchat.ui.components.chat

import com.course.imchat.ChatMessage
import com.course.imchat.data.TimeFormatter

// ── List item types for message grouping ─────────────────────
sealed class ChatListItem {
    data class MessageItem(
        val message: ChatMessage,
        val isFirstInGroup: Boolean = true,   // show avatar + name
        val isLastInGroup: Boolean = true,     // show timestamp + bubble tail
    ) : ChatListItem()
    data class DateSeparatorItem(val date: String) : ChatListItem()
}

/** 同一发送者间隔在这个窗口内就算同一组 */
private const val GROUPING_WINDOW_SECONDS = 5L * 60L

/**
 * 把消息列表转成带「日期分隔 + 同发送者分组」的列表项。
 *
 * 相比旧实现的三处改动：
 *  1. 判断跨天用 epochDay 整数比较，不再为每条消息生成一次 "yyyy-MM-dd" 字符串
 *     （500 条消息原本要 500 次 String.format）；
 *  2. 单次遍历，`prev` 用局部变量带下来，不再重复读 `messages[i-1]`；
 *  3. 结果列表按 size 预分配，避免扩容拷贝。
 */
fun messagesToListItems(messages: List<ChatMessage>): List<ChatListItem> {
    val n = messages.size
    if (n == 0) return emptyList()

    val items = ArrayList<ChatListItem>(n + (n shr 3) + 2)
    var currentDay = Long.MIN_VALUE
    var prev: ChatMessage? = null

    for (i in 0 until n) {
        val cur = messages[i]

        val day = TimeFormatter.epochDay(cur.timestampSeconds)
        if (day != currentDay) {
            // 只有跨天那一刻才格式化日期字符串
            items.add(ChatListItem.DateSeparatorItem(TimeFormatter.ymd(cur.timestampSeconds)))
            currentDay = day
            prev = null
        }

        val next = if (i + 1 < n) messages[i + 1] else null

        val isFirst = prev == null ||
            prev.userId != cur.userId ||
            prev.isMine != cur.isMine ||
            (cur.timestampSeconds - prev.timestampSeconds) > GROUPING_WINDOW_SECONDS

        val isLast = next == null ||
            next.userId != cur.userId ||
            next.isMine != cur.isMine ||
            (next.timestampSeconds - cur.timestampSeconds) > GROUPING_WINDOW_SECONDS

        items.add(ChatListItem.MessageItem(cur, isFirst, isLast))
        prev = cur
    }

    return items
}
