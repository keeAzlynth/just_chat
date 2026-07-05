package com.course.imchat.core.util

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * 值类(Value Class) - 编译时内联，零对象分配
 * JVM层面直接使用底层类型，避免装箱
 */
@JvmInline
value class UserId(val value: String) {
    init {
        require(value.isNotEmpty()) { "UserId cannot be empty" }
    }
    
    companion object {
        val EMPTY = UserId("")
        
        fun generate(prefix: String = "u_"): UserId = 
            UserId("${prefix}${System.nanoTime().toString(16)}")
    }
}

@JvmInline
value class MessageId(val value: Long) {
    companion object {
        val ZERO = MessageId(0)
        
        fun fromString(id: String): MessageId {
            val cleanId = id.removePrefix("local_")
                .removePrefix("pm_")
                .removePrefix("grp_")
                .removePrefix("hist_")
            return MessageId(cleanId.toLongOrNull() ?: 0L)
        }
    }
}

@JvmInline
value class GroupId(val value: String) {
    init {
        require(value.isNotEmpty()) { "GroupId cannot be empty" }
    }
    
    companion object {
        fun generate(): GroupId = 
            GroupId("grp_${System.nanoTime().toString(16)}")
    }
}

@JvmInline
value class ChatId(val value: String) {
    companion object {
        const val PUBLIC = "public"
        
        fun private(userId: UserId): ChatId = ChatId("private_${userId.value}")
        fun group(groupId: GroupId): ChatId = ChatId("group_${groupId.value}")
    }
}

@JvmInline
value class Timestamp(val seconds: Long) {
    companion object {
        fun now(): Timestamp = Timestamp(System.currentTimeMillis() / 1000)
    }
    
    fun toMillis(): Long = seconds * 1000
}

/**
 * 内联函数 - 减少lambda对象分配
 */
@OptIn(ExperimentalContracts::class)
inline fun <T> T.alsoIf(condition: Boolean, block: (T) -> Unit): T {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    if (condition) block(this)
    return this
}

/**
 * 内联函数 - 安全的类型转换
 */
inline fun <reified T> Any?.safeCast(): T? = this as? T

/**
 * 内联函数 - 批量操作优化
 */
inline fun <T> Iterable<T>.sumOfLong(selector: (T) -> Long): Long {
    var sum = 0L
    for (element in this) {
        sum += selector(element)
    }
    return sum
}
