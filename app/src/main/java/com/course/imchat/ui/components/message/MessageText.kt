package com.course.imchat.ui.components.message

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.course.imchat.ui.PrimaryBlue

// ── Main renderer ────────────────────────────────────────

@Composable
fun MessageText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val isDark = isSystemInDarkTheme()
    val tokens = remember(text) { MarkdownParser.parse(text) }

    // Check if there are any block-level tokens
    val hasBlocks = tokens.any {
        it is MdToken.CodeBlock || it is MdToken.Header ||
        it is MdToken.Blockquote || it is MdToken.UnorderedList ||
        it is MdToken.OrderedList || it is MdToken.HorizontalRule ||
        it is MdToken.Image
    }

    if (!hasBlocks) {
        // Fast path: all inline — single Text composable
        val annotated = remember(tokens, color, isDark) {
            buildInlineAnnotatedString(tokens, color, isDark)
        }
        Text(
            text = annotated,
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
            modifier = modifier,
        )
    } else {
        // Block layout — Column with mixed inline groups and blocks
        Column(modifier = modifier.fillMaxWidth()) {
            var i = 0
            while (i < tokens.size) {
                val token = tokens[i]
                when (token) {
                    // ── Block tokens ──────────────────────────────────
                    is MdToken.Header -> HeaderBlock(token, color, isDark)
                    is MdToken.CodeBlock -> CodeBlockView(token, isDark)
                    is MdToken.Blockquote -> BlockquoteBlock(token, color, isDark)
                    is MdToken.UnorderedList -> UnorderedListBlock(token, color, isDark)
                    is MdToken.OrderedList -> OrderedListBlock(token, color, isDark)
                    is MdToken.HorizontalRule -> HorizontalRuleBlock(isDark)
                    is MdToken.Image -> InlineImage(token, isDark)

                    // ── Inline tokens (group consecutive) ────────────
                    else -> {
                        val end = findNextBlock(tokens, i)
                        val annotated = buildInlineAnnotatedString(
                            tokens.subList(i, end), color, isDark
                        )
                        if (annotated.isNotEmpty()) {
                            Text(
                                text = annotated,
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                            )
                        }
                        i = end - 1
                    }
                }
                i++
            }
        }
    }
}

// ── Block renderers ──────────────────────────────────────

@Composable
private fun HeaderBlock(token: MdToken.Header, color: Color, isDark: Boolean) {
    val sizes = mapOf(1 to 22, 2 to 19, 3 to 17, 4 to 15, 5 to 14, 6 to 13)
    val size = sizes[token.level] ?: 14
    Text(
        text = token.content,
        style = MaterialTheme.typography.bodyMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = size.sp,
            lineHeight = (size + 4).sp,
        ),
        color = color,
        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
    )
}

@Composable
private fun CodeBlockView(token: MdToken.CodeBlock, isDark: Boolean) {
    val headerBg = if (isDark) Color(0xFF334155) else Color(0xFF475569)
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        color = if (isDark) Color(0xFF0F172A) else Color(0xFF1E293B),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .background(headerBg, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = token.language.ifEmpty { "code" },
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8),
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "```",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF64748B),
                )
            }
            Text(
                text = token.content,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 18.sp,
                ),
                color = if (isDark) Color(0xFFE5E7EB) else Color(0xFFF8FAFC),
            )
        }
    }
}

@Composable
private fun BlockquoteBlock(token: MdToken.Blockquote, color: Color, isDark: Boolean) {
    val barColor = PrimaryBlue.copy(alpha = 0.5f)
    val bgColor = if (isDark) PrimaryBlue.copy(alpha = 0.08f) else PrimaryBlue.copy(alpha = 0.06f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor),
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .heightIn(min = 20.dp)
                .background(barColor, RoundedCornerShape(2.dp)),
        )
        Column(modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 6.dp)) {
            for (child in token.children) {
                when (child) {
                    is MdToken.Text -> Text(
                        text = child.content,
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                        color = color.copy(alpha = 0.85f),
                    )
                    else -> {
                        val annotated = buildInlineAnnotatedString(listOf(child), color.copy(alpha = 0.85f), isDark)
                        Text(text = annotated, style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp))
                    }
                }
            }
        }
    }
}

@Composable
private fun UnorderedListBlock(token: MdToken.UnorderedList, color: Color, isDark: Boolean) {
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        token.items.forEach { itemTokens ->
            Row(modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 2.dp)) {
                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PrimaryBlue.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(16.dp),
                )
                val annotated = buildInlineAnnotatedString(itemTokens, color, isDark)
                Text(
                    text = annotated,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                )
            }
        }
    }
}

