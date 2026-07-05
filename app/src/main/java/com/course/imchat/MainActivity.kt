package com.course.imchat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.course.imchat.data.MessageRepository
import com.course.imchat.data.cache.MessageCache
import com.course.imchat.data.cache.SessionCache
import com.course.imchat.ui.ChatApp
import com.course.imchat.ui.theme.IMChatTheme
import android.provider.OpenableColumns

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize local dependencies
        val messageCache = MessageCache(applicationContext)
        val sessionCache = SessionCache(applicationContext)

        // Initialize notification channel
        com.course.imchat.core.notification.NotificationHelper.createChannel(applicationContext)

        setContent {
            val viewModel: ChatViewModel = viewModel(
                factory = ChatViewModelFactory(messageCache, sessionCache, applicationContext)
            )
            val state by viewModel.uiState.collectAsState()

            // ── Image picker ──────────────────────────────────
            val imagePicker = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri ->
                uri?.let { selectedUri ->
                    val fileName = getFileName(selectedUri) ?: "image.jpg"
                    viewModel.sendImage(selectedUri, fileName)
                }
            }

            // ── File picker ─────────────────────────────────
            val filePicker = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri ->
                uri?.let { selectedUri ->
                    val fileName = getFileName(selectedUri) ?: "file"
                    val fileSize = getFileSize(selectedUri)
                    viewModel.sendFile(selectedUri, fileName, fileSize)
                }
            }

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
                    onTakePhoto = { imagePicker.launch("image/*") },
                    onPickFile = { filePicker.launch("*/*") },
                    onToggleSearch = viewModel::toggleSearch,
                    onSearchQueryChange = viewModel::onSearchQueryChange,
                    onRecallMessage = viewModel::recallMessage,
                    onToggleDarkMode = viewModel::toggleDarkMode,
                    onToggleGroupManagement = viewModel::toggleGroupManagement,
                    onCreateGroup = viewModel::createGroup,
                    onSelectGroup = viewModel::selectGroup,
                    onJoinGroup = viewModel::joinGroup,
                    onLeaveGroup = viewModel::leaveGroup,
                    onJoin = viewModel::join,
                    onQuoteMessage = viewModel::quoteMessage,
                    onCancelQuote = viewModel::cancelQuote,
                    onDeleteMessage = viewModel::deleteMessage,
                    onEditMessage = viewModel::startEditMessage,
                    onCancelEditMessage = viewModel::cancelEditMessage,
                    onSaveEditMessage = viewModel::saveEditMessage,
                    onForwardMessage = viewModel::startForwardMessage,
                    onPinMessage = viewModel::pinMessage,
                    onSaveToCollection = viewModel::saveMessage,
                    onSelectMessage = { viewModel.toggleMessageSelection(it.id) },
                    onToggleMultiSelect = viewModel::toggleMultiSelectMode,
                    onDeleteSelected = viewModel::deleteSelectedMessages,
                    onForwardSelected = viewModel::forwardSelectedMessages,
                    onCancelMultiSelect = { viewModel.toggleMultiSelectMode() },
                    onSelectAll = viewModel::selectAllMessages,
                    onUnpinCurrent = viewModel::unpinCurrentChat,
                    onAddReaction = viewModel::addReaction,
                    onShowReactionPicker = viewModel::showReactionPicker,
                    onDismissReactionPicker = viewModel::dismissReactionPicker,
                    onShowCreateGroupDialog = viewModel::showCreateGroupDialog,
                    onDismissCreateGroupDialog = viewModel::dismissCreateGroupDialog,
                    onConfirmCreateGroup = viewModel::createGroup,
                    onShowMentionPicker = viewModel::showMentionPicker,
                    onDismissMentionPicker = viewModel::dismissMentionPicker,
                    onSelectMention = viewModel::selectMention,
                    onStartVoiceRecording = viewModel::startVoiceRecording,
                    onStopVoiceRecording = viewModel::stopVoiceRecording,
                )
            }
        }
    }

    /** Get display name from content URI */
    private fun getFileName(uri: android.net.Uri): String? {
        val cursor = contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) it.getString(nameIndex) else null
            } else null
        }
    }

    private fun getFileSize(uri: android.net.Uri): Long {
        val cursor = contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            if (it.moveToFirst()) {
                val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex >= 0) it.getLong(sizeIndex) else 0L
            } else 0L
        } ?: 0L
    }
}

class ChatViewModelFactory(
    private val messageCache: MessageCache,
    private val sessionCache: SessionCache,
    private val appContext: android.content.Context,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            return ChatViewModel(
                repository = MessageRepository(),
                messageCache = messageCache,
                sessionCache = sessionCache,
                appContext = appContext,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
