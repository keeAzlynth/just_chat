package com.course.imchat.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Modern Dark Theme (Slate/Indigo) ─────────────────────────
private val DarkColors = darkColorScheme(
    primary            = Color(0xFF818CF8),  // Indigo-400
    secondary          = Color(0xFFA78BFA),  // Violet-400
    tertiary           = Color(0xFFFBBF24),  // Amber-400
    background         = Color(0xFF0F172A),  // Slate-900
    surface            = Color(0xFF1E293B),  // Slate-800
    surfaceVariant     = Color(0xFF334155),  // Slate-700
    surfaceTint        = Color.Transparent,
    onPrimary          = Color.White,
    onSecondary        = Color.White,
    onBackground       = Color(0xFFF1F5F9),  // Slate-100
    onSurface          = Color(0xFFF1F5F9),  // Slate-100
    onSurfaceVariant    = Color(0xFF94A3B8),  // Slate-400
    primaryContainer   = Color(0xFF312E81),  // Indigo-900
    secondaryContainer = Color(0xFF4C1D95),  // Violet-900
    tertiaryContainer  = Color(0xFF78350F),  // Amber-900
    outlineVariant     = Color(0xFF475569),  // Slate-600
    error              = Color(0xFFF87171),  // Red-400
    onError            = Color.White,
    inverseSurface     = Color(0xFFF1F5F9),  // Slate-100
    inverseOnSurface   = Color(0xFF0F172A),  // Slate-900
)

// ── Modern Light Theme (Indigo) ──────────────────────────────
private val LightColors = lightColorScheme(
    primary            = Color(0xFF6366F1),  // Indigo-500
    secondary          = Color(0xFF8B5CF6),  // Violet-500
    tertiary           = Color(0xFFF59E0B),  // Amber-500
    background         = Color(0xFFF8FAFC),  // Slate-50
    surface            = Color(0xFFFFFFFF),
    surfaceVariant     = Color(0xFFF1F5F9),  // Slate-100
    surfaceTint        = Color.Transparent,
    onPrimary          = Color.White,
    onSecondary        = Color.White,
    onBackground       = Color(0xFF0F172A),  // Slate-900
    onSurface          = Color(0xFF0F172A),  // Slate-900
    onSurfaceVariant    = Color(0xFF64748B),  // Slate-500
    primaryContainer   = Color(0xFFE0E7FF),  // Indigo-100
    secondaryContainer = Color(0xFFEDE9FE),  // Violet-100
    tertiaryContainer  = Color(0xFFFEF3C7),  // Amber-100
    outlineVariant     = Color(0xFFE2E8F0),  // Slate-200
    error              = Color(0xFFEF4444),  // Red-500
    onError            = Color.White,
    inverseSurface     = Color(0xFF0F172A),  // Slate-900
    inverseOnSurface   = Color(0xFFFFFFFF),
)

@Composable
fun IMChatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme: ColorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content,
    )
}
