package com.course.imchat.ui

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ── Rich accent palette ──────────────────────────────────

val PrimaryBlue      = Color(0xFF4A90D9)  // cornflower — richer than Telegram blue
val PrimaryBlueDark  = Color(0xFF3A7BC8)
val PrimaryBlueLight = Color(0xFF6BA5F0)
val AccentGreen      = Color(0xFF34C759)  // vibrant green (send, success)
val AccentOrange     = Color(0xFFFF9500)  // warm orange (headers, warnings)
val AccentPurple     = Color(0xFFAF52DE)  // rich purple (code, image)
val AccentTeal       = Color(0xFF5AC8FA)  // sky blue (quotes, lists)
val AccentPink       = Color(0xFFFF6B6B)  // coral (destructive alt)
val ErrorRed         = Color(0xFFFF3B30)
val WarningAmber     = Color(0xFFFF9500)
val SuccessGreen     = Color(0xFF34C759)

// ── Bubble colors — Telegram-inspired with richer tones ──

val SentBubbleLight    = Color(0xFFE8F8E8)  // soft mint green
val SentBubbleDark     = Color(0xFF1E4D3A)  // deep forest green (better contrast)
val SentBorderLight    = Color(0xFFC8E6C9)  // green border
val SentBorderDark     = Color(0xFF2D6A4F)

val ReceivedBubbleLight = Color(0xFFF5F7FA)  // warm white with slight blue tint
val ReceivedBubbleDark  = Color(0xFF1E2A3A)  // deep navy
val ReceivedBorderLight  = Color(0xFFDFE3E8)
val ReceivedBorderDark   = Color(0xFF2D3A4A)

// ── Gradients ────────────────────────────────────────────

val PrimaryGradient = Brush.linearGradient(listOf(PrimaryBlue, PrimaryBlueDark))
val PrimaryGradientHorizontal = Brush.horizontalGradient(listOf(PrimaryBlue, PrimaryBlueDark))

// ── Avatar palette — 14 vibrant colors ───────────────────

private val avatarColors = listOf(
    Color(0xFF4A90D9), Color(0xFFFF6B6B), Color(0xFF34C759),
    Color(0xFFFFD43B), Color(0xFFAF52DE), Color(0xFF5AC8FA),
    Color(0xFFFF9500), Color(0xFFFF648C), Color(0xFF20C997),
    Color(0xFF7476E8), Color(0xFFFD7E14), Color(0xFF63C6F7),
    Color(0xFFE06666), Color(0xFF93C54B),
)
fun avatarColor(name: String): Color =
    avatarColors[name.hashCode().and(0x7FFFFFFF) % avatarColors.size]

// ── Legacy compat ────────────────────────────────────────
@Deprecated("Use AccentGreen", ReplaceWith("AccentGreen"))
val TelegramBlue = Color(0xFF3390EC)
