package com.course.imchat.data

/**
 * 收到的消息去重。
 *
 * 断线重连时服务端会补发历史，靠它避免同一条消息出现两次。
 *
 * 旧实现用 `LinkedHashMap(capacity, 0.75f, accessOrder = true)`：
 * 每次 `put` 都要分配一个 Entry，还要维护访问序链表（写指针）；
 * `put(id, true)` 的 Boolean 虽然被缓存，但键值对本身是堆对象。
 *
 * 这里换成 `HashSet` + `ArrayDeque` 的定长 FIFO：
 *  - 一次 `add` 同时完成「判重 + 记录」，哈希只算一次；
 *  - 淘汰走队头，没有链表重排；
 *  - 没有装箱，没有 Entry 对象。
 */
class MessageDeduplicator(private val capacity: Int = 500) {

    // HashSet 默认负载因子 0.75，容量给足避免扩容 rehash
    private val seen = HashSet<String>(capacity * 4 / 3 + 1)
    private val order = ArrayDeque<String>(capacity)

    /**
     * 已见过返回 true；否则记录并返回 false。
     * 集合本身不是线程安全的，跨线程调用请自行加锁（当前只跑在 ViewModel 协程里）。
     */
    fun isDuplicate(messageId: String): Boolean {
        if (!seen.add(messageId)) return true

        if (order.size >= capacity) {
            seen.remove(order.removeFirst())
        }
        order.addLast(messageId)
        return false
    }

    /** 只查不记，用于预检 */
    fun alreadySeen(messageId: String): Boolean = seen.contains(messageId)

    /** 补记一条（本地发送的消息被服务端回显时用） */
    fun markSeen(messageId: String) {
        if (seen.add(messageId)) {
            if (order.size >= capacity) seen.remove(order.removeFirst())
            order.addLast(messageId)
        }
    }

    fun clear() {
        seen.clear()
        order.clear()
    }

    fun size(): Int = seen.size
}
