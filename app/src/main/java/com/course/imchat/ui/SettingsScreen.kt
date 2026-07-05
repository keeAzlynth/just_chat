package com.course.imchat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.course.imchat.ChatUiState
import com.course.imchat.data.cache.PersistentCache
import com.course.imchat.data.cache.AppCache

@Composable
fun SettingsScreen(
    state: ChatUiState,
    onLogout: () -> Unit,
    onClose: () -> Unit,
    onToggleDarkMode: () -> Unit,
) {
    var showClearDialog by remember { mutableStateOf(false) }
    val cacheSizeBytes = remember { PersistentCache.getCacheSize() }
    val cacheChatCount = remember { PersistentCache.getCachedChatCount() }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState()),
    ) {
        Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().height(56.dp).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
                Text("设置", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(12.dp))

        // User info card
        Surface(color = MaterialTheme.colorScheme.surface) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TgAvatar(name = state.nickname, size = 56.dp)
                Column(modifier = Modifier.padding(start = 16.dp)) {
                    Text(state.nickname, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(state.username, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Server info
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("服务器", style = MaterialTheme.typography.labelLarge, color = PrimaryBlue)
                Spacer(Modifier.height(8.dp))
                Text(state.serverUrl, style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(Modifier.height(12.dp))

        // Dark mode toggle
        Surface(color = MaterialTheme.colorScheme.surface) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            ) {
                Text("深色模式", style = MaterialTheme.typography.bodyLarge)
                Switch(checked = state.isDarkMode, onCheckedChange = { onToggleDarkMode() })
            }
        }

        Spacer(Modifier.height(12.dp))

        // Cache management
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("缓存管理", style = MaterialTheme.typography.labelLarge, color = PrimaryBlue)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                ) {
                    Text(
                        "${cacheChatCount} 个聊天 · ${formatSize(cacheSizeBytes)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = { showClearDialog = true }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.padding(end = 4.dp), tint = ErrorRed)
                        Text("清除缓存", color = ErrorRed, fontSize = 14.sp)
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp).height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
        ) {
            Text("退出登录", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
    }

    // Clear cache confirmation dialog
    if (showClearDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("清除缓存") },
            text = { Text("将清除 ${cacheChatCount} 个聊天的本地缓存消息，不影响在线数据。") },
            confirmButton = {
                TextButton(onClick = {
                    PersistentCache.clearAll()
                    AppCache.clearAll()
                    showClearDialog = false
                }) { Text("清除", color = ErrorRed) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("取消") }
            },
        )
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes < 1024 -> "${bytes}B"
    bytes < 1024 * 1024 -> "${bytes / 1024}KB"
    else -> "${"%.1f".format(bytes.toDouble() / (1024 * 1024))}MB"
}
