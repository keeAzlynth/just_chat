package com.course.imchat.ui.components.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.course.imchat.*
import com.course.imchat.ui.PrimaryBlue
import com.course.imchat.ui.PrimaryBlueDark
import com.course.imchat.ui.TgAvatar

// 聊天项数据类
data class ChatItem(
    val id: String,
    val name: String,
    val lastMessage: String = "",
    val lastMessageTime: Long = 0,
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
    val isOnline: Boolean = false,
    val isGroup: Boolean = false,
    val avatarName: String = "",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: ChatUiState,
    messages: List<ChatMessage>,
    onSelectPublicChat: () -> Unit,
    onSelectPrivateChat: (OnlineUser) -> Unit,
    onSelectGroup: (ChatGroup) -> Unit,
    onCreateGroup: () -> Unit,
    onRefresh: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "消息",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                    IconButton(onClick = onCreateGroup) {
                        Icon(Icons.Default.Add, contentDescription = "创建群组")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        val chatItems = remember(state, messages) {
            buildChatItems(state, messages)
        }

        if (chatItems.isEmpty()) {
            // 空状态
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Icon(
                        Icons.Default.ChatBubbleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    )
                    Text(
                        "暂无消息",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    )
                    Text(
                        "点击右上角 + 创建群组",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                items(
                    items = chatItems,
                    key = { it.id },
                ) { chatItem ->
                    ChatListItem(
                        chatItem = chatItem,
                        onClick = {
                            when {
                                chatItem.id == "public" -> onSelectPublicChat()
                                chatItem.isGroup -> {
                                    val group = state.groups.find { it.groupId == chatItem.id }
                                    if (group != null) onSelectGroup(group)
                                }
                                else -> {
                                    val user = state.onlineUsers[chatItem.id]
                                    if (user != null) onSelectPrivateChat(user)
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatListItem(
    chatItem: ChatItem,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 头像
            Box {
                if (chatItem.isGroup) {
                    // 群组图标
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(PrimaryBlue, PrimaryBlueDark)
                                )
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.Group,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                } else if (chatItem.id == "public") {
                    // 公共聊天图标
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.Public,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                } else {
                    TgAvatar(
                        name = chatItem.avatarName,
                        size = 48.dp,
                    )
                }

                // 在线状态指示器
                if (chatItem.isOnline) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .align(Alignment.BottomEnd)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4CAF50))
                                .align(Alignment.Center)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 聊天信息
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = chatItem.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (chatItem.unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    
                    // 时间和置顶图标
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (chatItem.isPinned) {
                            Icon(
                                Icons.Default.PushPin,
                                contentDescription = "置顶",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        
                        if (chatItem.lastMessageTime > 0) {
                            Text(
                                text = formatChatTime(chatItem.lastMessageTime),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (chatItem.unreadCount > 0) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 最后一条消息
                    Text(
                        text = chatItem.lastMessage.ifEmpty { "暂无消息" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )

                    // 未读消息数
                    if (chatItem.unreadCount > 0) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White,
                            modifier = Modifier.padding(start = 8.dp),
                        ) {
                            Text(
                                text = if (chatItem.unreadCount > 99) "99+" else chatItem.unreadCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
            }
        }
    }
}

// 构建聊天列表项
private fun buildChatItems(state: ChatUiState, messages: List<ChatMessage>): List<ChatItem> {
    val items = mutableListOf<ChatItem>()

    // Single-pass message categorization: O(n) instead of O(n*m)
    var lastPublic: ChatMessage? = null
    val lastPrivateByUser = mutableMapOf<String, ChatMessage>()
    val lastGroupByGroup = mutableMapOf<String, ChatMessage>()

    for (msg in messages) {
        when {
            msg.groupId.isNotEmpty() -> lastGroupByGroup[msg.groupId] = msg
            msg.isPrivate -> {
                val key = if (msg.isMine) msg.receiverId else msg.userId
                lastPrivateByUser[key] = msg
            }
            else -> lastPublic = msg
        }
    }

    // 公共聊天
    items.add(
        ChatItem(
            id = "public",
            name = "公共聊天室",
            lastMessage = lastPublic?.let { "${it.nickname}: ${it.text}" } ?: "",
            lastMessageTime = lastPublic?.timestampSeconds ?: 0,
            unreadCount = state.unreadCounts["public"]?.count ?: state.totalUnreadCount,
            isPinned = true,
            avatarName = "公共",
        )
    )

    // 私聊
    state.onlineUsers.values.forEach { user ->
        val lastMessage = lastPrivateByUser[user.userId]
        items.add(
            ChatItem(
                id = user.userId,
                name = user.nickname,
                lastMessage = lastMessage?.text ?: "",
                lastMessageTime = lastMessage?.timestampSeconds ?: 0,
                unreadCount = state.unreadCounts["private_${user.userId}"]?.count ?: 0,
                isOnline = true,
                avatarName = user.nickname,
            )
        )
    }

    // 群组
    state.groups.forEach { group ->
        val lastMessage = lastGroupByGroup[group.groupId]
        items.add(
            ChatItem(
                id = group.groupId,
                name = group.groupName,
                lastMessage = lastMessage?.let { "${it.nickname}: ${it.text}" } ?: "",
                lastMessageTime = lastMessage?.timestampSeconds ?: group.createdAt,
                isGroup = true,
                avatarName = group.groupName,
            )
        )
    }

    // 按置顶和时间排序
    return items.sortedWith(
        compareByDescending<ChatItem> { it.isPinned }
            .thenByDescending { it.lastMessageTime }
    )
}

// 格式化聊天时间
private fun formatChatTime(timestampSeconds: Long): String {
    val now = System.currentTimeMillis() / 1000
    val diff = now - timestampSeconds
    
    return when {
        diff < 60 -> "刚刚"
        diff < 3600 -> "${diff / 60}分钟前"
        diff < 86400 -> {
            val date = java.util.Date(timestampSeconds * 1000)
            java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(date)
        }
        diff < 604800 -> {
            val date = java.util.Date(timestampSeconds * 1000)
            val dayOfWeek = java.text.SimpleDateFormat("E", java.util.Locale.getDefault()).format(date)
            dayOfWeek
        }
        else -> {
            val date = java.util.Date(timestampSeconds * 1000)
            java.text.SimpleDateFormat("MM/dd", java.util.Locale.getDefault()).format(date)
        }
    }
}
