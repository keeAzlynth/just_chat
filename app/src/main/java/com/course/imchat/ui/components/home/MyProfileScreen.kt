package com.course.imchat.ui.components.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.course.imchat.ChatUiState
import com.course.imchat.ui.ErrorRed
import com.course.imchat.ui.PrimaryBlue
import com.course.imchat.ui.TgAvatar

/**
 * 我的个人主页 — 显示当前登录用户信息、统计、快捷入口
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyProfileScreen(
    state: ChatUiState,
    onOpenSaved: () -> Unit,
    onOpenSettings: () -> Unit,
    onLogout: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
    ) {
        // Header card
        Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TgAvatar(name = state.nickname, size = 80.dp)
                Spacer(Modifier.height(16.dp))
                Text(state.nickname, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(4.dp))
                Text("@${state.username}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            }
        }

        Spacer(Modifier.height(12.dp))

        // Stats row
        Surface(color = MaterialTheme.colorScheme.surface) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                StatItem("${state.savedMessages.size}", "收藏")
                StatItem("${state.groups.size}", "群组")
                StatItem("在线", "状态")
            }
        }

        Spacer(Modifier.height(12.dp))

        // Menu items
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column {
                ProfileMenuRow(Icons.Default.Bookmark, "收藏消息", subtitle = "${state.savedMessages.size} 条", onClick = onOpenSaved)
                HorizontalDivider(Modifier.padding(horizontal = 56.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ProfileMenuRow(Icons.Default.Settings, "设置", onClick = onOpenSettings)
                HorizontalDivider(Modifier.padding(horizontal = 56.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ProfileMenuRow(Icons.Default.Logout, "退出登录", isDestructive = true, onClick = onLogout)
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(
            "Just Chat v1.0",
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
        )
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PrimaryBlue)
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ProfileMenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    isDestructive: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(), color = Color.Transparent) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, Modifier.size(24.dp), tint = if (isDestructive) ErrorRed else PrimaryBlue)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, color = if (isDestructive) ErrorRed else Color.Unspecified)
                if (subtitle != null) Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("›", fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        }
    }
}
