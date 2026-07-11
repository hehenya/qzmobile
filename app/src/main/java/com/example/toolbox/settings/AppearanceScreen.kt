package com.example.toolbox.settings

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.toolbox.data.Message
import com.example.toolbox.message.MessageBubble
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val settingsStorage = remember { SettingsStorage(context) }
    val scope = rememberCoroutineScope()

    var bubbleCornerRadius by remember { mutableFloatStateOf(16f) }
    var bubbleOpacity by remember { mutableFloatStateOf(0.9f) }
    var showMyBubbleAvatar by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        bubbleCornerRadius = settingsStorage.getBubbleCornerRadius()
        bubbleOpacity = settingsStorage.getBubbleOpacity()
    }

    // 预览用的假消息
    val previewMessages = remember {
        listOf(
            PreviewMessage("other", "小明", false, false, true, true, false, true),
            PreviewMessage("other", "小明", false, true, true, false, false, true),
            PreviewMessage("me", "我", true, true, true, false, false, false),
        )
    }

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
            // 预览区域
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("预览", style = MaterialTheme.typography.labelMedium)

                    MessageBubble(
                        context = context,
                        clipboard = androidx.compose.ui.platform.LocalClipboard.current,
                        message = createPreviewMessage("我们明天去哪里玩？", false, false),
                        isMine = false,
                        isFirstFromSender = false,
                        isLastFromSender = true,
                        isOlderSameSender = false,
                        isNewerSameSender = true,
                        showAvatar = false,
                        bubbleOpacity = bubbleOpacity,
                        bubbleCornerRadius = bubbleCornerRadius,
                        onRecall = {}, onEdit = {}, onImageClick = { _, _ -> }, onReply = {}
                    )

                    MessageBubble(
                        context = context,
                        clipboard = androidx.compose.ui.platform.LocalClipboard.current,
                        message = createPreviewMessage("去上海吧", false, false),
                        isMine = false,
                        isFirstFromSender = true,
                        isLastFromSender = false,
                        isOlderSameSender = true,
                        isNewerSameSender = false,
                        showAvatar = true,
                        bubbleOpacity = bubbleOpacity,
                        bubbleCornerRadius = bubbleCornerRadius,
                        onRecall = {}, onEdit = {}, onImageClick = { _, _ -> }, onReply = {}
                    )

                    MessageBubble(
                        context = context,
                        clipboard = androidx.compose.ui.platform.LocalClipboard.current,
                        message = createPreviewMessage("ok", true, false),
                        isMine = true,
                        isFirstFromSender = true,
                        isLastFromSender = true,
                        isOlderSameSender = false,
                        isNewerSameSender = false,
                        showAvatar = showMyBubbleAvatar,
                        bubbleOpacity = bubbleOpacity,
                        bubbleCornerRadius = bubbleCornerRadius,
                        onRecall = {}, onEdit = {}, onImageClick = { _, _ -> }, onReply = {}
                    )
                }
            }

            // 圆角调节
            SliderSettingItem(
                title = "气泡圆角",
                icon = Icons.Rounded.RoundedCorner,
                value = bubbleCornerRadius,
                onValueChange = { bubbleCornerRadius = it },
                onValueChangeFinished = {
                    scope.launch { settingsStorage.setBubbleCornerRadius(bubbleCornerRadius) }
                },
                valueRange = 0f..24f,
                displayText = "${bubbleCornerRadius.toInt()}dp"
            )

            // 透明度调节
            SliderSettingItem(
                title = "气泡不透明度",
                icon = Icons.Rounded.Opacity,
                value = bubbleOpacity,
                onValueChange = { bubbleOpacity = it },
                onValueChangeFinished = {
                    scope.launch { settingsStorage.setBubbleOpacity(bubbleOpacity) }
                },
                valueRange = 0.4f..1f,
                displayText = "${(bubbleOpacity * 100).toInt()}%"
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SliderSettingItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    displayText: String
) {
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
                    Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(12.dp))
                    Text(title, style = MaterialTheme.typography.bodyMedium)
                }
                Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                    Text(
                        displayText,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Slider(
                value = value,
                onValueChange = onValueChange,
                onValueChangeFinished = onValueChangeFinished,
                valueRange = valueRange,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// 创建预览用的 Message
fun createPreviewMessage(content: String, isMine: Boolean, isSticker: Boolean): Message {
    return Message(
        msgId = "preview_${System.currentTimeMillis()}",
        senderId = if (isMine) "me" else "other",
        senderUsername = if (isMine) "我" else "其他用户",
        senderAvatar = "",
        content = content,
        contentType = if (isSticker) 7 else 1,
        sendTime = System.currentTimeMillis(),
        direction = if (isMine) "right" else "left",
        isMine = isMine,
        isSticker = isSticker,
        isSystem = false,
        isRecalled = false,
        isMarkdown = false,
        images = emptyList(),
        displayAvatar = "",
        displayName = if (isMine) "我" else "其他用户",
        effectiveMsgId = "preview_${System.currentTimeMillis()}"
    )
}

data class PreviewMessage(
    val senderId: String,
    val senderName: String,
    val isMine: Boolean,
    val isFirstFromSender: Boolean,
    val isLastFromSender: Boolean,
    val isOlderSameSender: Boolean,
    val isNewerSameSender: Boolean,
    val showAvatar: Boolean
)