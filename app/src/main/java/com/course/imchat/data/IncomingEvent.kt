package com.course.imchat.data

import com.course.imchat.OnlineUser

sealed interface IncomingEvent {
    data object Connected : IncomingEvent
    data object Disconnected : IncomingEvent
    data class Failure(val message: String) : IncomingEvent
    data class LoggedIn(val userId: String, val username: String, val nickname: String, val sessionToken: String = "") : IncomingEvent
    data class Joined(val userId: String, val nickname: String) : IncomingEvent
    data class Message(
        val messageId: Long = 0,
        val userId: String,
        val nickname: String,
        val text: String,
        val timestampSeconds: Long,
        val messageType: String = "text",
        val fileUrl: String = "",
        val fileName: String = "",
        val fileSize: Long = 0,
    ) : IncomingEvent
    data class PrivateMessage(
        val messageId: Long = 0,
        val userId: String,
        val nickname: String,
        val receiverId: String,
        val text: String,
        val timestampSeconds: Long,
    ) : IncomingEvent
    data class Typing(val userId: String, val nickname: String) : IncomingEvent
    data class MessageRecalled(val messageId: Long, val userId: String) : IncomingEvent
    data class MessageRead(val messageId: Long, val userId: String) : IncomingEvent
    data class OnlineUsers(val users: List<OnlineUser>) : IncomingEvent
    data class MessageHistory(val messages: List<MessageData>) : IncomingEvent
    data class PrivateMessageHistory(val otherUserId: String, val messages: List<MessageData>) : IncomingEvent
    data class GroupMessageHistory(val groupId: String, val messages: List<MessageData>) : IncomingEvent
    data class ServerError(val code: String, val message: String) : IncomingEvent
    
    // 群组相关事件
    data class GroupCreated(val groupId: String, val name: String, val ownerId: String) : IncomingEvent
    data class GroupMessage(
        val messageId: Long = 0,
        val groupId: String,
        val userId: String,
        val nickname: String,
        val text: String,
        val timestampSeconds: Long,
    ) : IncomingEvent
    data class GroupMemberJoined(val groupId: String, val userId: String, val nickname: String) : IncomingEvent
    data class GroupMemberLeft(val groupId: String, val userId: String, val nickname: String) : IncomingEvent
    data class GroupMembers(val groupId: String, val members: List<GroupMember>) : IncomingEvent
    data class UserKicked(val groupId: String, val userId: String, val kickedBy: String) : IncomingEvent
    data class UserBanned(val groupId: String, val userId: String, val bannedBy: String) : IncomingEvent

    // Edit & Delete
    data class MessageEdited(val messageId: Long, val userId: String, val text: String, val timestampSeconds: Long) : IncomingEvent
    data class MessageDeleted(val messageId: Long, val userId: String) : IncomingEvent

    // Pin & Save
    data class MessagePinned(val messageId: Long, val chatId: String, val pinnedBy: String, val timestampSeconds: Long) : IncomingEvent
    data class MessageUnpinned(val messageId: Long, val chatId: String) : IncomingEvent
    data class PinnedMessages(val chatId: String, val messages: List<MessageData>) : IncomingEvent
    data class MessageSaved(val messageId: Long, val chatId: String) : IncomingEvent
    data class MessageUnsaved(val messageId: Long) : IncomingEvent
    data class SavedMessages(val messages: List<MessageData>) : IncomingEvent

    // User Info
    data class UserInfo(
        val userId: String,
        val username: String,
        val nickname: String,
        val level: Int,
        val online: Boolean,
        val lastActive: Long,
    ) : IncomingEvent
}

data class MessageData(
    val userId: String,
    val nickname: String,
    val receiverId: String = "",
    val text: String,
    val timestampSeconds: Long,
    val type: String = "text",
    val fileUrl: String = "",
    val fileName: String = "",
    val fileSize: Long = 0,
    val isRecalled: Boolean = false,
)

data class GroupMember(
    val userId: String,
    val nickname: String,
    val level: Int = 0,
    val isAdmin: Boolean = false,
    val isOwner: Boolean = false,
)
