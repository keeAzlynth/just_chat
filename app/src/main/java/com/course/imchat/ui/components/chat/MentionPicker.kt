package com.course.imchat.ui.components.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.course.imchat.OnlineUser
import com.course.imchat.ui.PrimaryBlue
import com.course.imchat.ui.TgAvatar

/**
 * @提及选择器 - 仿 Telegram/QQ 风格
 * 输入 @ 后弹出可 @ 的用户列表
 */
@Composable
fun MentionPicker(
    visible: Boolean,
    users: List<OnlineUser>,
    onSelectUser: (OnlineUser) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
        modifier = modifier,
    ) {
        val isDark = isSystemInDarkTheme()
        val bgColor = if (isDark) Color(0xFF1A2332) else Color.White
        val borderColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            shadowElevation = 8.dp,
            color = bgColor,
            tonalElevation = 2.dp,
        ) {
            Column {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "@提及用户",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B),
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "关闭",
                            modifier = Modifier.size(18.dp),
                            tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                        )
                    }
                }

                // Divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(borderColor)
                )

                if (users.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "暂无可 @ 的用户",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(users, key = { it.userId }) { user ->
                            MentionUserItem(
                                user = user,
                                onClick = { onSelectUser(user) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MentionUserItem(
    user: OnlineUser,
    onClick: () -> Unit,
) {
    val isDark = isSystemInDarkTheme()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Avatar
        TgAvatar(
            name = user.nickname,
            size = 40.dp,
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Name
        Column(modifier = Modifier.weight(1f)) {
            Text(
                user.nickname,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B),
            )
            Text(
                "@${user.nickname.lowercase().replace(" ", "_")}",
                style = MaterialTheme.typography.bodySmall,
                color = PrimaryBlue,
            )
        }

        // @ icon
        Icon(
            Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
        )
    }
}
