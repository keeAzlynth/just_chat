package com.course.imchat.ui.components.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.course.imchat.ChatGroup
import com.course.imchat.OnlineUser
import com.course.imchat.ui.GroupAvatar
import com.course.imchat.ui.PrimaryBlue
import com.course.imchat.ui.PrimaryGradient
import com.course.imchat.ui.SuccessGreen
import com.course.imchat.ui.TgAvatar

@Composable
fun OnlineUsersDrawer(
    users: List<OnlineUser>,
    currentUser: String,
    selectedUser: OnlineUser?,
    groups: List<ChatGroup>,
    selectedGroup: ChatGroup?,
    onSelectUser: (OnlineUser?) -> Unit,
    onSelectGroup: (ChatGroup?) -> Unit,
    onCreateGroup: (String) -> Unit,
    onClose: () -> Unit,
) {
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    
    if (showCreateGroupDialog) {
        CreateGroupDialog(
            onDismiss = { showCreateGroupDialog = false },
            onCreateGroup = { name ->
                onCreateGroup(name)
                showCreateGroupDialog = false
            }
        )
    }
    
    ModalDrawerSheet(
        modifier = Modifier.width(300.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .background(PrimaryGradient)
                    .padding(16.dp),
            ) {
                Text(
                    "聊天",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Text(
                    "${users.size} 人在线",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 公共聊天室
            NavigationDrawerItem(
                label = { Text("公共聊天室", fontWeight = FontWeight.Medium) },
                icon = { Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(22.dp)) },
                selected = selectedUser == null && selectedGroup == null,
                onClick = { 
                    onSelectUser(null)
                    onSelectGroup(null)
                },
                modifier = Modifier.padding(horizontal = 12.dp),
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = PrimaryBlue.copy(alpha = 0.12f),
                    selectedIconColor = PrimaryBlue,
                    selectedTextColor = PrimaryBlue,
                ),
            )

            // 群聊列表
            GroupListSection(
                groups = groups,
                selectedGroup = selectedGroup,
                onSelectGroup = { group ->
                    onSelectGroup(group)
                    onSelectUser(null)
                },
                onCreateGroup = { showCreateGroupDialog = true },
            )

            // 在线用户列表
            UserListSection(
                users = users,
                currentUser = currentUser,
                selectedUser = selectedUser,
                onSelectUser = { user ->
                    onSelectUser(user)
                    onSelectGroup(null)
                },
            )
        }
    }
}

@Composable
private fun GroupListSection(
    groups: List<ChatGroup>,
    selectedGroup: ChatGroup?,
    onSelectGroup: (ChatGroup?) -> Unit,
    onCreateGroup: () -> Unit,
) {
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "群聊",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        IconButton(
            onClick = onCreateGroup,
            modifier = Modifier.size(32.dp),
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "创建群聊",
                modifier = Modifier.size(18.dp),
                tint = PrimaryBlue,
            )
        }
    }
    
    if (groups.isNotEmpty()) {
        LazyColumn {
            items(groups) { group ->
                NavigationDrawerItem(
                    label = {
                        Column {
                            Text(group.groupName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text(
                                "${group.memberCount} 人",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    icon = {
                        GroupAvatar(name = group.groupName, size = 36.dp)
                    },
                    selected = selectedGroup?.groupId == group.groupId,
                    onClick = { onSelectGroup(group) },
                    modifier = Modifier.padding(horizontal = 12.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = PrimaryBlue.copy(alpha = 0.12f),
                    ),
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.UserListSection(
    users: List<OnlineUser>,
    currentUser: String,
    selectedUser: OnlineUser?,
    onSelectUser: (OnlineUser?) -> Unit,
) {
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
    
    Text(
        "在线用户",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )

    LazyColumn(modifier = Modifier.weight(1f)) {
        items(users.filter { it.nickname != currentUser }) { user ->
            NavigationDrawerItem(
                label = {
                    Column {
                        Text(user.nickname, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(6.dp).clip(CircleShape).background(SuccessGreen))
                            Text(" 在线", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                icon = {
                    TgAvatar(name = user.nickname, size = 36.dp)
                },
                selected = selectedUser?.userId == user.userId,
                onClick = { onSelectUser(user) },
                modifier = Modifier.padding(horizontal = 12.dp),
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = PrimaryBlue.copy(alpha = 0.12f),
                ),
            )
        }
    }
}

@Composable
private fun CreateGroupDialog(
    onDismiss: () -> Unit,
    onCreateGroup: (String) -> Unit,
) {
    var groupName by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "创建群聊",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column {
                Text(
                    "输入群聊名称",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
                
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it.take(20) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("群聊名称") },
                    placeholder = { Text("例如：技术交流群") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        cursorColor = PrimaryBlue,
                        focusedLabelColor = PrimaryBlue,
                    ),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreateGroup(groupName.trim()) },
                enabled = groupName.trim().isNotEmpty(),
            ) {
                Text("创建", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}
