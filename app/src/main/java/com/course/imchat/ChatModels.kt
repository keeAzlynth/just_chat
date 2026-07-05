package com.course.imchat

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

/**
 * 连接状态 - 使用sealed interface (Kotlin 1.5+)
 * 比sealed class更轻量，支持多继承
 */
sealed interface ConnectionStatus {
    data object Idle : ConnectionStatus
    data object Connecting : ConnectionStatus
    data object Connected : ConnectionStatus
    data object Reconnecting : ConnectionStatus
    data object Disconnected : ConnectionStatus
    data class Error(val message: String) : ConnectionStatus
}

/**
 * 投递状态
 */
enum class DeliveryStatus {
    Sending, Sent, Delivered, Read
}

/**
 * 消息类型
 */
enum class MessageType {
    Text, Image, File
}

/**
 * 认证状态 - 使用sealed interface
 */
sealed interface AuthStatus {
    data object NotAuthenticated : AuthStatus
    data object Authenticating : AuthStatus
    data object Authenticated : AuthStatus
    data class Error(val message: String) : AuthStatus
}

/**
 * 引用消息 - 不可变数据类
 */
@Immutable
data class QuotedMessage(
    val id: String,
    val nickname: String,
    val text: String,
)

/**
 * 聊天消息 - 不可变数据类
 * @Immutable告诉Compose编译器这个类不会改变
 */
@Immutable
data class ChatMessage(
    val id: String,
    val userId: String,
    val nickname: String,
    val text: String,
    val timestampSeconds: Long,
    val isMine: Boolean,
    val isPrivate: Boolean = false,
    val receiverId: String = "",
    val groupId: String = "",
    val deliveryStatus: DeliveryStatus = DeliveryStatus.Delivered,
    val readBy: List<String> = emptyList(),
    val messageType: MessageType = MessageType.Text,
    val fileUrl: String = "",
    val fileName: String = "",
    val fileSize: Long = 0,
    val avatarUrl: String = "",
    val isRecalled: Boolean = false,
    val recalledAt: Long = 0,
    val reactions: Map<String, Int> = emptyMap(),
    val myReaction: String = "",
    val quotedMessage: QuotedMessage? = null,
) {
    /**
     * 使用Kotlin的字符串模板和when表达式
     */
    fun formattedTime(): String {
        val date = java.util.Date(timestampSeconds * 1000)
        return java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(date)
    }
    
    fun formattedDate(): String {
        val date = java.util.Date(timestampSeconds * 1000)
        return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(date)
    }
    
    fun isRead(): Boolean = readBy.isNotEmpty()
    
    /**
     * 使用when表达式替代if-else链
     */
    fun formattedFileSize(): String = when {
        fileSize < 1024 -> "$fileSize B"
        fileSize < 1024 * 1024 -> "${fileSize / 1024} KB"
        fileSize < 1024 * 1024 * 1024 -> "${fileSize / (1024 * 1024)} MB"
        else -> "${fileSize / (1024 * 1024 * 1024)} GB"
    }
}

/**
 * 在线用户
 */
@Immutable
data class OnlineUser(
    val userId: String,
    val nickname: String,
    val avatarUrl: String = "",
)

/**
 * 聊天群组
 */
@Immutable
data class ChatGroup(
    val groupId: String,
    val groupName: String,
    val groupAvatar: String = "",
    val memberCount: Int = 0,
    val members: List<OnlineUser> = emptyList(),
    val createdAt: Long = 0,
)

/**
 * 未读消息计数
 */
@Immutable
data class UnreadCount(
    val chatId: String = "",
    val count: Int = 0,
    val lastMessageTime: Long = 0,
)

/**
 * 消息转发目标
 */
@Immutable
data class ForwardTarget(
    val userId: String = "",
    val nickname: String = "",
    val groupId: String = "",
    val groupName: String = "",
    val isGroup: Boolean = false,
)

/**
 * 置顶消息
 */
@Immutable
data class PinnedMessage(
    val messageId: String,
    val chatId: String,
    val message: ChatMessage,
    val pinnedBy: String,
    val pinnedAt: Long = System.currentTimeMillis() / 1000,
)

