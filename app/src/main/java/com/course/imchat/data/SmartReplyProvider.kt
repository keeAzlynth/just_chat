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
            text.length <= 4 -> shortReplies

            // Default general replies
            else -> defaultReplies
        }
    }

    // ── Pattern matching ─────────────────────────────────
    //
    // 这些词表原本写在函数体里，每次调用都 new 一个 List；
    // getSuggestions 每条消息都跑，等于每条消息白扔 5 个对象。
    // 提到 object 级别用 Array 存，遍历退化成「数组 + 下标」，没有迭代器分配。

    private val greetingPatterns = arrayOf("你好", "hi", "hello", "嗨", "早", "晚上好", "下午好")
    private val questionPatterns = arrayOf("什么", "怎么", "如何", "为什么", "哪里", "谁", "哪个", "可以", "能不能")
    private val appreciationPatterns = arrayOf("谢谢", "感谢", "多谢", "辛苦", "thx", "thanks")
    private val farewellPatterns = arrayOf("再见", "拜拜", "bye", "晚安", "走了", "先下了")
    private val confirmationPatterns = arrayOf("帮我", "麻烦", "请", "pls", "please", "看一下", "处理")

    // 短消息 / 兜底用的推荐组合只算一次（原来每次调用都在 + 和 take 里新建 List）
    private val shortReplies: List<String> = emojiReplies + generalReplies.subList(0, 3)
    private val defaultReplies: List<String> = generalReplies.subList(0, 4)

    private fun String.matchesGreeting(): Boolean = containsAny(greetingPatterns, true)

    private fun String.matchesQuestion(): Boolean {
        if (endsWith("?") || endsWith("？") || endsWith("吗")) return true
        return containsAny(questionPatterns, false)
    }

    private fun String.matchesAppreciation(): Boolean = containsAny(appreciationPatterns, true)

    private fun String.matchesFarewell(): Boolean = containsAny(farewellPatterns, true)

    private fun String.matchesConfirmation(): Boolean = containsAny(confirmationPatterns, true)

    /** 数组 + 下标遍历，避免 `Iterable.any {}` 的 lambda 与迭代器分配 */
    private fun String.containsAny(patterns: Array<String>, ignoreCase: Boolean): Boolean {
        for (i in patterns.indices) {
            if (contains(patterns[i], ignoreCase = ignoreCase)) return true
        }
        return false
    }
}
