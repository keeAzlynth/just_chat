package com.course.imchat.core.delegate

import android.net.Uri
import com.course.imchat.*
import com.course.imchat.data.MessageRepository
import java.util.UUID

/**
 * Builds and sends messages with local-echo.
 *
 * Decoupled from state management — just takes inputs, builds [ChatMessage],
 * sends via [MessageRepository], and returns the message for the caller to
 * insert into their state.
 *
 * Responsibilities:
 * — Construct [ChatMessage] objects with the correct fields
 * — Generate stable local message IDs
 * — Send the appropriate WebSocket message for each message type
 */
class MessageComposer(
    private val repository: MessageRepository,
) {

    /** The message factory supplies per-message context (myUserId, nickname, quote). */
    data class Context(
        val myUserId: String,
        val nickname: String,
        val quotingMessage: ChatMessage?,
    )

    /** Result of composing + sending a message. */
    data class Result(
        val localMessage: ChatMessage,
        val clearDraft: Boolean = true,
        val clearQuote: Boolean = true,
    )

    // ── Public compose methods ─────────────────────────

    fun composeText(
        ctx: Context,
        text: String,
        isPrivate: Boolean = false,
        receiverId: String = "",
    ): Result {
        val localId = localId()
        val quote = ctx.quotingMessage?.let {
            QuotedMessage(id = it.id, nickname = it.nickname, text = it.text)
        }
        val msg = ChatMessage(
            id = localId,
            userId = ctx.myUserId,
            nickname = ctx.nickname,
            text = text,
            timestampSeconds = nowSeconds(),
            isMine = true,
            isPrivate = isPrivate,
            receiverId = receiverId,
            deliveryStatus = DeliveryStatus.Sending,
            quotedMessage = quote,
        )
        return Result(msg)
    }

    fun composeAndSendText(
        ctx: Context,
        text: String,
        groupId: String? = null,
        receiverId: String? = null,
    ): Result {
        val localId = localId()
        val quote = ctx.quotingMessage?.let {
            QuotedMessage(id = it.id, nickname = it.nickname, text = it.text)
        }
        val isPrivate = receiverId != null
        val isGroup = groupId != null
        val msg = ChatMessage(
            id = localId,
            userId = ctx.myUserId,
            nickname = ctx.nickname,
            text = text,
            timestampSeconds = nowSeconds(),
            isMine = true,
            isPrivate = isPrivate,
            receiverId = receiverId ?: "",
            groupId = groupId ?: "",
            deliveryStatus = DeliveryStatus.Sending,
            quotedMessage = quote,
        )

        // Send via the correct WebSocket command
        when {
            groupId != null -> repository.sendGroupMessage(groupId, text)
            receiverId != null -> repository.sendPrivateMessage(text, receiverId)
            else -> repository.sendMessage(text)
        }
        return Result(msg)
    }

    fun composeAndSendImage(
        ctx: Context,
        uri: Uri,
        fileName: String,
        fileSize: Long,
    ): Result {
        val localId = localId()
        val msg = ChatMessage(
            id = localId,
            userId = ctx.myUserId,
            nickname = ctx.nickname,
            text = "[图片]",
            timestampSeconds = nowSeconds(),
            isMine = true,
            messageType = MessageType.Image,
            fileUrl = uri.toString(),
            fileName = fileName,
            fileSize = fileSize,
            deliveryStatus = DeliveryStatus.Sending,
        )
        repository.sendFile(uri.toString(), fileName, fileSize)
        return Result(msg)
    }

    fun composeAndSendFile(
        ctx: Context,
        uri: Uri,
        fileName: String,
        fileSize: Long,
    ): Result {
        val localId = localId()
        val msg = ChatMessage(
            id = localId,
            userId = ctx.myUserId,
            nickname = ctx.nickname,
            text = "[文件] $fileName",
            timestampSeconds = nowSeconds(),
            isMine = true,
            messageType = MessageType.File,
            fileUrl = uri.toString(),
            fileName = fileName,
            fileSize = fileSize,
            deliveryStatus = DeliveryStatus.Sending,
        )
        repository.sendFile(uri.toString(), fileName, fileSize)
        return Result(msg)
    }

    // ── Helpers ──────────────────────────────────────

    private fun localId() = "local_${UUID.randomUUID()}"
    private fun nowSeconds() = System.currentTimeMillis() / 1000
}
