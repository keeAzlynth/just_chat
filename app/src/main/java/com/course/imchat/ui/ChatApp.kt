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
import com.course.imchat.ui.components.home.MyProfileScreen
import com.course.imchat.ui.components.home.SavedMessagesScreen

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
    actions: ChatActions,
) {
    // ── Destructure actions into local vals (body references unchanged) ──
    val onServerUrlChange = actions.auth.onServerUrlChange
    val onUsernameChange = actions.auth.onUsernameChange
    val onPasswordChange = actions.auth.onPasswordChange
    val onNicknameChange = actions.auth.onNicknameChange
    val onToggleAuthMode = actions.auth.onToggleAuthMode
    val onConnect = actions.auth.onConnect
    val onLogin = actions.auth.onLogin
    val onRegister = actions.auth.onRegister
    val onLogout = actions.auth.onLogout
    val onReconnect = actions.auth.onReconnect
    val onDismissError = actions.auth.onDismissError
    val onJoin = actions.auth.onJoin
    val onDraftChange = actions.message.onDraftChange
    val onSend = actions.message.onSend
    val onToggleSearch = actions.message.onToggleSearch
    val onSearchQueryChange = actions.message.onSearchQueryChange
    val onRecallMessage = actions.message.onRecallMessage
    val onQuoteMessage = actions.message.onQuoteMessage
    val onCancelQuote = actions.message.onCancelQuote
    val onDeleteMessage = actions.message.onDeleteMessage
    val onEditMessage = actions.message.onEditMessage
    val onCancelEditMessage = actions.message.onCancelEditMessage
    val onSaveEditMessage = actions.message.onSaveEditMessage
    val onForwardMessage = actions.message.onForwardMessage
    val onSelectMessage = actions.message.onSelectMessage
    val onToggleMultiSelect = actions.message.onToggleMultiSelect
    val onDeleteSelected = actions.message.onDeleteSelected
    val onForwardSelected = actions.message.onForwardSelected
    val onCancelMultiSelect = actions.message.onCancelMultiSelect
    val onSelectAll = actions.message.onSelectAll
    val onAddReaction = actions.message.onAddReaction
    val onShowReactionPicker = actions.message.onShowReactionPicker
    val onDismissReactionPicker = actions.message.onDismissReactionPicker
    val onPinMessage = actions.pin.onPinMessage
    val onSaveToCollection = actions.pin.onSaveToCollection
    val onUnpinCurrent = actions.pin.onUnpinCurrent
    val onToggleOnlineUsers = actions.group.onToggleOnlineUsers
    val onSelectPrivateUser = actions.group.onSelectPrivateUser
    val onSelectGroup = actions.group.onSelectGroup
    val onCreateGroup = actions.group.onCreateGroup
    val onJoinGroup = actions.group.onJoinGroup
    val onLeaveGroup = actions.group.onLeaveGroup
    val onToggleGroupManagement = actions.group.onToggleGroupManagement
    val onShowCreateGroupDialog = actions.group.onShowCreateGroupDialog
    val onDismissCreateGroupDialog = actions.group.onDismissCreateGroupDialog
    val onConfirmCreateGroup = actions.group.onConfirmCreateGroup
    val onAttachFile = actions.attachment.onAttachFile
    val onPickImage = actions.attachment.onPickImage
    val onTakePhoto = actions.attachment.onTakePhoto
    val onPickFile = actions.attachment.onPickFile
    val onShowMentionPicker = actions.input.onShowMentionPicker
    val onDismissMentionPicker = actions.input.onDismissMentionPicker
    val onSelectMention = actions.input.onSelectMention
    val onStartVoiceRecording = actions.input.onStartVoiceRecording
    val onStopVoiceRecording = actions.input.onStopVoiceRecording
    val onToggleDarkMode = actions.nav.onToggleDarkMode
    val onToggleScreenshotProtection = actions.nav.onToggleScreenshotProtection

    var currentScreen by remember { mutableStateOf(Screen.Home) }
    var selectedUser by remember { mutableStateOf<OnlineUser?>(null) }
    var showSavedMessages by remember { mutableStateOf(false) }
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
                        onClick = { currentScreen = Screen.Profile },
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
                            val chatActions = actions.copy(
                                nav = actions.nav.copy(
                                    onOpenSettings = { currentScreen = Screen.Settings },
                                    onBack = { currentScreen = Screen.Home },
                                )
                            )
                            ChatScreen(
                                state = state,
                                messages = messages,
                                actions = chatActions,
                            )
                        }
                        Screen.Contacts -> {
                            ContactsScreen(
                                onlineUsers = state.onlineUsers.values.toList(),
                                currentUserId = state.myUserId,
                                onSelectUser = { user ->
                                    onSelectPrivateUser(user)
                                    currentScreen = Screen.Chat
                                },
                                onBack = { currentScreen = Screen.Home },
                            )
                        }
                        Screen.Profile -> {
                            MyProfileScreen(
                                state = state,
                                onOpenSaved = { showSavedMessages = true },
                                onOpenSettings = { currentScreen = Screen.Settings },
                                onLogout = onLogout,
                            )
                        }
                        Screen.Settings -> {
                            SettingsScreen(
                                state = state,
                                onLogout = onLogout,
                                onClose = { currentScreen = Screen.Home },
                                onToggleDarkMode = onToggleDarkMode,
                                onToggleScreenshotProtection = onToggleScreenshotProtection,
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

    // Saved messages overlay
    if (showSavedMessages) {
        SavedMessagesScreen(
            state = state,
            onClose = { showSavedMessages = false },
            onUnsave = { saved -> onSaveToCollection(saved.message) },
            onJumpToChat = { saved ->
                showSavedMessages = false
                when {
                    saved.chatId.startsWith("group_") -> {
                        val group = state.groups.find { it.groupId == saved.chatId.removePrefix("group_") }
                        if (group != null) onSelectGroup(group)
                    }
                    saved.chatId == "public" -> {
                        onSelectPrivateUser(null)
                    }
                    else -> {
                        val user = state.onlineUsers.values.toList().find { it.userId == saved.chatId.removePrefix("private_") }
                        if (user != null) onSelectPrivateUser(user)
                    }
                }
                currentScreen = Screen.Chat
            },
        )
    }
}
