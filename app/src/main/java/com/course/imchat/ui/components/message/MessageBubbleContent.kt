package com.course.imchat.ui.components.message

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.course.imchat.ChatMessage
import com.course.imchat.MessageType

/**
 * 气泡内容区，按消息类型拆成独立的 Composable。
 *
 * 原来这些全塞在 `MessageBubble` 的 `when (message.messageType)` 里，
 * 一个函数 536 行。拆开之后：
 *  - 每种类型各自是一个重组作用域，文本消息刷新不会连累图片/文件分支；
 *  - 主函数只负责气泡外形 + 分发，读起来清楚；
 *  - 新增消息类型（语音、投票卡片…）不用再改那个巨型函数。
 */

/** 撤回提示 */
@Composable
internal fun RecalledContent(
    nickname: String,
    isMine: Boolean,
    textColor: Color,
) {
    Text(
        text = if (isMine) "你撤回了一条消息" else "$nickname 撤回了一条消息",
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = textColor.copy(alpha = 0.6f),
        fontStyle = FontStyle.Italic,
    )
}

/** 图片消息 */
@Composable
internal fun ImageMessageContent(
    message: ChatMessage,
    isMultiSelectMode: Boolean,
    onViewImage: (ChatMessage) -> Unit,
    onLongPress: () -> Unit,
) {
    val context = LocalContext.current
    val imageModel = remember(message.fileUrl, message.text) {
        message.fileUrl.ifBlank { message.text }
    }

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(imageModel)
            .crossfade(true)
            .build(),
        contentDescription = "图片消息",
        modifier = Modifier
            .widthIn(max = 220.dp)
            .heightIn(max = 280.dp)
            .padding(2.dp)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = { if (!isMultiSelectMode) onViewImage(message) },
                onLongClick = onLongPress,
            ),
        contentScale = ContentScale.FillWidth,
    )
}

/** 文件消息 */
@Composable
internal fun FileMessageContent(
    message: ChatMessage,
    textColor: Color,
    subColor: Color,
) {
    Row(
        modifier = Modifier.padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.AttachFile,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = textColor,
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                text = message.fileName,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = message.formattedFileSize(),
                style = MaterialTheme.typography.bodySmall,
                color = subColor,
            )
        }
    }
}

/** 文本消息（含 Markdown 渲染、时间戳、链接预览） */
@Composable
internal fun TextMessageContent(
    message: ChatMessage,
    textColor: Color,
    timeColor: Color,
    isMine: Boolean,
) {
    Column(modifier = Modifier.padding(0.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            MessageText(
                text = message.text,
                color = textColor,
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = message.formattedTime(),
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = timeColor,
            )
        }

        val previewUrl = remember(message.text) { extractUrl(message.text) }
        if (previewUrl != null) {
            LinkPreviewCard(url = previewUrl, isMine = isMine)
        }
    }
}
