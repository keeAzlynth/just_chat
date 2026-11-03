package com.course.imchat.data

import com.course.imchat.ChatMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartReplyProviderTest {

    private fun msg(text: String, userId: String = "other") = ChatMessage(
        id = "1",
        userId = userId,
        nickname = userId,
        text = text,
        timestampSeconds = 1_700_000_000,
        isMine = false,
    )

    @Test
    fun `自己的消息不推荐回复`() {
        val result = SmartReplyProvider.getSuggestions(listOf(msg("你好", userId = "me")), "me")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `空列表返回空`() {
        assertTrue(SmartReplyProvider.getSuggestions(emptyList(), "me").isEmpty())
    }

    @Test
    fun `问候语命中问候模板`() {
        val result = SmartReplyProvider.getSuggestions(listOf(msg("你好呀")), "me")
        assertTrue(result.contains("你好！"))
    }

    @Test
    fun `问句命中问答模板`() {
        val result = SmartReplyProvider.getSuggestions(listOf(msg("这个接口怎么调？")), "me")
        assertTrue(result.contains("是的"))
    }

    @Test
    fun `感谢命中客气模板`() {
        val result = SmartReplyProvider.getSuggestions(listOf(msg("谢谢啦")), "me")
        assertTrue(result.contains("不客气！"))
    }

    @Test
    fun `短消息给出表情回复`() {
        val result = SmartReplyProvider.getSuggestions(listOf(msg("好")), "me")
        assertTrue(result.isNotEmpty())
        assertTrue(result.any { it == "👍" })
    }

    @Test
    fun `普通消息给出兜底模板`() {
        val result = SmartReplyProvider.getSuggestions(listOf(msg("今天把那个模块重构了一下")), "me")
        assertEquals(listOf("好的", "收到", "哈哈", "没错"), result)
    }
}
