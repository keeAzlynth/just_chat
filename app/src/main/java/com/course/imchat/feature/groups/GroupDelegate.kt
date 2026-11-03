package com.course.imchat.feature.groups

import com.course.imchat.ChatGroup
import com.course.imchat.ChatUiState
import com.course.imchat.OnlineUser
import com.course.imchat.data.GroupMember
import com.course.imchat.data.MessageRepository
import com.course.imchat.data.cache.MessageCache
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * Handles group operations: create, join, leave, kick, ban, select.
 * v2.1: Moved to feature/groups module.
 */
class GroupDelegate(
    private val state: MutableStateFlow<ChatUiState>,
    private val repository: MessageRepository,
    private val messageCache: MessageCache? = null,
) {
    fun createGroup(name: String) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return
        repository.createGroup(cleanName)
        state.update { it.copy(ui = it.ui.copy(showCreateGroupDialog = false)) }
    }

    fun joinGroup(groupId: String) = repository.joinGroup(groupId)

    fun leaveGroup(groupId: String) {
        repository.leaveGroup(groupId)
        state.update { it.copy(nav = it.nav.copy(selectedGroup = null)) }
    }

    fun selectGroup(group: ChatGroup?) {
        // 取消选中时回到公共频道，而不是留下 "group_" 这种半截 id
        val chatId = if (group != null) "group_${group.groupId}" else PUBLIC_CHAT_ID
        state.update {
            it.copy(
                nav = it.nav.copy(selectedGroup = group),
                chat = it.chat.copy(selectedPrivateUser = null, currentChatId = chatId),
            )
        }
        messageCache?.clearUnreadCount(chatId)
    }

    fun selectPrivateUser(user: OnlineUser?) {
        val chatId = if (user != null) "private_${user.userId}" else PUBLIC_CHAT_ID
        state.update {
            it.copy(
                nav = it.nav.copy(selectedGroup = null),
                chat = it.chat.copy(selectedPrivateUser = user, currentChatId = chatId),
            )
        }
        messageCache?.clearUnreadCount(chatId)
    }

    private companion object {
        const val PUBLIC_CHAT_ID = "public"
    }

    fun kickUser(targetUserId: String, groupId: String) = repository.kickUser(targetUserId, groupId)

    fun banUser(targetUserId: String, groupId: String) = repository.banUser(targetUserId, groupId)

    fun toggleGroupManagement() {
        state.update { it.copy(ui = it.ui.copy(showGroupManagement = !it.ui.showGroupManagement)) }
    }

    fun onGroupCreated(groupId: String, name: String, ownerId: String) {
        val group = ChatGroup(groupId = groupId, groupName = name, memberCount = 1)
        state.update {
            it.copy(
                nav = it.nav.copy(groups = it.nav.groups + group),
                ui = it.ui.copy(showCreateGroupDialog = false),
            )
        }
    }

    fun onGroupMembers(groupId: String, members: List<GroupMember>) {
        val onlineMembers = members.map {
            OnlineUser(userId = it.userId, nickname = it.nickname)
        }
        state.update { s ->
            s.copy(nav = s.nav.copy(groups = s.nav.groups.map { g ->
                if (g.groupId == groupId) g.copy(members = onlineMembers, memberCount = members.size) else g
            }))
        }
    }

    fun onGroupMemberJoined(groupId: String, userId: String, nickname: String) {
        state.update { s ->
            s.copy(nav = s.nav.copy(groups = s.nav.groups.map { g ->
                if (g.groupId == groupId) g.copy(memberCount = g.memberCount + 1) else g
            }))
        }
    }

    fun showCreateGroupDialog() {
        state.update { it.copy(ui = it.ui.copy(showCreateGroupDialog = true)) }
    }

    fun dismissCreateGroupDialog() {
        state.update { it.copy(ui = it.ui.copy(showCreateGroupDialog = false)) }
    }
}
