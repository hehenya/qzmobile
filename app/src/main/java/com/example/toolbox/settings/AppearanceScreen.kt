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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    LaunchedEffect(Unit) {
        bubbleCornerRadius = settingsStorage.getBubbleCornerRadius()
        bubbleOpacity = settingsStorage.getBubbleOpacity()
    }

    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val myBubbleColor = MaterialTheme.colorScheme.primary.copy(alpha = bubbleOpacity)
    val otherBubbleColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.8f)
    val myTextColor = MaterialTheme.colorScheme.onPrimary
    val otherTextColor = MaterialTheme.colorScheme.onSurface

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

                    // 别人的消息1（不是第一条）
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Surface(
                            shape = RoundedCornerShape(
                                topStart = bubbleCornerRadius.dp,
                                topEnd = bubbleCornerRadius.dp,
                                bottomStart = (bubbleCornerRadius * 0.3f).dp,
                                bottomEnd = bubbleCornerRadius.dp
                            ),
                            color = otherBubbleColor
                        ) {
                            Text(
                                "明天去哪里玩",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                color = otherTextColor,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // 别人的消息2（最后一条，带头像缩进）
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        // 头像占位
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(bubbleCornerRadius.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        )
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(
                                topStart = bubbleCornerRadius.dp,
                                topEnd = bubbleCornerRadius.dp,
                                bottomStart = bubbleCornerRadius.dp,
                                bottomEnd = bubbleCornerRadius.dp
                            ),
                            color = otherBubbleColor
                        ) {
                            Text(
                                "要不去上海吧",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                color = otherTextColor,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // 自己的消息
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Surface(
                            shape = RoundedCornerShape(
                                topStart = bubbleCornerRadius.dp,
                                topEnd = bubbleCornerRadius.dp,
                                bottomStart = bubbleCornerRadius.dp,
                                bottomEnd = (bubbleCornerRadius * 0.3f).dp
                            ),
                            color = myBubbleColor
                        ) {
                            Text(
                                "好",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                color = myTextColor,
                                fontSize = 14.sp
                            )
                        }
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