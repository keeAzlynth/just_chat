package com.course.imchat.ui

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ── Modern Color Palette ─────────────────────────────────────
val PrimaryBlue = Color(0xFF6366F1)      // Indigo-500
val PrimaryBlueDark = Color(0xFF4F46E5)  // Indigo-600
val PrimaryBlueLight = Color(0xFF818CF8) // Indigo-400
val AccentPurple = Color(0xFFA78BFA)     // Violet-400
val AccentPink = Color(0xFFF472B6)       // Pink-400
val SuccessGreen = Color(0xFF34D399)     // Emerald-400
val WarningAmber = Color(0xFFFBBF24)     // Amber-400
val ErrorRed = Color(0xFFF87171)         // Red-400

val SentBubbleLight = Color(0xFF6366F1)
val SentBubbleDark = Color(0xFF4F46E5)
val ReceivedBubbleLight = Color(0xFFFFFFFF)
val ReceivedBubbleDark = Color(0xFF1E293B)  // Slate-800
val BgLight = Color(0xFFF8FAFC)             // Slate-50
val BgDark = Color(0xFF0F172A)              // Slate-900
val SurfaceDark = Color(0xFF1E293B)         // Slate-800
val SurfaceLight = Color(0xFFFFFFFF)

// Gradient brushes
val PrimaryGradient = Brush.linearGradient(
    colors = listOf(PrimaryBlue, AccentPurple)
)
val PrimaryGradientHorizontal = Brush.horizontalGradient(
    colors = listOf(PrimaryBlue, AccentPurple)
)

// ── Avatar color palette (stable per-user) ──────────────────
private val avatarColors = listOf(
    Color(0xFF6AB2F2), Color(0xFFF28B6A), Color(0xFF6AF2B8),
    Color(0xFFF2C86A), Color(0xFFB86AF2), Color(0xFFF26A8B),
    Color(0xFF6AF2E0), Color(0xFFF29E6A),
)

fun avatarColor(name: String): Color =
    avatarColors[name.hashCode().and(0x7FFFFFFF) % avatarColors.size]