@Composable
private fun OrderedListBlock(token: MdToken.OrderedList, color: Color, isDark: Boolean) {
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        token.items.forEachIndexed { idx, itemTokens ->
            Row(modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 2.dp)) {
                Text(
                    text = "${idx + token.start}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PrimaryBlue.copy(alpha = 0.7f),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.width(20.dp),
                )
                val annotated = buildInlineAnnotatedString(itemTokens, color, isDark)
                Text(
                    text = annotated,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                )
            }
        }
    }
}

@Composable
private fun HorizontalRuleBlock(isDark: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .height(1.dp)
            .background(
                if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.12f)
            ),
    )
}

@Composable
private fun InlineImage(token: MdToken.Image, isDark: Boolean) {
    val ctx = LocalContext.current
    Column(
        modifier = Modifier.padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AsyncImage(
            model = ImageRequest.Builder(ctx)
                .data(token.url)
                .crossfade(true)
                .build(),
            contentDescription = token.alt.ifEmpty { "图片" },
            modifier = Modifier
                .widthIn(max = 240.dp)
                .heightIn(max = 200.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(0.5.dp, if (isDark) Color.White.copy(alpha=0.08f) else Color.Black.copy(alpha=0.06f), RoundedCornerShape(10.dp)),
            contentScale = ContentScale.FillWidth,
        )
        if (token.alt.isNotBlank()) {
            Text(
                text = token.alt,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

// ── Inline annotated string builder ──────────────────────

/** Find next block-level token from [start] */
private fun findNextBlock(tokens: List<MdToken>, start: Int): Int {
    for (i in start until tokens.size) {
        val t = tokens[i]
        if (t is MdToken.CodeBlock || t is MdToken.Header ||
            t is MdToken.Blockquote || t is MdToken.UnorderedList ||
            t is MdToken.OrderedList || t is MdToken.HorizontalRule ||
            t is MdToken.Image
        ) return i
    }
    return tokens.size
}

/** Build a single AnnotatedString from inline tokens */
private fun buildInlineAnnotatedString(tokens: List<MdToken>, color: Color, isDark: Boolean): AnnotatedString {
    val codeBg = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val codeFg = if (isDark) Color(0xFFF8FAFC) else Color(0xFF1E293B)

    return buildAnnotatedString {
        for (token in tokens) {
            when (token) {
                is MdToken.Text -> withStyle(SpanStyle(color = color)) { append(token.content) }
                is MdToken.Bold -> withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = color)) { append(token.content) }
                is MdToken.Italic -> withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = color)) { append(token.content) }
                is MdToken.Strikethrough -> withStyle(
                    SpanStyle(textDecoration = TextDecoration.LineThrough, color = color.copy(alpha = 0.55f))
                ) { append(token.content) }
                is MdToken.InlineCode -> withStyle(
                    SpanStyle(fontFamily = FontFamily.Monospace, background = codeBg, color = codeFg, fontSize = 13.sp)
                ) { append(token.content) }
                is MdToken.Link -> withStyle(
                    SpanStyle(color = PrimaryBlue, textDecoration = TextDecoration.Underline)
                ) { append(token.text) }
                is MdToken.Url -> withStyle(
                    SpanStyle(color = PrimaryBlue, textDecoration = TextDecoration.Underline)
                ) { append(token.url) }
                is MdToken.Mention -> withStyle(
                    SpanStyle(color = PrimaryBlue, fontWeight = FontWeight.SemiBold)
                ) { append("@${token.name}") }
                is MdToken.Newline -> append("\n")
                else -> {} // block-level should not appear in inline list
            }
        }
    }
}

// ── Message context menu ─────────────────────────────────

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
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
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
                ContextMenuItem(Icons.AutoMirrored.Filled.Reply, "回复", onClick = onQuote)
                ContextMenuItem(Icons.Default.ContentCopy, "复制", onClick = onCopy)
                if (onForward != null) ContextMenuItem(Icons.AutoMirrored.Filled.Forward, "转发", onClick = onForward)
                ContextMenuDivider()
                if (onEdit != null) ContextMenuItem(Icons.Default.Edit, "编辑", onClick = onEdit)
                if (onPin != null) ContextMenuItem(Icons.Default.PushPin, "置顶", onClick = onPin)
                if (onSave != null) ContextMenuItem(Icons.Default.Bookmark, "收藏", onClick = onSave)
                if (onSelect != null) ContextMenuItem(Icons.Default.CheckCircle, "多选", onClick = onSelect)
                ContextMenuDivider()
                if (onDelete != null) {
                    ContextMenuItem(Icons.Default.Delete, "删除", onClick = onDelete, isDestructive = true)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
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
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icons, contentDescription = null, modifier = Modifier.size(20.dp),
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
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            .height(0.5.dp).background(MaterialTheme.colorScheme.outlineVariant),
    )
}
