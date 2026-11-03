package com.course.imchat.data.cache

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 未读计数 + 最后一条消息的本地存储。
 *
 * 旧实现每次 `incrementUnreadCount()` 都：
 *  1. 读一次 SharedPreferences；
 *  2. `apply()` 写一次盘；
 *  3. `loadUnreadCounts()` 遍历 **全部** prefs 条目、重建 Map、再更新两个 StateFlow。
 * 于是每收到一条消息就是「全表扫描 + Map 重建 + 两次 Flow 发射」。
 *
 * 现在内存里维护一份计数，只在值真正变化时才更新 Flow 和落盘。
 * Flow 用 StateFlow，重复赋同值不会发射，Compose 端也不会白重组。
 */
class MessageCache(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("message_cache", Context.MODE_PRIVATE)

    private val counts = HashMap<String, Int>(16)

    private val _unreadCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val unreadCounts: StateFlow<Map<String, Int>> = _unreadCounts.asStateFlow()

    private val _totalUnreadCount = MutableStateFlow(0)
    val totalUnreadCount: StateFlow<Int> = _totalUnreadCount.asStateFlow()

    init {
        // 只读一次
        for ((key, value) in prefs.all) {
            if (value is Int && key.startsWith(PREFIX_UNREAD)) {
                val chatId = key.substring(PREFIX_UNREAD.length)
                if (value > 0) counts[chatId] = value
            }
        }
        if (counts.isNotEmpty()) publish()
    }

    fun saveUnreadCount(chatId: String, count: Int) = set(chatId, count)

    fun incrementUnreadCount(chatId: String) {
        set(chatId, (counts[chatId] ?: 0) + 1)
    }

    fun clearUnreadCount(chatId: String) {
        if ((counts[chatId] ?: 0) == 0) return
        set(chatId, 0)
    }

    fun getUnreadCount(chatId: String): Int = counts[chatId] ?: 0

    private fun set(chatId: String, value: Int) {
        val previous = counts[chatId] ?: 0
        if (previous == value) return

        if (value <= 0) counts.remove(chatId) else counts[chatId] = value

        // 单条写入，不再遍历全表；apply() 异步落盘，不阻塞调用线程
        prefs.edit().putInt(PREFIX_UNREAD + chatId, value).apply()
        publish()
    }

    /** 只有计数真的变了才会走到这里 */
    private fun publish() {
        _unreadCounts.value = HashMap(counts)
        var total = 0
        for (value in counts.values) total += value
        _totalUnreadCount.value = total
    }

    // ── 每个会话的最后一条消息 ──────────────────────────────

    fun saveLastMessage(chatId: String, messageId: String, timestamp: Long) {
        prefs.edit()
            .putString(PREFIX_LAST_MSG + chatId, messageId)
            .putLong(PREFIX_LAST_TIME + chatId, timestamp)
            .apply()
    }

    fun getLastMessageId(chatId: String): String? = prefs.getString(PREFIX_LAST_MSG + chatId, null)

    fun getLastMessageTime(chatId: String): Long = prefs.getLong(PREFIX_LAST_TIME + chatId, 0L)

    /** 生成会话 id，与服务器侧 chatId 约定保持一致 */
    fun generateChatId(
        isPrivate: Boolean,
        receiverId: String?,
        isGroup: Boolean = false,
        groupId: String? = null,
    ): String = when {
        isGroup && groupId != null -> "group_$groupId"
        isPrivate && receiverId != null -> "private_$receiverId"
        else -> "public"
    }

    private companion object {
        const val PREFIX_UNREAD = "unread_"
        const val PREFIX_LAST_MSG = "last_msg_"
        const val PREFIX_LAST_TIME = "last_time_"
    }
}
