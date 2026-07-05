package com.course.imchat.ui.components.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * 聊天背景 — 纯色平铺，不干扰气泡阅读。
 * Telegram 做法：背景就是一层淡灰，不做任何装饰。
 */
@Composable
fun ChatBackground() {
    val isDarkTheme = isSystemInDarkTheme()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (isDarkTheme) Color(0xFF0E1621)   // Telegram dark bg
                else Color(0xFFF6F7F9)                // Telegram light bg
            )
    )
}
