package com.course.imchat

import kotlinx.serialization.Serializable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

// ════════════════════════════════════════════════════════════
// Value classes — zero-overhead type safety
// Compiler erases them to the underlying type at runtime.
// Prevents accidental userId→messageId swaps, etc.
// ════════════════════════════════════════════════════════════

@JvmInline
value class UserId(val value: String) {
    override fun toString() = value
    companion object {
        val EMPTY = UserId("")
    }
}

@JvmInline
value class ChatId(val value: String) {
    override fun toString() = value
    companion object {
        val PUBLIC = ChatId("public")
    }
}

@JvmInline
value class MessageId(val value: String) {
    override fun toString() = value
    fun isLocal() = value.startsWith("local_")
}

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
@Serializable
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
@Serializable
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
        val instant = java.time.Instant.ofEpochSecond(timestampSeconds)
        val dt = java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault())
        return String.format("%02d:%02d", dt.hour, dt.minute)
    }

    fun formattedDate(): String {
        val instant = java.time.Instant.ofEpochSecond(timestampSeconds)
        val dt = java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault())
        return String.format("%d-%02d-%02d", dt.year, dt.monthValue, dt.dayOfMonth)
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
@Serializable
@Immutable
data class OnlineUser(
    val userId: String,
    val nickname: String,
    val avatarUrl: String = "",
)

/**
 * 聊天群组
 */
@Serializable
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
@Serializable
@Immutable
data class UnreadCount(
    val chatId: String = "",
    val count: Int = 0,
    val lastMessageTime: Long = 0,
)

/**
 * 消息转发目标
 */
@Serializable
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
@Serializable
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
@Serializable
@Immutable
data class SavedMessage(
    val messageId: String,
    val chatId: String,
    val message: ChatMessage,
    val savedAt: Long = System.currentTimeMillis() / 1000,
    val tags: List<String> = emptyList(),
)

/** A poll's live vote tracking (renamed to PollData to avoid collision with PollUiState) */
@Immutable
data class PollData(
    val votes: Map<String, Set<String>> = emptyMap(),  // optionId -> set of userIds
    val isClosed: Boolean = false,
)

// ── Sub-State Data Classes (v2.1: ChatUiState split) ────────

/**
 * 认证与连接子状态
 * 涵盖：登录/注册、WebSocket 连接、错误信息
 */
@Stable
data class AuthState(
    val serverUrl: String = "",
    val authStatus: AuthStatus = AuthStatus.NotAuthenticated,
    val isLoginMode: Boolean = true,
    val username: String = "",
    val password: String = "",
    val nickname: String = "",
    val myUserId: String? = null,
    val connectionStatus: ConnectionStatus = ConnectionStatus.Idle,
    val errorMessage: String? = null,
) {
    val canAuth: Boolean
        get() = username.trim().isNotEmpty() &&
                password.trim().isNotEmpty() &&
                (isLoginMode || nickname.trim().isNotEmpty())

    val isConnected: Boolean
        get() = connectionStatus is ConnectionStatus.Connected

    val isConnecting: Boolean
        get() = connectionStatus is ConnectionStatus.Connecting ||
                connectionStatus is ConnectionStatus.Reconnecting
}

/**
 * 聊天核心子状态
 * 涵盖：消息草稿、在线用户、私聊/群聊上下文、置顶、未读计数
 */
@Stable
data class ChatState(
    val joined: Boolean = false,
    val draft: String = "",
    val typingUsers: Set<String> = emptySet(),
    val onlineUsers: Map<String, OnlineUser> = emptyMap(),  // O(1) by userId
    val selectedPrivateUser: OnlineUser? = null,
    val selectedUserOnline: Boolean = false,
    val selectedUserLastSeen: Long = 0,
    val quotingMessage: ChatMessage? = null,
    val editingMessage: ChatMessage? = null,
    val unreadCounts: Map<String, UnreadCount> = emptyMap(),
    val totalUnreadCount: Int = 0,
    val pinnedMessages: Map<String, PinnedMessage> = emptyMap(),
    val isLoadingMore: Boolean = false,
    val hasMoreMessages: Boolean = true,
    val currentChatId: String = "public",
    val unreadBoundaryId: String? = null,
) {
    val canSend: Boolean
        get() = joined && draft.trim().isNotEmpty()

    val currentPinnedMessage: PinnedMessage?
        get() = pinnedMessages[currentChatId]
}

/**
 * UI 面板/弹窗子状态
 * 涵盖：所有 showXxx 开关、搜索、附件、语音录制、反应、提及、多选
 */
@Stable
data class UiState(
    val showOnlineUsers: Boolean = false,
    val isSearching: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<ChatMessage> = emptyList(),
    val isDarkMode: Boolean = true,
    val showForwardDialog: Boolean = false,
    val forwardTargets: List<ForwardTarget> = emptyList(),
    val forwardingMessage: ChatMessage? = null,
    val showSavedMessages: Boolean = false,
    val showAttachmentSheet: Boolean = false,
    val showScrollFab: Boolean = false,
    val showReactionPicker: Boolean = false,
    val reactingToMessageId: String? = null,
    val showCreateGroupDialog: Boolean = false,
    val createGroupName: String = "",
    val showMentionPicker: Boolean = false,
    val mentionCandidates: List<OnlineUser> = emptyList(),
    val isMultiSelectMode: Boolean = false,
    val selectedMessages: Set<String> = emptySet(),
    val showGroupManagement: Boolean = false,
    val localImageUri: String = "",
    val localFileName: String = "",
    val localFileSize: Long = 0,
    val isVoiceRecording: Boolean = false,
    val recordingSeconds: Float = 0f,
    val smartSuggestions: List<String> = emptyList(),
    val screenshotProtection: Boolean = true,
) {
    val selectedMessageCount: Int
        get() = selectedMessages.size
}

