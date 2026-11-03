package com.course.imchat.data

import com.course.imchat.ChatMessage
import org.junit.Assert.assertEquals
import org.junit.Test

class MessageIndexTest {

    private fun msg(id: String, ts: Long = 1_700_000_000L) = ChatMessage(
        id = id,
        userId = "u1",
        nickname = "u1",
        text = "text-$id",
        timestampSeconds = ts,
        isMine = false,
    )

    @Test
    fun `能找到对应下标`() {
        val list = listOf(msg("a"), msg("b"), msg("c"))
        val index = MessageIndex()

        assertEquals(0, index.indexOf(list, "a"))
        assertEquals(1, index.indexOf(list, "b"))
        assertEquals(2, index.indexOf(list, "c"))
    }

    @Test
    fun `找不到返回 -1`() {
        val list = listOf(msg("a"))
        assertEquals(-1, MessageIndex().indexOf(list, "zzz"))
    }

    @Test
    fun `列表增长后索引自动重建`() {
        val index = MessageIndex()
        val first = listOf(msg("a"))
        assertEquals(0, index.indexOf(first, "a"))

        val second = first + msg("b")   // size 变化
        assertEquals(1, index.indexOf(second, "b"))
        assertEquals(0, index.indexOf(second, "a"))
    }

    @Test
    fun `替换元素换了 id 也能正确命中`() {
        val index = MessageIndex()
        val list = mutableListOf(msg("local_1"))
        assertEquals(0, index.indexOf(list, "local_1"))

        // 服务端回执：同一条消息的 id 被替换成真实 id，size 不变
        list[0] = list[0].copy(id = "42")

        assertEquals(0, index.indexOf(list, "42"))
        assertEquals(-1, index.indexOf(list, "local_1"))
    }

    @Test
    fun `invalidate 之后依然正确`() {
        val index = MessageIndex()
        val list = listOf(msg("a"), msg("b"))
        assertEquals(1, index.indexOf(list, "b"))

        index.invalidate()
        assertEquals(1, index.indexOf(list, "b"))
    }

    @Test
    fun `大量消息下结果与线性查找一致`() {
        val list = List(2_000) { msg("m$it", 1_700_000_000L + it) }
        val index = MessageIndex()

        for (probe in listOf(0, 1, 999, 1_000, 1_999)) {
            val expected = list.indexOfFirst { it.id == "m$probe" }
            assertEquals(expected, index.indexOf(list, "m$probe"))
        }
    }
}
