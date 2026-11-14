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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import coil3.compose.AsyncImage
import com.example.toolbox.ApiAddress
import com.example.toolbox.TokenManager
import com.example.toolbox.data.GroupInfo
import com.example.toolbox.ui.theme.ToolBoxTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * 加入会话页（参考项目 murexide 的 NewConversation 添加会话页）：
 * - 用户 tab：输入用户ID → POST /friends/send_request 发送好友请求，成功后进入单聊
 * - 群聊 tab：输入群号 → GET /group/info/{id} 查询群聊 → POST /group/join 加入，成功后进入群聊
 */
class JoinConversationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val token = TokenManager.get(this) ?: return finish()

        setContent {
            ToolBoxTheme {
                JoinConversationScreen(token = token, onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinConversationScreen(token: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val client = remember { OkHttpClient() }

    var selectedTab by remember { mutableStateOf(0) } // 0=用户 1=群聊
    var inputId by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var groupResult by remember { mutableStateOf<GroupInfo?>(null) }
    var statusText by remember { mutableStateOf<String?>(null) }

    fun openConversation(chatType: Int, chatId: Int) {
        val intent = Intent(context, MessageDetailActivity::class.java).apply {
            putExtra("chat_type", chatType)
            putExtra("chat_id", chatId)
        }
        context.startActivity(intent)
    }

    fun sendFriendRequest(id: Int) {
        scope.launch {
            isLoading = true
            statusText = null
            val result = withContext(Dispatchers.IO) {
                try {
                    val body = JSONObject().put("friend_id", id).toString()
                    val request = Request.Builder()
                        .url("${ApiAddress}friends/send_request")
                        .header("x-access-token", token)
                        .post(body.toRequestBody("application/json".toMediaType()))
                        .build()
                    val response = client.newCall(request).execute()
                    val text = response.body.string()
                    val json = JSONObject(text)
                    val success = json.optBoolean("success", false)
                    val message = json.optString("message", if (success) "好友请求已发送" else "发送失败")
                    Pair(success, message)
                } catch (e: Exception) {
                    Pair(false, e.message ?: "网络错误")
                }
            }
            isLoading = false
            Toast.makeText(context, result.second, Toast.LENGTH_SHORT).show()
            if (result.first) {
                openConversation(1, id) // 进入单聊
            }
        }
    }

    fun searchGroup(id: Int) {
        scope.launch {
            isLoading = true
            statusText = null
            groupResult = null
            val result = withContext(Dispatchers.IO) {
                try {
                    val request = Request.Builder()
                        .url("${ApiAddress}group/info/$id")
                        .header("x-access-token", token)
                        .get()
                        .build()
                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        val text = response.body.string()
                        val json = JSONObject(text)
                        val groupObj = json.optJSONObject("group")
                        if (json.optBoolean("success") && groupObj != null) {
                            GroupInfo(
                                id = groupObj.optInt("id"),
                                name = groupObj.optString("name"),
                                avatarUrl = groupObj.optString("avatar_url"),
                                description = groupObj.optString("description"),
                                membersCount = groupObj.optInt("members_count", 0)
                            )
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    null
                }
            }
            isLoading = false
            if (result != null) {
                groupResult = result
            } else {
                statusText = "未找到该群聊，请检查群号"
                Toast.makeText(context, "未找到群聊", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun joinGroup(id: Int) {
        scope.launch {
            isLoading = true
            val result = withContext(Dispatchers.IO) {
                try {
                    val body = JSONObject().put("group_id", id).put("type", 1).toString()
                    val request = Request.Builder()
                        .url("${ApiAddress}group/join")
                        .header("x-access-token", token)
                        .post(body.toRequestBody("application/json".toMediaType()))
                        .build()
                    val response = client.newCall(request).execute()
                    val json = JSONObject(response.body.string())
                    val success = json.optBoolean("success", false)
                    val message = json.optString("message", if (success) "加入群聊成功" else "加入失败")
                    Pair(success, message)
                } catch (e: Exception) {
                    Pair(false, e.message ?: "网络错误")
                }
            }
            isLoading = false
            Toast.makeText(context, result.second, Toast.LENGTH_SHORT).show()
            if (result.first) {
                openConversation(2, id) // 进入群聊
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("加入会话") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        groupResult = null
                        statusText = null
                    },
                    text = { Text("用户") },
                    icon = { Icon(Icons.Default.Person, null, Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        groupResult = null
                        statusText = null
                    },
                    text = { Text("群聊") },
                    icon = { Icon(Icons.Default.Group, null, Modifier.size(18.dp)) }
                )
            }

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = inputId,
                onValueChange = { inputId = it.filter(Char::isDigit) },
                label = { Text(if (selectedTab == 0) "用户ID" else "群号") },
                placeholder = { Text(if (selectedTab == 0) "输入用户ID，发送好友请求" else "输入群号，加入群聊") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = {
                    focusManager.clearFocus()
                })
            )

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = {
                    val id = inputId.trim().toIntOrNull()
                    if (id == null) {
                        Toast.makeText(context, "请输入有效的数字ID", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    focusManager.clearFocus()
                    if (selectedTab == 0) sendFriendRequest(id) else searchGroup(id)
                },
                enabled = inputId.trim().isNotEmpty() && !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Search, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (selectedTab == 0) "发送好友请求" else "查询群聊")
                }
            }

            Spacer(Modifier.height(16.dp))

            statusText?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            groupResult?.let { group ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (group.avatarUrl.isNotBlank()) {
                            AsyncImage(
                                model = if (group.avatarUrl.startsWith("http")) group.avatarUrl
                                    else "${ApiAddress}uploads/${group.avatarUrl}",
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Image(
                                imageVector = Icons.Default.Group,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                group.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "${group.membersCount} 名成员",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = { joinGroup(group.id) },
                            enabled = !isLoading
                        ) {
                            Text("加入群")
                        }
                    }
                }
            }
        }
    }
}