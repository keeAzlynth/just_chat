package com.course.imchat.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.materialkolor.rememberDynamicColorScheme

/**
 * Rich modern theme using MaterialKolor HCT color science.
 * Seed color: rich cornflower blue (#4A90D9).
 * Produces a vibrant, layered palette in both light and dark modes.
 */
@Composable
fun IMChatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = rememberDynamicColorScheme(
        seedColor = Color(0xFF4A90D9),  // richer than the old #3390EC
        isDark = darkTheme,
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content,
    )
}
