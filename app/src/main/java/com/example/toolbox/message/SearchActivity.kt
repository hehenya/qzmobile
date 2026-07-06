package com.example.toolbox.message

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.rememberAsyncImagePainter
import com.example.toolbox.TokenManager
import com.example.toolbox.ui.theme.ToolBoxTheme
import androidx.compose.material3.FilterChip

class SearchActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val token = TokenManager.get(this) ?: return finish()

        setContent {
            ToolBoxTheme {
                SearchScreen(token = token, onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(token: String, onBack: () -> Unit) {
    val viewModel: SearchViewModel = viewModel(factory = SearchViewModelFactory(token))
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var searchMode by remember { mutableStateOf(0) } // 0=会话, 1=消息

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                if (lastVisibleIndex != null && lastVisibleIndex >= uiState.results.size - 3) {
                    viewModel.loadMore()
                }
            }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        TextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text("搜索...") },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                unfocusedIndicatorColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                focusManager.clearFocus()
                                if (searchMode == 0) viewModel.search(inputText)
                                else viewModel.searchMessages(inputText)
                            }),
                            trailingIcon = {
                                if (inputText.isNotEmpty()) {
                                    IconButton(onClick = { inputText = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "清除")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            focusManager.clearFocus()
                            if (searchMode == 0) viewModel.search(inputText)
                            else viewModel.searchMessages(inputText)
                        }) {
                            Icon(Icons.Default.Search, contentDescription = "搜索")
                        }
                    }
                )
                // 切换按钮
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = searchMode == 0,
                        onClick = { searchMode = 0 },
                        label = { Text("搜索会话") }
                    )
                    FilterChip(
                        selected = searchMode == 1,
                        onClick = { searchMode = 1 },
                        label = { Text("搜索消息") }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.error != null && uiState.results.isEmpty() && uiState.messageResults.isEmpty()) {
                Text(uiState.error ?: "搜索失败", color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center))
            } else if (uiState.results.isEmpty() && uiState.messageResults.isEmpty() && inputText.isNotEmpty()) {
                Text("未找到相关结果", modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    // 会话结果
                    if (uiState.results.isNotEmpty()) {
                        item {
                            Text(
                                "会话",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        items(uiState.results, key = { "chat_${it.type}_${it.id}" }) { chat ->
                            SearchResultItem(chat = chat, onClick = {
                                val intent = Intent(context, MessageDetailActivity::class.java)
                                intent.putExtra("chat_type", if (chat.type == "group") 2 else 1)
                                if (chat.type == "group") intent.putExtra("chat_id", chat.id)
                                else intent.putExtra("user_id", chat.id)
                                context.startActivity(intent)
                            })
                        }
                    }
        
                    // 分割器
                    if (uiState.results.isNotEmpty() && uiState.messageResults.isNotEmpty()) {
                        item {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                        }
                    }
        
                    // 消息结果
                    if (uiState.messageResults.isNotEmpty()) {
                        item {
                            Text(
                                "消息",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        items(uiState.messageResults, key = { "msg_${it.messageId}" }) { msg ->
                            SearchMessageItem(msg = msg, onClick = {
                                val intent = Intent(context, MessageDetailActivity::class.java)
                                intent.putExtra("chat_type", msg.chatType)
                                if (msg.chatType == 2) intent.putExtra("chat_id", msg.chatId)
                                else intent.putExtra("user_id", msg.chatId)
                                context.startActivity(intent)
                            })
                        }
                    }
        
                    if (uiState.isLoadingMore) {
                        item { Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchMessageItem(msg: SearchMessageItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Image(
            painter = rememberAsyncImagePainter(msg.chatAvatar),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(44.dp).clip(CircleShape)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(msg.chatName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.weight(1f))
                Text(msg.timestampDisplay ?: "", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(2.dp))
            Text(msg.senderUsername, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(2.dp))
            Text(msg.content, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
        }
    }
}

@Composable
fun SearchResultItem(chat: SearchChatItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            Image(
                painter = rememberAsyncImagePainter(chat.avatar),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
            )
            if (chat.unreadCount > 0) {
                Badge(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp)
                ) {
                    Text(text = chat.unreadCount.toString(), fontSize = 10.sp)
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = chat.name ?: chat.username ?: "未知",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                if (chat.title != null && chat.title.isNotBlank()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        modifier = Modifier.size(14.dp),
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = if (chat.type == "group") Icons.Default.Group else Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = chat.lastMessage ?: "暂无消息",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )

                if (chat.lastMessageTime != null) {
                    Text(
                        text = chat.lastMessageTime,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}