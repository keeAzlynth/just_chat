package com.course.imchat.core.util

import androidx.compose.runtime.*

/**
 * Compose稳定性优化
 * 参考Now in Android项目的做法
 */

/**
 * 稳定的List包装器
 * 解决Compose对List的不稳定性问题
 */
@Immutable
@JvmInline
value class StableList<out T>(val value: List<T>) : List<T> by value {
    companion object {
        fun <T> empty(): StableList<T> = StableList(emptyList())
        fun <T> of(vararg elements: T): StableList<T> = StableList(elements.toList())
    }
}

/**
 * 稳定的Set包装器
 */
@Immutable
@JvmInline
value class StableSet<out T>(val value: Set<T>) : Set<T> by value {
    companion object {
        fun <T> empty(): StableSet<T> = StableSet(emptySet())
    }
}

/**
 * 稳定的Map包装器 - 使用data class避免泛型问题
 */
@Immutable
data class StableMap<K, V>(val value: Map<K, V>) : Map<K, V> by value {
    companion object {
        fun <K, V> empty(): StableMap<K, V> = StableMap(emptyMap())
    }
}

/**
 * 内联函数 - 转换为稳定集合
 */
fun <T> List<T>.toStable(): StableList<T> = StableList(this)
fun <T> Set<T>.toStable(): StableSet<T> = StableSet(this)
fun <K, V> Map<K, V>.toStable(): StableMap<K, V> = StableMap(this)

/**
 * 记忆化派生状态
 */
@Composable
inline fun <T> rememberDerivedState(crossinline calculation: () -> T): State<T> =
    remember { derivedStateOf { calculation() } }

/**
 * 稳定的回调包装器
 */
@Immutable
@JvmInline
value class StableCallback<in T>(val callback: (T) -> Unit) {
    operator fun invoke(value: T) = callback(value)
}

@Immutable
@JvmInline
value class StableAction(val action: () -> Unit) {
    operator fun invoke() = action()
}

/**
 * 内联函数 - 创建稳定回调
 */
inline fun <T> stableCallback(crossinline callback: (T) -> Unit): StableCallback<T> =
    StableCallback { callback(it) }

inline fun stableAction(crossinline action: () -> Unit): StableAction =
    StableAction { action() }
