@file:Suppress("AssignedValueIsNeverRead")

package com.example.toolbox.message

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.example.toolbox.ApiAddress
import com.example.toolbox.DraftManager
import com.example.toolbox.R
import com.example.toolbox.TokenManager
import com.example.toolbox.community.UserInfoActivity
import com.example.toolbox.data.EditDialogState
import com.example.toolbox.data.displayAvatar
import com.example.toolbox.data.displayName
import com.example.toolbox.data.effectiveMsgId
import com.example.toolbox.ui.theme.ToolBoxTheme
import com.example.toolbox.utils.MarkdownRenderer
import com.example.toolbox.utils.MultiImageViewer
import com.example.toolbox.webview.WebViewActivity
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
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
import com.example.toolbox.data.displayTag
import com.example.toolbox.data.Message
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.filled.Share
import android.graphics.Paint
import android.text.TextPaint
import android.text.Layout
import android.text.StaticLayout
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.asAndroidBitmap
import android.graphics.Bitmap
import android.graphics.Canvas
import kotlinx.coroutines.launch
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.SaveAlt

// ---- Activity ----
class MessageDetailActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeMaterialsApi::class)
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

                val hazeState = remember { HazeState() }
                var showShareSheet by remember { mutableStateOf(false) }
                var shareSheetMessages by remember { mutableStateOf<List<Message>>(emptyList()) }

                LaunchedEffect(uiState.messages.size) {
                    
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets(0.dp),
                    topBar = {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .hazeEffect(
                                        state = hazeState,
                                        style = HazeMaterials.thin().copy(
                                            noiseFactor = 0f
                                        ),
                                        block = null
                                    )
                            )
            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(0.5.dp)
                                    .align(Alignment.BottomCenter)
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                            )

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
                                            if (uiState.selectedMessages.isNotEmpty()) {
                                                val selectedIds = uiState.selectedMessages.toSet()
                                                val selectedMsgs = uiState.messages.filter { it.effectiveMsgId in selectedIds }
                                                if (selectedMsgs.isNotEmpty()) {
                                                    IconButton(onClick = {
                                                        shareSheetMessages = selectedMsgs
                                                        showShareSheet = true
                                                        viewModel.exitSelectionMode()
                                                    }) {
                                                        Icon(Icons.Default.Image, contentDescription = "分享面板")
                                                    }
                                                }
                                                if (selectedMsgs.size == 1) {
                                                    val msg = selectedMsgs.first()
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
                                                            viewModel.startEditMessage(msg)
                                                            viewModel.exitSelectionMode()
                                                        }) {
                                                            Icon(Icons.Default.Edit, contentDescription = "编辑")
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                                    )
                                } else {
                                    TopAppBar(
                                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
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
                                                        val typingText by viewModel.typingText.collectAsState()
                                                        Text(
                                                            typingText ?: "${group.membersCount} 名成员", 
                                                            fontSize = 12.sp, 
                                                            color = if (typingText != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
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
                                                    val typingText by viewModel.typingText.collectAsState()
                                                    Column { 
                                                        Text(
                                                            if (typingText != null) "正在输入中..." else otherUser.username, 
                                                            fontWeight = FontWeight.Bold, 
                                                            fontSize = 16.sp
                                                        ) 
                                                    }
                                                }
                                            } else Text("聊天详情")
                                        },
                                        navigationIcon = { FilledTonalIconButton(onClick = { finish() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回") } }
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize().hazeSource(hazeState)) {
                        MessageDetailScreen(PaddingValues(0.dp), viewModel)
                        if (showShareSheet && shareSheetMessages.isNotEmpty()) {
                            val shareChatName = when {
                                chatType == 2 && uiState.groupInfo != null -> uiState.groupInfo!!.name
                                uiState.otherUser != null -> uiState.otherUser!!.username
                                else -> "会话"
                            }
                            val shareChatAvatar = when {
                                chatType == 2 && uiState.groupInfo != null -> uiState.groupInfo!!.avatarUrl
                                uiState.otherUser != null -> uiState.otherUser!!.avatar
                                else -> ""
                            }
                            MessageShareBottomSheet(
                                messages = shareSheetMessages,
                                chatName = shareChatName,
                                chatAvatar = shareChatAvatar,
                                chatType = chatType,
                                onDismiss = {
                                    showShareSheet = false
                                    shareSheetMessages = emptyList()
                                },
                                onSaveImage = { bitmap ->
                                    kotlinx.coroutines.MainScope().launch { saveBitmapToGallery(this@MessageDetailActivity, bitmap) }
                                },
                                onShareImage = { bitmap -> shareBitmap(this@MessageDetailActivity, bitmap) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
fun MessageDetailScreen(
    innerPadding: PaddingValues,
    viewModel: MessageDetailViewModel
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val settingsStorage = remember { com.example.toolbox.settings.SettingsStorage(context) }
    val bubbleCornerRadius by settingsStorage.bubbleCornerRadiusFlow.collectAsState(initial = 16f)
    val bubbleOpacity by settingsStorage.bubbleOpacityFlow.collectAsState(initial = 0.9f)
    
    val hazeState = remember { HazeState() }
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current
    var firstMessageId by remember { mutableStateOf<String?>(null) }
    val density = LocalDensity.current
    val isUploading by viewModel.isUploading.collectAsState()
    val uploadProgress by viewModel.uploadProgress.collectAsState()

    LaunchedEffect(
        uiState.messages.size,
        uiState.groupInfo,
        uiState.otherUser,
        uiState.isLoading
    ) {
        
    }

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
                        val isLastFromSender = newerMessage == null || newerMessage.isRecalled || newerMessage.isSystem || newerMessage.senderId != message.senderId
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
                                olderMessage == null || olderMessage.isRecalled || olderMessage.isSystem || olderMessage.senderId != message.senderId
                            val isLastFromSender =
                                newerMessage == null || newerMessage.isRecalled || newerMessage.isSystem || newerMessage.senderId != message.senderId
                            val isOlderSameSender =
                                olderMessage != null && !olderMessage.isRecalled && !olderMessage.isSystem && olderMessage.senderId == message.senderId
                            val isNewerSameSender =
                                newerMessage != null && !newerMessage.isRecalled && !newerMessage.isSystem && newerMessage.senderId == message.senderId

                            val isTopVisibleItem = message.msgId == topVisibleMessageId

                            val shouldShowItemAvatar = if (isTopVisibleItem) {
                                !showFloatingAvatar && (isLastFromSender || isFirstFromSender)
                            } else {
                                isLastFromSender
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
                                    onEdit = { viewModel.startEditMessage(message) },
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
                                    isFirstFromSender = isFirstFromSender,
                                    onClickInSelectionMode = { viewModel.toggleMessageSelection(message) },
                                    showMenu = showMenuMsgId == message.effectiveMsgId && !selectionMode,
                                    onShowMenuChanged = { msgId ->
                                        if (!selectionMode) {
                                            showMenuMsgId = if (showMenuMsgId == msgId) null else msgId
                                        }
                                    },
                                    onTimeClick = {
                                        val intent = Intent(context, HeatmapActivity::class.java).apply {
                                            putExtra("chat_type", uiState.chatType)
                                            putExtra("chat_id", uiState.chatId)
                                        }
                                        context.startActivity(intent)
                                    },
                                    onDateClick = { dateString ->
                                        val intent = Intent(context, HeatmapActivity::class.java).apply {
                                            putExtra("chat_type", uiState.chatType)
                                            putExtra("chat_id", uiState.chatId)
                                            putExtra("date_string", dateString)
                                        }
                                        context.startActivity(intent)
                                    },
                                    onCollectSticker = { viewModel.collectSticker(it) },
                                    onDeleteSticker = { viewModel.deleteSticker(it) },
                                    onCollectImageAsSticker = { viewModel.collectImageAsSticker(it) },
                                    onDeleteMessage = { viewModel.deleteMessage(it) },
                                    bubbleOpacity = bubbleOpacity,
                                    bubbleCornerRadius = bubbleCornerRadius,
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
                        .hazeEffect(state = hazeState, style = HazeMaterials.thin())
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(onClick = { viewModel.recallSelectedMessages() }) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("撤回")
                    }
                    
                    Button(onClick = { /* 转发选中 */ }) {
                        Icon(Icons.Filled.Share, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("转发")
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
                                        if (repliedMessage.contentType == 7 || repliedMessage.isSticker) "表情消息"
                                        else if (repliedMessage.content.isEmpty() && repliedMessage.images.isNotEmpty()) "[图片]"
                                        else if (repliedMessage.content.isEmpty()) "消息"
                                        else repliedMessage.content,
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

                    uiState.editingMessage?.let { editingMsg ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "编辑消息",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        editingMsg.content.take(30).ifEmpty { "图片消息" },
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.cancelEditMessage() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, "取消编辑", Modifier.size(16.dp))
                                }
                            }
                        }

                        // 编辑时的图片预览
                        if (uiState.editingImages.isNotEmpty()) {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(uiState.editingImages.size) { index ->
                                    Box(Modifier.size(60.dp).clip(RoundedCornerShape(4.dp))) {
                                        AsyncImage(
                                            model = uiState.editingImages[index],
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        IconButton(
                                            onClick = { viewModel.removeEditingImage(index) },
                                            modifier = Modifier.align(Alignment.TopEnd).size(18.dp)
                                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                        ) {
                                            Icon(Icons.Default.Close, "移除", Modifier.size(10.dp), tint = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    val emojiPanelVisible by viewModel.emojiPanelVisible.collectAsState()
                    val emojis by viewModel.emojis.collectAsState()
                    val isLoadingEmojis by viewModel.isLoadingEmojis.collectAsState()

                    

                    MessageInput(
                        inputText = if (uiState.editingMessage != null) uiState.editingContent else uiState.inputText,
                        selectedImages = uiState.selectedImages,
                        isMarkdown = uiState.isMarkdown,
                        onTextChange = { 
                            if (uiState.editingMessage != null) {
                                viewModel.updateEditingContent(it)
                            } else {
                                viewModel.onInputTextChanged(it) 
                            }
                        },
                        onSendClick = {
                            if (uiState.editingMessage != null) {
                                viewModel.submitEditMessage()
                            } else {
                                viewModel.sendMessage()
                            }
                        },
                        onAddImageClick = { imagePicker.launch("image/*") },
                        onRemoveImage = { viewModel.removeImage(it) },
                        onToggleMarkdown = { viewModel.toggleMarkdown() },
                        innerPadding = innerPadding,
                        isUploading = isUploading,
                        uploadProgress = uploadProgress,
                        onCancelUpload = { viewModel.cancelUpload() },
                        onEmojiClick = { viewModel.toggleEmojiPanel() }
                        
                    )
                    AnimatedVisibility(
                        visible = emojiPanelVisible,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        EmojiPanel(
                            emojis = emojis,
                            isLoading = isLoadingEmojis,
                            onEmojiClick = { viewModel.sendEmoji(it) },
                            onEmojiLongPress = { viewModel.deleteEmoji(it) },
                            modifier = Modifier.height(260.dp)
                        )
                    }
                }
            }
        }
    }
    // 热力图弹窗
    
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

    
}

@Composable
fun AnimatedScrollToBottomButton(visible: Boolean, unreadCount: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val animatedAlpha by animateFloatAsState(targetValue = if (visible) 1f else 0f, animationSpec = tween(300, easing = FastOutSlowInEasing), label = "alpha")
    val animatedScale by animateFloatAsState(targetValue = if (visible) 1f else 0.5f, animationSpec = tween(300, easing = FastOutSlowInEasing), label = "scale")
    Box(modifier = modifier.wrapContentSize().graphicsLayer { alpha = animatedAlpha; scaleX = animatedScale; scaleY = animatedScale }) {
        BadgedBox(badge = { if (unreadCount > 0) Badge(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError) { Text(if (unreadCount > 99) "99+" else unreadCount.toString(), fontSize = 10.sp) } }) {
            FloatingActionButton(onClick = onClick, shape = CircleShape, containerColor = MaterialTheme.colorScheme.surfaceContainerHigh, contentColor = MaterialTheme.colorScheme.onSurface, elevation = FloatingActionButtonDefaults.elevation(2.dp), modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.KeyboardArrowDown, "滚动到底部", modifier = Modifier.size(18.dp))
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
    avatarAlignment: Alignment.Vertical = Alignment.Bottom,
    onTimeClick: (() -> Unit)? = null,
    onDateClick: ((String) -> Unit)? = null,
    isFirstFromSender: Boolean = true,
    onCollectSticker: ((Message) -> Unit)? = null,
    onDeleteSticker: ((Message) -> Unit)? = null,
    onCollectImageAsSticker: ((Message) -> Unit)? = null,
    onDeleteMessage: ((Message) -> Unit)? = null,
    onShareClick: ((Message) -> Unit)? = null,
    bubbleOpacity: Float = 0.9f,
    bubbleCornerRadius: Float = 16f,
    showSenderInfo: Boolean = true,
    previewDisplayName: String? = null,
    previewDisplayTag: String? = null,
    previewAvatar: String? = null,
    forceIsMine: Boolean? = null,
    hideMyInfo: Boolean = false,
    hideSenderInfo: Boolean = false,
) {
    val effectiveIsMine = forceIsMine ?: (message.isMine || message.direction == "right")
    val isMine = effectiveIsMine
    val isRecalledMessage = message.msgDeleteTime != null
    val isSystemMessage = message.isSystem

    val timestampDisplay = message.timestampDisplay ?: message.sendTimeDisplay ?: remember(message.sendTime) {
        try { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.sendTime)) } catch (_: Exception) { "" }
    }
    val forwardColor = if (isMine) Color(0xFF2196F3) else Color(0xFF9C27B0)
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
    } else if (message.isSticker || message.contentType == 7) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 8.dp,
                    end = 8.dp,
                    top = if (isOlderSameSender) 0.dp else 4.dp,
                    bottom = if (isNewerSameSender) 0.dp else 4.dp
                )
                .combinedClickable(
                    onClick = {
                        if (isSelectionMode) onClickInSelectionMode?.invoke()
                        else onShowMenuChanged?.invoke(message.effectiveMsgId)
                    },
                    onLongClick = {
                        if (!isSelectionMode) onLongPress?.invoke()
                    }
                )
                .then(if (isSelected) Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(8.dp)) else Modifier),
            horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Bottom
        ) {
            if (!isMine && chatType == 2) {
                if (showAvatar) {
                    AsyncImage(
                        model = message.displayAvatar,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(36.dp).clip(CircleShape)
                    )
                    Spacer(Modifier.width(8.dp))
                } else {
                    Spacer(Modifier.width(44.dp))
                }
            }
            Box {
                AsyncImage(
                    model = message.content.ifEmpty { message.images.firstOrNull() ?: "" },
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onImageClick(listOf(message.content), 0) }
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.EmojiEmotions, null, tint = Color.White, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(2.dp))
                    Text(timestampDisplay, color = Color.White, fontSize = 11.sp)
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { onShowMenuChanged?.invoke(null) }
                ) {
                    DropdownMenuItem(
                        text = { Text("引用") },
                        onClick = { onReply(); onShowMenuChanged?.invoke(null) },
                        leadingIcon = { Icon(Icons.Default.FormatQuote, null, Modifier.size(18.dp)) }
                    )
                    DropdownMenuItem(
                        text = { Text("转发") },
                        onClick = {
                            onShowMenuChanged?.invoke(null)
                            val intent = Intent(context, ForwardActivity::class.java).apply {
                                putExtra("message_id", message.effectiveMsgId)
                            }
                            context.startActivity(intent)
                        },
                        leadingIcon = { Icon(Icons.Filled.Share, null, Modifier.size(18.dp)) }
                    )
                    if (isMine || isAdmin) {
                        DropdownMenuItem(
                            text = { Text("撤回") },
                            onClick = { onRecall(); onShowMenuChanged?.invoke(null) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Undo, null, Modifier.size(18.dp)) }
                        )
                    }

                    DropdownMenuItem(
                        text = { Text("收藏") },
                        onClick = { onCollectSticker?.invoke(message); onShowMenuChanged?.invoke(null) },
                        leadingIcon = { Icon(Icons.Filled.FavoriteBorder, null, Modifier.size(18.dp)) }
                    )
                    if (message.isMine) {
                        DropdownMenuItem(
                            text = { Text("删除") },
                            onClick = { onDeleteSticker?.invoke(message); onShowMenuChanged?.invoke(null) },
                            leadingIcon = { Icon(Icons.Filled.Delete, null, Modifier.size(18.dp)) }
                        )
                    }
                }
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (showDate && dateString != null) {
                Box(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp), 
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp), 
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.clickable { onDateClick?.invoke(dateString) } 
                    ) {
                        Text(
                            dateString, 
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), 
                            fontSize = 12.sp, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                                topStart = bubbleCornerRadius.dp, 
                                topEnd = bubbleCornerRadius.dp,
                                bottomStart = if (isMine) bubbleCornerRadius.dp 
                                            else if (!isNewerSameSender) bubbleCornerRadius.dp 
                                            else (bubbleCornerRadius * 0.3f).dp,
                                bottomEnd = if (isMine) if (!isNewerSameSender) bubbleCornerRadius.dp 
                                            else (bubbleCornerRadius * 0.3f).dp 
                                            else bubbleCornerRadius.dp
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    message.images.isNotEmpty() && message.content.isBlank() -> Color.Transparent
                                    isMine -> MaterialTheme.colorScheme.primary.copy(alpha = bubbleOpacity)
                                    else -> MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = bubbleOpacity)
                                }
                            ),
                            elevation = if (message.images.isNotEmpty()) CardDefaults.cardElevation(defaultElevation = 0.dp) else CardDefaults.cardElevation()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                if (message.forwardInfo != null) {
                                    val fi = message.forwardInfo!!
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(bottom = 4.dp).clickable {
                                            val intent = Intent(context, UserInfoActivity::class.java).apply {
                                                putExtra("userId", fi.userId)
                                            }
                                            context.startActivity(intent)
                                        }
                                    ) {
                                        Text("转发自 ", fontSize = 12.sp, color = forwardColor, fontWeight = FontWeight.Medium)
                                        AsyncImage(model = fi.avatarUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(20.dp).clip(CircleShape))
                                        Spacer(Modifier.width(4.dp))
                                        Text(fi.username, fontSize = 12.sp, color = forwardColor, fontWeight = FontWeight.Medium)
                                    }
                                }
                                                                if (!isMine && isFirstFromSender && chatType == 2 && message.content.isNotBlank()) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 2.dp)) {
                                        Text(message.displayName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                        if (message.displayTag.isNotBlank()) {
                                            Spacer(Modifier.width(4.dp))
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    message.displayTag,
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
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
                                            if (ref.contentType == 7) {
                                                Text("表情消息", fontSize = 12.sp, color = quoteTextColor)
                                            } else {
                                                if (ref.content.isNotBlank()) Text(ref.content, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, color = quoteTextColor)
                                                if (ref.images.isNotEmpty()) AsyncImage(model = ref.images.first(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(8.dp)).padding(top = 4.dp))
                                            }
                                        }
                                    }
                                }
                                if (message.content.isNotBlank()) {
                                    if (message.isMarkdown) MarkdownRenderer.Render(content = message.content)
                                    else Text(
                                        message.content,
                                        fontSize = 14.sp,
                                        color = if (isMine) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.95f)
                                                else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                if (message.images.isNotEmpty()) {
                                    Spacer(Modifier.height(4.dp)); val hasText = message.content.isNotBlank(); val imgCount = message.images.size
                                    if (imgCount == 1) {
                                        Box(modifier = Modifier.widthIn(max = 200.dp).clip(RoundedCornerShape(8.dp)).clickable { onImageClick(message.images, 0) }) {
                                            AsyncImage(model = message.images[0], contentDescription = null, contentScale = ContentScale.FillWidth, modifier = Modifier.fillMaxWidth())
                                            if (!hasText) {
                                                Text(
                                                    timestampDisplay,
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    modifier = Modifier
                                                        .align(Alignment.BottomEnd)
                                                        .padding(6.dp)
                                                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    } else if (imgCount == 2) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.height(180.dp).widthIn(max = 280.dp)) {
                                            message.images.forEachIndexed { index, url -> AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(8.dp)).clickable { onImageClick(message.images, index) }) }
                                        }
                                        if (!hasText) { Spacer(Modifier.height(2.dp)); Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomEnd) { Text(timestampDisplay, color = Color.White, fontSize = 11.sp, modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp)).padding(horizontal = 5.dp, vertical = 2.dp)) } }
                                    } else if (imgCount == 3) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.height(200.dp).widthIn(max = 280.dp)) {
                                            AsyncImage(model = message.images[0], contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(8.dp)).clickable { onImageClick(message.images, 0) })
                                            Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f).fillMaxHeight()) {
                                                AsyncImage(model = message.images[1], contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onImageClick(message.images, 1) })
                                                AsyncImage(model = message.images[2], contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onImageClick(message.images, 2) })
                                            }
                                        }
                                        if (!hasText) { Spacer(Modifier.height(2.dp)); Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomEnd) { Text(timestampDisplay, color = Color.White, fontSize = 11.sp, modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp)).padding(horizontal = 5.dp, vertical = 2.dp)) } }
                                    } else {
                                        val rows = (imgCount + 1) / 2
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.widthIn(max = 280.dp)) {
                                            for (row in 0 until rows) {
                                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.height(120.dp)) {
                                                    for (col in 0..1) { val idx = row * 2 + col; if (idx < imgCount) AsyncImage(model = message.images[idx], contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(8.dp)).clickable { onImageClick(message.images, idx) }) else Spacer(Modifier.weight(1f)) }
                                                }
                                            }
                                        }
                                        if (!hasText) { Spacer(Modifier.height(2.dp)); Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomEnd) { Text(timestampDisplay, color = Color.White, fontSize = 11.sp, modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp)).padding(horizontal = 5.dp, vertical = 2.dp)) } }
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
                                Row(
                                    modifier = Modifier
                                        .align(if (isMine) Alignment.End else Alignment.Start)
                                        .clickable { onTimeClick?.invoke() }  // 添加点击事件
                                ) {
                                    if (message.content.isNotBlank()) { 
                                        Text(timestampDisplay, fontSize = 10.sp, color = if (isMine) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant) 
                                    }
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
                        DropdownMenuItem(
                                text = { Text("删除") },
                                onClick = { onDeleteMessage?.invoke(message); onShowMenuChanged?.invoke(null) },
                                leadingIcon = { Icon(Icons.Filled.Delete, null, Modifier.size(18.dp)) }
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
                        DropdownMenuItem(
                            text = { Text("转发") },
                            onClick = {
                                onShowMenuChanged?.invoke(null)
                                val intent = Intent(context, ForwardActivity::class.java).apply {
                                    putExtra("message_id", message.effectiveMsgId)
                                }
                                context.startActivity(intent)
                            },
                            leadingIcon = { Icon(Icons.Filled.Share, null, Modifier.size(18.dp)) }
                        )
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
                        if (message.isSticker || message.contentType == 7) {
                            DropdownMenuItem(
                                text = { Text("删除消息") },
                                onClick = { onDeleteMessage?.invoke(message); onShowMenuChanged?.invoke(null) },
                                leadingIcon = { Icon(Icons.Filled.Delete, null, Modifier.size(18.dp)) }
                            )
                            DropdownMenuItem(
                                text = { Text("收藏") },
                                onClick = { onShowMenuChanged?.invoke(null); onCollectSticker?.invoke(message) },
                                leadingIcon = { Icon(Icons.Filled.FavoriteBorder, null, Modifier.size(18.dp)) }
                            )
                            
                            if (message.isMine) {
                                DropdownMenuItem(
                                    text = { Text("删除表情") },
                                    onClick = { onShowMenuChanged?.invoke(null); onDeleteSticker?.invoke(message) },
                                    leadingIcon = { Icon(Icons.Filled.Delete, null, Modifier.size(18.dp)) }
                                )
                                
                            }
                        }
                        if (message.images.isNotEmpty()) {
                            DropdownMenuItem(
                                text = { Text("收藏为表情") },
                                onClick = {
                                    onShowMenuChanged?.invoke(null)
                                    onCollectImageAsSticker?.invoke(message)
                                },
                                leadingIcon = { Icon(Icons.Filled.FavoriteBorder, null, Modifier.size(18.dp)) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessageShareBottomSheet(
    messages: List<Message>,
    chatName: String,
    chatAvatar: String,
    chatType: Int,
    onDismiss: () -> Unit,
    onSaveImage: (android.graphics.Bitmap) -> Unit,
    onShareImage: (android.graphics.Bitmap) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var screenshotView by remember { mutableStateOf<android.view.View?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val settingsStorage = remember { com.example.toolbox.settings.SettingsStorage(context) }
    val hideSenderInfo by settingsStorage.screenshotHideSenderInfoFlow.collectAsState(initial = false)
    val hideMyInfo by settingsStorage.screenshotHideMyInfoFlow.collectAsState(initial = false)
    val hideSessionInfo by settingsStorage.screenshotHideSessionInfoFlow.collectAsState(initial = false)
    val hideImages by settingsStorage.screenshotHideImagesFlow.collectAsState(initial = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("分享面板", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text("点击图片、用户、会话区域可切换是否隐藏", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))

            // FilterChips
            var localHideSenderInfo by remember(hideSenderInfo) { mutableStateOf(hideSenderInfo) }
            var localHideMyInfo by remember(hideMyInfo) { mutableStateOf(hideMyInfo) }
            var localHideSessionInfo by remember(hideSessionInfo) { mutableStateOf(hideSessionInfo) }
            var localHideImages by remember(hideImages) { mutableStateOf(hideImages) }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = localHideSenderInfo, onClick = { localHideSenderInfo = !localHideSenderInfo }, label = { Text("隐藏用户") })
                FilterChip(selected = localHideMyInfo, onClick = { localHideMyInfo = !localHideMyInfo }, label = { Text("匿名我方") })
                FilterChip(selected = localHideSessionInfo, onClick = { localHideSessionInfo = !localHideSessionInfo }, label = { Text("隐藏会话") })
                FilterChip(selected = localHideImages, onClick = { localHideImages = !localHideImages }, label = { Text("隐藏图片") })
            }

            Spacer(Modifier.height(12.dp))

            // AndroidView 截图
            AndroidView(
                factory = {
                    ComposeView(context).apply {
                        setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
                        setContent {
                            ToolBoxTheme {
                                MessageSharePreviewCard(
                                    messages = messages,
                                    chatName = chatName,
                                    chatAvatar = chatAvatar,
                                    chatType = chatType,
                                    hideSenderInfo = localHideSenderInfo,
                                    hideMyInfo = localHideMyInfo,
                                    hideSessionInfo = localHideSessionInfo,
                                    hideImages = localHideImages,
                                    onToggleSender = { localHideSenderInfo = !localHideSenderInfo },
                                    onToggleMyInfo = { localHideMyInfo = !localHideMyInfo },
                                    onToggleSession = { localHideSessionInfo = !localHideSessionInfo },
                                    onToggleImages = { localHideImages = !localHideImages }
                                )
                            }
                        }
                        screenshotView = this
                    }
                },
                modifier = Modifier.wrapContentSize()
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 保存
                Column(
                    modifier = Modifier.clickable {
                        scope.launch {
                            val view = screenshotView ?: return@launch
                            val bitmap = android.graphics.Bitmap.createBitmap(view.width.coerceAtLeast(1), view.height.coerceAtLeast(1), android.graphics.Bitmap.Config.ARGB_8888)
                            val canvas = android.graphics.Canvas(bitmap)
                            view.draw(canvas)
                            onSaveImage(bitmap)
                            onDismiss()
                        }
                    },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.SaveAlt, null, Modifier.size(22.dp))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("保存图片", fontSize = 12.sp)
                }

                Spacer(Modifier.width(32.dp))

                // 分享
                Column(
                    modifier = Modifier.clickable {
                        scope.launch {
                            val view = screenshotView ?: return@launch
                            val bitmap = android.graphics.Bitmap.createBitmap(view.width.coerceAtLeast(1), view.height.coerceAtLeast(1), android.graphics.Bitmap.Config.ARGB_8888)
                            val canvas = android.graphics.Canvas(bitmap)
                            view.draw(canvas)
                            onShareImage(bitmap)
                            onDismiss()
                        }
                    },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Share, null, Modifier.size(22.dp))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("分享", fontSize = 12.sp)
                }

                Spacer(Modifier.width(32.dp))

                // 取消
                Column(
                    modifier = Modifier.clickable { onDismiss() },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Close, null, Modifier.size(22.dp))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("取消", fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MessageSharePreviewCard(
    messages: List<Message>,
    chatName: String,
    chatAvatar: String,
    chatType: Int,
    hideSenderInfo: Boolean,
    hideMyInfo: Boolean,
    hideSessionInfo: Boolean,
    hideImages: Boolean,
    onToggleSender: () -> Unit,
    onToggleMyInfo: () -> Unit,
    onToggleSession: () -> Unit,
    onToggleImages: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleSession() }
            ) {
                if (hideSessionInfo) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                } else if (chatAvatar.isNotBlank()) {
                    AsyncImage(
                        model = chatAvatar,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(40.dp).clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(chatName.firstOrNull()?.toString()?.uppercase() ?: "会", color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (hideSessionInfo) "已隐藏会话" else chatName.ifBlank { "会话" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "共 ${messages.size} 条消息",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .clickable { onToggleImages() },
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val orderedMessages = messages.sortedBy {
                        it.sendTime.takeIf { time -> time > 0 } ?: it.timestamp?.toLongOrNull() ?: 0L
                    }
                    val placeholderNamesBySender = linkedMapOf<String, String>()
                    var placeholderIndex = 0
                    orderedMessages.forEachIndexed { index, message ->
                        val isMineMessage = message.isMine || message.direction == "right"
                        val shouldHideContent = hideImages && (message.images.isNotEmpty() || message.isSticker || message.contentType == 7)
                        val senderKey = message.senderId?.toString()
                            ?: message.displayName.ifBlank { message.effectiveMsgId.ifBlank { "sender_$index" } }
                        val assignedPlaceholderName = if (hideSenderInfo && !isMineMessage) {
                            placeholderNamesBySender.getOrPut(senderKey) {
                                val placeholderName = when (placeholderIndex) {
                                    0 -> "匿名用户"
                                    1 -> "匿名用户2"
                                    2 -> "匿名用户3"
                                    else -> "匿名用户${placeholderIndex + 1}"
                                }
                                placeholderIndex += 1
                                placeholderName
                            }
                        } else null
                        val previewName = when {
                            hideMyInfo && isMineMessage -> message.displayName.ifBlank { "我" }
                            hideSenderInfo && !isMineMessage -> assignedPlaceholderName
                            else -> null
                        }
                        val previewAvatar = when {
                            hideMyInfo && isMineMessage -> message.displayAvatar
                            hideSenderInfo && !isMineMessage -> SHARE_PREVIEW_PLACEHOLDER_AVATAR
                            else -> null
                        }
                        val previewTag = when {
                            hideMyInfo && isMineMessage -> message.displayTag.ifBlank { "" }
                            hideSenderInfo && !isMineMessage -> ""
                            else -> null
                        }
                        val previewShowSenderInfo = !(hideSenderInfo && !isMineMessage)
                        val previewForceIsMine = if (hideMyInfo && isMineMessage) false else null
                        if (shouldHideContent) {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = if (message.isMine || message.direction == "right") Alignment.CenterEnd else Alignment.CenterStart
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.widthIn(max = 260.dp)
                                ) {
                                    Text(
                                        text = "图片/表情已隐藏",
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            MessageBubble(
                                context = context,
                                clipboard = clipboard,
                                message = message,
                                onRecall = {},
                                onEdit = {},
                                onImageClick = { _, _ -> },
                                onReply = {},
                                isAdmin = false,
                                isOlderSameSender = index > 0 && orderedMessages[index - 1].senderId == message.senderId,
                                isNewerSameSender = index < orderedMessages.lastIndex && orderedMessages[index + 1].senderId == message.senderId,
                                showAvatar = (!isMineMessage && !message.isSystem && chatType == 2) || (hideMyInfo && isMineMessage && chatType == 2),
                                chatType = chatType,
                                showDate = false,
                                isSelectionMode = false,
                                isSelected = false,
                                showMenu = false,
                                onShowMenuChanged = null,
                                avatarAlignment = Alignment.Bottom,
                                bubbleOpacity = 0.95f,
                                bubbleCornerRadius = 16f,
                                showSenderInfo = previewShowSenderInfo,
                                previewDisplayName = previewName,
                                previewDisplayTag = previewTag,
                                previewAvatar = previewAvatar,
                                forceIsMine = previewForceIsMine,
                                hideMyInfo = hideMyInfo,
                                hideSenderInfo = hideSenderInfo

                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = SHARE_PREVIEW_FOOTER_TEXT,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

private fun createShareBitmap(view: View): Bitmap {
    val bitmap = Bitmap.createBitmap(view.width.coerceAtLeast(1), view.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    view.draw(canvas)
    return bitmap
}

private fun shareBitmap(context: Context, bitmap: Bitmap) {
    kotlinx.coroutines.MainScope().launch {
        val filename = "message_share_${System.currentTimeMillis()}.png"
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }
        }

        val uri = withContext(Dispatchers.IO) {
            context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        }

        if (uri != null) {
            withContext(Dispatchers.IO) {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    out.flush()
                }
            }
            withContext(Dispatchers.Main) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TEXT, "分享多条消息")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "分享消息截图"))
            }
        } else {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "生成分享图片失败", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

private suspend fun saveBitmapToGallery(context: Context, bitmap: Bitmap) = withContext(Dispatchers.IO) {
    val filename = "message_share_${System.currentTimeMillis()}.png"
    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, filename)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }
    }

    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    uri?.let {
        context.contentResolver.openOutputStream(it)?.use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
        }
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "截图已保存到相册", Toast.LENGTH_SHORT).show()
        }
    } ?: withContext(Dispatchers.Main) {
        Toast.makeText(context, "保存失败", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun MessageInput(
    inputText: String, selectedImages: List<String>, isMarkdown: Boolean,
    onTextChange: (String) -> Unit, onSendClick: () -> Unit, onAddImageClick: () -> Unit,
    onRemoveImage: (Int) -> Unit, onToggleMarkdown: () -> Unit, innerPadding: PaddingValues,
    isUploading: Boolean = false, uploadProgress: Float = 0f, onCancelUpload: () -> Unit = {},
    onEmojiClick: () -> Unit = {}
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
                        Box(modifier = Modifier.size(70.dp).clip(RoundedCornerShape(8.dp))) {
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
                IconButton(onClick = onEmojiClick, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Filled.EmojiEmotions, contentDescription = "表情", tint = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(Modifier.width(5.dp))
                TextField(
                    value = inputText, onValueChange = onTextChange,
                    modifier = Modifier
                        .weight(1f)
                        .background(Color.Transparent, RoundedCornerShape(20.dp)),
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
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
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

private fun shareMessageAsBitmap(context: Context, message: Message) {
    kotlinx.coroutines.MainScope().launch {
        val bitmap = withContext(Dispatchers.Default) { buildMessageShareBitmap(message) }
        val filename = "message_share_${System.currentTimeMillis()}.png"
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }
        }

        val uri = withContext(Dispatchers.IO) {
            context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        }

        if (uri != null) {
            withContext(Dispatchers.IO) {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    out.flush()
                }
            }
            withContext(Dispatchers.Main) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TEXT, "分享多条消息")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "分享消息截图"))
            }
        } else {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "生成分享图片失败", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

private fun buildMessageShareBitmap(message: Message): Bitmap {
    val width = 720
    val height = 1080
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF1F2937.toInt()
        textSize = 30f
        isFakeBoldText = true
    }
    val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF111827.toInt()
        textSize = 24f
    }
    val subPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF6B7280.toInt()
        textSize = 18f
    }
    val bubblePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFF3F4F6.toInt()
    }

    canvas.drawRoundRect(36f, 72f, width - 36f, height - 72f, 28f, 28f, bubblePaint)
    canvas.drawText("消息截图", 72f, 140f, titlePaint)

    val senderText = message.displayName.ifBlank { "未知用户" }
    val timeText = message.sendTimeDisplay ?: message.timestampDisplay ?: ""
    canvas.drawText(senderText, 72f, 200f, subPaint)
    if (timeText.isNotBlank()) {
        canvas.drawText(timeText, width - 72f, 200f, subPaint)
    }

    val contentText = message.content.ifBlank {
        when {
            message.images.isNotEmpty() -> "[图片消息]"
            message.isSticker || message.contentType == 7 -> "[表情消息]"
            else -> "[消息]"
        }
    }

    val textWidth = width - 144
    val contentLayout = StaticLayout.Builder.obtain(
        contentText,
        0,
        contentText.length,
        bodyPaint,
        textWidth
    ).setAlignment(Layout.Alignment.ALIGN_NORMAL)
        .setLineSpacing(0f, 1.1f)
        .setIncludePad(false)
        .build()

    canvas.save()
    canvas.translate(72f, 240f)
    contentLayout.draw(canvas)
    canvas.restore()

    return bitmap
}
private const val SHARE_PREVIEW_PLACEHOLDER_AVATAR = "https://www.helloimg.com/i/2025/03/30/67e8e4d5ec8b9.png"
private const val SHARE_PREVIEW_FOOTER_TEXT = "由轻昼ce生成"
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