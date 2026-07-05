package com.course.imchat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
