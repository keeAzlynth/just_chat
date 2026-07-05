package com.course.imchat.ui

import com.course.imchat.ChatGroup
import com.course.imchat.ChatMessage
import com.course.imchat.OnlineUser

/**
 * Unified callback holder for all chat actions.
 * Reduces parameter threading through multiple composable levels.
 */
data class ChatActions(
    // Connection & Auth
    val onServerUrlChange: (String) -> Unit = {},
    val onUsernameChange: (String) -> Unit = {},
    val onPasswordChange: (String) -> Unit = {},
    val onNicknameChange: (String) -> Unit = {},
    val onToggleAuthMode: () -> Unit = {},
    val onConnect: () -> Unit = {},
    val onLogin: () -> Unit = {},
    val onRegister: () -> Unit = {},
    val onLogout: () -> Unit = {},
    val onReconnect: () -> Unit = {},
    val onDismissError: () -> Unit = {},
    val onJoin: () -> Unit = {},
    val onOpenSettings: () -> Unit = {},
    val onBack: () -> Unit = {},

    // Messages
    val onDraftChange: (String) -> Unit = {},
    val onSend: () -> Unit = {},
    val onToggleSearch: () -> Unit = {},
    val onSearchQueryChange: (String) -> Unit = {},
    val onRecallMessage: (String) -> Unit = {},
    val onQuoteMessage: (ChatMessage) -> Unit = {},
    val onCancelQuote: () -> Unit = {},
    val onDeleteMessage: (String) -> Unit = {},
    val onEditMessage: (ChatMessage) -> Unit = {},
    val onCancelEditMessage: () -> Unit = {},
    val onSaveEditMessage: () -> Unit = {},
    val onForwardMessage: (ChatMessage) -> Unit = {},
    val onPinMessage: (ChatMessage) -> Unit = {},
    val onSaveToCollection: (ChatMessage) -> Unit = {},
    val onSelectMessage: (ChatMessage) -> Unit = {},
    val onToggleMultiSelect: () -> Unit = {},
    val onDeleteSelected: () -> Unit = {},
    val onForwardSelected: () -> Unit = {},
    val onCancelMultiSelect: () -> Unit = {},
    val onSelectAll: () -> Unit = {},
    val onUnpinCurrent: () -> Unit = {},
    val onAddReaction: (String, String) -> Unit = { _, _ -> },
    val onShowReactionPicker: (String) -> Unit = {},
    val onDismissReactionPicker: () -> Unit = {},

    // Groups & Users
    val onToggleOnlineUsers: () -> Unit = {},
    val onSelectPrivateUser: (OnlineUser?) -> Unit = {},
    val onSelectGroup: (ChatGroup?) -> Unit = {},
    val onCreateGroup: (String) -> Unit = {},
    val onJoinGroup: (String) -> Unit = {},
    val onLeaveGroup: (String) -> Unit = {},
    val onToggleGroupManagement: () -> Unit = {},
    val onShowCreateGroupDialog: () -> Unit = {},
    val onDismissCreateGroupDialog: () -> Unit = {},
    val onConfirmCreateGroup: (String) -> Unit = {},

    // Attachments
    val onAttachFile: () -> Unit = {},
    val onPickImage: () -> Unit = {},
    val onTakePhoto: () -> Unit = {},
    val onPickFile: () -> Unit = {},

    // Misc
    val onToggleDarkMode: () -> Unit = {},
    val onForwardToUser: (String) -> Unit = {},

    // @Mention & Voice
    val onShowMentionPicker: () -> Unit = {},
    val onDismissMentionPicker: () -> Unit = {},
    val onSelectMention: (OnlineUser) -> Unit = {},
    val onStartVoiceRecording: () -> Unit = {},
    val onStopVoiceRecording: (Boolean) -> Unit = {},
)
