package com.course.imchat.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.course.imchat.ChatMessage
import com.course.imchat.ChatUiState
import com.course.imchat.ui.components.chat.AttachmentSheet
import com.course.imchat.ui.components.chat.ChatBackground
import com.course.imchat.ui.components.chat.ChatListItem
import com.course.imchat.ui.components.chat.ChatTopBar
import com.course.imchat.ui.components.chat.DateSeparator
import com.course.imchat.ui.components.chat.EmojiPicker
import com.course.imchat.data.EmojiData
import com.course.imchat.ui.components.chat.InputBar
import com.course.imchat.ui.components.chat.MentionPicker
import com.course.imchat.ui.components.chat.MultiSelectToolbar
import com.course.imchat.ui.components.chat.PinnedMessageBar
import com.course.imchat.ui.components.chat.ReactionPicker
import com.course.imchat.ui.components.chat.ScrollToBottomFab
import com.course.imchat.ui.components.chat.SearchBar
import com.course.imchat.ui.components.chat.SmartReplyBar
import com.course.imchat.ui.components.chat.TypingIndicator
import com.course.imchat.ui.components.chat.messagesToListItems
import com.course.imchat.ui.components.dialog.ForwardDialog
import com.course.imchat.ui.components.drawer.OnlineUsersDrawer
import com.course.imchat.ui.components.message.MarkdownPreviewSheet
import com.course.imchat.ui.components.message.MediaViewer
import com.course.imchat.ui.components.message.MessageBubble
import kotlinx.coroutines.launch

/**
 * 聊天界面 — 重构版，使用 ChatActions 统一回调，参数从 40+ 减到 3。
 */
@Composable
fun ChatScreen(
    state: ChatUiState,
    messages: List<ChatMessage>,
    actions: ChatActions,
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    androidx.compose.material3.ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            OnlineUsersDrawer(
                users = state.onlineUsers.values.toList(),
                currentUser = state.nickname,
                selectedUser = state.selectedPrivateUser,
                groups = state.groups,
                selectedGroup = state.selectedGroup,
                onSelectUser = { user ->
                    actions.onSelectPrivateUser(user)
                    scope.launch { drawerState.close() }
                },
                onSelectGroup = { group ->
                    actions.onSelectGroup(group)
                    scope.launch { drawerState.close() }
                },
                onCreateGroup = actions.onCreateGroup,
                onClose = { scope.launch { drawerState.close() } },
            )
        },
    ) {
        ChatContent(
            state = state,
            messages = messages,
            actions = actions,
            onToggleDrawer = {
                actions.onToggleOnlineUsers()
                scope.launch { drawerState.open() }
            },
        )
    }
}

