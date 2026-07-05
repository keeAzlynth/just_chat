package com.course.imchat.ui

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Custom accent colors (MaterialKolor handles all theme colors from seed #3390EC)
val PrimaryBlue  = Color(0xFF3390EC)
val PrimaryBlueDark = Color(0xFF2B7CD3)
val PrimaryBlueLight = Color(0xFF5FAAFF)
val SuccessGreen = Color(0xFF4CD964)
val ErrorRed      = Color(0xFFFF3B30)
val WarningAmber  = Color(0xFFFF9500)

// Legacy compat aliases
val AccentPurple = Color(0xFF8774E1)
val AccentPink   = Color(0xFFFF6B6B)

// Bubble tints (overrides — MaterialKolor can't know these)
val SentBubbleLight    = Color(0xFFEEFFDE)
val SentBubbleDark     = Color(0xFF2B5278)
val ReceivedBubbleLight = Color(0xFFFFFFFF)
val ReceivedBubbleDark  = Color(0xFF182533)

// Subtle gradients (still needed for decorative purposes)
val PrimaryGradient = Brush.linearGradient(listOf(PrimaryBlue, PrimaryBlueDark))
val PrimaryGradientHorizontal = Brush.horizontalGradient(listOf(PrimaryBlue, PrimaryBlueDark))

// Avatar palette
private val avatarColors = listOf(
    Color(0xFF4DABF7), Color(0xFFFF6B6B), Color(0xFF51CF66),
    Color(0xFFFFD43B), Color(0xFFCC5DE8), Color(0xFFFF8787),
    Color(0xFF20C997), Color(0xFFF06595),
)
fun avatarColor(name: String): Color = avatarColors[name.hashCode().and(0x7FFFFFFF) % avatarColors.size]
