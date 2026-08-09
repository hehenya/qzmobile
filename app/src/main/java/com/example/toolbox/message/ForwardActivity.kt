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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.example.toolbox.TokenManager
import com.example.toolbox.ui.theme.ToolBoxTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

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
                val hazeState = remember { HazeState() }
                ForwardScreen(
                    token = token,
                    sourceChatType = sourceChatType,
                    messageIds = messageIds,
                    hazeState = hazeState,
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeMaterialsApi::class)
@Composable
fun ForwardScreen(
    token: String,
    sourceChatType: Int,
    messageIds: List<String>,
    hazeState: HazeState,
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
                else -> {}
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            FloatingChatTopBar(
                hazeState = hazeState,
                showBackButton = true,
                onBackClick = { if (!uiState.isSending) onBack() },
                title = { Text("转发消息") },
                onMoreClick = {},
                moreMenu = {}
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .hazeSource(hazeState)
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
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
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
                                if (uiState.query.isBlank()) "暂无可转发的会话" else "未找到相关会话",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 96.dp)
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

            androidx.compose.animation.AnimatedVisibility(
                visible = uiState.selectedKeys.isNotEmpty(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 24.dp),
                enter = scaleIn(initialScale = 0.68f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(tween(120)),
                exit = scaleOut(targetScale = 0.78f, animationSpec = tween(120)) + fadeOut(tween(90))
            ) {
                FloatingActionButton(
                    onClick = { if (uiState.canSend) viewModel.send() },
                    shape = CircleShape,
                    containerColor = if (uiState.canSend || uiState.isSending) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (uiState.canSend || uiState.isSending) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    if (uiState.isSending) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "发送")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
private fun FloatingChatTopBar(
    hazeState: HazeState,
    showBackButton: Boolean,
    onBackClick: () -> Unit,
    title: @Composable () -> Unit,
    onMoreClick: () -> Unit,
    moreMenu: @Composable BoxScope.() -> Unit
) {
    val controlSize = 48.dp
    val buttonShape = CircleShape
    val topBarColor = MaterialTheme.colorScheme.surface
    val buttonHazeStyle = HazeMaterials.thin(containerColor = topBarColor).copy(blurRadius = 32.dp, noiseFactor = 0f)
    val cardShape = RoundedCornerShape(24.dp)

    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            topBarColor.copy(alpha = 0.8f),
                            topBarColor.copy(alpha = 0.7f),
                            topBarColor.copy(alpha = 0.6f),
                            Color.Transparent
                        )
                    )
                )
        )
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (showBackButton) {
                Box(
                    modifier = Modifier.size(controlSize)
                        .shadow(2.dp, buttonShape).clip(buttonShape)
                        .hazeEffect(state = hazeState, style = buttonHazeStyle, block = null)
                        .clickable(onClick = onBackClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", modifier = Modifier.size(24.dp))
                }
            }
            Box(
                modifier = Modifier.weight(1f).height(controlSize)
                    .shadow(2.dp, cardShape).clip(cardShape)
                    .hazeEffect(state = hazeState, style = buttonHazeStyle, block = null),
                contentAlignment = Alignment.CenterStart
            ) { title() }
            Box(
                modifier = Modifier.size(controlSize)
                    .shadow(2.dp, buttonShape).clip(buttonShape)
                    .hazeEffect(state = hazeState, style = buttonHazeStyle, block = null)
                    .clickable(onClick = onMoreClick),
                contentAlignment = Alignment.Center
            ) {
                moreMenu()
                Icon(Icons.Default.MoreVert, contentDescription = "更多", modifier = Modifier.size(24.dp))
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

    Box(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 头像加载
            AsyncImage(
                model = target.avatarUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(52.dp).clip(CircleShape)
            )
            Spacer(Modifier.width(14.dp))
            Text(
                text = target.displayName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        // 选中勾号
        androidx.compose.animation.AnimatedVisibility(
            visible = selected,
            modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 4.dp, end = 4.dp),
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
}