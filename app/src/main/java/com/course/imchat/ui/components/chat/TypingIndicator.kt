package com.course.imchat.ui.components.chat

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.course.imchat.ui.PrimaryBlue

@Composable
fun TypingIndicator(users: Set<String>) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    val dotAlphas = List(3) { i ->
        infiniteTransition.animateFloat(
            initialValue = 0.3f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(500, delayMillis = i * 200), RepeatMode.Reverse),
            label = "dot$i",
        )
    }
    val dotScales = List(3) { i ->
        infiniteTransition.animateFloat(
            initialValue = 0.8f, targetValue = 1.2f,
            animationSpec = infiniteRepeatable(tween(500, delayMillis = i * 200), RepeatMode.Reverse),
            label = "dotScale$i",
        )
    }

    val isDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    
    Surface(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(20.dp),
        color = if (isDarkTheme) androidx.compose.ui.graphics.Color.White.copy(alpha = 0.05f) 
                else androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.03f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                dotAlphas.forEachIndexed { index, alpha ->
                    Box(
                        Modifier
                            .size(6.dp)
                            .scale(dotScales[index].value)
                            .clip(CircleShape)
                            .background(PrimaryBlue.copy(alpha = alpha.value))
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${users.joinToString("、")} 正在输入",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
