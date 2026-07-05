package com.course.imchat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import com.course.imchat.AuthStatus
import com.course.imchat.ChatGroup
import com.course.imchat.ChatUiState
import com.course.imchat.ConnectionStatus
import com.course.imchat.OnlineUser
import com.course.imchat.ChatMessage
import com.course.imchat.ui.components.home.ContactsScreen
import com.course.imchat.ui.components.home.HomeScreen
import com.course.imchat.ui.components.home.ProfileScreen

// 导航状态
enum class Screen {
    Home,
    Chat,
    Contacts,
    Profile,
    Settings,
}

// ── Root ────────────────────────────────────────────────────
@Composable
fun ChatApp(
    state: ChatUiState,
    messages: List<ChatMessage>,
    onServerUrlChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onNicknameChange: (String) -> Unit,
    onToggleAuthMode: () -> Unit,
    onConnect: () -> Unit,
    onLogin: () -> Unit,
    onRegister: () -> Unit,
    onLogout: () -> Unit,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onReconnect: () -> Unit,
    onDismissError: () -> Unit,
    onToggleOnlineUsers: () -> Unit,
    onSelectPrivateUser: (OnlineUser?) -> Unit,
    onAttachFile: () -> Unit,
    onPickImage: () -> Unit,
    onTakePhoto: () -> Unit,
    onPickFile: () -> Unit,
    onToggleSearch: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onRecallMessage: (String) -> Unit,
    onToggleDarkMode: () -> Unit,
    onToggleGroupManagement: () -> Unit,
    onCreateGroup: (String) -> Unit,
    onSelectGroup: (ChatGroup?) -> Unit,
    onJoinGroup: (String) -> Unit,
    onLeaveGroup: (String) -> Unit,
    onJoin: () -> Unit,
    onQuoteMessage: (ChatMessage) -> Unit = {},
    onCancelQuote: () -> Unit = {},
    onDeleteMessage: (String) -> Unit = {},
    onEditMessage: (ChatMessage) -> Unit = {},
    onCancelEditMessage: () -> Unit = {},
    onSaveEditMessage: () -> Unit = {},
    onForwardMessage: (ChatMessage) -> Unit = {},
    onPinMessage: (ChatMessage) -> Unit = {},
    onSaveToCollection: (ChatMessage) -> Unit = {},
    onSelectMessage: (ChatMessage) -> Unit = {},
    onToggleMultiSelect: () -> Unit = {},
    onDeleteSelected: () -> Unit = {},
    onForwardSelected: () -> Unit = {},
    onCancelMultiSelect: () -> Unit = {},
    onSelectAll: () -> Unit = {},
    onUnpinCurrent: () -> Unit = {},
    onAddReaction: (String, String) -> Unit = { _, _ -> },
    onShowReactionPicker: (String) -> Unit = {},
    onDismissReactionPicker: () -> Unit = {},
    onShowCreateGroupDialog: () -> Unit = {},
    onDismissCreateGroupDialog: () -> Unit = {},
    onCreateGroupNameChange: (String) -> Unit = {},
    onConfirmCreateGroup: (String) -> Unit = {},
    onShowMentionPicker: () -> Unit = {},
    onDismissMentionPicker: () -> Unit = {},
    onSelectMention: (OnlineUser) -> Unit = {},
    onStartVoiceRecording: () -> Unit = {},
    onStopVoiceRecording: (Boolean) -> Unit = {},
) {
    var currentScreen by remember { mutableStateOf(Screen.Home) }
    var selectedUser by remember { mutableStateOf<OnlineUser?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // ── Back gesture: go to previous screen, not exit app ──
    BackHandler(enabled = currentScreen != Screen.Home) {
        currentScreen = Screen.Home
    }

    LaunchedEffect(state.errorMessage) {
        val msg = state.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        onDismissError()
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent,
        bottomBar = {
            // 底部导航栏（仅在主页面显示）
            if (state.connectionStatus is ConnectionStatus.Connected &&
                state.authStatus is AuthStatus.Authenticated &&
                state.joined &&
                currentScreen != Screen.Settings
            ) {
                NavigationBar {
                    NavigationBarItem(
                        icon = {
                            if (state.totalUnreadCount > 0) {
                                BadgedBox(badge = {
                                    Badge {
                                        Text(
                                            text = if (state.totalUnreadCount > 99) "99+" 
                                                   else state.totalUnreadCount.toString(),
                                            fontSize = 10.sp,
                                        )
                                    }
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "消息")
                                }
                            } else {
                                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "消息")
                            }
                        },
                        label = { Text("消息") },
                        selected = currentScreen == Screen.Home || currentScreen == Screen.Chat,
                        onClick = { currentScreen = Screen.Home },
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.People, contentDescription = "联系人") },
                        label = { Text("联系人") },
                        selected = currentScreen == Screen.Contacts,
                        onClick = { currentScreen = Screen.Contacts },
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Person, contentDescription = "我的") },
                        label = { Text("我的") },
                        selected = currentScreen == Screen.Profile,
                        onClick = {
                            selectedUser = OnlineUser(
                                userId = state.myUserId.orEmpty(),
                                nickname = state.nickname,
                            )
                            currentScreen = Screen.Profile
                        },
                    )
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
        ) {
            when {
                state.connectionStatus !is ConnectionStatus.Connected ->
                    ConnectScreen(state, onServerUrlChange, onConnect, onReconnect)
                state.authStatus !is AuthStatus.Authenticated ->
                    AuthScreen(state, onUsernameChange, onPasswordChange, onNicknameChange, onToggleAuthMode, onLogin, onRegister)
                !state.joined ->
                    JoinScreen(state, onNicknameChange, onJoin)
                else -> {
                    when (currentScreen) {
                        Screen.Home -> {
                            HomeScreen(
                                state = state,
                                messages = messages,
                                onSelectPublicChat = {
                                    onSelectPrivateUser(null)
                                    currentScreen = Screen.Chat
                                },
                                onSelectPrivateChat = { user ->
                                    onSelectPrivateUser(user)
                                    currentScreen = Screen.Chat
                                },
                                onSelectGroup = { group ->
                                    onSelectGroup(group)
                                    currentScreen = Screen.Chat
                                },
                                onCreateGroup = {
                                    onShowCreateGroupDialog()
                                },
                                onRefresh = {
                                    onToggleOnlineUsers()
                                },
                            )
                        }
                        Screen.Chat -> {
                            // Build ChatActions from available callbacks
                            val chatActions = remember(
                                onDraftChange, onSend, onReconnect, onLogout,
                                onToggleOnlineUsers, onSelectPrivateUser, onAttachFile,
                                onPickImage, onTakePhoto, onPickFile, onToggleSearch,
                                onSearchQueryChange, onRecallMessage, onSelectGroup,
                                onCreateGroup, onQuoteMessage, onCancelQuote,
                                onDeleteMessage, onEditMessage, onCancelEditMessage,
                                onSaveEditMessage, onForwardMessage, onPinMessage,
                                onSaveToCollection, onSelectMessage, onToggleMultiSelect,
                                onDeleteSelected, onForwardSelected, onCancelMultiSelect,
                                onSelectAll, onUnpinCurrent, onAddReaction,
                                onShowReactionPicker, onDismissReactionPicker,
                                onShowMentionPicker, onDismissMentionPicker, onSelectMention,
                                onStartVoiceRecording, onStopVoiceRecording,
                            ) {
                                ChatActions(
                                    onDraftChange = onDraftChange,
                                    onSend = onSend,
                                    onReconnect = onReconnect,
                                    onLogout = onLogout,
                                    onToggleOnlineUsers = onToggleOnlineUsers,
                                    onSelectPrivateUser = onSelectPrivateUser,
                                    onAttachFile = onAttachFile,
                                    onPickImage = onPickImage,
                                    onTakePhoto = onTakePhoto,
                                    onPickFile = onPickFile,
                                    onToggleSearch = onToggleSearch,
                                    onSearchQueryChange = onSearchQueryChange,
                                    onRecallMessage = onRecallMessage,
                                    onOpenSettings = { currentScreen = Screen.Settings },
                                    onBack = { currentScreen = Screen.Home },
                                    onSelectGroup = onSelectGroup,
                                    onCreateGroup = onCreateGroup,
                                    onQuoteMessage = onQuoteMessage,
                                    onCancelQuote = onCancelQuote,
                                    onDeleteMessage = onDeleteMessage,
                                    onEditMessage = onEditMessage,
                                    onCancelEditMessage = onCancelEditMessage,
                                    onSaveEditMessage = onSaveEditMessage,
                                    onForwardMessage = onForwardMessage,
                                    onPinMessage = onPinMessage,
                                    onSaveToCollection = onSaveToCollection,
                                    onSelectMessage = onSelectMessage,
                                    onToggleMultiSelect = onToggleMultiSelect,
                                    onDeleteSelected = onDeleteSelected,
                                    onForwardSelected = onForwardSelected,
                                    onCancelMultiSelect = onCancelMultiSelect,
                                    onSelectAll = onSelectAll,
                                    onUnpinCurrent = onUnpinCurrent,
                                    onAddReaction = onAddReaction,
                                    onShowReactionPicker = onShowReactionPicker,
                                    onDismissReactionPicker = onDismissReactionPicker,
                                    onShowMentionPicker = onShowMentionPicker,
                                    onDismissMentionPicker = onDismissMentionPicker,
                                    onSelectMention = onSelectMention,
                                    onStartVoiceRecording = onStartVoiceRecording,
                                    onStopVoiceRecording = onStopVoiceRecording,
                                )
                            }
                            ChatScreen(
                                state = state,
                                messages = messages,
                                actions = chatActions,
                            )
                        }
                        Screen.Contacts -> {
                            ContactsScreen(
                                onlineUsers = state.onlineUsers,
                                currentUserId = state.myUserId,
                                onSelectUser = { user ->
                                    onSelectPrivateUser(user)
                                    currentScreen = Screen.Chat
                                },
                                onBack = { currentScreen = Screen.Home },
                            )
                        }
                        Screen.Profile -> {
                            selectedUser?.let { user ->
                                ProfileScreen(
                                    user = user,
                                    isOnline = state.onlineUsers.any { it.userId == user.userId },
                                    onSendMessage = {
                                        onSelectPrivateUser(user)
                                        currentScreen = Screen.Chat
                                    },
                                    onBack = { currentScreen = Screen.Home },
                                )
                            }
                        }
                        Screen.Settings -> {
                            SettingsScreen(
                                state = state,
                                onLogout = onLogout,
                                onClose = { currentScreen = Screen.Home },
                                onToggleDarkMode = onToggleDarkMode,
                            )
                        }
                    }
                }
            }
        }
    }

    // ── Create Group Dialog ──────────────────────────────────
    if (state.showCreateGroupDialog) {
        var groupName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = onDismissCreateGroupDialog,
            title = { Text("创建群组") },
            text = {
                Column {
                    Text("请输入群组名称", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it.take(30) },
                        label = { Text("群组名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (groupName.isNotBlank()) {
                            onConfirmCreateGroup(groupName.trim())
                        }
                    },
                    enabled = groupName.isNotBlank(),
                ) {
                    Text("创建")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissCreateGroupDialog) {
                    Text("取消")
                }
            },
        )
    }
}
