package com.course.imchat.ui.components.message

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Forward
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.course.imchat.ChatMessage
import com.course.imchat.MessageType
import com.course.imchat.QuotedMessage
import com.course.imchat.ui.AccentPurple
import com.course.imchat.ui.PrimaryBlue
import com.course.imchat.ui.PrimaryBlueDark
import com.course.imchat.ui.PrimaryGradient
import com.course.imchat.ui.ReceivedBubbleDark
import com.course.imchat.ui.ReceivedBubbleLight
import com.course.imchat.ui.SuccessGreen
import com.course.imchat.ui.TgAvatar
import com.course.imchat.ui.avatarColor
import java.util.regex.Pattern
import kotlin.math.roundToInt

private const val SWIPE_THRESHOLD = 100f
private const val MAX_SWIPE_OFFSET = 150f

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: ChatMessage,
    onRecallMessage: (String) -> Unit,
    onQuoteMessage: (ChatMessage) -> Unit = {},
    onDeleteMessage: (String) -> Unit = {},
    onEditMessage: (ChatMessage) -> Unit = {},
    onForwardMessage: (ChatMessage) -> Unit = {},
    onPinMessage: (ChatMessage) -> Unit = {},
    onSaveMessage: (ChatMessage) -> Unit = {},
    onSelectMessage: (ChatMessage) -> Unit = {},
    isMultiSelectMode: Boolean = false,
    isSelected: Boolean = false,
    onAddReaction: (ChatMessage, String) -> Unit = { _, _ -> },
    onShowReactionPicker: (String) -> Unit = {},
    onViewImage: (ChatMessage) -> Unit = {},
) {
    val isMine = message.isMine
    val isDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    val context = LocalContext.current
    val density = LocalDensity.current
    
    var showContextMenu by remember { mutableStateOf(false) }
    var showTimeDetail by remember { mutableStateOf(false) }
    var swipeOffset by remember { mutableFloatStateOf(0f) }
    val animatedOffset by animateFloatAsState(
        targetValue = swipeOffset,
        animationSpec = spring(stiffness = 300f),
        label = "swipeOffset"
    )
    
    val bubbleShape = remember(isMine) {
        RoundedCornerShape(
            topStart = if (isMine) 20.dp else 6.dp,
            topEnd = if (isMine) 6.dp else 20.dp,
            bottomStart = 20.dp,
            bottomEnd = 20.dp,
        )
    }
    
    val bubbleBrush = remember(isMine, isDarkTheme, isSelected) {
        if (isSelected) {
            Brush.linearGradient(
                colors = listOf(
                    PrimaryBlue.copy(alpha = 0.3f),
                    PrimaryBlue.copy(alpha = 0.2f)
                )
            )
        } else if (isMine) {
            Brush.linearGradient(colors = listOf(PrimaryBlue, PrimaryBlueDark))
        } else {
            Brush.linearGradient(
                colors = if (isDarkTheme) listOf(
                    ReceivedBubbleDark,
                    ReceivedBubbleDark.copy(alpha = 0.95f)
                ) else listOf(
                    ReceivedBubbleLight,
                    Color(0xFFF8FAFC)
                )
            )
        }
    }

    // Text colors for received bubbles — guaranteed contrast across themes
    val bubbleTextColor = if (isMine) Color.White
        else if (isDarkTheme) Color(0xFFF1F5F9)
        else Color(0xFF1E293B)
    val bubbleTimeColor = bubbleTextColor.copy(alpha = 0.5f)
    val bubbleSubColor = bubbleTextColor.copy(alpha = 0.7f)

    if (showContextMenu) {
        MessageContextMenu(
            message = message,
            onQuote = { 
                onQuoteMessage(message)
                showContextMenu = false
            },
            onCopy = {
                copyToClipboard(context, message.text)
                showContextMenu = false
            },
            onDelete = if (isMine) {
                {
                    onDeleteMessage(message.id)
                    showContextMenu = false
                }
            } else null,
            onEdit = if (isMine && !message.isRecalled) {
                {
                    onEditMessage(message)
                    showContextMenu = false
                }
            } else null,
            onForward = {
                onForwardMessage(message)
                showContextMenu = false
            },
            onPin = {
                onPinMessage(message)
                showContextMenu = false
            },
            onSave = {
                onSaveMessage(message)
                showContextMenu = false
            },
            onSelect = {
                onSelectMessage(message)
                showContextMenu = false
            },
            onDismiss = { showContextMenu = false }
        )
    }
    
    Box {
        // Reply icon that appears during swipe
        if (animatedOffset > 10f) {
            Box(
                modifier = Modifier
                    .align(if (isMine) Alignment.CenterEnd else Alignment.CenterStart)
                    .offset { IntOffset(x = if (isMine) (animatedOffset - 40).roundToInt() else 0, y = 0) }
                    .size(32.dp)
                    .background(
                        color = PrimaryBlue.copy(alpha = (animatedOffset / SWIPE_THRESHOLD).coerceIn(0f, 0.8f)),
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Reply,
                    contentDescription = "回复",
                    modifier = Modifier.size(18.dp),
                    tint = Color.White.copy(alpha = (animatedOffset / SWIPE_THRESHOLD).coerceIn(0f, 1f))
                )
            }
        }

        // ── Reaction bar ─────────────────────────────────────
        if (message.reactions.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .align(if (isMine) Alignment.CenterEnd else Alignment.CenterStart)
                    .padding(horizontal = if (isMine) 0.dp else 44.dp)
                    .clickable { onShowReactionPicker(message.id) },
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                message.reactions.entries.take(3).forEach { (emoji, count) ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (message.myReaction == emoji)
                            PrimaryBlue.copy(alpha = 0.2f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    ) {
                        Text(
                            text = "$emoji $count",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 11.sp,
                        )
                    }
                }
            }
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 3.dp)
                .offset { IntOffset(x = animatedOffset.roundToInt(), y = 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (swipeOffset > SWIPE_THRESHOLD) {
                                onQuoteMessage(message)
                            }
                            swipeOffset = 0f
                        },
                        onDragCancel = {
                            swipeOffset = 0f
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            val newOffset = (swipeOffset + dragAmount).coerceIn(0f, MAX_SWIPE_OFFSET)
                            swipeOffset = newOffset
                        }
                    )
                },
            horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
        ) {
            if (!isMine) {
                TgAvatar(name = message.nickname, size = 36.dp)
                Spacer(modifier = Modifier.width(8.dp))
            }

            Column(
                modifier = Modifier.widthIn(max = 280.dp),
                horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
            ) {
                if (!isMine) {
                    Text(
                        text = message.nickname,
                        modifier = Modifier.padding(start = 6.dp, bottom = 3.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = avatarColor(message.nickname),
                        fontWeight = FontWeight.Bold,
                    )
                }

                Box(
                    modifier = Modifier
                        .shadow(
                            elevation = if (isMine) 4.dp else 2.dp,
                            shape = bubbleShape,
                            ambientColor = if (isMine) PrimaryBlue.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.1f),
                            spotColor = if (isMine) PrimaryBlue.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.15f),
                        )
                        .clip(bubbleShape)
                        .background(brush = bubbleBrush)
                        .combinedClickable(
                            onClick = {
                                when {
                                    isMultiSelectMode -> onSelectMessage(message)
                                    message.messageType == MessageType.Image -> onViewImage(message)
                                }
                            },
                            onDoubleClick = {
                                if (!isMultiSelectMode && message.messageType != MessageType.Image) {
                                    onQuoteMessage(message)
                                }
                            },
                            onLongClick = { showContextMenu = true }
                        )
                ) {
                    if (message.quotedMessage != null && !message.isRecalled) {
                        QuotedMessagePreview(
                            quotedMessage = message.quotedMessage,
                            isMine = isMine,
                        )
                    }
                    
                    if (message.isRecalled) {
                        Text(
                            text = if (isMine) "你撤回了一条消息" else "${message.nickname} 撤回了一条消息",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = bubbleTextColor.copy(alpha = 0.6f),
                            fontStyle = FontStyle.Italic,
                        )
                    } else when (message.messageType) {
                        MessageType.Image -> {
                            Column(
                                modifier = Modifier
                                    .padding(14.dp)
                                    .widthIn(max = 220.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isMine) Color.White.copy(alpha = 0.2f)
                                                else PrimaryBlue.copy(alpha = 0.1f)
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            Icons.Default.OpenInFull,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = if (isMine) Color.White else PrimaryBlue,
                                        )
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "\uD83D\uDDBC\uFE0F ${message.fileName.ifEmpty { "图片" }}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = bubbleTextColor,
                                            fontWeight = FontWeight.Medium,
                                        )
                                        Text(
                                            text = "点击查看大图",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = bubbleSubColor,
                                        )
                                    }
                                }
                            }
                        }
                        MessageType.File -> {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Default.AttachFile, contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = bubbleTextColor)
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(message.fileName, style = MaterialTheme.typography.bodyMedium,
                                        color = bubbleTextColor, fontWeight = FontWeight.Medium)
                                    Text(message.formattedFileSize(), style = MaterialTheme.typography.bodySmall,
                                        color = bubbleSubColor)
                                }
                            }
                        }
                        else -> {
                            Column(modifier = Modifier.padding(0.dp)) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.Bottom,
                                ) {
                                    MessageText(
                                        text = message.text,
                                        color = bubbleTextColor,
                                        modifier = Modifier.weight(1f, fill = false),
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = message.formattedTime(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        color = bubbleTimeColor,
                                    )
                                }
                                // URL preview card
                                val previewUrl = remember(message.text) { extractUrl(message.text) }
                                if (previewUrl != null) {
                                    LinkPreviewCard(
                                        url = previewUrl,
                                        isMine = isMine,
                                    )
                                }
                            }
                        }
                    }
                }

                if (isMine && !message.isRecalled) {
                    Row(
                        modifier = Modifier.padding(top = 2.dp, end = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Time detail toggle (Telegram style)
                        Row(
                            modifier = Modifier.clickable { showTimeDetail = !showTimeDetail },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = if (message.isRead()) "✓✓" else deliveryText(message.deliveryStatus),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (message.isRead()) PrimaryBlue
                                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            )
                        }
                    }

                    // Time detail popup (Telegram style)
                    if (showTimeDetail) {
                        MessageTimeDetailPopup(
                            message = message,
                            onDismiss = { showTimeDetail = false },
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuotedMessagePreview(
    quotedMessage: QuotedMessage,
    isMine: Boolean,
) {
    val bgColor = if (isMine) Color.White.copy(alpha = 0.15f) else PrimaryBlue.copy(alpha = 0.08f)
    val textColor = if (isMine) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurface
    val nameColor = if (isMine) Color.White.copy(alpha = 0.7f) else PrimaryBlue
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .background(
                color = bgColor,
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(
            text = quotedMessage.nickname,
            style = MaterialTheme.typography.labelSmall,
            color = nameColor,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = quotedMessage.text,
            style = MaterialTheme.typography.bodySmall,
            color = textColor.copy(alpha = 0.8f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun deliveryText(status: com.course.imchat.DeliveryStatus): String = when (status) {
    com.course.imchat.DeliveryStatus.Sending   -> "发送中..."
    com.course.imchat.DeliveryStatus.Sent      -> "已发送"
    com.course.imchat.DeliveryStatus.Delivered -> "已送达"
    com.course.imchat.DeliveryStatus.Read      -> "已读"
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("chat_message", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
}

// ── URL preview card (Telegram style) ─────────────────────

private val URL_REGEX = Pattern.compile("https?://[\\w\\-._~:/?#@!$&'()*+,;=%]+")

private fun extractUrl(text: String): String? {
    val matcher = URL_REGEX.matcher(text)
    return if (matcher.find()) matcher.group() else null
}

@Composable
private fun LinkPreviewCard(url: String, isMine: Boolean) {
    val isDark = isSystemInDarkTheme()
    val domain = remember(url) {
        try { java.net.URI(url).host ?: url } catch (_: Exception) { url }
    }
    val bgColor = if (isMine) Color.White.copy(alpha = 0.08f)
                  else if (isDark) Color(0xFF334155).copy(alpha = 0.3f)
                  else Color(0xFFF1F5F9)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Link icon
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (isMine) Color.White.copy(alpha = 0.1f) else PrimaryBlue.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "\uD83D\uDD17",
                fontSize = 14.sp,
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = domain,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (isMine) Color.White.copy(alpha = 0.7f) else PrimaryBlue,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = url.take(60) + if (url.length > 60) "..." else "",
                style = MaterialTheme.typography.bodySmall,
                color = if (isMine) Color.White.copy(alpha = 0.4f)
                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 10.sp,
            )
        }
    }
}

/**
 * 消息时间详情弹出框 - 仿 Telegram 风格
 * 点击消息的已读/送达状态时显示精确时间和状态
 */
@Composable
fun MessageTimeDetailPopup(
    message: ChatMessage,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()

    // Format full datetime - use remember to avoid non-observable Locale reads
    val dateDetail = remember(message.timestampSeconds) {
        val date = java.util.Date(message.timestampSeconds * 1000)
        val dateFormat = java.text.SimpleDateFormat("yyyy年M月d日", java.util.Locale.getDefault())
        val timeFormat = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        val dayOfWeek = java.text.SimpleDateFormat("EEEE", java.util.Locale.getDefault()).format(date)
        Triple(dateFormat.format(date), timeFormat.format(date), dayOfWeek)
    }
    val (dateStr, timeStr, dayOfWeekStr) = dateDetail

    Surface(
        modifier = modifier
            .widthIn(min = 140.dp),
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 8.dp,
        color = if (isDark) Color(0xFF1E293B) else Color.White,
        tonalElevation = 4.dp,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            // Date row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "\uD83D\uDCC5",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "\uD83D\uDCC5 $dateStr $dayOfWeekStr",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B),
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Time row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "\uD83D\uDD52",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "\uD83D\uDD52 $timeStr",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Status row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = when (message.deliveryStatus) {
                        com.course.imchat.DeliveryStatus.Sending -> "\uD83D\uDCE4"
                        com.course.imchat.DeliveryStatus.Sent -> "\u2709\uFE0F"
                        com.course.imchat.DeliveryStatus.Delivered -> "\uD83D\uDCEC"
                        com.course.imchat.DeliveryStatus.Read -> "\uD83D\uDC41\uFE0F"
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = when (message.deliveryStatus) {
                        com.course.imchat.DeliveryStatus.Sending -> "发送中"
                        com.course.imchat.DeliveryStatus.Sent -> "已发送"
                        com.course.imchat.DeliveryStatus.Delivered -> "已送达"
                        com.course.imchat.DeliveryStatus.Read -> "已读"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (message.isRead()) SuccessGreen
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
