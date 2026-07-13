package com.course.imchat.ui.components.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.course.imchat.ChatMessage

// ── Rich color tokens — modern messaging app palette ─────

private val AccentBlue    = Color(0xFF4A90D9)  // rich cornflower blue
private val AccentGreen   = Color(0xFF34C759)  // vibrant green (send button)
private val AccentPurple  = Color(0xFFAF52DE)  // code & links
private val AccentOrange  = Color(0xFFFF9500)  // headers
private val AccentTeal    = Color(0xFF5AC8FA)  // quotes

private val BarBgLight    = Color(0xFFF0F0F5)  // warmer light gray
private val BarBgDark     = Color(0xFF1C1C1E)
private val InputBgLight  = Color(0xFFFFFFFF)
private val InputBgDark   = Color(0xFF2C2C2E)
private val InputBorderFocus = Color(0xFF4A90D9)
private val InputBorderLight = Color(0xFFDCDCE0)
private val InputBorderDark  = Color(0xFF3A3A3C)
private val ToolbarBgLight   = Color(0xFFE6E6ED)
private val ToolbarBgDark    = Color(0xFF252528)
private val ChipBgLight      = Color(0xFFFFFFFF)
private val ChipBgDark       = Color(0xFF3A3A3C)
private val TextPrimaryLight = Color(0xFF1C1C1E)
private val TextPrimaryDark  = Color(0xFFF5F5F5)

