package com.example.toolbox.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.example.toolbox.data.Message
import com.example.toolbox.message.MessageBubble

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val settingsStorage = remember { SettingsStorage(context) }
    val scope = rememberCoroutineScope()

    var bubbleCornerRadius by remember { mutableFloatStateOf(16f) }
    var bubbleOpacity by remember { mutableFloatStateOf(0.9f) }

    LaunchedEffect(Unit) {
        bubbleCornerRadius = settingsStorage.getBubbleCornerRadius()
        bubbleOpacity = settingsStorage.getBubbleOpacity()
    }

    val previewMessages = listOf(
        Message(
            msgId = "preview_1",
            direction = "left",
            isMine = false,
            senderUsername = "演示用户",
            senderAvatar = "https://www.helloimg.com/i/2025/03/30/67e8e4d5ec8b9.png",
            content = "轻昼支持自定义气泡圆角和透明度设置，快来试试吧！",
            timestampDisplay = "12:30",
            isFirstFromSender = true
        ),
        Message(
            msgId = "preview_2",
            direction = "left",
            isMine = false,
            senderUsername = "演示用户",
            senderAvatar = "https://www.helloimg.com/i/2025/03/30/67e8e4d5ec8b9.png",
            content = "可以自定义圆角和透明度",
            timestampDisplay = "12:31"
        ),
        Message(
            msgId = "preview_3",
            direction = "right",
            isMine = true,
            content = "wow，真是太好用了",
            timestampDisplay = "12:32"
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("外观设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // ===== 预览区域 =====
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "预览",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    previewMessages.forEachIndexed { index, previewMessage ->
                        val showAvatar = previewMessage.direction == "left" && index == 1
                        MessageBubble(
                            context = context,
                            clipboard = clipboard,
                            message = previewMessage,
                            onRecall = {},
                            onEdit = {},
                            onImageClick = { _, _ -> },
                            onReply = {},
                            isSelectionMode = false,
                            isSelected = false,
                            showMenu = false,
                            onShowMenuChanged = {},
                            showAvatar = showAvatar,
                            isOlderSameSender = index == 1,
                            isNewerSameSender = index == 0,
                            chatType = 2,
                            bubbleOpacity = bubbleOpacity,
                            bubbleCornerRadius = bubbleCornerRadius
                        )
                    }
                }
            }

            // ===== 圆角调节 =====
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.RoundedCorner, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(12.dp))
                            Text("气泡圆角", style = MaterialTheme.typography.bodyMedium)
                        }
                        Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                            Text(
                                "${bubbleCornerRadius.toInt()}dp",
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Slider(
                        value = bubbleCornerRadius,
                        onValueChange = { bubbleCornerRadius = it },
                        onValueChangeFinished = {
                            scope.launch { settingsStorage.setBubbleCornerRadius(bubbleCornerRadius) }
                        },
                        valueRange = 0f..24f,
                        steps = 23,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("0dp", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("24dp", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // ===== 透明度调节 =====
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Opacity, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(12.dp))
                            Text("气泡不透明度", style = MaterialTheme.typography.bodyMedium)
                        }
                        Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                            Text(
                                "${(bubbleOpacity * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Slider(
                        value = bubbleOpacity,
                        onValueChange = { bubbleOpacity = it },
                        onValueChangeFinished = {
                            scope.launch { settingsStorage.setBubbleOpacity(bubbleOpacity) }
                        },
                        valueRange = 0.4f..1f,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("40%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("100%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}