package com.course.imchat.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.course.imchat.ChatUiState

@Composable
fun AuthScreen(
    state: ChatUiState,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onNicknameChange: (String) -> Unit,
    onToggleAuthMode: () -> Unit,
    onLogin: () -> Unit,
    onRegister: () -> Unit,
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val passwordFocusRequester = remember { FocusRequester() }
    val nicknameFocusRequester = remember { FocusRequester() }
    
    // 按钮动画
    val buttonScale by animateFloatAsState(
        targetValue = if (state.canAuth) 1f else 0.95f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "buttonScale"
    )
    
    Box(modifier = Modifier.fillMaxSize()) {
        // 简洁背景
        AuthBackground()
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.weight(0.15f))
            
            // Logo区域 - Telegram风格简洁Logo
            AuthLogo()
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 标题文字
            AnimatedContent(
                targetState = state.isLoginMode,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith
                    fadeOut(animationSpec = tween(300))
                },
                label = "title"
            ) { isLogin ->
                Text(
                    text = if (isLogin) "欢迎回来" else "创建账号",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            AnimatedContent(
                targetState = state.isLoginMode,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith
                    fadeOut(animationSpec = tween(300))
                },
                label = "subtitle"
            ) { isLogin ->
                Text(
                    text = if (isLogin) "登录以继续聊天" else "注册开始聊天之旅",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // 输入框区域 - Telegram风格简洁输入框
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // 用户名输入框
                AuthTextField(
                    value = state.username,
                    onValueChange = onUsernameChange,
                    label = "用户名",
                    icon = Icons.Default.Person,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { passwordFocusRequester.requestFocus() }
                    ),
                )
                
                // 密码输入框
                AuthTextField(
                    value = state.password,
                    onValueChange = onPasswordChange,
                    label = "密码",
                    icon = Icons.Default.Lock,
                    singleLine = true,
                    isPassword = true,
                    passwordVisible = passwordVisible,
                    onTogglePasswordVisibility = { passwordVisible = !passwordVisible },
                    keyboardOptions = KeyboardOptions(
                        imeAction = if (state.isLoginMode) ImeAction.Done else ImeAction.Next,
                    ),
                    keyboardActions = if (state.isLoginMode) {
                        KeyboardActions(onDone = {
                            focusManager.clearFocus()
                            if (state.canAuth) {
                                if (state.isLoginMode) onLogin() else onRegister()
                            }
                        })
                    } else {
                        KeyboardActions(onNext = { nicknameFocusRequester.requestFocus() })
                    },
                    modifier = Modifier.focusRequester(passwordFocusRequester),
                )
                
                // 昵称输入框（仅注册时显示）
                AnimatedVisibility(
                    visible = !state.isLoginMode,
                    enter = expandVertically(
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                    ) + fadeIn(),
                    exit = shrinkVertically(
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                    ) + fadeOut(),
                ) {
                    AuthTextField(
                        value = state.nickname,
                        onValueChange = onNicknameChange,
                        label = "昵称",
                        icon = Icons.Default.Person,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            focusManager.clearFocus()
                            if (state.canAuth) onRegister()
                        }),
                        modifier = Modifier.focusRequester(nicknameFocusRequester),
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 登录/注册按钮 - Telegram风格渐变按钮
            Button(
                onClick = {
                    focusManager.clearFocus()
                    if (state.isLoginMode) onLogin() else onRegister()
                },
                enabled = state.canAuth,
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
                AnimatedContent(
                    targetState = state.isLoginMode,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(200)) togetherWith
                        fadeOut(animationSpec = tween(200))
                    },
                    label = "buttonText"
                ) { isLogin ->
                    Text(
                        text = if (isLogin) "登录" else "注册",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        letterSpacing = 1.sp,
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(0.2f))
            
            // 切换登录/注册模式
            Row(
                modifier = Modifier.padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (state.isLoginMode) "还没有账号？" else "已有账号？",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(
                    onClick = {
                        focusManager.clearFocus()
                        onToggleAuthMode()
                    },
                ) {
                    Text(
                        text = if (state.isLoginMode) "立即注册" else "去登录",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryBlue,
                    )
                }
            }
        }
    }
}

// ── 认证页面背景 ──────────────────────────────────────────
@Composable
private fun AuthBackground() {
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

// ── 认证页面Logo ──────────────────────────────────────────
@Composable
private fun AuthLogo() {
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
            imageVector = Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = Color.White,
        )
    }
}

// ── 认证页面输入框 ────────────────────────────────────────
@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    singleLine: Boolean = true,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePasswordVisibility: (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    modifier: Modifier = Modifier,
) {
    val isDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (value.isNotEmpty()) PrimaryBlue 
                       else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = { onTogglePasswordVisibility?.invoke() }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility 
                                     else Icons.Default.VisibilityOff,
                        contentDescription = if (passwordVisible) "隐藏密码" else "显示密码",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else null,
        singleLine = singleLine,
        visualTransformation = if (isPassword && !passwordVisible) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PrimaryBlue,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
            focusedLabelColor = PrimaryBlue,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            cursorColor = PrimaryBlue,
            // 增加背景色对比度
            focusedContainerColor = if (isDarkTheme) 
                MaterialTheme.colorScheme.surface.copy(alpha = 0.8f) 
            else 
                MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = if (isDarkTheme) 
                MaterialTheme.colorScheme.surface.copy(alpha = 0.6f) 
            else 
                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            // 增强文字颜色
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        ),
    )
}
