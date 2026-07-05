package com.course.imchat.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Telegram-style Avatar ───────────────────────────────────
@Composable
fun TgAvatar(
    name: String,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val color = avatarColor(name)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.take(1).uppercase(),
            style = if (size >= 48.dp) MaterialTheme.typography.titleSmall
                    else MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}

// ── Animated Logo Component ─────────────────────────────────
@Composable
fun AnimatedLogo(
    icon: ImageVector,
    size: Dp = 110.dp,
    iconSize: Dp = 55.dp,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "logo")
    
    val logoScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = androidx.compose.animation.core.EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoScale"
    )
    
    val logoRotation by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = androidx.compose.animation.core.EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoRotation"
    )
    
    Box(
        modifier = Modifier
            .size(size)
            .scale(logoScale)
            .graphicsLayer(rotationZ = logoRotation)
            .clip(CircleShape)
            .background(PrimaryGradient)
            .shadow(
                elevation = 16.dp,
                shape = CircleShape,
                ambientColor = PrimaryBlue.copy(alpha = 0.4f),
                spotColor = PrimaryBlue.copy(alpha = 0.6f)
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            tint = Color.White,
        )
    }
}

// ── Group Avatar ──────────────────────────────────────────
@Composable
fun GroupAvatar(
    name: String,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size / 4))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        AccentPurple.copy(alpha = 0.8f),
                        PrimaryBlue.copy(alpha = 0.8f),
                    )
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Group,
            contentDescription = null,
            modifier = Modifier.size(size * 0.55f),
            tint = Color.White,
        )
    }
}

// ── Logo Section with Title ─────────────────────────────────
@Composable
fun LogoSection(
    icon: ImageVector,
    title: String,
    subtitle: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(bottom = 40.dp),
    ) {
        AnimatedLogo(icon = icon)
        
        Spacer(modifier = Modifier.height(28.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge.copy(
                brush = PrimaryGradientHorizontal
            ),
            fontWeight = FontWeight.Bold,
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.5.sp,
        )
    }
}