/** 聊天内容区 — 仅保留 UI 局部状态，所有业务回调通过 actions 代理 */
@Composable
private fun ChatContent(
    state: ChatUiState,
    messages: List<ChatMessage>,
    actions: ChatActions,
    onToggleDrawer: () -> Unit,
) {
    val listState = rememberLazyListState()
    var showEmojiPicker by remember { mutableStateOf(false) }
    var showPreview by remember { mutableStateOf(false) }
    var viewingImage by remember { mutableStateOf<ChatMessage?>(null) }
    val scope = rememberCoroutineScope()

    val messageCount = remember { derivedStateOf { messages.size } }

    // Smart auto-scroll: only scroll to bottom if user is near the bottom
    val isAtBottom = remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            total == 0 || lastVisible >= total - 2
        }
    }

    LaunchedEffect(messageCount.value) {
        if (messageCount.value > 0 && isAtBottom.value) {
            listState.animateScrollToItem(messageCount.value - 1)
        }
    }

    val listItems = remember(messages.size) { messagesToListItems(messages) }

    Box(modifier = Modifier.fillMaxSize()) {
        ChatBackground()

        if (messageCount.value == 0 && !state.isSearching) {
            EmptyChatPlaceholder(
                modifier = Modifier.align(Alignment.Center),
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            if (state.isMultiSelectMode) {
                MultiSelectToolbar(
                    selectedCount = state.selectedMessageCount,
                    onSelectAll = actions.onSelectAll,
                    onDelete = actions.onDeleteSelected,
                    onForward = actions.onForwardSelected,
                    onCancel = actions.onCancelMultiSelect,
                )
            } else {
                ChatTopBar(
                    nickname = state.nickname,
                    status = state.connectionStatus,
                    selectedUser = state.selectedPrivateUser,
                    selectedGroup = state.selectedGroup,
                    selectedUserOnline = state.selectedUserOnline,
                    selectedUserLastSeen = state.selectedUserLastSeen,
                    onlineUserCount = state.onlineUsers.size,
                    onReconnect = actions.onReconnect,
                    onToggleOnlineUsers = onToggleDrawer,
                    onToggleSearch = actions.onToggleSearch,
                    onOpenSettings = actions.onOpenSettings,
                    onBack = actions.onBack,
                )
            }

            PinnedMessageBar(
                pinnedMessage = state.currentPinnedMessage,
                onClick = {
                    state.currentPinnedMessage?.let { pinned ->
                        val index = listItems.indexOfFirst {
                            it is ChatListItem.MessageItem && it.message.id == pinned.messageId
                        }
                        if (index >= 0) scope.launch { listState.animateScrollToItem(index) }
                    }
                },
                onUnpin = actions.onUnpinCurrent,
            )

            AnimatedVisibility(visible = state.isSearching) {
                SearchBar(state.searchQuery, actions.onSearchQueryChange, actions.onToggleSearch, state.searchResults.size)
            }

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 70.dp),
            ) {
                items(
                    count = listItems.size,
                    key = { idx ->
                        when (val item = listItems[idx]) {
                            is ChatListItem.MessageItem -> "msg_${item.message.id}"
                            is ChatListItem.DateSeparatorItem -> "date_${item.date}"
                        }
                    },
                    contentType = { idx ->
                        when (listItems[idx]) {
                            is ChatListItem.MessageItem -> "message"
                            is ChatListItem.DateSeparatorItem -> "separator"
                        }
                    }
                ) { idx ->
                    when (val item = listItems[idx]) {
                        is ChatListItem.MessageItem -> MessageBubble(
                            message = item.message,
                            isFirstInGroup = item.isFirstInGroup,
                            isLastInGroup = item.isLastInGroup,
                            onRecallMessage = actions.onRecallMessage,
                            onQuoteMessage = actions.onQuoteMessage,
                            onDeleteMessage = actions.onDeleteMessage,
                            onEditMessage = actions.onEditMessage,
                            onForwardMessage = actions.onForwardMessage,
                            onPinMessage = actions.onPinMessage,
                            onSaveMessage = actions.onSaveToCollection,
                            onSelectMessage = actions.onSelectMessage,
                            isMultiSelectMode = state.isMultiSelectMode,
                            isSelected = item.message.id in state.selectedMessages,
                            onAddReaction = { msg, emoji -> actions.onAddReaction(msg.id, emoji) },
                            onShowReactionPicker = actions.onShowReactionPicker,
                            onViewImage = { viewingImage = it },
                        )
                        is ChatListItem.DateSeparatorItem -> DateSeparator(item.date)
                    }
                }
            }

            // Loading indicator
            if (state.isLoadingMore) {
                Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = PrimaryBlue.copy(alpha = 0.6f))
                }
            }
        }

        // Scroll-to-bottom FAB
        val showFab = remember {
            derivedStateOf {
                listState.layoutInfo.visibleItemsInfo.isNotEmpty() &&
                listState.layoutInfo.totalItemsCount > 0 &&
                listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index != listState.layoutInfo.totalItemsCount - 1
            }
        }
        ScrollToBottomFab(
            visible = showFab.value,
            onClick = {
                scope.launch {
                    val count = listState.layoutInfo.totalItemsCount
                    if (count > 0) listState.animateScrollToItem(count - 1)
                }
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 90.dp),
        )

        // Bottom bar with frosted glass
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding()
                .frostedGlass(cornerRadius = 0.dp, backgroundAlpha = 0.72f),
        ) {
            AnimatedVisibility(visible = state.typingUsers.isNotEmpty()) {
                TypingIndicator(state.typingUsers)
            }
            AnimatedVisibility(
                visible = showEmojiPicker,
                enter = slideInVertically(spring(stiffness = Spring.StiffnessMediumLow)) { it / 2 } + fadeIn(),
                exit = slideOutVertically(spring(stiffness = Spring.StiffnessMediumLow)) { it / 2 } + fadeOut(),
            ) {
                EmojiPicker(
                    onEmojiSelected = { emoji ->
                        EmojiData.recordEmoji(emoji)
                        actions.onDraftChange(state.draft + emoji)
                    },
                    onDismiss = { showEmojiPicker = false },
                )
            }
            SmartReplyBar(
                suggestions = state.smartSuggestions,
                visible = true,
                onReplyClick = { text -> actions.onDraftChange(text) },
            )
            ReactionPicker(
                visible = state.showReactionPicker,
                onReaction = { emoji ->
                    state.reactingToMessageId?.let { actions.onAddReaction(it, emoji) }
                },
                onDismiss = actions.onDismissReactionPicker,
            )
            InputBar(
                draft = state.draft,
                canSend = state.canSend,
                showEmojiPicker = showEmojiPicker,
                quotingMessage = state.quotingMessage,
                editingMessage = state.editingMessage,
                onDraftChange = actions.onDraftChange,
                onSend = actions.onSend,
                onToggleEmoji = { showEmojiPicker = !showEmojiPicker },
                onAttachFile = actions.onAttachFile,
                onPickImage = actions.onPickImage,
                onPreview = { showPreview = true },
                onCancelQuote = actions.onCancelQuote,
                onCancelEdit = actions.onCancelEditMessage,
                onMention = actions.onShowMentionPicker,
                onStartVoice = actions.onStartVoiceRecording,
                onStopVoice = actions.onStopVoiceRecording,
                isVoiceRecording = state.isVoiceRecording,
                recordingSeconds = state.recordingSeconds,
                onPaste = { text -> actions.onDraftChange(state.draft + text) },
            )
            MentionPicker(
                visible = state.showMentionPicker,
                users = state.mentionCandidates,
                onSelectUser = { user ->
                    actions.onSelectMention(user)
                    actions.onDismissMentionPicker()
                },
                onDismiss = actions.onDismissMentionPicker,
            )
        }
    }

    if (showPreview) {
        MarkdownPreviewSheet(
            text = state.draft,
            onDismiss = { showPreview = false },
            onSwitchToEdit = { showPreview = false },
        )
    }

    AttachmentSheet(
        visible = state.showAttachmentSheet,
        onDismiss = actions.onAttachFile,
        onPickImage = actions.onPickImage,
        onTakePhoto = actions.onTakePhoto,
        onPickFile = actions.onPickFile,
    )

    MediaViewer(
        visible = viewingImage != null,
        imageUrl = viewingImage?.fileUrl ?: "",
        fileName = viewingImage?.fileName ?: "",
        onDismiss = { viewingImage = null },
    )
}

// ── Empty chat welcome ───────────────────────────────────

@Composable
private fun EmptyChatPlaceholder(modifier: Modifier = Modifier) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Chat,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "暂无消息",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "发送第一条消息开始聊天",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
        )
    }
}
