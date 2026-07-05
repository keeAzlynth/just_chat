package com.course.imchat.data.cache

import com.course.imchat.ChatMessage
import com.course.imchat.ChatGroup
import com.course.imchat.OnlineUser
import java.util.concurrent.ConcurrentHashMap

/**
 * 应用级缓存管理器
 *
 * 按聊天 ID 分片的 LRU 消息缓存 + 全局用户/群组缓存。
 * 所有操作线程安全。
 */
object AppCache {
    /** 每个聊天最多缓存的消息数 */
    private const val MESSAGES_PER_CHAT = 200

    /** 最大缓存的聊天数 */
    private const val MAX_CHATS = 10

    /** 用户信息缓存上限 */
    private const val MAX_USERS = 200

    /** 群组信息缓存上限 */
    private const val MAX_GROUPS = 50

    // ── 消息缓存（按 chatId 分片） ──────────────────────
    private val messageCaches = object : LinkedHashMap<String, LruCache<String, ChatMessage>>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, LruCache<String, ChatMessage>>): Boolean {
            if (size > MAX_CHATS) {
                eldest.value.clear()
                return true
            }
            return false
        }
    }

    // ── 用户/群组缓存 ──────────────────────────────────
    private val userCache = LruCache<String, OnlineUser>(MAX_USERS)
    private val groupCache = LruCache<String, ChatGroup>(MAX_GROUPS)

    // ── 最近活跃时间戳 ────────────────────────────────
    private val lastActiveTimestamps = ConcurrentHashMap<String, Long>()

    // ── Public API ─────────────────────────────────────

    @Synchronized
    fun cacheMessages(chatId: String, messages: List<ChatMessage>) {
        val cache = messageCaches.getOrPut(chatId) { LruCache(MESSAGES_PER_CHAT) }
        messages.forEach { cache[it.id] = it }
    }

    @Synchronized
    fun getCachedMessages(chatId: String): List<ChatMessage>? {
        val cache = messageCaches[chatId] ?: return null
        return cache.values().toList()
    }

    @Synchronized
    fun addMessage(chatId: String, message: ChatMessage) {
        val cache = messageCaches.getOrPut(chatId) { LruCache(MESSAGES_PER_CHAT) }
        cache[message.id] = message
    }

    @Synchronized
    fun removeMessage(chatId: String, messageId: String) {
        messageCaches[chatId]?.remove(messageId)
    }

    @Synchronized
    fun clearChatMessages(chatId: String) {
        messageCaches[chatId]?.clear()
    }

    @Synchronized
    fun cacheUser(user: OnlineUser) { userCache[user.userId] = user }

    @Synchronized
    fun getUser(userId: String): OnlineUser? = userCache[userId]

    @Synchronized
    fun cacheUsers(users: List<OnlineUser>) { users.forEach { userCache[it.userId] = it } }

    @Synchronized
    fun cacheGroup(group: ChatGroup) { groupCache[group.groupId] = group }

    @Synchronized
    fun getGroup(groupId: String): ChatGroup? = groupCache[groupId]

    @Synchronized
    fun cacheGroups(groups: List<ChatGroup>) { groups.forEach { groupCache[it.groupId] = it } }

    @Synchronized
    fun getCachedUsers(): List<OnlineUser> = userCache.values().toList()

    @Synchronized
    fun getCachedGroups(): List<ChatGroup> = groupCache.values().toList()

    // ── 时间戳缓存 ─────────────────────────────────────

    fun setLastActive(chatId: String) { lastActiveTimestamps[chatId] = System.currentTimeMillis() }
    fun getLastActive(chatId: String): Long = lastActiveTimestamps[chatId] ?: 0L

    // ── 生命周期 ───────────────────────────────────────

    @Synchronized
    fun clearAll() {
        messageCaches.values.forEach { it.clear() }
        messageCaches.clear()
        userCache.clear()
        groupCache.clear()
        lastActiveTimestamps.clear()
    }
}
