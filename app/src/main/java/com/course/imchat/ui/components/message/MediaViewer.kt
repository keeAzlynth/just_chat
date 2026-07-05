package com.course.imchat.ui.components.message

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Scale

/**
 * Full-screen media viewer with pinch-to-zoom and double-tap.
 * Uses Coil for efficient image loading + caching.
 */
@Composable
fun MediaViewer(
    visible: Boolean,
    imageUrl: String,
    fileName: String = "",
    onDismiss: () -> Unit,
    onDownload: (() -> Unit)? = null,
) {
    if (!visible || imageUrl.isBlank()) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
        ),
    ) {
        val context = LocalContext.current
        var scale by remember { mutableFloatStateOf(1f) }
        var offsetX by remember { mutableFloatStateOf(0f) }
        var offsetY by remember { mutableFloatStateOf(0f) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.96f))
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.5f, 5f)
                        if (scale > 1f) { offsetX += pan.x; offsetY += pan.y }
                        else { offsetX = 0f; offsetY = 0f }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { scale = if (scale > 1f) 1f else 2.5f; if (scale <= 1f) { offsetX = 0f; offsetY = 0f } },
                        onTap = { if (scale <= 1f) onDismiss() },
                    )
                },
        ) {
            // Coil image with zoom
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context).data(imageUrl).crossfade(true).scale(Scale.FIT).build(),
                contentDescription = fileName,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offsetX, translationY = offsetY),
                contentScale = ContentScale.Fit,
                loading = { Box(Modifier.fillMaxSize().background(Color.Transparent), contentAlignment = Alignment.Center) {
                    Text("加载中...", color = Color.White.copy(alpha = 0.5f))
                }},
                error = { Box(Modifier.fillMaxSize().background(Color.Transparent), contentAlignment = Alignment.Center) {
                    Text("加载失败", color = Color.White.copy(alpha = 0.5f))
                }},
            )

            // Close button
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Default.Close, "关闭", tint = Color.White, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.weight(1f))
                if (onDownload != null) {
                    IconButton(onClick = onDownload, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Default.Download, "下载", tint = Color.White)
                    }
                }
            }

            // Zoom hint
            Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp)) {
                Text(if (scale > 1f) "双击还原" else "双击放大 · 点击关闭", color = Color.White.copy(alpha = 0.4f))
            }
        }
    }
}
