package com.course.imchat.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.materialkolor.rememberDynamicColorScheme

/**
 * Telegram-inspired theme using MaterialKolor's HCT color science.
 * Seed color: Telegram Blue (#3390EC).
 * MaterialKolor auto-generates all dark/light palettes from this single color.
 */
@Composable
fun IMChatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = rememberDynamicColorScheme(
        seedColor = androidx.compose.ui.graphics.Color(0xFF3390EC),
        isDark = darkTheme,
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content,
    )
}
