package com.course.imchat.data.cache

import android.content.Context
import android.content.SharedPreferences
import com.course.imchat.ChatMessage
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Persistent message cache — SharedPreferences + kotlinx.serialization.
 * Each chat stores up to 50 recent messages on disk.
 *
 * Migration path: SharedPreferences → DataStore (already has dependency, just swap impl later).
 */
object PersistentCache {

    private const val PREFS_NAME = "imchat_msg_cache"
    private const val MAX_MSGS_PER_CHAT = 50
    private const val DRAFT_PREFIX = "draft_"
    private const val MSG_PREFIX = "msgs_"

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // ── Messages ───────────────────────────────────────────

    @Synchronized
    fun saveMessages(chatId: String, messages: List<ChatMessage>) {
        if (!::prefs.isInitialized) return
        try {
            prefs.edit().putString(MSG_PREFIX + chatId, json.encodeToString(messages.takeLast(MAX_MSGS_PER_CHAT))).apply()
        } catch (_: Exception) {}
    }

    @Synchronized
    fun loadMessages(chatId: String): List<ChatMessage>? {
        if (!::prefs.isInitialized) return null
        return try {
            val str = prefs.getString(MSG_PREFIX + chatId, null) ?: return null
            json.decodeFromString<List<ChatMessage>>(str)
        } catch (_: Exception) { null }
    }

    // ── Drafts ─────────────────────────────────────────────

    @Synchronized
    fun saveDraft(chatId: String, text: String) {
        if (!::prefs.isInitialized) return
        try { prefs.edit().putString(DRAFT_PREFIX + chatId, text).apply() } catch (_: Exception) {}
    }

    @Synchronized
    fun loadDraft(chatId: String): String? {
        if (!::prefs.isInitialized) return null
        return try { prefs.getString(DRAFT_PREFIX + chatId, null) } catch (_: Exception) { null }
    }

    // ── Management ─────────────────────────────────────────

    fun getCacheSize(): Long {
        if (!::prefs.isInitialized) return 0
        var size = 0L
        try {
            for (key in prefs.all.keys) {
                if (key.startsWith(MSG_PREFIX) || key.startsWith(DRAFT_PREFIX))
                    size += (prefs.getString(key, "")?.length?.toLong() ?: 0L)
            }
        } catch (_: Exception) {}
        return size
    }

    fun getCachedChatCount(): Int {
        if (!::prefs.isInitialized) return 0
        return prefs.all.keys.count { it.startsWith(MSG_PREFIX) }
    }

    @Synchronized
    fun clearAll() {
        if (!::prefs.isInitialized) return
        try {
            val editor = prefs.edit()
            for (key in prefs.all.keys) {
                if (key.startsWith(MSG_PREFIX) || key.startsWith(DRAFT_PREFIX)) editor.remove(key)
            }
            editor.apply()
        } catch (_: Exception) {}
    }
}
