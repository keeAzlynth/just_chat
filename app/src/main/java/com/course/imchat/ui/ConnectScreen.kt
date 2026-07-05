package com.course.imchat.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.course.imchat.ChatUiState
import com.course.imchat.ConnectionStatus

@Composable
fun ConnectScreen(
    state: ChatUiState,
    onServerUrlChange: (String) -> Unit,
    onConnect: () -> Unit,
    onReconnect: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    
    // 按钮动画
    val buttonScale by animateFloatAsState(
        targetValue = if (state.serverUrl.trim().isNotEmpty()) 1f else 0.95f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "buttonScale"
    )
    
    // 默认地址
    val defaultUrl = com.course.imchat.BuildConfig.WS_URL
    
    Box(modifier = Modifier.fillMaxSize()) {
        // 背景
        ConnectBackground()
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.weight(0.2f))
            
            // Logo区域
            ConnectLogo()
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 标题
            Text(
                text = "IM Chat",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "连接服务器开始聊天",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // 服务器地址输入框
            val isDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
            OutlinedTextField(
                value = state.serverUrl,
                onValueChange = onServerUrlChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                label = { Text("服务器地址") },
                placeholder = { 
                    Text(
                        defaultUrl,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    ) 
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (state.serverUrl.isNotEmpty()) PrimaryBlue 
                               else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    onConnect()
                }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                    focusedLabelColor = PrimaryBlue,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    cursorColor = PrimaryBlue,
                    focusedContainerColor = if (isDarkTheme) 
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.8f) 
                    else 
                        MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = if (isDarkTheme) 
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.6f) 
                    else 
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 连接按钮
            Button(
                onClick = {
                    focusManager.clearFocus()
                    onConnect()
                },
                enabled = state.serverUrl.trim().isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(horizontal = 32.dp)
                    .scale(buttonScale),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryBlue,
                    disabledContainerColor = PrimaryBlue.copy(alpha = 0.5f),
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                    disabledElevation = 0.dp,
                ),
            ) {
                Text(
                    text = "连接",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    letterSpacing = 1.sp,
                )
            }
            
            // 快速连接按钮（使用默认地址）
            TextButton(
                onClick = {
                    focusManager.clearFocus()
                    onServerUrlChange(defaultUrl)
                    onConnect()
                },
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text(
                    text = "使用默认地址快速连接",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PrimaryBlue,
                    fontWeight = FontWeight.Medium,
                )
            }
            
            // 重新连接按钮
            AnimatedVisibility(
                visible = state.connectionStatus is ConnectionStatus.Error || 
                          state.connectionStatus is ConnectionStatus.Disconnected,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                TextButton(
                    onClick = onReconnect,
                    modifier = Modifier.padding(top = 16.dp),
                ) {
                    Icon(
                        Icons.Default.Refresh, 
                        contentDescription = null, 
                        modifier = Modifier.size(18.dp),
                        tint = PrimaryBlue,
                    )
                    Text(
                        "重新连接", 
                        modifier = Modifier.padding(start = 6.dp),
                        color = PrimaryBlue,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            
            // 提示信息
            Text(
                text = "默认地址: $defaultUrl",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 16.dp),
            )
            
            Spacer(modifier = Modifier.weight(0.3f))
        }
    }
}

// ── 连接页面背景 ──────────────────────────────────────────
@Composable
private fun ConnectBackground() {
    val isDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isDarkTheme) listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E293B),
                        Color(0xFF0F172A),
                    ) else listOf(
                        Color(0xFFF8FAFC),
                        Color(0xFFFFFFFF),
                        Color(0xFFF1F5F9),
                    )
                )
            )
    ) {
        // 装饰性渐变圆
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.TopStart)
                .offset(x = (-50).dp, y = (-50).dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            PrimaryBlue.copy(alpha = if (isDarkTheme) 0.08f else 0.05f),
                            Color.Transparent,
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(250.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 50.dp, y = 50.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            AccentPurple.copy(alpha = if (isDarkTheme) 0.06f else 0.04f),
                            Color.Transparent,
                        )
                    )
                )
        )
    }
}

// ── 连接页面Logo ──────────────────────────────────────────
@Composable
private fun ConnectLogo() {
    Box(
        modifier = Modifier
            .size(88.dp)
            .shadow(
                elevation = 8.dp,
                shape = CircleShape,
                ambientColor = PrimaryBlue.copy(alpha = 0.2f),
                spotColor = PrimaryBlue.copy(alpha = 0.3f),
            )
            .clip(CircleShape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(PrimaryBlue, AccentPurple)
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = Color.White,
        )
    }
}