@Composable
fun InputBar(
    draft: String,
    canSend: Boolean,
    showEmojiPicker: Boolean,
    showMarkdownToolbar: Boolean = false,
    quotingMessage: ChatMessage? = null,
    editingMessage: ChatMessage? = null,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onToggleEmoji: () -> Unit,
    onToggleMarkdownToolbar: () -> Unit = {},
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
    onInsertMarkdown: (String) -> Unit = {},
) {
    val isDark = isSystemInDarkTheme()

    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val clipboardText = remember { derivedStateOf { clipboardManager.getText()?.text } }
    val hasClipboard = remember { derivedStateOf { !clipboardText.value.isNullOrBlank() } }

    // Colors
    val barBg       = if (isDark) BarBgDark      else BarBgLight
    val inputBg     = if (isDark) InputBgDark     else InputBgLight
    val inputBorder = if (isDark) InputBorderDark else InputBorderLight
    val textColor   = if (isDark) TextPrimaryDark else TextPrimaryLight
    val chipBg      = if (isDark) ChipBgDark      else ChipBgLight
    val toolbarBg   = if (isDark) ToolbarBgDark   else ToolbarBgLight
    val sendColor   = if (canSend) AccentGreen else Color.Gray.copy(alpha = 0.30f)
    val hintColor   = if (isDark) Color(0xFF636366) else Color(0xFFC0C0C8)
    val iconColor   = if (isDark) Color(0xFF98989E) else Color(0xFF8A8A90)
    val barShadow   = if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.08f)

    Surface(
        color = barBg,
        shadowElevation = 1.dp,
        modifier = Modifier.border(0.5.dp, barShadow, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
    ) {
        Column {
            // Top hairline
            Box(
                modifier = Modifier.fillMaxWidth().height(0.5.dp)
                    .background(barShadow)
            )

            if (quotingMessage != null) QuotedMessageInputPreview(message = quotingMessage, onCancel = onCancelQuote)
            if (editingMessage != null) EditingMessageIndicator(message = editingMessage, onCancel = onCancelEdit)

            // ── Markdown toolbar (2-row, scrollable) ──────────
            AnimatedVisibility(
                visible = showMarkdownToolbar,
                enter = expandVertically(spring()) + fadeIn(),
                exit = shrinkVertically(spring()) + fadeOut(),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().background(toolbarBg),
                ) {
                    // Row 1: Headings + inline formatting
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        FormatChip("H₁", AccentOrange, chipBg) { onInsertMarkdown("# ") }
                        FormatChip("H₂", AccentOrange, chipBg) { onInsertMarkdown("## ") }
                        FormatChip("H₃", AccentOrange, chipBg) { onInsertMarkdown("### ") }
                        Spacer(modifier = Modifier.width(6.dp).height(1.dp).background(Color.Transparent))
                        FormatChip("B", iconColor, chipBg) { onInsertMarkdown("**text**") }
                        FormatChip("I", iconColor, chipBg, italic = true) { onInsertMarkdown("*text*") }
                        FormatChip("~\u0336", iconColor, chipBg, strikethrough = true) { onInsertMarkdown("~~text~~") }
                        FormatChip("{ }", AccentPurple, chipBg, mono = true) { onInsertMarkdown("`code`") }
                        FormatChip("🔗", AccentBlue, chipBg) { onInsertMarkdown("[text](url)") }
                    }

                    // Row 2: Block elements
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        FormatChip("• 列表", AccentTeal, chipBg) { onInsertMarkdown("- 项目\n- 项目") }
                        FormatChip("1. 序号", AccentTeal, chipBg) { onInsertMarkdown("1. 第一\n2. 第二") }
                        FormatChip("❝ 引用", AccentTeal, chipBg) { onInsertMarkdown("> 引用内容") }
                        FormatChip("— 分割", iconColor, chipBg) { onInsertMarkdown("\n---\n") }
                        FormatChip("<> 代码", AccentPurple, chipBg) { onInsertMarkdown("```\n代码块\n```") }
                        FormatChip("▦ 表格", AccentBlue, chipBg) {
                            onInsertMarkdown("| 列A | 列B |\n| --- | --- |\n| 值1 | 值2 |")
                        }
                        FormatChip("🖼 图片", AccentPurple, chipBg) { onInsertMarkdown("![描述](url)") }
                        FormatChip("@", AccentBlue, chipBg) { onMention() }
                    }
                }
            }

            // ── Main input row ─────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                // ＋ Attachment
                IconButton(onClick = onAttachFile, modifier = Modifier.size(38.dp)) {
                    Icon(Icons.Default.Add, contentDescription = "添加",
                        modifier = Modifier.size(26.dp), tint = AccentBlue)
                }

                // Text field
                TextField(
                    value = draft,
                    onValueChange = onDraftChange,
                    modifier = Modifier.weight(1f).heightIn(min = 38.dp, max = 120.dp),
                    maxLines = 4,
                    placeholder = { Text("消息", style = MaterialTheme.typography.bodyMedium, color = hintColor) },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = textColor, fontWeight = FontWeight.Normal,
                    ),
                    shape = RoundedCornerShape(19.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = inputBg,
                        unfocusedContainerColor = inputBg,
                        focusedIndicatorColor = InputBorderFocus,
                        unfocusedIndicatorColor = inputBorder,
                        cursorColor = AccentBlue,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                    keyboardActions = KeyboardActions(onSend = { if (canSend) onSend() }),
                )

                Spacer(modifier = Modifier.width(4.dp))

                // Emoji
                Box(
                    modifier = Modifier.size(34.dp).clickable(onClick = onToggleEmoji),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "\uD83D\uDE42", fontSize = 18.sp,
                        color = if (showEmojiPicker) AccentOrange else iconColor,
                    )
                }

                // Markdown toggle B
                Box(
                    modifier = Modifier.size(34.dp).clip(RoundedCornerShape(8.dp))
                        .background(if (showMarkdownToolbar) AccentBlue.copy(alpha = 0.12f) else Color.Transparent)
                        .clickable(onClick = onToggleMarkdownToolbar),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "B", fontWeight = FontWeight.Black, fontSize = 15.sp,
                        color = if (showMarkdownToolbar) AccentBlue else iconColor,
                    )
                }

                // Send button — gradient circle
                val sendGradient = if (canSend)
                    Brush.linearGradient(listOf(AccentGreen, Color(0xFF30B350)))
                else
                    Brush.linearGradient(listOf(Color.Gray.copy(alpha = 0.25f), Color.Gray.copy(alpha = 0.15f)))

                Box(
                    modifier = Modifier.size(34.dp).clip(CircleShape)
                        .background(sendGradient)
                        .clickable(enabled = canSend) { onSend() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "发送",
                        modifier = Modifier.size(22.dp), tint = if (canSend) Color.White else Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}

// ── Format chip component ────────────────────────────────


@Composable
private fun FormatChip(
    label: String,
    color: Color,
    bgColor: Color,
    italic: Boolean = false,
    strikethrough: Boolean = false,
    mono: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(0.5.dp, color.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (strikethrough) FontWeight.Normal else FontWeight.SemiBold,
            fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
            fontFamily = if (mono) FontFamily.Monospace else MaterialTheme.typography.labelSmall.fontFamily,
            color = color,
        )
    }
}

// ── Indicators ───────────────────────────────────────────

@Composable
fun EditingMessageIndicator(message: ChatMessage, onCancel: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        color = AccentBlue.copy(alpha = 0.08f),
    ) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.width(3.dp).height(32.dp)
                    .background(color = AccentBlue, shape = RoundedCornerShape(2.dp)),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("编辑消息", style = MaterialTheme.typography.labelSmall,
                    color = AccentBlue, fontWeight = FontWeight.SemiBold)
                Text(message.text, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onCancel, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun QuotedMessageInputPreview(message: ChatMessage, onCancel: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        color = AccentBlue.copy(alpha = 0.08f),
    ) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.width(3.dp).height(32.dp)
                    .background(color = AccentBlue, shape = RoundedCornerShape(2.dp)),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(message.nickname, style = MaterialTheme.typography.labelSmall,
                    color = AccentBlue, fontWeight = FontWeight.SemiBold)
                Text(message.text, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onCancel, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
