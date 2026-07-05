package com.course.imchat.presentation

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.course.imchat.core.util.*
import com.course.imchat.domain.model.*
import com.course.imchat.domain.repository.ChatRepository
import com.course.imchat.domain.repository.ChatResult
import com.course.imchat.domain.repository.ConnectionState
import com.course.imchat.domain.repository.onFailure
import com.course.imchat.domain.repository.onSuccess
import com.course.imchat.domain.usecase.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 优化的ChatViewModel
 * 参考Signal和Tachiyomi的架构
 */
@Stable
class ChatViewModelOptimized(
    private val repository: ChatRepository,
    private val sendMessageUseCase: SendMessageUseCase,
    private val editMessageUseCase: EditMessageUseCase,
    private val deleteMessageUseCase: DeleteMessageUseCase,
    private val getMessageHistoryUseCase: GetMessageHistoryUseCase,
    private val pinMessageUseCase: PinMessageUseCase,
    private val saveMessageUseCase: SaveMessageUseCase,
    private val getOnlineUsersUseCase: GetOnlineUsersUseCase,
) : BaseViewModel() {
    
    // 状态流 - 使用StateFlow避免不必要的重组
    private val _uiState = mutableStateFlow(ChatUiStateOptimized())
    val uiState: StateFlow<ChatUiStateOptimized> = _uiState.asStateFlow()
    
    // 一次性事件
    private val _events = mutableSharedFlow<ChatEvent>()
    val events: SharedFlow<ChatEvent> = _events.asSharedFlow()
    
    init {
        // 收集连接状态
        launchMain {
            repository.connectionState.collect { state ->
                _uiState.update { it.copy(connectionState = state) }
            }
        }
        
        // 收集输入状态
        launchMain {
            repository.typingUsers.collect { users ->
                _uiState.update { it.copy(typingUsers = users.toStable()) }
            }
        }
    }
    
    // ── 连接操作 ──────────────────────────────────────────
    
    fun connect(url: String) {
        _uiState.update { it.copy(isConnecting = true) }
        repository.connect(url)
    }
    
    fun disconnect() {
        repository.disconnect()
    }
    
    // ── 认证操作 ──────────────────────────────────────────
    
    fun login(username: String, password: String) {
        launchIO {
            repository.login(username, password).collect { result: ChatResult<UserId> ->
                result.onSuccess { userId: UserId ->
                    _uiState.update { it.copy(userId = userId, isLoggedIn = true) }
                    _events.emit(ChatEvent.LoginSuccess)
                }.onFailure { error: Throwable ->
                    _events.emit(ChatEvent.Error(error.message ?: "登录失败"))
                }
            }
        }
    }
    
    fun register(username: String, password: String, nickname: String) {
        launchIO {
            repository.register(username, password, nickname).collect { result: ChatResult<UserId> ->
                result.onSuccess { userId: UserId ->
                    _uiState.update { it.copy(userId = userId, isLoggedIn = true) }
                    _events.emit(ChatEvent.RegisterSuccess)
                }.onFailure { error: Throwable ->
                    _events.emit(ChatEvent.Error(error.message ?: "注册失败"))
                }
            }
        }
    }
    
    fun logout() {
        repository.logout()
        _uiState.update { ChatUiStateOptimized() }
    }
    
    // ── 消息操作 ──────────────────────────────────────────
    
    fun sendMessage(text: String) {
        if (text.isBlank()) return
        
        launchIO {
            sendMessageUseCase(text).collect { result: ChatResult<Message> ->
                result.onSuccess { message: Message ->
                    // 本地立即显示
                    _uiState.update { state ->
                        state.copy(messages = (state.messages + message).toStable())
                    }
                }.onFailure { error: Throwable ->
                    launchMain {
                        _events.emit(ChatEvent.Error("发送失败: ${error.message}"))
                    }
                }
            }
        }
    }
    
    fun editMessage(messageId: MessageId, newText: String) {
        launchIO {
            editMessageUseCase(messageId, newText).collect { result: ChatResult<Message> ->
                result.onSuccess { message: Message ->
                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages.value.fastMap { msg ->
                                if (msg.id == messageId) message else msg
                            }.toStable()
                        )
                    }
                }
            }
        }
    }
    
    fun deleteMessage(messageId: MessageId) {
        launchIO {
            deleteMessageUseCase(messageId).collect { result: ChatResult<Unit> ->
                result.onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages.value.fastFilter { it.id != messageId }.toStable()
                        )
                    }
                }
            }
        }
    }
    
    // ── 消息查询 ──────────────────────────────────────────
    
    fun loadMessages(limit: Int = 50, before: Timestamp? = null) {
        launchIO {
            getMessageHistoryUseCase(limit, before).collect { messages: List<Message> ->
                _uiState.update { state ->
                    if (before != null) {
                        // 追加旧消息
                        state.copy(
                            messages = (messages + state.messages.value).toStable(),
                            hasMoreMessages = messages.size >= limit,
                        )
                    } else {
                        // 新消息
                        state.copy(messages = messages.toStable())
                    }
                }
            }
        }
    }
    
    // ── 置顶与收藏 ──────────────────────────────────────
    
    fun pinMessage(messageId: MessageId, chatId: ChatId) {
        launchIO {
            pinMessageUseCase(messageId, chatId).collect { result: ChatResult<Unit> ->
                result.onSuccess {
                    _events.emit(ChatEvent.MessagePinned)
                }
            }
        }
    }
    
    fun saveMessage(messageId: MessageId, chatId: ChatId) {
        launchIO {
            saveMessageUseCase(messageId, chatId).collect { result: ChatResult<Unit> ->
                result.onSuccess {
                    _events.emit(ChatEvent.MessageSaved)
                }
            }
        }
    }
    
    // ── 用户操作 ──────────────────────────────────────────
    
    fun loadOnlineUsers() {
        launchIO {
            getOnlineUsersUseCase().collect { users: List<User> ->
                _uiState.update { it.copy(onlineUsers = users.toStable()) }
            }
        }
    }
    
    // ── 输入状态 ──────────────────────────────────────────
    
    fun updateDraft(text: String) {
        _uiState.update { it.copy(draft = text) }
        repository.sendTyping()
    }
    
    fun clearDraft() {
        _uiState.update { it.copy(draft = "") }
    }
}

