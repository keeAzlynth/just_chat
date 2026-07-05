package com.course.imchat.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.sin

@Composable
fun AnimatedBackground() {
    val isDarkTheme = isSystemInDarkTheme()
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    
    // 缓存背景渐变颜色
    val backgroundColors = remember(isDarkTheme) {
        if (isDarkTheme) listOf(
            Color(0xFF0F172A),
            Color(0xFF1E293B),
            Color(0xFF0F172A),
        ) else listOf(
            Color(0xFFF0F4FF),
            Color(0xFFF8FAFC),
            Color(0xFFEEF2FF),
        )
    }
    
    // 使用单个动画值驱动所有效果
    val animationProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "progress"
    )
    
    // 缓存bubble配置
    val bubbles = remember {
        listOf(
            Triple(120f, -0.1f, 0.2f),
            Triple(180f, 0.8f, 0.1f),
            Triple(100f, 0.3f, 0.7f),
            Triple(150f, 0.9f, 0.6f),
        )
    }
    
    val bubbleColors = remember(isDarkTheme) {
        if (isDarkTheme) listOf(
            PrimaryBlue.copy(alpha = 0.06f),
            AccentPurple.copy(alpha = 0.05f),
            AccentPink.copy(alpha = 0.04f),
            PrimaryBlueLight.copy(alpha = 0.05f),
        ) else listOf(
            PrimaryBlue.copy(alpha = 0.08f),
            AccentPurple.copy(alpha = 0.06f),
            AccentPink.copy(alpha = 0.05f),
            PrimaryBlueLight.copy(alpha = 0.07f),
        )
    }
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        // 绘制背景渐变
        drawRect(
            brush = Brush.verticalGradient(colors = backgroundColors),
            size = size
        )
        
        // 绘制动画气泡
        bubbles.forEachIndexed { index, (baseSize, startX, startY) ->
            val offset = sin(animationProgress * Math.PI * 2 + index * 1.5).toFloat() * 0.1f
            val scale = 0.9f + sin(animationProgress * Math.PI * 3 + index * 2).toFloat() * 0.1f
            
            val centerX = size.width * (startX + offset)
            val centerY = size.height * (startY + offset * 0.5f)
            val radius = baseSize * scale * density
            
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        bubbleColors[index],
                        bubbleColors[index].copy(alpha = bubbleColors[index].alpha * 0.3f),
                        Color.Transparent,
                    ),
                    center = Offset(centerX, centerY),
                    radius = radius,
                ),
                radius = radius,
                center = Offset(centerX, centerY),
            )
        }
        
        // 绘制网格渐变覆盖层
        val meshAlpha = sin(animationProgress * Math.PI).toFloat() * 0.03f
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    PrimaryBlue.copy(alpha = meshAlpha),
                    AccentPurple.copy(alpha = 0.02f * (1 - animationProgress)),
                    AccentPink.copy(alpha = meshAlpha),
                ),
                start = Offset(0f, 0f),
                end = Offset(size.width, size.height),
            ),
            size = size
        )
    }
}
