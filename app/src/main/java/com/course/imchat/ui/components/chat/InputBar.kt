package com.course.imchat.ui.components.chat

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.course.imchat.ChatMessage
import com.course.imchat.ui.PrimaryBlue
import com.course.imchat.ui.ErrorRed

@Composable
fun InputBar(
    draft: String,
    canSend: Boolean,
    showEmojiPicker: Boolean,
    quotingMessage: ChatMessage? = null,
    editingMessage: ChatMessage? = null,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onToggleEmoji: () -> Unit,
    onAttachFile: () -> Unit,
    onPickImage: () -> Unit,
    onPreview: () -> Unit = {},
    onCancelQuote: () -> Unit = {},
    onCancelEdit: () -> Unit = {},
    onMention: () -> Unit = {},
    onStartVoice: () -> Unit = {},
    onStopVoice: (Boolean) -> Unit = {},
    isVoiceRecording: Boolean = false,
    recordingSeconds: Float = 0f,
    onPaste: (String) -> Unit = {},
) {
    val isDarkTheme = isSystemInDarkTheme()
    val hasContent = draft.isNotBlank()

    // ── Paste from clipboard ──────────────────────────────
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val clipboardText = remember { derivedStateOf { clipboardManager.getText()?.text } }
    val hasClipboard = remember { derivedStateOf { !clipboardText.value.isNullOrBlank() } }

    // ── Color tokens with guaranteed contrast ──────────────────
    val barBackground = if (isDarkTheme) Color(0xFF1A2332) else Color(0xFFF8FAFC)
    val inputBackground = if (isDarkTheme) Color(0xFF243447) else Color(0xFFFFFFFF)
    val inputBorderColor = if (isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0)
    val inputTextColor = if (isDarkTheme) Color(0xFFF1F5F9) else Color(0xFF1E293B)
    val inputPlaceholderColor = if (isDarkTheme) Color(0xFF64748B) else Color(0xFF94A3B8)
    val iconColor = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)

    val sendButtonBrush = remember(canSend) {
        if (canSend) Brush.linearGradient(listOf(PrimaryBlue, PrimaryBlue))
        else Brush.linearGradient(
            listOf(
                Color.Gray.copy(alpha = 0.2f),
                Color.Gray.copy(alpha = 0.15f),
            )
        )
    }

    // Cache regex to avoid re-creating on every recomposition
    val markdownRegex = remember { Regex("[*_~`#\\[\\]]") }

    // ── Layout ────────────────────────────────────────────────
    Surface(color = barBackground) {
        Column {
            // Top separator — subtle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(
                        if (isDarkTheme) Color.White.copy(alpha = 0.06f)
                        else Color.Black.copy(alpha = 0.06f)
                    )
            )

            // Quoting / editing indicators
            if (quotingMessage != null) {
                QuotedMessageInputPreview(message = quotingMessage, onCancel = onCancelQuote)
            }
            if (editingMessage != null) {
                EditingMessageIndicator(message = editingMessage, onCancel = onCancelEdit)
            }

            // Main input row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .imePadding(),
                verticalAlignment = Alignment.Bottom,
            ) {
                // ── Attach button ──
                IconButton(
                    onClick = onAttachFile,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        Icons.Default.AttachFile,
                        contentDescription = "附件",
                        modifier = Modifier.size(22.dp),
                        tint = iconColor,
                    )
                }

                // ── @Mention button ──
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clickable(onClick = onMention),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "@",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = iconColor,
                    )
                }

                // ── Paste button (show when clipboard has text) ──
                if (hasClipboard.value) {
                    IconButton(
                        onClick = {
                            clipboardText.value?.let { onPaste(it) }
                        },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            Icons.Default.ContentPaste,
                            contentDescription = "粘贴",
                            modifier = Modifier.size(18.dp),
                            tint = iconColor,
                        )
                    }
                }

                // ── Text input (filled, Telegram-style) ──
                TextField(
                    value = draft,
                    onValueChange = onDraftChange,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp, max = 160.dp),
                    maxLines = 6,
                    placeholder = {
                        Text(
                            "消息...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = inputPlaceholderColor,
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = inputTextColor,
                        fontWeight = FontWeight.Normal,
                    ),
                    shape = RoundedCornerShape(22.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = inputBackground,
                        unfocusedContainerColor = inputBackground,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = inputTextColor,
                        unfocusedTextColor = inputTextColor,
                        cursorColor = PrimaryBlue,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { if (canSend) onSend() }),
                )

                Spacer(modifier = Modifier.width(4.dp))

                // ── Emoji toggle ──
                IconButton(
                    onClick = onToggleEmoji,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        Icons.Default.EmojiEmotions,
                        contentDescription = "表情",
                        modifier = Modifier.size(22.dp),
                        tint = if (showEmojiPicker) PrimaryBlue else iconColor,
                    )
                }

                // ── Markdown preview (when has content) ──
                // Show preview button when text likely contains Markdown formatting
                val hasMarkdown = hasContent && draft.contains(markdownRegex)
                if (hasMarkdown) {
                    IconButton(
                        onClick = onPreview,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            Icons.Default.Visibility,
                            contentDescription = "预览",
                            modifier = Modifier.size(20.dp),
                            tint = iconColor,
                        )
                    }
                }

                // Character count (show when approaching limit)
                if (draft.length > 180) {
                    Text(
                        text = "${draft.length}/500",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (draft.length > 450) ErrorRed else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.padding(end = 6.dp),
                    )
                }

                // ── Send button ──
                val sendScale by animateFloatAsState(
                    targetValue = if (canSend) 1f else 0.85f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "sendScale",
                )
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .scale(sendScale)
                        .shadow(
                            elevation = if (canSend) 4.dp else 0.dp,
                            shape = CircleShape,
                            ambientColor = PrimaryBlue.copy(alpha = 0.25f),
                            spotColor = PrimaryBlue.copy(alpha = 0.4f),
                        )
                        .clip(CircleShape)
                        .background(brush = sendButtonBrush)
                        .clickable(enabled = canSend) { onSend() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "发送",
                        modifier = Modifier.size(20.dp),
                        tint = Color.White,
                    )
                }
            }
        }
    }
}

// ── Editing indicator ────────────────────────────────────────

@Composable
fun EditingMessageIndicator(
    message: ChatMessage,
    onCancel: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        color = PrimaryBlue.copy(alpha = 0.1f),
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(36.dp)
                    .background(color = PrimaryBlue, shape = RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "编辑消息",
                    style = MaterialTheme.typography.labelSmall,
                    color = PrimaryBlue,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    message.text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onCancel, modifier = Modifier.size(24.dp)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "取消编辑",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ── Quote preview ────────────────────────────────────────────

@Composable
private fun QuotedMessageInputPreview(
    message: ChatMessage,
    onCancel: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        color = PrimaryBlue.copy(alpha = 0.1f),
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(36.dp)
                    .background(color = PrimaryBlue, shape = RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    message.nickname,
                    style = MaterialTheme.typography.labelSmall,
                    color = PrimaryBlue,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    message.text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onCancel, modifier = Modifier.size(24.dp)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "取消引用",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
