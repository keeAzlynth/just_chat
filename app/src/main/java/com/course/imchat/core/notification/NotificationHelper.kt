package com.course.imchat.core.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.course.imchat.MainActivity

/**
 * Local notification helper — Telegram-style message push notifications.
 * Shows notification when app is in background and a new message arrives.
 */
object NotificationHelper {

    private const val CHANNEL_ID = "imchat_messages"
    private const val CHANNEL_NAME = "新消息"
    private const val GROUP_KEY = "imchat_group"
    private const val SUMMARY_ID = 0

    // Notification ID counter per conversation
    private val notificationIds = mutableMapOf<String, Int>()
    private var nextId = 100

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "聊天消息通知"
                enableVibration(true)
                setShowBadge(true)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Show a notification for an incoming message.
     * Groups notifications by chat to avoid spamming.
     */
    fun showMessageNotification(
        context: Context,
        chatId: String,
        chatTitle: String,
        senderName: String,
        messageText: String,
    ) {
        if (!hasPermission(context)) return

        val id = notificationIds.getOrPut(chatId) { nextId++ }

        // Intent to open the app on tap
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("chat_id", chatId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(senderName)
            .setContentText(messageText.take(200))
            .setSubText(chatTitle)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setGroup(GROUP_KEY)
            .setContentIntent(pendingIntent)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(messageText.take(500))
            )
            .build()

        NotificationManagerCompat.from(context).notify(id, notification)

        // Show group summary
        showGroupSummary(context)
    }

    private fun showGroupSummary(context: Context) {
        val summary = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("新消息")
            .setContentText("你有未读消息")
            .setAutoCancel(true)
            .setGroup(GROUP_KEY)
            .setGroupSummary(true)
            .setStyle(
                NotificationCompat.InboxStyle()
                    .setSummaryText("未读消息")
            )
            .build()
        NotificationManagerCompat.from(context).notify(SUMMARY_ID, summary)
    }

    fun cancelChatNotifications(context: Context, chatId: String) {
        val id = notificationIds[chatId] ?: return
        NotificationManagerCompat.from(context).cancel(id)
    }

    fun cancelAll(context: Context) {
        NotificationManagerCompat.from(context).cancelAll()
    }

    private fun hasPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }
}