/**
 * 优化的UI状态 - 使用@Immutable注解
 */
@Immutable
data class ChatUiStateOptimized(
    val connectionState: ConnectionState = ConnectionState.IDLE,
    val isLoggedIn: Boolean = false,
    val userId: UserId? = null,
    val nickname: String = "",
    val messages: StableList<Message> = StableList.empty(),
    val onlineUsers: StableList<User> = StableList.empty(),
    val typingUsers: StableSet<String> = StableSet.empty(),
    val draft: String = "",
    val isConnecting: Boolean = false,
    val hasMoreMessages: Boolean = true,
    val currentChatId: ChatId = ChatId(ChatId.PUBLIC),
    val selectedMessages: StableSet<MessageId> = StableSet.empty(),
    val isMultiSelectMode: Boolean = false,
    val pinnedMessages: StableMap<ChatId, PinnedMessage> = StableMap.empty(),
    val savedMessages: StableList<SavedMessage> = StableList.empty(),
    val errorMessage: String? = null,
) {
    val canSend: Boolean get() = draft.isNotBlank() && connectionState == ConnectionState.CONNECTED
    val selectedMessageCount: Int get() = selectedMessages.value.size
}

/**
 * 一次性事件
 */
sealed interface ChatEvent {
    data object LoginSuccess : ChatEvent
    data object RegisterSuccess : ChatEvent
    data object MessagePinned : ChatEvent
    data object MessageSaved : ChatEvent
    data class Error(val message: String) : ChatEvent
}
