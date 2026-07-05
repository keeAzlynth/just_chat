package com.course.imchat.domain.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.course.imchat.core.util.*

/**
 * 不可变领域模型 - Compose稳定性优化
 * 使用@Immutable注解告诉Compose编译器这些类不会改变
 */

@Immutable
data class User(
    val id: UserId,
    val username: String,
    val nickname: String,
    val avatarUrl: String = "",
    val level: UserLevel = UserLevel.MEMBER,
    val isOnline: Boolean = false,
    val lastActive: Timestamp = Timestamp.now(),
)

@Immutable
enum class UserLevel(val value: Int) {
    GUEST(0),
    MEMBER(1),
    VIP(2),
    MODERATOR(3),
    ADMIN(4),
    OWNER(5);
    
    companion object {
        fun fromInt(value: Int): UserLevel = 
            entries.firstOrNull { it.value == value } ?: GUEST
    }
}

@Immutable
data class Message(
    val id: MessageId,
    val senderId: UserId,
    val senderNickname: String,
    val text: String,
    val timestamp: Timestamp,
    val type: MessageType = MessageType.TEXT,
    val isMine: Boolean = false,
    val isPrivate: Boolean = false,
    val receiverId: UserId? = null,
    val groupId: GroupId? = null,
    val deliveryStatus: DeliveryStatus = DeliveryStatus.SENT,
    val isRecalled: Boolean = false,
    val recalledAt: Timestamp? = null,
    val quotedMessage: QuotedMessage? = null,
    val fileUrl: String = "",
    val fileName: String = "",
    val fileSize: Long = 0,
    val readBy: List<UserId> = emptyList(),
) {
    fun isRead(): Boolean = readBy.isNotEmpty()
    
    fun formattedFileSize(): String = when {
        fileSize < 1024 -> "$fileSize B"
        fileSize < 1024 * 1024 -> "${fileSize / 1024} KB"
        fileSize < 1024 * 1024 * 1024 -> "${fileSize / (1024 * 1024)} MB"
        else -> "${fileSize / (1024 * 1024 * 1024)} GB"
    }
}

@Immutable
enum class MessageType {
    TEXT, IMAGE, FILE, GROUP
}

@Immutable
enum class DeliveryStatus {
    SENDING, SENT, DELIVERED, READ
}

@Immutable
data class QuotedMessage(
    val id: MessageId,
    val nickname: String,
    val text: String,
)

@Immutable
data class Group(
    val id: GroupId,
    val name: String,
    val avatarUrl: String = "",
    val memberCount: Int = 0,
    val members: List<User> = emptyList(),
    val createdAt: Timestamp = Timestamp.now(),
)

@Immutable
data class ChatSession(
    val id: ChatId,
    val name: String,
    val lastMessage: String = "",
    val lastMessageTime: Timestamp? = null,
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
    val isGroup: Boolean = false,
)

@Immutable
data class PinnedMessage(
    val message: Message,
    val pinnedBy: String,
    val pinnedAt: Timestamp,
)

@Immutable
data class SavedMessage(
    val message: Message,
    val savedAt: Timestamp,
    val chatId: ChatId,
)
