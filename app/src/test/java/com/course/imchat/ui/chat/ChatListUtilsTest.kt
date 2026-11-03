package com.course.imchat.ui.chat

import com.course.imchat.ChatMessage
import com.course.imchat.data.TimeFormatter
import com.course.imchat.ui.components.chat.ChatListItem
import com.course.imchat.ui.components.chat.messagesToListItems
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatListUtilsTest {

    private fun msg(
        id: String,
        userId: String,
        ts: Long,
        isMine: Boolean = false,
    ) = ChatMessage(
        id = id,
        userId = userId,
        nickname = userId,
        text = "text-$id",
        timestampSeconds = ts,
        isMine = isMine,
    )

    private fun List<ChatListItem>.messages() = filterIsInstance<ChatListItem.MessageItem>()

    @Test
    fun `空列表返回空`() {
        assertTrue(messagesToListItems(emptyList()).isEmpty())
    }

    @Test
    fun `单条消息既是首条也是末条`() {
        val items = messagesToListItems(listOf(msg("1", "u1", 1_700_000_000)))
        assertEquals(2, items.size) // 日期分隔 + 1 条消息
        assertTrue(items[0] is ChatListItem.DateSeparatorItem)

        val m = items.messages().single()
        assertTrue(m.isFirstInGroup)
        assertTrue(m.isLastInGroup)
    }

    @Test
    fun `同发送者五分钟内合并成一组`() {
        val base = 1_700_000_000L
        val items = messagesToListItems(
            listOf(
                msg("1", "u1", base),
                msg("2", "u1", base + 60),
                msg("3", "u1", base + 120),
            )
        ).messages()

        assertTrue(items[0].isFirstInGroup)
        assertFalse(items[1].isFirstInGroup)
        assertFalse(items[1].isLastInGroup)
        assertTrue(items[2].isLastInGroup)
    }

    @Test
    fun `超过五分钟切成新组`() {
        val base = 1_700_000_000L
        val items = messagesToListItems(
            listOf(
                msg("1", "u1", base),
                msg("2", "u1", base + 301),
            )
        ).messages()

        assertTrue(items[0].isLastInGroup)
        assertTrue(items[1].isFirstInGroup)
    }

    @Test
    fun `换人即断开分组`() {
        val base = 1_700_000_000L
        val items = messagesToListItems(
            listOf(
                msg("1", "u1", base),
                msg("2", "u2", base + 10),
                msg("3", "u1", base + 20),
            )
        ).messages()

        assertTrue(items[0].isLastInGroup)
        assertTrue(items[1].isFirstInGroup)
        assertTrue(items[1].isLastInGroup)
        assertTrue(items[2].isFirstInGroup)
    }

    @Test
    fun `自己与别人的消息不合并`() {
        val base = 1_700_000_000L
        val items = messagesToListItems(
            listOf(
                msg("1", "u1", base, isMine = false),
                msg("2", "u1", base + 5, isMine = true),
            )
        ).messages()

        assertTrue(items[0].isLastInGroup)
        assertTrue(items[1].isFirstInGroup)
    }

    @Test
    fun `跨天插入日期分隔`() {
        val day1 = 1_700_000_000L
        val day2 = day1 + 24 * 3600

        val items = messagesToListItems(
            listOf(
                msg("1", "u1", day1),
                msg("2", "u1", day2),
            )
        )

        val separators = items.filterIsInstance<ChatListItem.DateSeparatorItem>()
        assertEquals(2, separators.size)
        assertEquals(TimeFormatter.ymd(day1), separators[0].date)
        assertEquals(TimeFormatter.ymd(day2), separators[1].date)
        assertEquals(4, items.size)
    }

    @Test
    fun `同一天只插一次分隔`() {
        val base = 1_700_000_000L
        val items = messagesToListItems(
            listOf(
                msg("1", "u1", base),
                msg("2", "u2", base + 3600),
                msg("3", "u1", base + 7200),
            )
        )

        assertEquals(1, items.count { it is ChatListItem.DateSeparatorItem })
        assertEquals(4, items.size)
    }
}
