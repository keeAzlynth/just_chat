package com.course.imchat.data.cache

import android.content.Context
import android.content.SharedPreferences
import com.course.imchat.ChatMessage
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Persistent message cache backed by SharedPreferences.
 *
 * Thread-safe initialization via [ensureInit]. Once initialised, all reads/writes
 * are safe without the `::prefs.isInitialized` guard.
 *
 * Each chat stores up to [MAX_MSGS_PER_CHAT] recent messages.
 *
 * TODO: swap SharedPreferences → DataStore for large workloads.
 */
object PersistentCache {

    private const val PREFS_NAME = "imchat_msg_cache"
    private const val MAX_MSGS_PER_CHAT = 50
    private const val DRAFT_PREFIX = "draft_"
    private const val MSG_PREFIX = "msgs_"

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Volatile
    private var prefs: SharedPreferences? = null
    private val lock = Any()

    /**
     * Idempotent initializer — safe to call from multiple threads.
     * Must be called once, typically in ChatViewModel.init {}.
     */
    fun ensureInit(context: Context) {
        if (prefs != null) return
        synchronized(lock) {
            if (prefs != null) return
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    private fun pref(): SharedPreferences = prefs
        ?: throw IllegalStateException("PersistentCache not initialized — call ensureInit(context) first")

    // ── Messages ─────────────────────────────────────────────

    @Synchronized
    fun saveMessages(chatId: String, messages: List<ChatMessage>) {
        try {
            pref().edit()
                .putString(MSG_PREFIX + chatId, json.encodeToString(messages.takeLast(MAX_MSGS_PER_CHAT)))
                .apply()
        } catch (_: Exception) {}
    }

    @Synchronized
    fun loadMessages(chatId: String): List<ChatMessage>? {
        return try {
            val str = pref().getString(MSG_PREFIX + chatId, null) ?: return null
            json.decodeFromString<List<ChatMessage>>(str)
        } catch (_: Exception) { null }
    }

    // ── Drafts ───────────────────────────────────────────────

    @Synchronized
    fun saveDraft(chatId: String, text: String) {
        try { pref().edit().putString(DRAFT_PREFIX + chatId, text).apply() } catch (_: Exception) {}
    }

    @Synchronized
    fun loadDraft(chatId: String): String? {
        return try { pref().getString(DRAFT_PREFIX + chatId, null) } catch (_: Exception) { null }
    }

    // ── Cache metadata ───────────────────────────────────────

    fun getCacheSize(): Long {
        var size = 0L
        try {
            for (key in pref().all.keys) {
                if (key.startsWith(MSG_PREFIX) || key.startsWith(DRAFT_PREFIX))
                    size += (pref().getString(key, "")?.length?.toLong() ?: 0L)
            }
        } catch (_: Exception) {}
        return size
    }

    fun getCachedChatCount(): Int {
        return pref().all.keys.count { it.startsWith(MSG_PREFIX) }
    }

    @Synchronized
    fun clearAll() {
        try {
            val editor = pref().edit()
            for (key in pref().all.keys) {
                if (key.startsWith(MSG_PREFIX) || key.startsWith(DRAFT_PREFIX)) editor.remove(key)
            }
            editor.apply()
        } catch (_: Exception) {}
    }
}
