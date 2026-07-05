package com.course.imchat.data.cache

import java.util.LinkedHashMap

/**
 * 通用 LRU 缓存 — 基于 LinkedHashMap access-order
 *
 * 线程安全（synchronized），适合单写多读的消息列表场景。
 *
 * @param maxSize 最大容量，超出时淘汰最久未访问的条目
 * @param onEvict 淘汰回调，可用于持久化或统计
 */
open class LruCache<K, V>(
    private val maxSize: Int,
    private val onEvict: ((K, V) -> Unit)? = null,
) {
    private val map: LinkedHashMap<K, V> = object : LinkedHashMap<K, V>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>): Boolean {
            val shouldRemove = size > maxSize
            if (shouldRemove) onEvict?.invoke(eldest.key, eldest.value)
            return shouldRemove
        }
    }

    @Synchronized
    operator fun get(key: K): V? = map[key]

    @Synchronized
    operator fun set(key: K, value: V) { map[key] = value }

    @Synchronized
    fun remove(key: K): V? = map.remove(key)

    @Synchronized
    fun clear() = map.clear()

    @Synchronized
    fun contains(key: K): Boolean = map.containsKey(key)

    @Synchronized
    fun keys(): Set<K> = map.keys.toSet()

    @Synchronized
    fun values(): Collection<V> = map.values.toList()

    val size: Int @Synchronized get() = map.size
}
