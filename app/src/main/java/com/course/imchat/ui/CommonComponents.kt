package com.course.imchat.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Telegram-style user avatar with optional online dot */
@Composable
fun TgAvatar(
    name: String,
    size: Dp,
    modifier: Modifier = Modifier,
    isOnline: Boolean = false,
) {
    Box(modifier = modifier.size(size)) {
        Box(
            modifier = Modifier.size(size).clip(CircleShape).background(avatarColor(name)),
            contentAlignment = Alignment.Center,
        ) {
            Text(name.take(1).uppercase(),
                style = if (size >= 48.dp) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold, color = Color.White)
        }
        if (isOnline) {
            Box(Modifier.align(Alignment.BottomEnd).offset(2.dp, 2.dp).size(10.dp).background(SuccessGreen, CircleShape))
        }
    }
}

/** Group avatar with gradient background */
@Composable
fun GroupAvatar(
    name: String,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.size(size).clip(RoundedCornerShape(size / 4))
            .background(Brush.linearGradient(listOf(PrimaryBlue, PrimaryBlueDark))),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Default.Group, null, Modifier.size(size * 0.55f), tint = Color.White)
    }
}

// ── Skeleton / Shimmer Loading (v2.3) ─────────────────────

/**
 * A shimmer-animated placeholder box for skeleton loading.
 * Mimics Telegram's shimmer effect with a gradient sweep animation.
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = true,
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateX by transition.animateFloat(
        initialValue = -300f,
        targetValue = 300f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerTranslate",
    )

    val baseColor = if (isDarkTheme) Color(0xFF2A3340) else Color(0xFFE0E0E0)
    val shimmerColor = if (isDarkTheme) Color(0xFF3D4A5C) else Color(0xFFF0F0F0)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(baseColor, shimmerColor, baseColor),
                    start = Offset(translateX, 0f),
                    end = Offset(translateX + 100f, 0f),
                )
            ),
    )
}

/** Skeleton chat message placeholder row */
@Composable
fun SkeletonMessageBubble(isDarkTheme: Boolean = true) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        ShimmerBox(modifier = Modifier.size(40.dp).clip(CircleShape), isDarkTheme = isDarkTheme)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.4f).height(12.dp), isDarkTheme = isDarkTheme)
            Spacer(Modifier.height(6.dp))
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.75f).height(24.dp), isDarkTheme = isDarkTheme)
            Spacer(Modifier.height(4.dp))
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.55f).height(24.dp), isDarkTheme = isDarkTheme)
        }
    }
}

/** Loading state for chat history */
@Composable
fun ChatLoadingState(
    count: Int = 8,
    isDarkTheme: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        repeat(count) {
            SkeletonMessageBubble(isDarkTheme = isDarkTheme)
        }
    }
}
