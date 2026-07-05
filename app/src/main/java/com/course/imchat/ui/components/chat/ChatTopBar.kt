package com.course.imchat.ui.components.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.course.imchat.ChatGroup
import com.course.imchat.ConnectionStatus
import com.course.imchat.OnlineUser
import com.course.imchat.ui.GroupAvatar
import com.course.imchat.ui.PrimaryBlue
import com.course.imchat.ui.PrimaryGradient
import com.course.imchat.ui.SuccessGreen
import com.course.imchat.ui.TgAvatar

@Composable
fun ChatTopBar(
    nickname: String,
    status: ConnectionStatus,
    selectedUser: OnlineUser?,
    selectedGroup: ChatGroup? = null,
    selectedUserOnline: Boolean = false,
    selectedUserLastSeen: Long = 0,
    onlineUserCount: Int = 0,
    onReconnect: () -> Unit,
    onToggleOnlineUsers: () -> Unit,
    onToggleSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit = {},
) {
    val isDarkTheme = isSystemInDarkTheme()
    
    val displayName = when {
        selectedGroup != null -> selectedGroup.groupName
        selectedUser != null -> selectedUser.nickname
        else -> "公共聊天室"
    }

    val statusDescription = when {
        selectedGroup != null -> "${selectedGroup.memberCount} 位成员"
        selectedUser != null -> {
            if (selectedUserOnline) "在线"
            else {
                val lastSeen = selectedUserLastSeen
                if (lastSeen > 0) formatLastSeen(lastSeen) else "离线"
            }
        }
        else -> {
            val onlineCount = onlineUserCount
            if (onlineCount > 0) "$onlineCount 人在线" else "在线"
        }
    }

    val isOnline = status is ConnectionStatus.Connected &&
        (selectedUser == null || selectedUserOnline)
    
    Surface(
        color = if (isDarkTheme) 
            Color(0xFF1E293B).copy(alpha = 0.9f)
        else 
            Color.White.copy(alpha = 0.9f),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .height(60.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 返回按钮
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        modifier = Modifier.size(22.dp),
                    )
                }
                
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(PrimaryGradient)
                        .padding(2.dp)
                ) {
                    if (selectedGroup != null) {
                        GroupAvatar(
                            name = selectedGroup.groupName,
                            size = 38.dp,
                        )
                    } else {
                        TgAvatar(
                            name = selectedUser?.nickname ?: nickname,
                            size = 38.dp,
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f).padding(start = 4.dp),
                ) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isOnline) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(SuccessGreen).shadow(2.dp, CircleShape))
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            text = when {
                                status !is ConnectionStatus.Connected -> statusText(status)
                                else -> statusDescription
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = if (isOnline) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (status !is ConnectionStatus.Connected) {
                    IconButton(onClick = onReconnect) {
                        Icon(Icons.Default.Refresh, contentDescription = "重连", modifier = Modifier.size(22.dp))
                    }
                }
                IconButton(onClick = onToggleSearch) {
                    Icon(Icons.Default.Search, contentDescription = "搜索", modifier = Modifier.size(22.dp))
                }
                IconButton(onClick = onToggleOnlineUsers) {
                    Icon(Icons.Default.People, contentDescription = "在线", modifier = Modifier.size(22.dp))
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "设置", modifier = Modifier.size(22.dp))
                }
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                PrimaryBlue.copy(alpha = 0.3f),
                                com.course.imchat.ui.AccentPurple.copy(alpha = 0.3f),
                                PrimaryBlue.copy(alpha = 0.1f),
                            )
                        )
                    )
            )
            
            if (status is ConnectionStatus.Connecting || status is ConnectionStatus.Reconnecting) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = PrimaryBlue,
                )
            }
        }
    }
}

private fun statusText(status: ConnectionStatus): String = when (status) {
    is ConnectionStatus.Idle         -> "未连接"
    is ConnectionStatus.Connecting   -> "连接中..."
    is ConnectionStatus.Connected    -> "在线"
    is ConnectionStatus.Reconnecting -> "重连中..."
    is ConnectionStatus.Disconnected -> "已断开"
    is ConnectionStatus.Error        -> "连接失败: ${status.message}"
}

private fun formatLastSeen(lastSeenSeconds: Long): String {
    val now = System.currentTimeMillis() / 1000
    val diff = now - lastSeenSeconds
    return when {
        diff < 60 -> "刚刚在线"
        diff < 3600 -> "${diff / 60} 分钟前在线"
        diff < 86400 -> {
            val hours = diff / 3600
            if (hours == 1L) "1 小时前在线" else "$hours 小时前在线"
        }
        diff < 604800 -> "${diff / 86400} 天前在线"
        else -> "很久以前在线"
    }
}
