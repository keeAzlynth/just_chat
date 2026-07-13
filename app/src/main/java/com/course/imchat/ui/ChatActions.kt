package com.course.imchat.ui

import com.course.imchat.ChatGroup
import com.course.imchat.ChatMessage
import com.course.imchat.OnlineUser

// ════════════════════════════════════════════════════════════
// Feature-scoped action groups
// Each domain has its own actions, independent of others.
// Adding a new feature only touches one group.
// ════════════════════════════════════════════════════════════

/** Connection & authentication actions */
data class AuthActions(
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
)

/** Message send/edit/delete/react actions */
data class MessageActions(
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
    val onSelectMessage: (ChatMessage) -> Unit = {},
    val onToggleMultiSelect: () -> Unit = {},
    val onDeleteSelected: () -> Unit = {},
    val onForwardSelected: () -> Unit = {},
    val onCancelMultiSelect: () -> Unit = {},
    val onSelectAll: () -> Unit = {},
    val onAddReaction: (String, String) -> Unit = { _, _ -> },
    val onShowReactionPicker: (String) -> Unit = {},
    val onDismissReactionPicker: () -> Unit = {},
)

/** Pin & save (collection) actions */
data class PinActions(
    val onPinMessage: (ChatMessage) -> Unit = {},
    val onSaveToCollection: (ChatMessage) -> Unit = {},
    val onUnpinCurrent: () -> Unit = {},
)

/** Group & user selection actions */
data class GroupActions(
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
)

/** Attachment (image/file/voice) actions */
data class AttachmentActions(
    val onAttachFile: () -> Unit = {},
    val onPickImage: () -> Unit = {},
    val onTakePhoto: () -> Unit = {},
    val onPickFile: () -> Unit = {},
)

/** @Mention & voice recording actions */
data class InputActions(
    val onShowMentionPicker: () -> Unit = {},
    val onDismissMentionPicker: () -> Unit = {},
    val onSelectMention: (OnlineUser) -> Unit = {},
    val onStartVoiceRecording: () -> Unit = {},
    val onStopVoiceRecording: (Boolean) -> Unit = {},
)

/** Navigation actions (screen switches) */
data class NavActions(
    val onOpenSettings: () -> Unit = {},
    val onBack: () -> Unit = {},
    val onToggleDarkMode: () -> Unit = {},
    val onToggleScreenshotProtection: () -> Unit = {},
)

// ════════════════════════════════════════════════════════════
// Aggregate — all action groups in one container
// ════════════════════════════════════════════════════════════

/**
 * All chat actions grouped by feature domain.
 * Pass this single object through the UI tree instead of 50+ lambdas.
 *
 * Usage:
 *   val actions = ChatActions.create(viewModel)
 *   ChatApp(state, messages, actions)
 *
 * In composables, access only what you need:
 *   actions.message.onSend()
 *   actions.group.onSelectGroup(g)
 */
data class ChatActions(
    val auth: AuthActions = AuthActions(),
    val message: MessageActions = MessageActions(),
    val pin: PinActions = PinActions(),
    val group: GroupActions = GroupActions(),
    val attachment: AttachmentActions = AttachmentActions(),
    val input: InputActions = InputActions(),
    val nav: NavActions = NavActions(),
) {
    companion object {
        /** Build ChatActions from ChatViewModel — single mapping point */
        fun from(viewModel: com.course.imchat.ChatViewModel): ChatActions = ChatActions(
        auth = AuthActions(
            onServerUrlChange = viewModel::onServerUrlChange,
            onUsernameChange = viewModel::onUsernameChange,
            onPasswordChange = viewModel::onPasswordChange,
            onNicknameChange = viewModel::onNicknameChange,
            onToggleAuthMode = viewModel::toggleAuthMode,
            onConnect = viewModel::connect,
            onLogin = viewModel::login,
            onRegister = viewModel::register,
            onLogout = viewModel::logout,
            onReconnect = viewModel::reconnect,
            onDismissError = viewModel::dismissError,
            onJoin = viewModel::join,
        ),
        message = MessageActions(
            onDraftChange = viewModel::onDraftChange,
            onSend = viewModel::sendMessage,
            onToggleSearch = viewModel::toggleSearch,
            onSearchQueryChange = viewModel::onSearchQueryChange,
            onRecallMessage = viewModel::recallMessage,
            onQuoteMessage = { viewModel.quoteMessage(it) },
            onCancelQuote = { viewModel.cancelQuote() },
            onDeleteMessage = viewModel::deleteMessage,
            onEditMessage = { viewModel.startEditMessage(it) },
            onCancelEditMessage = { viewModel.cancelEditMessage() },
            onSaveEditMessage = { viewModel.saveEditMessage() },
            onForwardMessage = { viewModel.startForwardMessage(it) },
            onSelectMessage = { msg -> viewModel.toggleMessageSelection(msg.id) },
            onToggleMultiSelect = viewModel::toggleMultiSelectMode,
            onDeleteSelected = viewModel::deleteSelectedMessages,
            onForwardSelected = viewModel::forwardSelectedMessages,
            onCancelMultiSelect = viewModel::toggleMultiSelectMode,
            onSelectAll = viewModel::selectAllMessages,
            onAddReaction = viewModel::addReaction,
            onShowReactionPicker = viewModel::showReactionPicker,
            onDismissReactionPicker = viewModel::dismissReactionPicker,
        ),
        pin = PinActions(
            onPinMessage = viewModel.pin::pinMessage,
            onSaveToCollection = viewModel.pin::saveMessage,
            onUnpinCurrent = viewModel.pin::unpinCurrentChat,
        ),
        group = GroupActions(
            onToggleOnlineUsers = viewModel::toggleOnlineUsers,
            onSelectPrivateUser = viewModel::selectPrivateUser,
            onSelectGroup = viewModel::selectGroup,
            onCreateGroup = viewModel::createGroup,
            onJoinGroup = viewModel::joinGroup,
            onLeaveGroup = viewModel::leaveGroup,
            onToggleGroupManagement = viewModel.group::toggleGroupManagement,
            onShowCreateGroupDialog = viewModel.group::showCreateGroupDialog,
            onDismissCreateGroupDialog = viewModel.group::dismissCreateGroupDialog,
            onConfirmCreateGroup = { name -> viewModel.createGroup(name) },
        ),
        attachment = AttachmentActions(
            onAttachFile = viewModel::toggleAttachmentSheet,
        ),
        input = InputActions(
            onShowMentionPicker = viewModel::showMentionPicker,
            onDismissMentionPicker = viewModel::dismissMentionPicker,
            onSelectMention = viewModel::selectMention,
            onStartVoiceRecording = viewModel::startVoiceRecording,
        ),
        nav = NavActions(
            onToggleDarkMode = viewModel::toggleDarkMode,
            onToggleScreenshotProtection = viewModel::toggleScreenshotProtection,
        ),
    )
    }
}
