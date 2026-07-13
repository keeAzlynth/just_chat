package com.course.imchat

import android.os.Bundle
import android.provider.OpenableColumns
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.course.imchat.ui.ChatActions
import com.course.imchat.ui.ChatApp
import com.course.imchat.ui.theme.IMChatTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: ChatViewModel = viewModel()
            val state by viewModel.uiState.collectAsState()

            // v2.2: Screenshot protection — user toggleable via settings
            val secureFlag = WindowManager.LayoutParams.FLAG_SECURE
            if (state.screenshotProtection) {
                window.setFlags(secureFlag, secureFlag)
            } else {
                window.clearFlags(secureFlag)
            }

            // ── App lifecycle → ViewModel bridge ──────────────
            val lifecycleObserver = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> viewModel.onAppForeground()
                    Lifecycle.Event.ON_PAUSE -> viewModel.onAppBackground()
                    else -> {}
                }
            }
            lifecycle.addObserver(lifecycleObserver)

            val imagePicker = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri -> uri?.let { viewModel.sendImage(it, getName(it) ?: "image.jpg") } }

            val filePicker = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri -> uri?.let { viewModel.sendFile(it, getName(it) ?: "file", 0) } }

            val actions = remember(viewModel) {
                ChatActions.from(viewModel).copy(
                    attachment = ChatActions.from(viewModel).attachment.copy(
                        onPickImage = { imagePicker.launch("image/*") },
                        onPickFile = { filePicker.launch("*/*") },
                    )
                )
            }

            IMChatTheme(darkTheme = state.isDarkMode) {
                ChatApp(
                    state = state,
                    messages = viewModel.messages,
                    actions = actions,
                )
            }
        }
    }

    private fun getName(uri: android.net.Uri): String? {
        var name: String? = null
        contentResolver.query(uri, null, null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (i >= 0) name = c.getString(i)
            }
        }
        return name
    }
}
