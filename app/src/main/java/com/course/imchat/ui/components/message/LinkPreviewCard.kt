package com.course.imchat.ui.components.message

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.course.imchat.ui.PrimaryBlue
import java.util.regex.Pattern

/** Extract first URL from message text */
fun extractUrl(text: String): String? {
    val m = URL_REGEX.matcher(text)
    return if (m.find()) m.group() else null
}

private val URL_REGEX = Pattern.compile("https?://[\\w\\-._~:/?#@!$&'()*+,;=%]+")

/** Telegram-style URL preview card inside message bubble */
@Composable
fun LinkPreviewCard(url: String, isMine: Boolean) {
    val isDark = isSystemInDarkTheme()
    val domain = remember(url) {
        try { java.net.URI(url).host ?: url } catch (_: Exception) { url }
    }
    val bgColor = if (isMine) Color.White.copy(alpha = 0.08f)
                  else if (isDark) Color(0xFF334155).copy(alpha = 0.3f)
                  else Color(0xFFF1F5F9)

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(8.dp)).background(bgColor).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(32.dp).clip(RoundedCornerShape(6.dp))
            .background(if (isMine) Color.White.copy(alpha = 0.1f) else PrimaryBlue.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) { Text("\uD83D\uDD17", fontSize = 14.sp) }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(domain, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold,
                color = if (isMine) Color.White.copy(alpha = 0.7f) else PrimaryBlue, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(url.take(60) + if (url.length > 60) "..." else "", style = MaterialTheme.typography.bodySmall,
                color = if (isMine) Color.White.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 10.sp)
        }
    }
}
