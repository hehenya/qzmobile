@file:Suppress("AssignedValueIsNeverRead")

package com.example.toolbox.message

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.example.toolbox.ApiAddress
import com.example.toolbox.R
import com.example.toolbox.TokenManager
import com.example.toolbox.community.UserInfoActivity
import com.example.toolbox.data.EditDialogState
import com.example.toolbox.data.Message
import com.example.toolbox.data.displayAvatar
import com.example.toolbox.data.displayName
import com.example.toolbox.data.effectiveMsgId
import com.example.toolbox.ui.theme.ToolBoxTheme
import com.example.toolbox.utils.MarkdownRenderer
import com.example.toolbox.utils.MultiImageViewer
import com.example.toolbox.webview.WebViewActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

// ---- Activity ----
class MessageDetailActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val chatType = intent.getIntExtra("chat_type", 1)
        val chatId = intent.getIntExtra("chat_id", 0)
        val finalChatId = if (chatId == 0) intent.getIntExtra("user_id", 0) else chatId
        setContent {
            ToolBoxTheme {
                val token = TokenManager.get(this)
                val viewModel: MessageDetailViewModel = viewModel(
                    factory = token?.let { MessageDetailViewModelFactory(it, chatType, finalChatId) }
                )
                val uiState by viewModel.uiState.collectAsState()
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets(0.dp),
                    topBar = {
                        AnimatedContent(
                            targetState = uiState.selectionMode,
                            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                            label = "topbar"
                        ) { isSelecting ->
                            if (isSelecting) {
                                TopAppBar(
                                    title = { Text("${uiState.selectedMessages.size} 条选中") },
                                    navigationIcon = {
                                        IconButton(onClick = { viewModel.exitSelectionMode() }) {
                                            Icon(Icons.Default.Close, contentDescription = "退出多选")
                                        }
                                    },
                                    actions = {
                                        if (uiState.selectedMessages.size == 1) {
                                            val msgId = uiState.selectedMessages.first()
                                            val msg = uiState.messages.find { it.effectiveMsgId == msgId }
                                            if (msg != null) {
                                                if (msg.content.isNotBlank()) {
                                                    IconButton(onClick = {
                                                        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                        clipboardManager.setPrimaryClip(ClipData.newPlainText("text", msg.content))
                                                        Toast.makeText(this@MessageDetailActivity, "已复制", Toast.LENGTH_SHORT).show()
                                                    }) {
                                                        Icon(Icons.Default.ContentCopy, contentDescription = "复制")
                                                    }
                                                }
                                                IconButton(onClick = {
                                                    viewModel.setReplyTo(msg)
                                                    viewModel.exitSelectionMode()
                                                }) {
                                                    Icon(Icons.Default.FormatQuote, contentDescription = "引用")
                                                }
                                                if (msg.isMine && msg.content.isNotBlank()) {
                                                    IconButton(onClick = {
                                                        viewModel.showEditDialog(msg)
                                                        viewModel.exitSelectionMode()
                                                    }) {
                                                        Icon(Icons.Default.Edit, contentDescription = "编辑")
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                )
                            } else {
                                TopAppBar(
                                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
                                    title = {
                                        if (chatType == 2 && uiState.groupInfo != null) {
                                            val group = uiState.groupInfo!!
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth().clickable {
                                                    startActivity(Intent(this@MessageDetailActivity, GroupInfoActivity::class.java).apply {
                                                        putExtra("group_id", chatId)
                                                        putExtra("is_joined", true)
                                                        putExtra("group_name", group.name)
                                                        putExtra("group_avatar", group.avatarUrl)
                                                        putExtra("group_description", group.description)
                                                        putExtra("group_members_count", group.membersCount)
                                                        putExtra("group_created_at", group.createdAt)
                                                        putExtra("group_is_private", group.isPrivate)
                                                    })
                                                }
                                            ) {
                                                AsyncImage(
                                                    model = if (group.avatarUrl.startsWith("http")) group.avatarUrl else "${ApiAddress}uploads/${group.avatarUrl}",
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.size(36.dp).clip(CircleShape)
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Column {
                                                    Text(group.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                                    Text("${group.membersCount} 名成员", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                        } else if (uiState.otherUser != null) {
                                            val otherUser = uiState.otherUser!!
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth().clickable {
                                                    startActivity(Intent(this@MessageDetailActivity, UserInfoActivity::class.java).apply { putExtra("userId", otherUser.id) })
                                                }
                                            ) {
                                                AsyncImage(model = otherUser.avatar, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(36.dp).clip(CircleShape))
                                                Spacer(Modifier.width(8.dp))
                                                Column { Text(otherUser.username, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                                            }
                                        } else Text("聊天详情")
                                    },
                                    navigationIcon = { FilledTonalIconButton(onClick = { finish() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回") } }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        MessageDetailScreen(PaddingValues(0.dp), viewModel)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MessageDetailScreen(
    innerPadding: PaddingValues,
    viewModel: MessageDetailViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current
    val uiState by viewModel.uiState.collectAsState()
    var firstMessageId by remember { mutableStateOf<String?>(null) }
    val density = LocalDensity.current
    val isUploading by viewModel.isUploading.collectAsState()
    val uploadProgress by viewModel.uploadProgress.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.toastMessage.collect {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        viewModel.handleImageSelected(uri, context, scope)
    }

    val recallDialog by viewModel.recallDialog.collectAsState()
    val editDialog by viewModel.editDialog.collectAsState()
    val listState = rememberLazyListState()
    var showScrollToBottom by remember { mutableStateOf(false) }
    var unreadCount by remember { mutableIntStateOf(0) }
    var showImageViewer by remember { mutableStateOf(false) }
    var imageViewerUrls by remember { mutableStateOf<List<String>>(emptyList()) }
    var imageViewerInitialPage by remember { mutableIntStateOf(0) }
    val replyTo by viewModel.replyTo.collectAsState()
    val selectionMode = uiState.selectionMode
    val selectedMessages = uiState.selectedMessages
    var showMenuMsgId by remember { mutableStateOf<String?>(null) }

    val floatingAvatarState by remember {
        derivedStateOf {
            if (uiState.chatType != 2) return@derivedStateOf Triple(false, "", false)

            val visibleItems = listState.layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty() || uiState.messages.isEmpty()) {
                Triple(false, "", false)
            } else {
                val topVisibleItem = visibleItems.minByOrNull { it.index }
                if (topVisibleItem == null) {
                    Triple(false, "", false)
                } else {
                    val firstVisibleIndex = topVisibleItem.index
                    val message = uiState.messages.getOrNull(firstVisibleIndex)

                    if (message == null || message.isRecalled || message.isMine || message.isSystem) {
                        Triple(false, "", false)
                    } else {
                        val itemHeightDp = with(density) { topVisibleItem.size.toDp() }.value
                        val visibleHeightDp = with(density) {
                            (topVisibleItem.size + topVisibleItem.offset.coerceAtMost(0)).toDp()
                        }.value
                        val hasEnoughSpace = visibleHeightDp >= 44 && itemHeightDp >= 44
                        val isFirstLoad = firstMessageId == null && visibleHeightDp >= 44

                        val currentIndex = uiState.messages.indexOfFirst {
                            it.effectiveMsgId == message.effectiveMsgId
                        }
                        val newerMessage = if (currentIndex > 0) {
                            uiState.messages[currentIndex - 1]
                        } else {
                            null
                        }
                        val olderMessage = if (currentIndex < uiState.messages.size - 1) {
                            uiState.messages[currentIndex + 1]
                        } else {
                            null
                        }
                        val isLastFromSender = olderMessage == null ||
                                olderMessage.isRecalled ||
                                olderMessage.isSystem ||
                                olderMessage.senderId != message.senderId
                        val hasOtherSameSender = (newerMessage != null &&
                                !newerMessage.isRecalled &&
                                !newerMessage.isSystem &&
                                newerMessage.senderId == message.senderId &&
                                !isLastFromSender) ||
                                (olderMessage != null &&
                                        !olderMessage.isRecalled &&
                                        !olderMessage.isSystem &&
                                        olderMessage.senderId == message.senderId)

                        if (hasEnoughSpace || isFirstLoad) {
                            Triple(true, message.displayAvatar, message.isMine)
                        } else if (hasOtherSameSender && message.displayAvatar.isNotEmpty()) {
                            Triple(true, message.displayAvatar, message.isMine)
                        } else {
                            Triple(false, "", false)
                        }
                    }
                }
            }
        }
    }

    val showFloatingAvatar = floatingAvatarState.first
    val floatingAvatarUrl = floatingAvatarState.second
    val floatingAvatarIsMine = floatingAvatarState.third

    val topVisibleMessage by remember {
        derivedStateOf {
            val visibleItems = listState.layoutInfo.visibleItemsInfo
            if (visibleItems.isNotEmpty()) {
                val topIndex = visibleItems.minByOrNull { it.index }?.index
                topIndex?.let { uiState.messages.getOrNull(it) }
            } else {
                null
            }
        }
    }

    val topVisibleMessageId = topVisibleMessage?.msgId

    LaunchedEffect(listState) {
        snapshotFlow {
            val vi = listState.layoutInfo.visibleItemsInfo
            if (vi.isNotEmpty()) {
                vi.last().index >= listState.layoutInfo.totalItemsCount - 5 &&
                        uiState.hasMore &&
                        !uiState.isLoadingMore &&
                        !uiState.isRefreshing
            } else {
                false
            }
        }
            .distinctUntilChanged()
            .filter { it }
            .collect {
                viewModel.loadMore()
            }
    }

    LaunchedEffect(listState) {
        snapshotFlow {
            val vi = listState.layoutInfo.visibleItemsInfo
            if (vi.isNotEmpty()) vi.first().index == 0 else true
        }
            .distinctUntilChanged()
            .collect { atBottom ->
                showScrollToBottom = !atBottom
                if (atBottom) {
                    unreadCount = 0
                    if (uiState.messages.isNotEmpty()) {
                        firstMessageId = uiState.messages.first().effectiveMsgId
                    }
                }
            }
    }

    LaunchedEffect(uiState.messages) {
        if (uiState.messages.isEmpty()) return@LaunchedEffect
        val cur = uiState.messages.first().effectiveMsgId
        if (firstMessageId != null && cur != firstMessageId) {
            if (showScrollToBottom) {
                unreadCount += 1
            } else {
                listState.scrollToItem(0)
                unreadCount = 0
            }
        }
        firstMessageId = cur
    }

    val scrollToBottom: () -> Unit = {
        scope.launch {
            listState.animateScrollToItem(0)
            unreadCount = 0
            if (uiState.messages.isNotEmpty()) {
                firstMessageId = uiState.messages.first().effectiveMsgId
            }
        }
    }

    if (showImageViewer) {
        MultiImageViewer(
            images = imageViewerUrls,
            initialPage = imageViewerInitialPage,
            isVisible = showImageViewer,
            onDismiss = { showImageViewer = false }
        )
    }

    BackHandler(enabled = selectionMode) {
        viewModel.exitSelectionMode()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val backgroundUrl by viewModel.backgroundUrl.collectAsState()
        backgroundUrl?.takeIf { it.isNotEmpty() }?.let { bgUrl ->
            AsyncImage(
                model = bgUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            if (uiState.chatType == 1 && uiState.relationship != "friend") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.errorContainer,
                    tonalElevation = 2.dp
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "你们不是好友，此对话具有时限性",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(12.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    val token = TokenManager.get(context) ?: return@launch
                                    val friendId = uiState.otherUser?.id ?: return@launch
                                    if (sendFriendRequest(token, friendId)) {
                                        Toast.makeText(context, "好友请求已发送", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "发送失败", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.PersonAdd, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("添加好友")
                        }
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        reverseLayout = true,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(
                            items = uiState.messages,
                            key = { it.effectiveMsgId }
                        ) { message ->
                            val index = uiState.messages.indexOf(message)

                            val newerMessage = uiState.messages.getOrNull(index - 1)
                            val olderMessage = uiState.messages.getOrNull(index + 1)

                            val isFirstFromSender =
                                newerMessage == null || newerMessage.isRecalled || newerMessage.isSystem || newerMessage.senderId != message.senderId
                            val isLastFromSender =
                                olderMessage == null || olderMessage.isRecalled || olderMessage.isSystem || olderMessage.senderId != message.senderId
                            val isOlderSameSender =
                                olderMessage != null && !olderMessage.isRecalled && !olderMessage.isSystem && olderMessage.senderId == message.senderId
                            val isNewerSameSender =
                                newerMessage != null && !newerMessage.isRecalled && !newerMessage.isSystem && newerMessage.senderId == message.senderId

                            val isTopVisibleItem = message.msgId == topVisibleMessageId

                            val shouldShowItemAvatar = if (isTopVisibleItem) {
                                !showFloatingAvatar && (isLastFromSender || isFirstFromSender)
                            } else {
                                isFirstFromSender
                            }
    
                            val avatarAlignment =
                                if (isTopVisibleItem && shouldShowItemAvatar) {
                                    if (isLastFromSender) Alignment.Top else Alignment.Bottom
                                } else {
                                    Alignment.Bottom
                                }    
                        
                            val isMenuOpen = showMenuMsgId != null
                            val isCurrentMsg = message.effectiveMsgId == showMenuMsgId
                            val itemAlpha = if (isMenuOpen && !isCurrentMsg) 0.4f else 1f

                            Box(modifier = Modifier.graphicsLayer(alpha = itemAlpha)) {
                                MessageBubble(
                                    context = context,
                                    clipboard = clipboard,
                                    message = message,
                                    onRecall = { viewModel.showRecallDialog(message.effectiveMsgId) },
                                    onEdit = { viewModel.showEditDialog(message) },
                                    onImageClick = { urls, idx ->
                                        imageViewerUrls = urls
                                        imageViewerInitialPage = idx
                                        showImageViewer = true
                                    },
                                    onReply = { viewModel.setReplyTo(message) },
                                    isAdmin = uiState.isAdmin,
                                    showAvatar = shouldShowItemAvatar,
                                    isOlderSameSender = isOlderSameSender,
                                    isNewerSameSender = isNewerSameSender,
                                    avatarAlignment = avatarAlignment,
                                    chatType = uiState.chatType,
                                    showDate = message.showDate,
                                    dateString = message.dateIndicator,
                                    isSelectionMode = selectionMode,
                                    isSelected = message.effectiveMsgId in selectedMessages,
                                    onLongPress = { viewModel.enterSelectionMode(message) },
                                    onClickInSelectionMode = { viewModel.toggleMessageSelection(message) },
                                    showMenu = showMenuMsgId == message.effectiveMsgId && !selectionMode,
                                    onShowMenuChanged = { msgId ->
                                        if (!selectionMode) {
                                            showMenuMsgId = if (showMenuMsgId == msgId) null else msgId
                                        }
                                    }
                                )
                            }
                        }

                        if (uiState.isLoadingMore) {
                            item {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    ContainedLoadingIndicator()
                                }
                            }
                        }
                    }
                }

                if (uiState.error != null && uiState.messages.isEmpty()) {
                    Text(
                        "错误: ${uiState.error}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                if (showFloatingAvatar) {
                    AsyncImage(
                        model = floatingAvatarUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .align(if (floatingAvatarIsMine) Alignment.BottomEnd else Alignment.BottomStart)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                    )
                }

                AnimatedScrollToBottomButton(
                    visible = showScrollToBottom,
                    unreadCount = unreadCount,
                    onClick = scrollToBottom,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                )
            }

            if (selectionMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(onClick = { viewModel.recallSelectedMessages() }) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("撤回")
                    }
                }
            } else {
                Column {
                    replyTo?.let { repliedMessage ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                        ) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier
                                        .width(3.dp)
                                        .height(32.dp)
                                        .background(
                                            MaterialTheme.colorScheme.primary,
                                            RoundedCornerShape(2.dp)
                                        )
                                )
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        repliedMessage.displayName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        if (repliedMessage.content.isEmpty()) "消息" else repliedMessage.content,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.clearReplyTo() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, "取消引用", Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    MessageInput(
                        inputText = uiState.inputText,
                        selectedImages = uiState.selectedImages,
                        isMarkdown = uiState.isMarkdown,
                        onTextChange = { viewModel.updateInputText(it) },
                        onSendClick = { viewModel.sendMessage() },
                        onAddImageClick = { imagePicker.launch("image/*") },
                        onRemoveImage = { viewModel.removeImage(it) },
                        onToggleMarkdown = { viewModel.toggleMarkdown() },
                        innerPadding = innerPadding,
                        isUploading = isUploading,
                        uploadProgress = uploadProgress,
                        onCancelUpload = { viewModel.cancelUpload() }
                    )
                }
            }
        }
    }

    if (recallDialog.isOpen) {
        AlertDialog(
            onDismissRequest = { viewModel.hideRecallDialog() },
            title = { Text("撤回消息") },
            text = { Text("确定要撤回这条消息吗？") },
            confirmButton = {
                TextButton(onClick = { viewModel.recallMessage() }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideRecallDialog() }) {
                    Text("取消")
                }
            }
        )
    }

    if (editDialog.isOpen && editDialog.message != null) {
        EditMessageDialog(
            state = editDialog,
            onDismiss = { viewModel.hideEditDialog() },
            onContentChange = { viewModel.updateEditContent(it) },
            onSave = { viewModel.editMessage() },
            onToggleMarkdown = { viewModel.toggleEditMarkdown() }
        )
    }
}

@Composable
fun AnimatedScrollToBottomButton(visible: Boolean, unreadCount: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val animatedAlpha by animateFloatAsState(targetValue = if (visible) 1f else 0f, animationSpec = tween(300, easing = FastOutSlowInEasing), label = "alpha")
    val animatedScale by animateFloatAsState(targetValue = if (visible) 1f else 0.5f, animationSpec = tween(300, easing = FastOutSlowInEasing), label = "scale")
    Box(modifier = modifier.wrapContentSize().graphicsLayer { alpha = animatedAlpha; scaleX = animatedScale; scaleY = animatedScale }) {
        BadgedBox(badge = { if (unreadCount > 0) Badge(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError) { Text(if (unreadCount > 99) "99+" else unreadCount.toString(), fontSize = 10.sp) } }) {
            FloatingActionButton(onClick = onClick, containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary, elevation = FloatingActionButtonDefaults.elevation(6.dp)) {
                Icon(Icons.Default.KeyboardArrowDown, "滚动到底部")
            }
        }
    }
}

@Composable
fun MessageBubble(
    context: Context,
    clipboard: Clipboard,
    message: Message,
    onRecall: () -> Unit,
    onEdit: () -> Unit,
    onImageClick: (List<String>, Int) -> Unit,
    onReply: () -> Unit,
    isAdmin: Boolean = false,
    isOlderSameSender: Boolean = false,
    isNewerSameSender: Boolean = false,
    showAvatar: Boolean = true,
    chatType: Int = 1,
    showDate: Boolean = false,
    dateString: String? = null,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onLongPress: (() -> Unit)? = null,
    onClickInSelectionMode: (() -> Unit)? = null,
    showMenu: Boolean = false,
    onShowMenuChanged: ((String?) -> Unit)? = null,
    avatarAlignment: Alignment.Vertical = Alignment.Bottom
) {
    val isMine = message.isMine || message.direction == "right"
    val isRecalledMessage = message.msgDeleteTime != null
    val isSystemMessage = message.isSystem

    val timestampDisplay = message.timestampDisplay ?: message.sendTimeDisplay ?: remember(message.sendTime) {
        try { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.sendTime)) } catch (_: Exception) { "" }
    }

    if (isRecalledMessage) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
            Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), modifier = Modifier.widthIn(max = 250.dp)) {
                Text(message.recallHint ?: "消息已撤回", fontSize = 12.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
            }
        }
    } else if (isSystemMessage) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
            Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), modifier = Modifier.widthIn(max = 300.dp)) {
                Text(message.content, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (showDate && dateString != null) {
                Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)) {
                        Text(dateString, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            if (isSelectionMode) {
                                onClickInSelectionMode?.invoke()
                            } else {
                                onShowMenuChanged?.invoke(message.effectiveMsgId)
                            }
                        },
                        onLongClick = {
                            if (!isSelectionMode) {
                                onLongPress?.invoke()
                            }
                        }
                    )
                    .padding(
                        start = 8.dp,
                        end = 8.dp,
                        top = if (isOlderSameSender) 0.dp else 4.dp,
                        bottom = if (isNewerSameSender) 0.dp else 4.dp
                    )
                    .then(if (isSelected) Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(8.dp)) else Modifier),
                verticalAlignment = avatarAlignment,
                horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
            ) {
                if (!isMine && chatType == 2) {
                    if (showAvatar) {
                        AsyncImage(
                            model = message.displayAvatar,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable {
                                    val intent = Intent(context, UserInfoActivity::class.java).apply {
                                        putExtra("userId", message.senderId ?: return@clickable)
                                    }
                                    context.startActivity(intent)
                                }
                        )
                        Spacer(Modifier.width(8.dp))
                    } else {
                        Spacer(Modifier.width(44.dp))
                    }
                }

                Box(modifier = Modifier.weight(1f, fill = false)) {
                    Column(horizontalAlignment = if (isMine) Alignment.End else Alignment.Start) {
                        Card(
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = if (isMine) if (message.isLastFromSender) 16.dp else 4.dp else 16.dp,
                                bottomStart = if (isMine) 16.dp else if (message.isLastFromSender) 16.dp else 4.dp,
                                bottomEnd = if (isMine) if (message.isLastFromSender) 16.dp else 4.dp else 16.dp
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isMine) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                if (!isMine && message.isFirstFromSender && chatType == 2) {
                                    Text(message.displayName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 2.dp))
                                }
                                if (message.quoteMsgInfo != null) {
                                    val ref = message.quoteMsgInfo
                                    val quoteBarColor = if (isMine) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.primary
                                    val quoteTextColor = if (isMine) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant
                                    val quoteNameColor = if (isMine) Color.White else MaterialTheme.colorScheme.primary
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 2.dp)) {
                                        Box(Modifier.width(3.dp).height(32.dp).background(quoteBarColor, RoundedCornerShape(2.dp)))
                                        Spacer(Modifier.width(8.dp))
                                        Column {
                                            Text(ref.senderUsername, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = quoteNameColor)
                                            if (ref.content.isNotBlank()) Text(ref.content, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, color = quoteTextColor)
                                            if (ref.images.isNotEmpty()) AsyncImage(model = ref.images.first(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(4.dp)).padding(top = 4.dp))
                                        }
                                    }
                                }
                                if (message.content.isNotBlank()) {
                                    if (message.isMarkdown) MarkdownRenderer.Render(content = message.content)
                                    else Text(message.content, fontSize = 14.sp, color = if (isMine) Color.White else Color.Black)
                                }
                                if (message.images.isNotEmpty()) {
                                    Spacer(Modifier.height(4.dp)); val hasText = message.content.isNotBlank(); val imgCount = message.images.size
                                    if (imgCount == 1) {
                                        Box(modifier = Modifier.widthIn(max = 280.dp).clip(RoundedCornerShape(8.dp)).clickable { onImageClick(message.images, 0) }) {
                                            AsyncImage(model = message.images[0], contentDescription = null, contentScale = ContentScale.FillWidth, modifier = Modifier.fillMaxWidth())
                                            if (!hasText) { Text(timestampDisplay, color = Color.White, fontSize = 11.sp, modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp).background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 2.dp)) }
                                        }
                                    } else if (imgCount == 2) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.height(180.dp).widthIn(max = 280.dp)) {
                                            message.images.forEachIndexed { index, url -> AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(8.dp)).clickable { onImageClick(message.images, index) }) }
                                        }
                                        if (!hasText) { Spacer(Modifier.height(2.dp)); Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomEnd) { Text(timestampDisplay, color = Color.White, fontSize = 11.sp, modifier = Modifier.background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 2.dp)) } }
                                    } else if (imgCount == 3) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.height(200.dp).widthIn(max = 280.dp)) {
                                            AsyncImage(model = message.images[0], contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(8.dp)).clickable { onImageClick(message.images, 0) })
                                            Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f).fillMaxHeight()) {
                                                AsyncImage(model = message.images[1], contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onImageClick(message.images, 1) })
                                                AsyncImage(model = message.images[2], contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onImageClick(message.images, 2) })
                                            }
                                        }
                                        if (!hasText) { Spacer(Modifier.height(2.dp)); Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomEnd) { Text(timestampDisplay, color = Color.White, fontSize = 11.sp, modifier = Modifier.background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 2.dp)) } }
                                    } else {
                                        val rows = (imgCount + 1) / 2
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.widthIn(max = 280.dp)) {
                                            for (row in 0 until rows) {
                                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.height(120.dp)) {
                                                    for (col in 0..1) { val idx = row * 2 + col; if (idx < imgCount) AsyncImage(model = message.images[idx], contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(8.dp)).clickable { onImageClick(message.images, idx) }) else Spacer(Modifier.weight(1f)) }
                                                }
                                            }
                                        }
                                        if (!hasText) { Spacer(Modifier.height(2.dp)); Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomEnd) { Text(timestampDisplay, color = Color.White, fontSize = 11.sp, modifier = Modifier.background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 2.dp)) } }
                                    }
                                }
                                if (message.linkInfo != null && message.linkInfo.isNotEmpty()) {
                                    Spacer(Modifier.height(6.dp))
                                    message.linkInfo.forEach { info ->
                                        LinkPreviewCard(url = info.url, title = info.title.ifEmpty { info.domain }, onClick = {
                                            val intent = Intent(context, WebViewActivity::class.java).apply { putExtra("url", info.url) }
                                            context.startActivity(intent)
                                        })
                                    }
                                }
                                Row(modifier = Modifier.align(if (isMine) Alignment.End else Alignment.Start)) {
                                    if (message.content.isNotBlank()) { Text(timestampDisplay, fontSize = 10.sp, color = if (isMine) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant) }
                                    if (message.editTime != null) Text("已编辑", fontSize = 10.sp, color = if (isMine) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))
                                }
                            }
                        }
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { onShowMenuChanged?.invoke(null) },
                        modifier = Modifier.align(if (isMine) Alignment.TopStart else Alignment.TopEnd)
                    ) {
                        if (message.content.isNotBlank()) {
                            DropdownMenuItem(
                                text = { Text("复制") },
                                onClick = {
                                    clipboard.nativeClipboard.setPrimaryClip(ClipData.newPlainText("text", message.content))
                                    onShowMenuChanged?.invoke(null)
                                    Toast.makeText(context, "复制成功", Toast.LENGTH_SHORT).show()
                                },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, null, Modifier.size(18.dp)) }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("引用") },
                            onClick = {
                                onShowMenuChanged?.invoke(null)
                                onReply()
                            },
                            leadingIcon = { Icon(Icons.Default.FormatQuote, null, Modifier.size(18.dp)) }
                        )
                        if (isMine || isAdmin) {
                            DropdownMenuItem(
                                text = { Text("撤回") },
                                onClick = {
                                    onShowMenuChanged?.invoke(null)
                                    onRecall()
                                },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Undo, null, Modifier.size(18.dp)) }
                            )
                        }
                        if (isMine && message.content.isNotBlank()) {
                            DropdownMenuItem(
                                text = { Text("编辑") },
                                onClick = {
                                    onShowMenuChanged?.invoke(null)
                                    onEdit()
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, null, Modifier.size(18.dp)) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessageInput(
    inputText: String, selectedImages: List<String>, isMarkdown: Boolean,
    onTextChange: (String) -> Unit, onSendClick: () -> Unit, onAddImageClick: () -> Unit,
    onRemoveImage: (Int) -> Unit, onToggleMarkdown: () -> Unit, innerPadding: PaddingValues,
    isUploading: Boolean = false, uploadProgress: Float = 0f, onCancelUpload: () -> Unit = {}
) {
    var showAttachmentMenu by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp).border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp).padding(bottom = innerPadding.calculateBottomPadding())) {
            if (isUploading) { UploadProgressBar(progress = uploadProgress, onCancel = onCancelUpload) }
            else if (selectedImages.isNotEmpty()) {
                LazyRow(modifier = Modifier.fillMaxWidth().height(80.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(selectedImages.size) { index ->
                        Box(modifier = Modifier.size(70.dp).clip(RoundedCornerShape(4.dp))) {
                            AsyncImage(model = selectedImages[index], contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            IconButton(onClick = { onRemoveImage(index) }, modifier = Modifier.align(Alignment.TopEnd).size(20.dp).background(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), shape = CircleShape)) { Icon(Icons.Default.Close, contentDescription = "移除", modifier = Modifier.size(12.dp)) }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Box {
                    IconButton(onClick = { showAttachmentMenu = true }, modifier = Modifier.size(40.dp)) { Icon(Icons.Default.MoreVert, contentDescription = "附件", tint = MaterialTheme.colorScheme.onSurface) }
                    DropdownMenu(expanded = showAttachmentMenu, onDismissRequest = { showAttachmentMenu = false }) {
                        DropdownMenuItem(text = { Text("发送图片") }, onClick = { showAttachmentMenu = false; onAddImageClick() }, leadingIcon = { Icon(Icons.Default.Image, null) })
                        DropdownMenuItem(text = { Text(if (isMarkdown) "Markdown 模式 (开)" else "Markdown 模式 (关)") }, onClick = { showAttachmentMenu = false; onToggleMarkdown() }, leadingIcon = { Icon(painter = painterResource(R.drawable.markdown), contentDescription = null, modifier = Modifier.size(24.dp)) })
                    }
                }
                Spacer(Modifier.width(5.dp))
                TextField(
                    value = inputText, onValueChange = onTextChange,
                    modifier = Modifier.weight(1f).background(Color.Transparent, RoundedCornerShape(20.dp)),
                    placeholder = { Text("输入消息...") }, shape = RoundedCornerShape(20.dp), maxLines = 5,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
                Spacer(Modifier.width(5.dp))
                Box(contentAlignment = Alignment.Center) {
                    IconButton(onClick = onSendClick, modifier = Modifier.size(40.dp), enabled = inputText.isNotBlank() || selectedImages.isNotEmpty()) { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "发送") }
                    if (isMarkdown) { Text("MD", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.TopEnd).padding(2.dp)) }
                }
            }
        }
    }
}

@Composable
fun UploadProgressBar(progress: Float, onCancel: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxSize(), strokeWidth = 3.dp, color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.surfaceVariant)
            IconButton(onClick = onCancel, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Close, contentDescription = "取消上传", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp)) }
        }
        Spacer(Modifier.width(12.dp))
        Text("正在上传图片...", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun LinkPreviewCard(url: String, title: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shadowElevation = 2.dp,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Language,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = url,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "链接预览",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun EditMessageDialog(state: EditDialogState, onDismiss: () -> Unit, onContentChange: (String) -> Unit, onSave: () -> Unit, onToggleMarkdown: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("编辑消息") }, text = {
        Column {
            OutlinedTextField(value = state.newContent, onValueChange = onContentChange, modifier = Modifier.fillMaxWidth(), label = { Text("新内容") }, minLines = 3)
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Markdown")
                Switch(checked = state.isMarkdown, onCheckedChange = { onToggleMarkdown() }, thumbContent = { Icon(imageVector = if (state.isMarkdown) Icons.Default.Check else Icons.Default.Close, contentDescription = null, modifier = Modifier.size(SwitchDefaults.IconSize), tint = if (state.isMarkdown) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest) })
            }
        }
    }, confirmButton = { Button(onClick = onSave) { Text("保存") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
}

// ---- 好友请求 ----
private suspend fun sendFriendRequest(token: String, friendId: Int): Boolean {
    val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS).build()
    val jsonBody = JSONObject().put("friend_id", friendId).toString()
    val requestBody = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())
    val request = Request.Builder().url("${ApiAddress}friends/send_request").post(requestBody).addHeader("x-access-token", token).build()
    return withContext(Dispatchers.IO) {
        try { client.newCall(request).execute().use { r -> if (!r.isSuccessful) false else { val b = r.body.string(); if (b.isBlank()) false else (try { JSONObject(b) } catch (_: Exception) { return@withContext false }).optBoolean("success", false) } } }
        catch (e: Exception) { android.util.Log.e("NetworkError", "请求失败", e); false }
    }
}