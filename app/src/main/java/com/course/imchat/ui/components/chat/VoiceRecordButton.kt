package com.course.imchat.ui.components.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardVoice
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.course.imchat.ui.PrimaryBlue
import kotlin.math.roundToInt

/**
 * 语音录制按钮 - 仿微信/QQ 长按录音功能
 */
@Composable
fun VoiceRecordButton(
    isRecording: Boolean,
    recordingSeconds: Float,
    onStartRecording: () -> Unit,
    onStopRecording: (cancelled: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = isSystemInDarkTheme()

    // Recording animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseScale",
    )

    var dragOffset by remember { mutableFloatStateOf(0f) }
    val cancelThreshold = -80f

    Box(modifier = modifier) {
        // Normal mic button (when not recording)
        if (!isRecording) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .shadow(2.dp, CircleShape)
                    .clip(CircleShape)
                    .background(
                        if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9)
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                onStartRecording()
                                tryAwaitRelease()
                                onStopRecording(dragOffset < cancelThreshold)
                            }
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.KeyboardVoice,
                    contentDescription = "语音",
                    modifier = Modifier.size(22.dp),
                    tint = if (isDark) Color(0xFFF1F5F9) else Color(0xFF64748B),
                )
            }
        }

        // Recording UI
        AnimatedVisibility(
            visible = isRecording,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
        ) {
            val isCancelling = dragOffset < cancelThreshold

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 8.dp,
                color = if (isCancelling) Color(0xFFEF4444).copy(alpha = 0.15f)
                        else PrimaryBlue.copy(alpha = 0.1f),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    // Cancel hint
                    if (isCancelling) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "松手取消",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFEF4444),
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    } else {
                        Text(
                            "← 左滑取消",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                        )
                    }

                    // Recording indicator
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Pulsing dot
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(
                                    if (isCancelling) Color(0xFFEF4444) else Color(0xFFEF4444)
                                )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            formatDuration(recordingSeconds),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B),
                        )
                        Spacer(modifier = Modifier.width(12.dp))

                        // Audio wave animation bars
                        AudioWaveVisualizer(isActive = !isCancelling, isDark = isDark)
                    }

                    // Slide up to send
                    if (!isCancelling) {
                        Text(
                            "上滑发送 →",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                        )
                    }
                }
            }
        }

        // Gesture overlay for recording
        if (isRecording) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .offset { IntOffset(0, (-dragOffset * 0.3f).roundToInt()) }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                onStopRecording(dragOffset < cancelThreshold)
                                dragOffset = 0f
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                dragOffset += dragAmount
                            }
                        )
                    }
            )
        }
    }
}

@Composable
private fun AudioWaveVisualizer(isActive: Boolean, isDark: Boolean) {
    val bars = listOf(3, 7, 4, 9, 5, 8, 4, 6, 3, 7)
    val infiniteTransition = rememberInfiniteTransition(label = "wave")

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        bars.forEachIndexed { index, maxHeight ->
            val animHeight by infiniteTransition.animateFloat(
                initialValue = 3f,
                targetValue = maxHeight.toFloat(),
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 300 + index * 40,
                    ),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "bar$index",
            )
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(animHeight.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(
                        if (isActive) PrimaryBlue else Color.Gray.copy(alpha = 0.3f)
                    )
            )
        }
    }
}

private fun formatDuration(seconds: Float): String {
    val totalSecs = seconds.toInt()
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    return String.format("%d:%02d", mins, secs)
}
