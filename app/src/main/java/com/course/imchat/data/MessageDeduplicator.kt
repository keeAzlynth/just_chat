package com.course.imchat.data

/**
 * Lightweight deduplication store for received messages.
 * Reconnect events can cause the server to replay history — this
 * prevents duplicate messages from appearing in the UI.
 *
 * Uses a simple LRU-ish circular buffer (fixed capacity, O(1) lookup).
 * Not persisted — valid only for the current session.
 */
class MessageDeduplicator(private val capacity: Int = 500) {

    private val seen = object : LinkedHashMap<String, Boolean>(capacity, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Boolean>?): Boolean {
            return size > capacity
        }
    }

    /**
     * Returns true if this messageId has already been seen.
     * Side effect: marks the id as seen for subsequent checks.
     */
    fun isDuplicate(messageId: String): Boolean {
        return seen.put(messageId, true) != null
    }

    /**
     * Check without marking — for pre-flight checks only.
     */
    fun alreadySeen(messageId: String): Boolean {
        return seen.containsKey(messageId)
    }

    /**
     * Manually mark an id as seen (e.g., for locally-sent messages after
     * the server echoes them back).
     */
    fun markSeen(messageId: String) {
        seen[messageId] = true
    }

    fun clear() {
        seen.clear()
    }

    fun size(): Int = seen.size
}
