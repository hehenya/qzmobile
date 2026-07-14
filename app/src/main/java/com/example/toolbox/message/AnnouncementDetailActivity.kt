package com.example.toolbox.message

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.toolbox.ApiAddress
import com.example.toolbox.AppJson
import com.example.toolbox.TokenManager
import com.example.toolbox.data.AnnouncementListResponse
import com.example.toolbox.data.Message
import com.example.toolbox.data.displayName
import com.example.toolbox.data.displayTag
import com.example.toolbox.data.displayAvatar
import com.example.toolbox.data.effectiveMsgId
import com.example.toolbox.ui.theme.ToolBoxTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import androidx.compose.ui.layout.ContentScale
import android.util.Log
import androidx.compose.ui.res.painterResource
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

class AnnouncementDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val groupId = intent.getIntExtra("group_id", 0)
        val isAdmin = intent.getBooleanExtra("is_admin", false)
        val token = TokenManager.get(this) ?: ""

        enableEdgeToEdge()
        setContent {
            ToolBoxTheme {
                AnnouncementDetailScreen(
                    groupId = groupId,
                    isAdmin = isAdmin,
                    token = token,
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeMaterialsApi::class)
@Composable
fun AnnouncementDetailScreen(
    groupId: Int,
    isAdmin: Boolean,
    token: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current
    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var backgroundUrl by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val settingsStorage = remember { com.example.toolbox.settings.SettingsStorage(context) }
    val bubbleCornerRadius by settingsStorage.bubbleCornerRadiusFlow.collectAsState(initial = 16f)
    val bubbleOpacity by settingsStorage.bubbleOpacityFlow.collectAsState(initial = 0.9f)

    val hazeState = remember { HazeState() }

    // 加载公告历史
    LaunchedEffect(Unit) {
        try {
            val client = OkHttpClient()
            val url = "${ApiAddress}group/announcements"
            val json = JSONObject().apply {
                put("group_id", groupId)
                put("page", 1)
                put("per_page", 50)
            }
            val request = Request.Builder()
                .url(url)
                .header("x-access-token", token)
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()

            withContext(Dispatchers.IO) {
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string() ?: ""
                    Log.d("ANNOUNCEMENT_LIST", "响应体: $body")
                    val result = AppJson.json.decodeFromString<AnnouncementListResponse>(body)
                    withContext(Dispatchers.Main) {
                        if (result.success) {
                            messages = result.messages
                        } else {
                            Toast.makeText(context, "加载公告失败", Toast.LENGTH_SHORT).show()
                        }
                        isLoading = false
                    }
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                isLoading = false
                Toast.makeText(context, "加载失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 加载聊天背景
    LaunchedEffect(Unit) {
        try {
            val client = OkHttpClient()
            val json = JSONObject().apply {
                put("chat_type", 2)
                put("target_id", groupId)
            }
            val request = Request.Builder()
                .url("${ApiAddress}chat/get_background")
                .header("x-access-token", token)
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()

            withContext(Dispatchers.IO) {
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string() ?: ""
                    val result = JSONObject(body)
                    val rawUrl = result.optString("background_url", "")
                    withContext(Dispatchers.Main) {
                        backgroundUrl = if (rawUrl.isNotBlank()) {
                            if (rawUrl.startsWith("http")) rawUrl else "${ApiAddress}uploads/$rawUrl"
                        } else null
                    }
                }
            }
        } catch (_: Exception) { }
    }

    // 取消公告
    fun cancelAnnouncement(messageId: String) {
        scope.launch {
            try {
                val client = OkHttpClient()
                val json = JSONObject().apply {
                    put("message_id", messageId.toIntOrNull() ?: 0)
                }
                val request = Request.Builder()
                    .url("${ApiAddress}group/set_announcement")
                    .header("x-access-token", token)
                    .post(json.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                withContext(Dispatchers.IO) {
                    client.newCall(request).execute().use { response ->
                        val body = response.body?.string() ?: ""
                        val result = JSONObject(body)
                        withContext(Dispatchers.Main) {
                            if (result.optBoolean("success")) {
                                messages = messages.filter { it.msgId != messageId }
                                Toast.makeText(context, "已取消公告", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "操作失败", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "操作失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .hazeEffect(
                            state = hazeState,
                            style = HazeMaterials.thin().copy(noiseFactor = 0f),
                            block = null
                        )
                )

                Divider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )

                TopAppBar(
                    title = {
                        Text(
                            if (messages.isNotEmpty()) "${messages.size}条置顶消息" else "历史置顶消息"
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .hazeSource(hazeState)
        ) {
            backgroundUrl?.takeIf { it.isNotEmpty() }?.let { bgUrl ->
                AsyncImage(
                    model = bgUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    error = painterResource(android.R.drawable.ic_menu_gallery)
                )
            }

            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                    )
                }
                messages.isEmpty() -> {
                    Text(
                        "暂无置顶消息",
                        modifier = Modifier
                            .align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> {
                    LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(0f), 
                            reverseLayout = false,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                        items(
                            items = messages,
                            key = { it.effectiveMsgId }
                        ) { message ->
                            val index = messages.indexOf(message)

                            val previousMessage = messages.getOrNull(index - 1)
                            val isSameSenderAsPrevious = previousMessage != null &&
                                    previousMessage.senderId == message.senderId &&
                                    !previousMessage.isRecalled &&
                                    !previousMessage.isSystem

                            val shouldShowAvatar = true

                            MessageBubble(
                                context = context,
                                clipboard = clipboard,
                                message = message,
                                onRecall = {},
                                onEdit = {},
                                onImageClick = { _, _ -> },
                                onReply = {},
                                isAdmin = isAdmin,
                                showAvatar = shouldShowAvatar,
                                isOlderSameSender = isSameSenderAsPrevious,
                                isNewerSameSender = false,
                                avatarAlignment = Alignment.Bottom,
                                chatType = 2,
                                showDate = false,
                                dateString = null,
                                isSelectionMode = false,
                                isSelected = false,
                                showMenu = false,
                                onShowMenuChanged = null,
                                bubbleOpacity = bubbleOpacity,
                                bubbleCornerRadius = bubbleCornerRadius,
                                previewDisplayName = if (shouldShowAvatar) message.displayName else null,
                                previewDisplayTag = if (shouldShowAvatar) message.displayTag else null,
                                previewAvatar = if (shouldShowAvatar) message.displayAvatar else null,
                            )
                        }
                    }
                }
            }
        }
    }
}