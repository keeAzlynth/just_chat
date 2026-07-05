package com.course.imchat.domain.repository

import com.course.imchat.core.util.*
import com.course.imchat.domain.model.*
import kotlinx.coroutines.flow.Flow

/**
 * 聊天仓库接口 - 领域层
 * 定义业务操作，不关心具体实现
 */
interface ChatRepository {
    // 连接管理
    fun connect(url: String)
    fun disconnect()
    val connectionState: Flow<ConnectionState>
    
    // 认证
    fun login(username: String, password: String): Flow<ChatResult<UserId>>
    fun register(username: String, password: String, nickname: String): Flow<ChatResult<UserId>>
    fun logout()
    
    // 聊天室
    fun join(nickname: String): Flow<ChatResult<UserId>>
    fun leave()
    
    // 消息
    fun sendMessage(text: String): Flow<ChatResult<Message>>
    fun sendPrivateMessage(text: String, receiverId: UserId): Flow<ChatResult<Message>>
    fun sendGroupMessage(text: String, groupId: GroupId): Flow<ChatResult<Message>>
    fun sendFile(text: String, fileUrl: String, fileName: String, fileSize: Long): Flow<ChatResult<Message>>
    
    // 消息管理
    fun editMessage(messageId: MessageId, newText: String): Flow<ChatResult<Message>>
    fun deleteMessage(messageId: MessageId): Flow<ChatResult<Unit>>
    fun recallMessage(messageId: MessageId): Flow<ChatResult<Unit>>
    fun markRead(messageId: MessageId)
    
    // 消息查询
    fun getMessageHistory(limit: Int = 50, before: Timestamp? = null): Flow<List<Message>>
    fun getPrivateMessageHistory(otherUserId: UserId, limit: Int = 50): Flow<List<Message>>
    fun getGroupMessageHistory(groupId: GroupId, limit: Int = 50): Flow<List<Message>>
    
    // 置顶与收藏
    fun pinMessage(messageId: MessageId, chatId: ChatId): Flow<ChatResult<Unit>>
    fun unpinMessage(messageId: MessageId, chatId: ChatId): Flow<ChatResult<Unit>>
    fun getPinnedMessages(chatId: ChatId): Flow<List<PinnedMessage>>
    fun saveMessage(messageId: MessageId, chatId: ChatId): Flow<ChatResult<Unit>>
    fun unsaveMessage(messageId: MessageId): Flow<ChatResult<Unit>>
    fun getSavedMessages(): Flow<List<SavedMessage>>
    
    // 用户
    fun getOnlineUsers(): Flow<List<User>>
    fun getUserInfo(userId: UserId): Flow<User?>
    
    // 群组
    fun createGroup(name: String): Flow<ChatResult<Group>>
    fun joinGroup(groupId: GroupId): Flow<ChatResult<Unit>>
    fun leaveGroup(groupId: GroupId): Flow<ChatResult<Unit>>
    fun getGroupMembers(groupId: GroupId): Flow<List<User>>
    fun kickUser(targetUserId: UserId, groupId: GroupId): Flow<ChatResult<Unit>>
    fun banUser(targetUserId: UserId, groupId: GroupId): Flow<ChatResult<Unit>>
    
    // 输入状态
    fun sendTyping()
    val typingUsers: Flow<Set<String>>
}

/**
 * 连接状态
 */
enum class ConnectionState {
    IDLE, CONNECTING, CONNECTED, RECONNECTING, DISCONNECTED, ERROR
}

/**
 * 结果密封类 - 类型安全
 */
sealed class ChatResult<out T> {
    data class Success<T>(val data: T) : ChatResult<T>()
    data class Failure(val exception: Throwable) : ChatResult<Nothing>()
    
    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure
    
    fun getOrNull(): T? = (this as? Success)?.data
    fun exceptionOrNull(): Throwable? = (this as? Failure)?.exception
}

/**
 * 扩展函数 - 内联优化
 */
inline fun <T> ChatResult<T>.onSuccess(action: (T) -> Unit): ChatResult<T> {
    if (this is ChatResult.Success) action(data)
    return this
}

inline fun <T> ChatResult<T>.onFailure(action: (Throwable) -> Unit): ChatResult<T> {
    exceptionOrNull()?.let(action)
    return this
}

inline fun <T, R> ChatResult<T>.map(transform: (T) -> R): ChatResult<R> = 
    when (this) {
        is ChatResult.Success -> ChatResult.Success(transform(data))
        is ChatResult.Failure -> this
    }
