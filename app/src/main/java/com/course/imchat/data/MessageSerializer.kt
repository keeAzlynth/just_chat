package com.course.imchat.data

import com.course.imchat.OnlineUser
import com.google.gson.JsonObject
import com.google.gson.JsonParser

class MessageSerializer {
    fun register(username: String, password: String, nickname: String): String = JsonObject().apply {
        addProperty("type", "register")
        addProperty("username", username)
        addProperty("password", password)
        addProperty("nickname", nickname)
    }.toString()

    fun login(username: String, password: String): String = JsonObject().apply {
        addProperty("type", "login")
        addProperty("username", username)
        addProperty("password", password)
    }.toString()

    fun resumeSession(token: String): String = JsonObject().apply {
        addProperty("type", "resume_session")
        addProperty("sessionToken", token)
    }.toString()

    fun logout(): String = JsonObject().apply {
        addProperty("type", "logout")
    }.toString()

    fun join(nickname: String): String = JsonObject().apply {
        addProperty("type", "join")
        addProperty("nickname", nickname)
    }.toString()

    fun send(text: String): String = JsonObject().apply {
        addProperty("type", "send")
        addProperty("text", text)
    }.toString()

    fun sendFile(text: String, fileUrl: String, fileName: String, fileSize: Long): String = JsonObject().apply {
        addProperty("type", "send_file")
        addProperty("text", text)
        addProperty("fileUrl", fileUrl)
        addProperty("fileName", fileName)
        addProperty("fileSize", fileSize)
    }.toString()

    fun privateMessage(text: String, receiverId: String): String = JsonObject().apply {
        addProperty("type", "private_message")
        addProperty("text", text)
        addProperty("receiverId", receiverId)
    }.toString()

    fun typing(): String = JsonObject().apply {
        addProperty("type", "typing")
    }.toString()

    fun recallMessage(messageId: Long): String = JsonObject().apply {
        addProperty("type", "recall_message")
        addProperty("messageId", messageId)
    }.toString()

    fun deleteMessage(messageId: Long): String = JsonObject().apply {
        addProperty("type", "delete_message")
        addProperty("messageId", messageId)
    }.toString()

    fun editMessage(messageId: Long, newText: String): String = JsonObject().apply {
        addProperty("type", "edit_message")
        addProperty("messageId", messageId)
        addProperty("text", newText)
    }.toString()

    fun markRead(messageId: Long): String = JsonObject().apply {
        addProperty("type", "mark_read")
        addProperty("messageId", messageId)
    }.toString()

    fun createGroup(name: String): String = JsonObject().apply {
        addProperty("type", "create_group")
        addProperty("name", name)
    }.toString()

    fun joinGroup(groupId: String): String = JsonObject().apply {
        addProperty("type", "join_group")
        addProperty("groupId", groupId)
    }.toString()

    fun leaveGroup(groupId: String): String = JsonObject().apply {
        addProperty("type", "leave_group")
        addProperty("groupId", groupId)
    }.toString()

    fun getGroupMembers(groupId: String): String = JsonObject().apply {
        addProperty("type", "get_group_members")
        addProperty("groupId", groupId)
    }.toString()

    fun sendGroupMessage(groupId: String, text: String): String = JsonObject().apply {
        addProperty("type", "send_group_message")
        addProperty("groupId", groupId)
        addProperty("text", text)
    }.toString()

    fun getGroupMessageHistory(groupId: String, limit: Int = 50): String = JsonObject().apply {
        addProperty("type", "get_group_message_history")
        addProperty("groupId", groupId)
        addProperty("limit", limit)
    }.toString()

    fun kickUser(targetUserId: String, groupId: String): String = JsonObject().apply {
        addProperty("type", "kick_user")
        addProperty("targetUserId", targetUserId)
        addProperty("groupId", groupId)
    }.toString()

    fun banUser(targetUserId: String, groupId: String): String = JsonObject().apply {
        addProperty("type", "ban_user")
        addProperty("targetUserId", targetUserId)
        addProperty("groupId", groupId)
    }.toString()

    // Pin & Save
    fun pinMessage(messageId: Long, chatId: String): String = JsonObject().apply {
        addProperty("type", "pin_message")
        addProperty("messageId", messageId)
        addProperty("chatId", chatId)
    }.toString()

    fun unpinMessage(messageId: Long, chatId: String): String = JsonObject().apply {
        addProperty("type", "unpin_message")
        addProperty("messageId", messageId)
        addProperty("chatId", chatId)
    }.toString()

    fun getPinnedMessages(chatId: String): String = JsonObject().apply {
        addProperty("type", "get_pinned_messages")
        addProperty("chatId", chatId)
    }.toString()

    fun saveMessage(messageId: Long, chatId: String): String = JsonObject().apply {
        addProperty("type", "save_message")
        addProperty("messageId", messageId)
        addProperty("chatId", chatId)
    }.toString()

