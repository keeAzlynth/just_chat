package com.course.imchat.core.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * 优化的ViewModel基类
 * 参考Tachiyomi和Signal的架构
 */
abstract class BaseViewModel : ViewModel() {
    
    /**
     * 内联函数 - 创建StateFlow
     */
    protected fun <T> mutableStateFlow(initialValue: T): MutableStateFlow<T> =
        MutableStateFlow(initialValue)
    
    /**
     * 内联函数 - 创建SharedFlow
     */
    protected fun <T> mutableSharedFlow(
        replay: Int = 0,
        extraBufferCapacity: Int = 64,
    ): MutableSharedFlow<T> = MutableSharedFlow(replay, extraBufferCapacity)
    
    /**
     * 安全的协程启动
     */
    protected fun launchIO(
        errorHandler: (Throwable) -> Unit = { it.printStackTrace() },
        block: suspend CoroutineScope.() -> Unit,
    ): Job = viewModelScope.launch(Dispatchers.IO + CoroutineExceptionHandler { _, throwable ->
        errorHandler(throwable)
    }) {
        block()
    }
    
    protected fun launchMain(
        errorHandler: (Throwable) -> Unit = { it.printStackTrace() },
        block: suspend CoroutineScope.() -> Unit,
    ): Job = viewModelScope.launch(Dispatchers.Main + CoroutineExceptionHandler { _, throwable ->
        errorHandler(throwable)
    }) {
        block()
    }
    
    /**
     * 内联函数 - 收集Flow到StateFlow
     */
    protected fun <T> Flow<T>.stateIn(
        initialValue: T,
        scope: CoroutineScope = viewModelScope,
    ): StateFlow<T> = stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = initialValue,
    )
    
    /**
     * 内联函数 - 安全更新StateFlow
     */
    protected inline fun <T> MutableStateFlow<T>.update(block: (T) -> T) {
        value = block(value)
    }
}

/**
 * 委托属性 - 懒加载StateFlow
 */
class LazyStateFlow<T>(
    private val scope: CoroutineScope,
    private val flow: Flow<T>,
    private val initialValue: T,
) : ReadOnlyProperty<Any?, StateFlow<T>> {
    
    private val stateFlow by lazy {
        flow.stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = initialValue,
        )
    }
    
    override fun getValue(thisRef: Any?, property: KProperty<*>): StateFlow<T> = stateFlow
}

/**
 * 扩展函数 - 创建懒加载StateFlow
 */
fun <T> Flow<T>.lazyStateIn(
    scope: CoroutineScope,
    initialValue: T,
): LazyStateFlow<T> = LazyStateFlow(scope, this, initialValue)

/**
 * 内联函数 - 批量操作
 */
inline fun <T> List<T>.fastForEach(action: (T) -> Unit) {
    for (i in indices) {
        action(get(i))
    }
}

inline fun <T, R> List<T>.fastMap(transform: (T) -> R): List<R> {
    val result = ArrayList<R>(size)
    for (i in indices) {
        result.add(transform(get(i)))
    }
    return result
}

inline fun <T> List<T>.fastFilter(predicate: (T) -> Boolean): List<T> {
    val result = ArrayList<T>()
    for (i in indices) {
        val item = get(i)
        if (predicate(item)) result.add(item)
    }
    return result
}