/**
 * 收藏消息
 */
@Immutable
data class SavedMessage(
    val messageId: String,
    val chatId: String,
    val message: ChatMessage,
    val savedAt: Long = System.currentTimeMillis() / 1000,
    val tags: List<String> = emptyList(),
)

/**
 * 聊天UI状态 - 使用@Stable注解
 * @Stable告诉Compose这个类的equals方法是稳定的
 */
@Stable
data class ChatUiState(
    val serverUrl: String = "",
    val authStatus: AuthStatus = AuthStatus.NotAuthenticated,
    val isLoginMode: Boolean = true,
    val username: String = "",
    val password: String = "",
    val nickname: String = "",
    val myUserId: String? = null,
    val joined: Boolean = false,
    val connectionStatus: ConnectionStatus = ConnectionStatus.Idle,
    val draft: String = "",
    val typingUsers: Set<String> = emptySet(),
    val onlineUsers: List<OnlineUser> = emptyList(),
    val errorMessage: String? = null,
    val showOnlineUsers: Boolean = false,
    val selectedPrivateUser: OnlineUser? = null,
    val isSearching: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<ChatMessage> = emptyList(),
    val isDarkMode: Boolean = true,
    val groups: List<ChatGroup> = emptyList(),
    val selectedGroup: ChatGroup? = null,
    val showGroupManagement: Boolean = false,
    val unreadCounts: Map<String, UnreadCount> = emptyMap(),
    val totalUnreadCount: Int = 0,
    val quotingMessage: ChatMessage? = null,
    val editingMessage: ChatMessage? = null,
    val forwardingMessage: ChatMessage? = null,
    val forwardTargets: List<ForwardTarget> = emptyList(),
    val showForwardDialog: Boolean = false,
    val pinnedMessages: Map<String, PinnedMessage> = emptyMap(),
    val savedMessages: List<SavedMessage> = emptyList(),
    val showSavedMessages: Boolean = false,
    val selectedMessages: Set<String> = emptySet(),
    val isMultiSelectMode: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMoreMessages: Boolean = true,
    val currentChatId: String = "public",
    // Attachment
    val showAttachmentSheet: Boolean = false,
    val localImageUri: String = "",
    val localFileName: String = "",
    val localFileSize: Long = 0,
    // Scroll / unread
    val showScrollFab: Boolean = false,
    val unreadBoundaryId: String? = null,
    // Reaction picker
    val showReactionPicker: Boolean = false,
    val reactingToMessageId: String? = null,
    // Create group dialog
    val showCreateGroupDialog: Boolean = false,
    val createGroupName: String = "",
    // Smart replies
    val smartSuggestions: List<String> = emptyList(),
    // Online status of selected private chat user
    val selectedUserOnline: Boolean = false,
    val selectedUserLastSeen: Long = 0,
    // @Mention
    val showMentionPicker: Boolean = false,
    val mentionCandidates: List<OnlineUser> = emptyList(),
    // Voice recording
    val isVoiceRecording: Boolean = false,
    val recordingSeconds: Float = 0f,
) {
    /**
     * 使用Kotlin的属性访问语法
     */
    val canAuth: Boolean 
        get() = username.trim().isNotEmpty() && 
                password.trim().isNotEmpty() &&
                (isLoginMode || nickname.trim().isNotEmpty())
    
    val canSend: Boolean 
        get() = joined && draft.trim().isNotEmpty() && connectionStatus is ConnectionStatus.Connected
    
    val currentPinnedMessage: PinnedMessage? 
        get() = pinnedMessages[currentChatId]
    
    val selectedMessageCount: Int 
        get() = selectedMessages.size
    
    /**
     * 使用when表达式检查连接状态
     */
    val isConnected: Boolean
        get() = connectionStatus is ConnectionStatus.Connected
    
    val isConnecting: Boolean
        get() = connectionStatus is ConnectionStatus.Connecting || connectionStatus is ConnectionStatus.Reconnecting
}
