package com.course.imchat

import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.course.imchat.ui.ChatApp
import com.course.imchat.ui.theme.IMChatTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: ChatViewModel = viewModel()
            val state by viewModel.uiState.collectAsState()

            val imagePicker = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri -> uri?.let { viewModel.sendImage(it, getName(it) ?: "image.jpg") } }

            val filePicker = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri -> uri?.let { viewModel.sendFile(it, getName(it) ?: "file", 0) } }

            IMChatTheme(darkTheme = state.isDarkMode) {
                ChatApp(
                    state = state,
                    messages = viewModel.messages,
                    onServerUrlChange = viewModel::onServerUrlChange,
                    onUsernameChange = viewModel::onUsernameChange,
                    onPasswordChange = viewModel::onPasswordChange,
                    onNicknameChange = viewModel::onNicknameChange,
                    onToggleAuthMode = viewModel::toggleAuthMode,
                    onConnect = viewModel::connect,
                    onLogin = viewModel::login,
                    onRegister = viewModel::register,
                    onLogout = viewModel::logout,
                    onDraftChange = viewModel::onDraftChange,
                    onSend = viewModel::sendMessage,
                    onReconnect = viewModel::reconnect,
                    onDismissError = viewModel::dismissError,
                    onToggleOnlineUsers = viewModel::toggleOnlineUsers,
                    onSelectPrivateUser = viewModel::selectPrivateUser,
                    onAttachFile = viewModel::toggleAttachmentSheet,
                    onPickImage = { imagePicker.launch("image/*") },
                    onTakePhoto = {},
                    onPickFile = { filePicker.launch("*/*") },
                    onToggleSearch = viewModel::toggleSearch,
                    onSearchQueryChange = viewModel::onSearchQueryChange,
                    onRecallMessage = viewModel::recallMessage,
                    onToggleDarkMode = viewModel::toggleDarkMode,
                    onToggleGroupManagement = viewModel.group::toggleGroupManagement,
                    onCreateGroup = viewModel::createGroup,
                    onSelectGroup = viewModel::selectGroup,
                    onJoinGroup = viewModel::joinGroup,
                    onLeaveGroup = viewModel::leaveGroup,
                    onJoin = viewModel::join,
                    onQuoteMessage = { viewModel.quoteMessage(it) },
                    onCancelQuote = { viewModel.cancelQuote() },
                    onDeleteMessage = viewModel::deleteMessage,
                    onEditMessage = { viewModel.startEditMessage(it) },
                    onCancelEditMessage = { viewModel.cancelEditMessage() },
                    onSaveEditMessage = { viewModel.saveEditMessage() },
                    onForwardMessage = { viewModel.startForwardMessage(it) },
                    onPinMessage = viewModel.pin::pinMessage,
                    onSaveToCollection = viewModel.pin::saveMessage,
                    onSelectMessage = { msg -> viewModel.toggleMessageSelection(msg.id) },
                    onToggleMultiSelect = viewModel::toggleMultiSelectMode,
                    onDeleteSelected = viewModel::deleteSelectedMessages,
                    onForwardSelected = viewModel::forwardSelectedMessages,
                    onCancelMultiSelect = viewModel::toggleMultiSelectMode,
                    onSelectAll = viewModel::selectAllMessages,
                    onUnpinCurrent = viewModel.pin::unpinCurrentChat,
                    onAddReaction = viewModel::addReaction,
                    onShowReactionPicker = viewModel::showReactionPicker,
                    onDismissReactionPicker = viewModel::dismissReactionPicker,
                    onShowCreateGroupDialog = viewModel.group::showCreateGroupDialog,
                    onDismissCreateGroupDialog = viewModel.group::dismissCreateGroupDialog,
                    onCreateGroupNameChange = { text -> viewModel.onDraftChange(text) },
                    onConfirmCreateGroup = { name -> viewModel.createGroup(name) },
                    onShowMentionPicker = viewModel::showMentionPicker,
                    onDismissMentionPicker = viewModel::dismissMentionPicker,
                    onSelectMention = viewModel::selectMention,
                    onStartVoiceRecording = viewModel::startVoiceRecording,
                    onStopVoiceRecording = {},
                )
            }
        }
    }

    private fun getName(uri: android.net.Uri): String? {
        var name: String? = null
        contentResolver.query(uri, null, null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (i >= 0) name = c.getString(i)
            }
        }
        return name
    }
}
