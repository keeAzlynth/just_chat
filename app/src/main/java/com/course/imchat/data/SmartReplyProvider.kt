package com.course.imchat.data

import com.course.imchat.ChatMessage

/**
 * Context-aware smart reply suggestion engine.
 * Telegram-style: analyzes the last message and provides quick reply chips.
 * Pure Kotlin — no ML dependencies required.
 */
object SmartReplyProvider {

    // ── Rule-based suggestion sets ────────────────────────

    private val greetingReplies = listOf(
        "你好！", "你好呀～", "好久不见", "😊"
    )

    private val questionReplies = listOf(
        "是的", "不是的", "不太确定", "让我想想"
    )

    private val confirmReplies = listOf(
        "好的", "收到👍", "OK", "没问题"
    )

    private val appreciationReplies = listOf(
        "不客气！", "应该的 😊", "嘿嘿"
    )

    private val farewellReplies = listOf(
        "再见！", "拜拜 👋", "下次聊", "回见"
    )

    private val emojiReplies = listOf(
        "👍", "❤️", "😂", "🎉", "😮", "👏"
    )

    private val generalReplies = listOf(
        "好的", "收到", "哈哈", "没错", "嗯嗯", "了解了"
    )

    /**
     * Get smart reply suggestions based on the last message context.
     * Returns 3-6 suggestions max.
     */
    fun getSuggestions(lastMessages: List<ChatMessage>, currentUserId: String?): List<String> {
        val lastMsg = lastMessages.lastOrNull() ?: return emptyList()
        val text = lastMsg.text.trim()

        // Don't suggest for own messages
        if (lastMsg.userId == currentUserId) return emptyList()

        return when {
            // Greeting patterns
            text.matchesGreeting() -> greetingReplies

            // Question patterns
            text.matchesQuestion() -> questionReplies

            // Appreciation patterns
            text.matchesAppreciation() -> appreciationReplies

            // Farewell patterns
            text.matchesFarewell() -> farewellReplies

            // Confirmation / instruction
            text.matchesConfirmation() -> confirmReplies

            // Short message → emoji replies
            text.length <= 4 -> emojiReplies + generalReplies.take(3)

            // Default general replies
            else -> generalReplies.take(4)
        }
    }

    // ── Pattern matching ─────────────────────────────────

    private fun String.matchesGreeting(): Boolean {
        val patterns = listOf("你好", "hi", "hello", "嗨", "早", "晚上好", "下午好")
        return patterns.any { this.contains(it, ignoreCase = true) }
    }

    private fun String.matchesQuestion(): Boolean {
        val hasQuestionMark = this.endsWith("?") || this.endsWith("？") || this.endsWith("吗")
        val questionWords = listOf("什么", "怎么", "如何", "为什么", "哪里", "谁", "哪个", "可以", "能不能")
        return hasQuestionMark || questionWords.any { this.contains(it) }
    }

    private fun String.matchesAppreciation(): Boolean {
        val patterns = listOf("谢谢", "感谢", "多谢", "辛苦", "thx", "thanks")
        return patterns.any { this.contains(it, ignoreCase = true) }
    }

    private fun String.matchesFarewell(): Boolean {
        val patterns = listOf("再见", "拜拜", "bye", "晚安", "走了", "先下了")
        return patterns.any { this.contains(it, ignoreCase = true) }
    }

    private fun String.matchesConfirmation(): Boolean {
        val patterns = listOf("帮我", "麻烦", "请", "pls", "please", "看一下", "处理")
        return patterns.any { this.contains(it, ignoreCase = true) }
    }
}
