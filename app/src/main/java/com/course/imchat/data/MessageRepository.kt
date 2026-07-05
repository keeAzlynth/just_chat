package com.course.imchat.data

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import okhttp3.OkHttpClient

class MessageRepository {
    private val eventSink = MutableSharedFlow<IncomingEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<IncomingEvent> = eventSink

    private val serializer = MessageSerializer()
    private val client = OkHttpClient.Builder().build()
    private var webSocketClient: WebSocketClient? = null

    fun connect(url: String) {
        // Close any existing connection to avoid duplicate sockets
        webSocketClient?.close()
        webSocketClient = WebSocketClient(
            client = client,
            serializer = serializer,
            events = eventSink,
        ).also { it.connect(url, "") }
    }

    fun register(username: String, password: String, nickname: String): Boolean {
        return webSocketClient?.send(serializer.register(username, password, nickname)) ?: false
    }

    fun login(username: String, password: String): Boolean {
        return webSocketClient?.send(serializer.login(username, password)) ?: false
    }

    fun resumeSession(token: String): Boolean {
        return webSocketClient?.send(serializer.resumeSession(token)) ?: false
    }

    fun logout(): Boolean {
        return webSocketClient?.send(serializer.logout()) ?: false
    }

    fun join(nickname: String): Boolean {
        return webSocketClient?.send(serializer.join(nickname)) ?: false
    }

    fun sendMessage(text: String): Boolean {
        return webSocketClient?.send(serializer.send(text)) ?: false
    }

    fun sendPrivateMessage(text: String, receiverId: String): Boolean {
        return webSocketClient?.send(serializer.privateMessage(text, receiverId)) ?: false
    }

    fun sendFile(fileUrl: String, fileName: String, fileSize: Long): Boolean {
        return webSocketClient?.send(serializer.sendFile("[文件]", fileUrl, fileName, fileSize)) ?: false
    }

    fun sendTyping(): Boolean {
        return webSocketClient?.send(serializer.typing()) ?: false
    }

    fun getOnlineUsers(): Boolean {
        return webSocketClient?.send(serializer.getOnlineUsers()) ?: false
    }

    fun getUserInfo(targetUserId: String): Boolean {
        return webSocketClient?.send(serializer.getUserInfo(targetUserId)) ?: false
    }

    fun getMessageHistory(limit: Int = 50, before: Long = 0): Boolean {
        return webSocketClient?.send(serializer.getMessageHistory(limit, before)) ?: false
    }

    fun getPrivateMessageHistory(otherUserId: String, limit: Int = 50): Boolean {
        return webSocketClient?.send(serializer.getPrivateMessageHistory(otherUserId, limit)) ?: false
    }

    fun recallMessage(messageId: String): Boolean {
        val id = messageId.removePrefix("local_").toLongOrNull() ?: return false
        return webSocketClient?.send(serializer.recallMessage(id)) ?: false
    }

    fun createGroup(groupName: String): Boolean {
        return webSocketClient?.send(serializer.createGroup(groupName)) ?: false
    }

    fun getGroupMessageHistory(groupId: String, limit: Int): Boolean {
        return webSocketClient?.send(serializer.getGroupMessageHistory(groupId, limit)) ?: false
    }

    fun joinGroup(groupId: String): Boolean {
        return webSocketClient?.send(serializer.joinGroup(groupId)) ?: false
    }

    fun leaveGroup(groupId: String): Boolean {
        return webSocketClient?.send(serializer.leaveGroup(groupId)) ?: false
    }

    fun sendGroupMessage(groupId: String, text: String): Boolean {
        return webSocketClient?.send(serializer.sendGroupMessage(groupId, text)) ?: false
    }

    fun getGroupMembers(groupId: String): Boolean {
        return webSocketClient?.send(serializer.getGroupMembers(groupId)) ?: false
    }

    fun kickUser(targetUserId: String, groupId: String): Boolean {
        return webSocketClient?.send(serializer.kickUser(targetUserId, groupId)) ?: false
    }

    fun banUser(targetUserId: String, groupId: String): Boolean {
        return webSocketClient?.send(serializer.banUser(targetUserId, groupId)) ?: false
    }

    // Edit & Delete
    fun editMessage(messageId: String, newText: String): Boolean {
        val id = messageId.removePrefix("local_").removePrefix("pm_").removePrefix("grp_")
            .removePrefix("hist_").toLongOrNull() ?: return false
        return webSocketClient?.send(serializer.editMessage(id, newText)) ?: false
    }

    fun deleteMessage(messageId: String): Boolean {
        val id = messageId.removePrefix("local_").removePrefix("pm_").removePrefix("grp_")
            .removePrefix("hist_").toLongOrNull() ?: return false
        return webSocketClient?.send(serializer.deleteMessage(id)) ?: false
    }

    // Pin & Save
    fun pinMessage(messageId: String, chatId: String): Boolean {
        val id = messageId.removePrefix("local_").removePrefix("pm_").removePrefix("grp_")
            .removePrefix("hist_").toLongOrNull() ?: return false
        return webSocketClient?.send(serializer.pinMessage(id, chatId)) ?: false
    }

    fun unpinMessage(messageId: String, chatId: String): Boolean {
        val id = messageId.removePrefix("local_").removePrefix("pm_").removePrefix("grp_")
            .removePrefix("hist_").toLongOrNull() ?: return false
        return webSocketClient?.send(serializer.unpinMessage(id, chatId)) ?: false
    }

    fun getPinnedMessages(chatId: String): Boolean {
        return webSocketClient?.send(serializer.getPinnedMessages(chatId)) ?: false
    }

    fun saveMessageToCollection(messageId: String, chatId: String): Boolean {
        val id = messageId.removePrefix("local_").removePrefix("pm_").removePrefix("grp_")
            .removePrefix("hist_").toLongOrNull() ?: return false
        return webSocketClient?.send(serializer.saveMessage(id, chatId)) ?: false
    }

    fun unsaveMessageFromCollection(messageId: String): Boolean {
        val id = messageId.removePrefix("local_").removePrefix("pm_").removePrefix("grp_")
            .removePrefix("hist_").toLongOrNull() ?: return false
        return webSocketClient?.send(serializer.unsaveMessage(id)) ?: false
    }

    fun getSavedMessages(): Boolean {
        return webSocketClient?.send(serializer.getSavedMessages()) ?: false
    }

    fun disconnect() {
        webSocketClient?.close()
        webSocketClient = null
    }
}
