package com.course.imchat.ui.components.message

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.course.imchat.ui.PrimaryBlue
import java.util.regex.Pattern

// ── Compose renderer (optimized: single Text for inline, Column only for blocks) ─────────

@Composable
fun MessageText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val isDark = isSystemInDarkTheme()
    val tokens = remember(text) { MarkdownParser.parse(text) }

    // Check if we have block-level elements that require Column layout
    val hasBlocks = tokens.any { it is MdToken.CodeBlock || it is MdToken.Header }

    if (!hasBlocks) {
        // Fast path: render all inline tokens as single AnnotatedString
        val annotated = remember(tokens, color, isDark) { buildInlineAnnotatedString(tokens, color, isDark) }
        Text(
            text = annotated,
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
            modifier = modifier,
        )
    } else {
        // Block-level: use Column but merge consecutive Text tokens where possible
        Column(modifier = modifier.fillMaxWidth()) {
            var i = 0
            while (i < tokens.size) {
                val token = tokens[i]
                when (token) {
                    is MdToken.Text -> {
                        // Merge consecutive Text/Bold/Italic etc into a single string
                        val merged = buildInlineAnnotatedString(
                            tokens.subList(i, findBlockBoundary(tokens, i)), color, isDark
                        )
                        Text(text = merged, style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp))
                        i = findBlockBoundary(tokens, i) - 1
                    }
                    is MdToken.CodeBlock -> CodeBlockView(token, isDark)
                    is MdToken.Header -> {
                        Text(
                            text = buildAnnotatedString {
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = (20 - token.level * 2).sp, color = color)) {
                                    append(token.content)
                                }
                            },
                            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                        )
                    }
                    else -> {
                        // Individual inline token (shouldn't normally reach here in Column path)
                        val annotated = buildInlineAnnotatedString(listOf(token), color, isDark)
                        Text(text = annotated, style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp))
                    }
                }
                i++
            }
        }
    }
}

/** Find the next block-level boundary from start (first CodeBlock or Header) */
private fun findBlockBoundary(tokens: List<MdToken>, start: Int): Int {
    for (i in start until tokens.size) {
        if (tokens[i] is MdToken.CodeBlock || tokens[i] is MdToken.Header) return i
    }
    return tokens.size
}

/** Build a single AnnotatedString from inline tokens */
private fun buildInlineAnnotatedString(tokens: List<MdToken>, color: Color, isDark: Boolean): AnnotatedString {
    val codeBgColor = if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9)
    return buildAnnotatedString {
        for (token in tokens) {
            when (token) {
                is MdToken.Text -> {
                    // Also check for @mentions within plain text
                    val matcher = MENTION_PATTERN.matcher(token.content)
                    var lastEnd = 0
                    while (matcher.find()) {
                        if (matcher.start() > lastEnd) withStyle(SpanStyle(color = color)) { append(token.content, lastEnd, matcher.start()) }
                        withStyle(SpanStyle(color = PrimaryBlue, fontWeight = FontWeight.SemiBold)) { append(token.content, matcher.start(), matcher.end()) }
                        lastEnd = matcher.end()
                    }
                    if (lastEnd < token.content.length) withStyle(SpanStyle(color = color)) { append(token.content, lastEnd, token.content.length) }
                }
                is MdToken.Bold -> withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = color)) { append(token.content) }
                is MdToken.Italic -> withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = color)) { append(token.content) }
                is MdToken.Strikethrough -> withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough, color = color.copy(alpha = 0.5f))) { append(token.content) }
                is MdToken.InlineCode -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBgColor)) { append(token.content) }
                is MdToken.Link -> withStyle(SpanStyle(color = PrimaryBlue, textDecoration = TextDecoration.Underline)) { append(token.text) }
                is MdToken.Url -> withStyle(SpanStyle(color = PrimaryBlue, textDecoration = TextDecoration.Underline)) { append(token.url) }
                is MdToken.Mention -> withStyle(SpanStyle(color = PrimaryBlue, fontWeight = FontWeight.SemiBold)) { append("@${token.name}") }
                else -> {} // Skip block-level (shouldn't be in inline list)
            }
        }
    }
}

@Composable
private fun CodeBlockView(token: MdToken.CodeBlock, isDark: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        color = if (isDark) Color(0xFF0F172A) else Color(0xFF1E293B),
    ) {
        Column {
            if (token.language.isNotEmpty()) {
                Text(
                    text = token.language,
                    modifier = Modifier.padding(start = 12.dp, top = 8.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8),
                )
            }
            Text(
                text = token.content,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, lineHeight = 18.sp),
                color = if (isDark) Color(0xFFE5E7EB) else Color(0xFFF8FAFC),
            )
        }
    }
}

// ── @Mention only (fast path for plain text) ──────────────
private val MENTION_PATTERN = Pattern.compile("@(\\S+)")

private fun buildMentionAnnotatedString(text: String, baseColor: Color): AnnotatedString {
    return buildAnnotatedString {
        val matcher = MENTION_PATTERN.matcher(text)
        var lastEnd = 0
        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                withStyle(SpanStyle(color = baseColor)) {
                    append(text.substring(lastEnd, matcher.start()))
                }
            }
            withStyle(SpanStyle(color = PrimaryBlue, fontWeight = FontWeight.SemiBold)) {
                append(text.substring(matcher.start(), matcher.end()))
            }
            lastEnd = matcher.end()
        }
        if (lastEnd < text.length) {
            withStyle(SpanStyle(color = baseColor)) {
                append(text.substring(lastEnd))
            }
        }
    }
}

// ── Message context menu (Telegram style) ─────────────────
@Composable
fun MessageContextMenu(
    message: com.course.imchat.ChatMessage,
    onQuote: () -> Unit,
    onCopy: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onForward: (() -> Unit)? = null,
    onPin: (() -> Unit)? = null,
    onSave: (() -> Unit)? = null,
    onSelect: (() -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = null,
        text = {
            Column {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = message.nickname,
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimaryBlue,
                            fontWeight = FontWeight.SemiBold,
                        )
                        MessageText(
                            text = message.text.take(100) + if (message.text.length > 100) "..." else "",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                ContextMenuItem(icons = Icons.AutoMirrored.Filled.Reply, text = "回复", onClick = onQuote)
                ContextMenuItem(icons = Icons.Default.ContentCopy, text = "复制", onClick = onCopy)
                if (onForward != null) {
                    ContextMenuItem(icons = Icons.AutoMirrored.Filled.Forward, text = "转发", onClick = onForward)
                }
                ContextMenuDivider()
                if (onEdit != null) ContextMenuItem(icons = Icons.Default.Edit, text = "编辑", onClick = onEdit)
                if (onPin != null) ContextMenuItem(icons = Icons.Default.PushPin, text = "置顶", onClick = onPin)
                if (onSave != null) ContextMenuItem(icons = Icons.Default.Bookmark, text = "收藏", onClick = onSave)
                if (onSelect != null) ContextMenuItem(icons = Icons.Default.CheckCircle, text = "多选", onClick = onSelect)
                ContextMenuDivider()
                if (onDelete != null) {
                    ContextMenuItem(
                        icons = Icons.Default.Delete,
                        text = "删除",
                        onClick = onDelete,
                        isDestructive = true,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        },
    )
}

@Composable
private fun ContextMenuItem(
    icons: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color.Transparent,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icons,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (isDestructive) MaterialTheme.colorScheme.error else PrimaryBlue,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = text,
                color = if (isDestructive) MaterialTheme.colorScheme.error else Color.Unspecified,
            )
        }
    }
}

@Composable
private fun ContextMenuDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .height(0.5.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}
