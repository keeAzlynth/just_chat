package com.course.imchat.ui

import android.os.Build
import android.graphics.RenderEffect
import android.graphics.Shader
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 毛玻璃效果 Modifier — API 31+ 用原生 RenderEffect 模糊，低版本用半透明遮罩回退。
 *
 * 用法:
 *   Box(Modifier.frostedGlass()) { ... }
 *   Column(Modifier.frostedGlass(cornerRadius = 12.dp, blurRadius = 20f)) { ... }
 */
@Stable
fun Modifier.frostedGlass(
    cornerRadius: Dp = 0.dp,
    blurRadius: Float = 25f,
    borderAlpha: Float = 0.15f,
    backgroundAlpha: Float = 0.75f,
): Modifier = composed {
    val isDark = isSystemInDarkTheme()
    val density = LocalDensity.current
    val radiusPx = with(density) { blurRadius.dp.toPx() }

    val shape = if (cornerRadius > 0.dp) RoundedCornerShape(cornerRadius) else RoundedCornerShape(0.dp)

    this
        .clip(shape)
        .graphicsLayer {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                renderEffect = android.graphics.RenderEffect
                    .createBlurEffect(radiusPx, radiusPx, Shader.TileMode.CLAMP)
                    .asComposeRenderEffect()
            }
            alpha = 0.98f
        }
        .background(
            if (isDark) Color(0xFF182533).copy(alpha = backgroundAlpha)
            else Color(0xFFFFFFFF).copy(alpha = backgroundAlpha)
        )
        .border(
            0.5.dp,
            Brush.linearGradient(listOf(
                if (isDark) Color.White.copy(alpha = borderAlpha) else Color.White.copy(alpha = borderAlpha),
                if (isDark) Color.White.copy(alpha = borderAlpha * 0.3f) else Color.White.copy(alpha = borderAlpha * 0.2f),
            )),
            shape,
        )
}

/**
 * 轻量毛玻璃 — 仅半透明背景 + 边框，无模糊（兼容低端设备）
 */
@Stable
fun Modifier.glassSurface(
    cornerRadius: Dp = 12.dp,
    alpha: Float = 0.8f,
): Modifier = composed {
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(cornerRadius)

    this
        .clip(shape)
        .background(
            if (isDark) Color(0xFF17212B).copy(alpha = alpha)
            else Color.White.copy(alpha = alpha)
        )
        .border(
            0.5.dp,
            Brush.verticalGradient(listOf(
                Color.White.copy(alpha = 0.15f),
                Color.White.copy(alpha = 0.05f),
            )),
            shape,
        )
}

/** 旧的 glassCard 别名，保持兼容 */
@Stable
fun Modifier.glassCard(
    cornerRadius: Dp = 16.dp,
    tintColor: Color = Color.White.copy(alpha = 0.06f),
    borderAlpha: Float = 0.12f,
): Modifier = glassSurface(cornerRadius, tintColor.alpha)
