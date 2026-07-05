package com.course.imchat.domain.usecase

import com.course.imchat.core.util.*
import com.course.imchat.domain.model.*
import com.course.imchat.domain.repository.ChatRepository
import com.course.imchat.domain.repository.ChatResult
import kotlinx.coroutines.flow.Flow

/**
 * 用例基类
 */
abstract class UseCase<in Params, out Result> {
    abstract operator fun invoke(params: Params): Result
}

/**
 * 无参数用例
 */
abstract class NoParamsUseCase<out Result> {
    abstract operator fun invoke(): Result
}

/**
 * 消息用例
 */
class SendMessageUseCase(
    private val repository: ChatRepository,
) {
    operator fun invoke(text: String): Flow<ChatResult<Message>> = 
        repository.sendMessage(text)
}

class SendPrivateMessageUseCase(
    private val repository: ChatRepository,
) {
    operator fun invoke(text: String, receiverId: UserId): Flow<ChatResult<Message>> = 
        repository.sendPrivateMessage(text, receiverId)
}

class SendGroupMessageUseCase(
    private val repository: ChatRepository,
) {
    operator fun invoke(text: String, groupId: GroupId): Flow<ChatResult<Message>> = 
        repository.sendGroupMessage(text, groupId)
}

class EditMessageUseCase(
    private val repository: ChatRepository,
) {
    operator fun invoke(messageId: MessageId, newText: String): Flow<ChatResult<Message>> = 
        repository.editMessage(messageId, newText)
}

class DeleteMessageUseCase(
    private val repository: ChatRepository,
) {
    operator fun invoke(messageId: MessageId): Flow<ChatResult<Unit>> = 
        repository.deleteMessage(messageId)
}

class RecallMessageUseCase(
    private val repository: ChatRepository,
) {
    operator fun invoke(messageId: MessageId): Flow<ChatResult<Unit>> = 
        repository.recallMessage(messageId)
}

/**
 * 获取消息历史用例
 */
class GetMessageHistoryUseCase(
    private val repository: ChatRepository,
) {
    operator fun invoke(limit: Int = 50, before: Timestamp? = null): Flow<List<Message>> = 
        repository.getMessageHistory(limit, before)
}

class GetPrivateMessageHistoryUseCase(
    private val repository: ChatRepository,
) {
    operator fun invoke(otherUserId: UserId, limit: Int = 50): Flow<List<Message>> = 
        repository.getPrivateMessageHistory(otherUserId, limit)
}

class GetGroupMessageHistoryUseCase(
    private val repository: ChatRepository,
) {
    operator fun invoke(groupId: GroupId, limit: Int = 50): Flow<List<Message>> = 
        repository.getGroupMessageHistory(groupId, limit)
}

/**
 * 置顶与收藏用例
 */
class PinMessageUseCase(
    private val repository: ChatRepository,
) {
    operator fun invoke(messageId: MessageId, chatId: ChatId): Flow<ChatResult<Unit>> = 
        repository.pinMessage(messageId, chatId)
}

class SaveMessageUseCase(
    private val repository: ChatRepository,
) {
    operator fun invoke(messageId: MessageId, chatId: ChatId): Flow<ChatResult<Unit>> = 
        repository.saveMessage(messageId, chatId)
}

/**
 * 用户用例
 */
class GetOnlineUsersUseCase(
    private val repository: ChatRepository,
) {
    operator fun invoke(): Flow<List<User>> = 
        repository.getOnlineUsers()
}

class GetUserInfoUseCase(
    private val repository: ChatRepository,
) {
    operator fun invoke(userId: UserId): Flow<User?> = 
        repository.getUserInfo(userId)
}

/**
 * 群组用例
 */
class CreateGroupUseCase(
    private val repository: ChatRepository,
) {
    operator fun invoke(name: String): Flow<ChatResult<Group>> = 
        repository.createGroup(name)
}

class JoinGroupUseCase(
    private val repository: ChatRepository,
) {
    operator fun invoke(groupId: GroupId): Flow<ChatResult<Unit>> = 
        repository.joinGroup(groupId)
}

/**
 * 连接用例
 */
class ConnectUseCase(
    private val repository: ChatRepository,
) {
    operator fun invoke(url: String) = repository.connect(url)
}

class LoginUseCase(
    private val repository: ChatRepository,
) {
    operator fun invoke(username: String, password: String): Flow<ChatResult<UserId>> = 
        repository.login(username, password)
}

class RegisterUseCase(
    private val repository: ChatRepository,
) {
    operator fun invoke(username: String, password: String, nickname: String): Flow<ChatResult<UserId>> = 
        repository.register(username, password, nickname)
}
