package com.course.imchat.data.cache

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

class MessageCache(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("message_cache", Context.MODE_PRIVATE)
    private val _unreadCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    private val _totalUnreadCount = MutableStateFlow(0)
    
    val unreadCounts: Flow<Map<String, Int>> = _unreadCounts.asStateFlow()
    val totalUnreadCount: Flow<Int> = _totalUnreadCount.asStateFlow()
    
    init {
        loadUnreadCounts()
    }
    
    // 保存未读消息数
    fun saveUnreadCount(chatId: String, count: Int) {
        prefs.edit().putInt("unread_$chatId", count).apply()
        loadUnreadCounts()
    }
    
    // 增加未读消息数
    fun incrementUnreadCount(chatId: String) {
        val current = prefs.getInt("unread_$chatId", 0)
        prefs.edit().putInt("unread_$chatId", current + 1).apply()
        loadUnreadCounts()
    }
    
    // 清零未读消息数
    fun clearUnreadCount(chatId: String) {
        prefs.edit().putInt("unread_$chatId", 0).apply()
        loadUnreadCounts()
    }
    
    // 获取未读消息数
    fun getUnreadCount(chatId: String): Int {
        return prefs.getInt("unread_$chatId", 0)
    }
    
    // 加载所有未读消息数
    private fun loadUnreadCounts() {
        val counts = mutableMapOf<String, Int>()
        var total = 0
        
        prefs.all.forEach { (key, value) ->
            if (key.startsWith("unread_") && value is Int) {
                val chatId = key.removePrefix("unread_")
                counts[chatId] = value
                total += value
            }
        }
        
        _unreadCounts.value = counts
        _totalUnreadCount.value = total
    }
    
    // 保存最后一条消息
    fun saveLastMessage(chatId: String, messageId: String, timestamp: Long) {
        prefs.edit()
            .putString("last_msg_$chatId", messageId)
            .putLong("last_time_$chatId", timestamp)
            .apply()
    }
    
    // 获取最后一条消息ID
    fun getLastMessageId(chatId: String): String? {
        return prefs.getString("last_msg_$chatId", null)
    }
    
    // 获取最后一条消息时间
    fun getLastMessageTime(chatId: String): Long {
        return prefs.getLong("last_time_$chatId", 0)
    }
    
    // 生成聊天ID
    fun generateChatId(isPrivate: Boolean, receiverId: String?, isGroup: Boolean = false, groupId: String? = null): String {
        return when {
            isGroup && groupId != null -> "group_$groupId"
            isPrivate && receiverId != null -> "private_$receiverId"
            else -> "public"
        }
    }
}
