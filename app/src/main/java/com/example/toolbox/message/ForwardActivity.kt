package com.example.toolbox.message

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.toolbox.TokenManager
import com.example.toolbox.ui.theme.ToolBoxTheme
import kotlinx.coroutines.launch


class ForwardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val token = TokenManager.get(this) ?: return finish()
        val sourceChatType = intent.getIntExtra("source_chat_type", 1)
        val messageIdRaw = intent.getStringExtra("message_id") ?: ""
        val messageIds = messageIdRaw.split(",").filter { it.isNotBlank() }.distinct()

        if (messageIds.isEmpty()) {
            finish()
            return
        }

        setContent {
            ToolBoxTheme {
                ForwardScreen(
                    token = token,
                    sourceChatType = sourceChatType,
                    messageIds = messageIds,
                    onBack = { finish() },
                    onNavigateToChat = { chatId, chatType ->
                        val intent = Intent(this@ForwardActivity, MessageDetailActivity::class.java).apply {
                            putExtra("chat_type", chatType)
                            putExtra("chat_id", chatId)
                        }
                        startActivity(intent)
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForwardScreen(
    token: String,
    sourceChatType: Int,
    messageIds: List<String>,
    onBack: () -> Unit,
    onNavigateToChat: (chatId: Int, chatType: Int) -> Unit
) {
    val viewModel: ForwardViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return ForwardViewModel(token) as T
            }
        }
    )

    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.open(sourceChatType, messageIds)
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ForwardEvent.Completed -> {
                    val recipients = event.recipients
                    val target = recipients.singleOrNull()
                    val result = snackbarHostState.showSnackbar(
                        message = if (target != null) "已转发到 ${target.displayName}" else "消息已转发到 ${recipients.size} 个对话当中",
                        actionLabel = target?.let { "查看" },
                        duration = if (target == null) SnackbarDuration.Short else SnackbarDuration.Long
                    )
                    if (result == SnackbarResult.ActionPerformed && target != null) {
                        onNavigateToChat(target.chatId, target.chatType)
                    }
                }
                is ForwardEvent.SourceForwarded -> {}
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("转发消息") },
                navigationIcon = {
                    IconButton(onClick = { if (!uiState.isSending) onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = viewModel::updateQuery,
                    enabled = !uiState.isLocked && !uiState.isSending,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (uiState.query.isNotEmpty() && !uiState.isLocked) {
                            IconButton(onClick = { viewModel.updateQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = "清除搜索")
                            }
                        }
                    },
                    placeholder = { Text("搜索会话") },
                    shape = RoundedCornerShape(18.dp)
                )

                if (uiState.error != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = uiState.error ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        if (uiState.targets.isEmpty() && !uiState.isLocked) {
                            TextButton(onClick = { viewModel.retryLoad() }) { Text("重试") }
                        }
                    }
                }

                when {
                    uiState.isLoading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    uiState.filteredTargets.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (uiState.query.isBlank()) "暂无可转发的会话" else "未找到相关会话",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 8.dp,
                                end = 8.dp,
                                top = 4.dp,
                                bottom = 96.dp
                            )
                        ) {
                            items(
                                items = uiState.filteredTargets,
                                key = { "${it.chatType}:${it.chatId}" }
                            ) { target ->
                                ForwardTargetRow(
                                    target = target,
                                    selected = target.key in uiState.selectedKeys,
                                    enabled = !uiState.isLocked && !uiState.isSending,
                                    onClick = { viewModel.toggleTarget(target) }
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = uiState.selectedKeys.isNotEmpty(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 24.dp),
                enter = scaleIn(
                    initialScale = 0.68f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeIn(animationSpec = tween(120)),
                exit = scaleOut(
                    targetScale = 0.78f,
                    animationSpec = tween(120)
                ) + fadeOut(animationSpec = tween(90))
            ) {
                FloatingActionButton(
                    onClick = { if (uiState.canSend) viewModel.send() },
                    shape = CircleShape,
                    containerColor = if (uiState.canSend || uiState.isSending) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    contentColor = if (uiState.canSend || uiState.isSending) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                ) {
                    if (uiState.isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "发送")
                    }
                }
            }
        }
    }
}

@Composable
private fun ForwardTargetRow(
    target: ForwardTarget,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            selected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
            target.isPinned -> MaterialTheme.colorScheme.surfaceContainerHigh
            else -> Color.Transparent
        },
        animationSpec = tween(180),
        label = "bg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(52.dp)) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(target.displayName.firstOrNull()?.toString() ?: "?")
                }
            }

            // 使用全限定名避免与 RowScope.AnimatedVisibility 冲突
            androidx.compose.animation.AnimatedVisibility(
                visible = selected,
                modifier = Modifier.align(Alignment.BottomEnd),
                enter = scaleIn(initialScale = 0.42f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(tween(100)),
                exit = scaleOut(targetScale = 0.5f, animationSpec = tween(100)) + fadeOut(tween(80))
            ) {
                Surface(
                    modifier = Modifier.size(22.dp),
                    shape = CircleShape,
                    color = Color(0xFF4CAF50),
                    contentColor = Color.White
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Check, contentDescription = "已选择", modifier = Modifier.size(15.dp))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = target.displayName,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}