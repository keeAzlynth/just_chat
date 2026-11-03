package com.course.imchat.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageDeduplicatorTest {

    @Test
    fun `重复消息只认一次`() {
        val dedup = MessageDeduplicator(10)

        assertFalse(dedup.isDuplicate("m1"))
        assertTrue(dedup.isDuplicate("m1"))
        assertTrue(dedup.isDuplicate("m1"))
        assertEquals(1, dedup.size())
    }

    @Test
    fun `超过容量后最旧的被淘汰`() {
        val dedup = MessageDeduplicator(3)

        dedup.isDuplicate("a")
        dedup.isDuplicate("b")
        dedup.isDuplicate("c")
        assertEquals(3, dedup.size())

        dedup.isDuplicate("d")             // 淘汰 a
        assertEquals(3, dedup.size())
        assertTrue(dedup.alreadySeen("d"))

        // a 已被淘汰，可以重新加入；此时最旧的变成 b
        assertFalse(dedup.isDuplicate("a"))
        assertTrue(dedup.isDuplicate("a"))
        assertFalse(dedup.alreadySeen("b"))
        assertTrue(dedup.alreadySeen("d"))
        assertEquals(3, dedup.size())
    }

    @Test
    fun `markSeen 之后再遇到算重复`() {
        val dedup = MessageDeduplicator(4)
        dedup.markSeen("local_1")

        assertTrue(dedup.alreadySeen("local_1"))
        assertTrue(dedup.isDuplicate("local_1"))
    }

    @Test
    fun `clear 之后清空`() {
        val dedup = MessageDeduplicator(4)
        dedup.isDuplicate("x")
        dedup.clear()

        assertEquals(0, dedup.size())
        assertFalse(dedup.isDuplicate("x"))
    }

    @Test
    fun `大量消息不爆容量`() {
        val capacity = 100
        val dedup = MessageDeduplicator(capacity)
        repeat(10_000) { i -> dedup.isDuplicate("m$i") }

        assertEquals(capacity, dedup.size())
        // 最近的还在
        assertTrue(dedup.alreadySeen("m9999"))
        // 最早的已被淘汰
        assertFalse(dedup.alreadySeen("m0"))
    }
}
