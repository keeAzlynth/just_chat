package com.course.imchat.data

import com.course.imchat.ChatMessage

/**
 * 消息 id → 下标的索引。
 *
 * 编辑、撤回、加表情、回执都要按 id 找到消息再替换回列表，
 * 原来的 `messages.indexOfFirst { it.id == messageId }` 是 O(n) 字符串比较，
 * 500 条消息等于 500 次 equals，而且每次都把整个列表读一遍。
 *
 * 这里用 HashMap 建一次索引后查 O(1)。三点设计：
 *  1. 用 **size** 判断是否需要重建：增删都会改变 size；
 *  2. 替换元素不改 size，但可能换掉 id（本地消息收到服务端回执时会改 id），
 *     所以命中后再校验一次 `list[i].id == id`，不一致就重建 —— 自愈，不依赖调用方记得通知；
 *  3. 所有方法都要求传入当前列表，避免持有列表引用导致快照泄漏。
 */
class MessageIndex {

    private val positions = HashMap<String, Int>(256)
    private var indexedSize = -1

    fun indexOf(messages: List<ChatMessage>, id: String): Int {
        if (messages.size != indexedSize) rebuild(messages)

        val position = positions[id]
        if (position != null && position < messages.size && messages[position].id == id) {
            return position
        }

        // 未命中或索引过期（id 被替换过），重建一次再查
        rebuild(messages)
        return positions[id] ?: -1
    }

    /** 结构性变化后主动失效，下次查询会重建 */
    fun invalidate() {
        indexedSize = -1
    }

    private fun rebuild(messages: List<ChatMessage>) {
        positions.clear()
        for (i in messages.indices) {
            positions[messages[i].id] = i
        }
        indexedSize = messages.size
    }
}