    fun unsaveMessage(messageId: Long): String = JsonObject().apply {
        addProperty("type", "unsave_message")
        addProperty("messageId", messageId)
    }.toString()

    fun getSavedMessages(): String = JsonObject().apply {
        addProperty("type", "get_saved_messages")
    }.toString()

    fun getOnlineUsers(): String = JsonObject().apply {
        addProperty("type", "get_online_users")
    }.toString()

    fun getUserInfo(targetUserId: String): String = JsonObject().apply {
        addProperty("type", "get_user_info")
        addProperty("targetUserId", targetUserId)
    }.toString()

    fun getMessageHistory(limit: Int = 50, before: Long = 0): String = JsonObject().apply {
        addProperty("type", "get_message_history")
        addProperty("limit", limit)
        if (before > 0) addProperty("before", before)
    }.toString()

    fun getPrivateMessageHistory(otherUserId: String, limit: Int = 50): String = JsonObject().apply {
        addProperty("type", "get_private_message_history")
        addProperty("otherUserId", otherUserId)
        addProperty("limit", limit)
    }.toString()

    fun parse(raw: String): IncomingEvent {
        val json = JsonParser.parseString(raw).asJsonObject
        return when (json.string("type")) {
            "logged_in" -> IncomingEvent.LoggedIn(
                userId = json.string("userId"),
                username = json.string("username"),
                nickname = json.string("nickname"),
                sessionToken = json.string("sessionToken"),
            )
            "joined" -> IncomingEvent.Joined(
                userId = json.string("userId"),
                nickname = json.string("nickname"),
            )
            "message" -> IncomingEvent.Message(
                messageId = json.long("messageId"),
                userId = json.string("userId"),
                nickname = json.string("nickname"),
                text = json.string("text"),
                timestampSeconds = json.long("ts"),
                messageType = json.string("messageType").ifEmpty { "text" },
                fileUrl = json.string("fileUrl"),
                fileName = json.string("fileName"),
                fileSize = json.long("fileSize"),
            )
            "private_message" -> IncomingEvent.PrivateMessage(
                messageId = json.long("messageId"),
                userId = json.string("userId"),
                nickname = json.string("nickname"),
                receiverId = json.string("receiverId"),
                text = json.string("text"),
                timestampSeconds = json.long("ts"),
            )
            "typing" -> IncomingEvent.Typing(
                userId = json.string("userId"),
                nickname = json.string("nickname"),
            )
            "message_recalled" -> IncomingEvent.MessageRecalled(
                messageId = json.long("messageId"),
                userId = json.string("userId"),
            )
            "message_read" -> IncomingEvent.MessageRead(
                messageId = json.long("messageId"),
                userId = json.string("userId"),
            )
            "online_users" -> {
                val usersArray = json.getAsJsonArray("users")
                val users = usersArray.map { element ->
                    val userObj = element.asJsonObject
                    OnlineUser(
                        userId = userObj.string("userId"),
                        nickname = userObj.string("nickname"),
                    )
                }
                IncomingEvent.OnlineUsers(users)
            }
            "message_history" -> {
                val messagesArray = json.getAsJsonArray("messages")
                val messages = messagesArray.map { element ->
                    val msgObj = element.asJsonObject
                    MessageData(
                        userId = msgObj.string("userId"),
                        nickname = msgObj.string("nickname"),
                        text = msgObj.string("text"),
                        timestampSeconds = msgObj.long("ts"),
                        type = msgObj.string("type"),
                        fileUrl = msgObj.string("fileUrl"),
                        fileName = msgObj.string("fileName"),
                        fileSize = msgObj.long("fileSize"),
                        isRecalled = msgObj.boolean("isRecalled"),
                    )
                }
                IncomingEvent.MessageHistory(messages)
            }
            "private_message_history" -> {
                val messagesArray = json.getAsJsonArray("messages")
                val messages = messagesArray.map { element ->
                    val msgObj = element.asJsonObject
                    MessageData(
                        userId = msgObj.string("userId"),
                        nickname = msgObj.string("nickname"),
                        receiverId = msgObj.string("receiverId"),
                        text = msgObj.string("text"),
                        timestampSeconds = msgObj.long("ts"),
                        type = msgObj.string("type"),
                        fileUrl = msgObj.string("fileUrl"),
                        fileName = msgObj.string("fileName"),
                        fileSize = msgObj.long("fileSize"),
                        isRecalled = msgObj.boolean("isRecalled"),
                    )
                }
                IncomingEvent.PrivateMessageHistory(
                    otherUserId = json.string("otherUserId"),
                    messages = messages,
                )
            }
            "group_message_history" -> {
                val messagesArray = json.getAsJsonArray("messages")
                val messages = messagesArray.map { element ->
                    val msgObj = element.asJsonObject
                    MessageData(
                        userId = msgObj.string("userId"),
                        nickname = msgObj.string("nickname"),
                        text = msgObj.string("text"),
                        timestampSeconds = msgObj.long("ts"),
                        type = msgObj.string("type"),
                        fileUrl = msgObj.string("fileUrl"),
                        fileName = msgObj.string("fileName"),
                        fileSize = msgObj.long("fileSize"),
                        isRecalled = msgObj.boolean("isRecalled"),
                    )
                }
                IncomingEvent.GroupMessageHistory(
                    groupId = json.string("groupId"),
                    messages = messages,
                )
            }
            "group_created" -> IncomingEvent.GroupCreated(
                groupId = json.string("groupId"),
                name = json.string("name"),
                ownerId = json.string("ownerId"),
            )
            "group_message" -> IncomingEvent.GroupMessage(
                messageId = json.long("messageId"),
                groupId = json.string("groupId"),
                userId = json.string("userId"),
                nickname = json.string("nickname"),
                text = json.string("text"),
                timestampSeconds = json.long("ts"),
            )
            "group_member_joined" -> IncomingEvent.GroupMemberJoined(
                groupId = json.string("groupId"),
                userId = json.string("userId"),
                nickname = json.string("nickname"),
            )
            "group_member_left" -> IncomingEvent.GroupMemberLeft(
                groupId = json.string("groupId"),
                userId = json.string("userId"),
                nickname = json.string("nickname"),
            )
            "group_members" -> {
                val membersArray = json.getAsJsonArray("members")
                val members = membersArray.map { element ->
                    val memberObj = element.asJsonObject
                    GroupMember(
                        userId = memberObj.string("userId"),
                        nickname = memberObj.string("nickname"),
                        level = memberObj.int("level"),
                        isAdmin = memberObj.boolean("isAdmin"),
                        isOwner = memberObj.boolean("isOwner"),
                    )
                }
                IncomingEvent.GroupMembers(
                    groupId = json.string("groupId"),
                    members = members,
                )
            }
            "user_kicked" -> IncomingEvent.UserKicked(
                groupId = json.string("groupId"),
                userId = json.string("userId"),
                kickedBy = json.string("kickedBy"),
            )
            "user_banned" -> IncomingEvent.UserBanned(
                groupId = json.string("groupId"),
                userId = json.string("userId"),
                bannedBy = json.string("bannedBy"),
            )
            "error" -> IncomingEvent.ServerError(
                code = json.string("code"),
                message = json.string("message"),
            )
            "message_edited" -> IncomingEvent.MessageEdited(
                messageId = json.long("messageId"),
                userId = json.string("userId"),
                text = json.string("text"),
                timestampSeconds = json.long("ts"),
            )
            "message_deleted" -> IncomingEvent.MessageDeleted(
                messageId = json.long("messageId"),
                userId = json.string("userId"),
            )
            "message_pinned" -> IncomingEvent.MessagePinned(
                messageId = json.long("messageId"),
                chatId = json.string("chatId"),
                pinnedBy = json.string("pinnedBy"),
                timestampSeconds = json.long("ts"),
            )
            "message_unpinned" -> IncomingEvent.MessageUnpinned(
                messageId = json.long("messageId"),
                chatId = json.string("chatId"),
            )
            "pinned_messages" -> {
                val messagesArray = json.getAsJsonArray("messages")
                val messages = messagesArray.map { element ->
                    val msgObj = element.asJsonObject
                    MessageData(
                        userId = msgObj.string("userId"),
                        nickname = msgObj.string("nickname"),
                        text = msgObj.string("text"),
                        timestampSeconds = msgObj.long("ts"),
                    )
                }
                IncomingEvent.PinnedMessages(
                    chatId = json.string("chatId"),
                    messages = messages,
                )
            }
            "message_saved" -> IncomingEvent.MessageSaved(
                messageId = json.long("messageId"),
                chatId = json.string("chatId"),
            )
            "message_unsaved" -> IncomingEvent.MessageUnsaved(
                messageId = json.long("messageId"),
            )
            "saved_messages" -> {
                val messagesArray = json.getAsJsonArray("messages")
                val messages = messagesArray.map { element ->
                    val msgObj = element.asJsonObject
                    MessageData(
                        userId = msgObj.string("userId"),
                        nickname = msgObj.string("nickname"),
                        text = msgObj.string("text"),
                        timestampSeconds = msgObj.long("ts"),
                    )
                }
                IncomingEvent.SavedMessages(messages = messages)
            }
            "user_info" -> IncomingEvent.UserInfo(
                userId = json.string("userId"),
                username = json.string("username"),
                nickname = json.string("nickname"),
                level = json.int("level"),
                online = json.boolean("online"),
                lastActive = json.long("lastActive"),
            )
            else -> IncomingEvent.ServerError("INVALID_FORMAT", "消息格式错误")
        }
    }

    private fun JsonObject.string(name: String): String = get(name)?.asString.orEmpty()
    private fun JsonObject.long(name: String): Long = get(name)?.asLong ?: 0L
    private fun JsonObject.int(name: String): Int = get(name)?.asInt ?: 0
    private fun JsonObject.boolean(name: String): Boolean = get(name)?.asBoolean ?: false
}