/**
 * 投票子状态
 * 涵盖：活跃投票数据、创建投票对话框
 */
@Stable
data class PollUiState(
    val activePolls: Map<String, PollData> = emptyMap(),  // messageId -> PollData
    val showCreatePollDialog: Boolean = false,
    val pollTitle: String = "",
    val pollOptions: String = "",
)

/**
 * 导航/群组子状态
 * 涵盖：群组列表、当前选中群组、收藏消息
 */
@Stable
data class NavigationState(
    val groups: List<ChatGroup> = emptyList(),
    val selectedGroup: ChatGroup? = null,
    val savedMessages: List<SavedMessage> = emptyList(),
)

// ── Composite ChatUiState (v2.1 split) ──────────────────────

/**
 * 聊天UI状态 — 复合体，包含 5 个子状态
 *
 * 每次更新只 copy 变化的子 State，其余子 State 引用不变，
 * Compose 可据此跳过无关子树的重组。
 *
 * 所有历史字段通过 forwarding 计算属性保持向后兼容。
 */
@Stable
data class ChatUiState(
    val auth: AuthState = AuthState(),
    val chat: ChatState = ChatState(),
    val ui: UiState = UiState(),
    val poll: PollUiState = PollUiState(),
    val nav: NavigationState = NavigationState(),
) {
    // ── Forwarding properties (backward compatibility) ──────
    // AuthState
    val serverUrl get() = auth.serverUrl
    val authStatus get() = auth.authStatus
    val isLoginMode get() = auth.isLoginMode
    val username get() = auth.username
    val password get() = auth.password
    val nickname get() = auth.nickname
    val myUserId get() = auth.myUserId
    val connectionStatus get() = auth.connectionStatus
    val errorMessage get() = auth.errorMessage
    val canAuth get() = auth.canAuth
    val isConnected get() = auth.isConnected
    val isConnecting get() = auth.isConnecting

    // ChatState
    val joined get() = chat.joined
    val draft get() = chat.draft
    val typingUsers get() = chat.typingUsers
    val onlineUsers get() = chat.onlineUsers
    val selectedPrivateUser get() = chat.selectedPrivateUser
    val selectedUserOnline get() = chat.selectedUserOnline
    val selectedUserLastSeen get() = chat.selectedUserLastSeen
    val quotingMessage get() = chat.quotingMessage
    val editingMessage get() = chat.editingMessage
    val unreadCounts get() = chat.unreadCounts
    val totalUnreadCount get() = chat.totalUnreadCount
    val pinnedMessages get() = chat.pinnedMessages
    val isLoadingMore get() = chat.isLoadingMore
    val hasMoreMessages get() = chat.hasMoreMessages
    val currentChatId get() = chat.currentChatId
    val unreadBoundaryId get() = chat.unreadBoundaryId
    val canSend get() = chat.canSend
    val currentPinnedMessage get() = chat.currentPinnedMessage

    // UiState
    val showOnlineUsers get() = ui.showOnlineUsers
    val isSearching get() = ui.isSearching
    val searchQuery get() = ui.searchQuery
    val searchResults get() = ui.searchResults
    val isDarkMode get() = ui.isDarkMode
    val showForwardDialog get() = ui.showForwardDialog
    val forwardTargets get() = ui.forwardTargets
    val forwardingMessage get() = ui.forwardingMessage
    val showSavedMessages get() = ui.showSavedMessages
    val showAttachmentSheet get() = ui.showAttachmentSheet
    val showScrollFab get() = ui.showScrollFab
    val showReactionPicker get() = ui.showReactionPicker
    val reactingToMessageId get() = ui.reactingToMessageId
    val showCreateGroupDialog get() = ui.showCreateGroupDialog
    val createGroupName get() = ui.createGroupName
    val showMentionPicker get() = ui.showMentionPicker
    val mentionCandidates get() = ui.mentionCandidates
    val isMultiSelectMode get() = ui.isMultiSelectMode
    val selectedMessages get() = ui.selectedMessages
    val showGroupManagement get() = ui.showGroupManagement
    val localImageUri get() = ui.localImageUri
    val localFileName get() = ui.localFileName
    val localFileSize get() = ui.localFileSize
    val isVoiceRecording get() = ui.isVoiceRecording
    val recordingSeconds get() = ui.recordingSeconds
    val smartSuggestions get() = ui.smartSuggestions
    val screenshotProtection get() = ui.screenshotProtection
    val selectedMessageCount get() = ui.selectedMessageCount

    // PollUiState
    val activePolls: Map<String, PollData> get() = poll.activePolls
    val showCreatePollDialog get() = poll.showCreatePollDialog
    val pollTitle get() = poll.pollTitle
    val pollOptions get() = poll.pollOptions

    // NavigationState
    val groups get() = nav.groups
    val selectedGroup get() = nav.selectedGroup
    val savedMessages get() = nav.savedMessages
}
