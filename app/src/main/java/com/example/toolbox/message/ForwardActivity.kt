package com.example.toolbox.message

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.toolbox.ApiAddress
import com.example.toolbox.AppJson
import com.example.toolbox.TokenManager
import com.example.toolbox.data.FriendsResponse
import com.example.toolbox.ui.theme.ToolBoxTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class ForwardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val messageId = intent.getStringExtra("message_id") ?: return finish()
        val token = TokenManager.get(this) ?: return finish()

        setContent {
            ToolBoxTheme {
                ForwardScreen(
                    token = token,
                    messageId = messageId,
                    onBack = { finish() },
                    onForwarded = {
                        Toast.makeText(this, "转发成功", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                )
            }
        }
    }
}

data class ForwardChatItem(
    val id: Int,
    val name: String,
    val avatar: String,
    val type: String,
    val chatType: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForwardScreen(
    token: String,
    messageId: String,
    onBack: () -> Unit,
    onForwarded: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var searchQuery by remember { mutableStateOf("") }
    var chats by remember { mutableStateOf<List<ForwardChatItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            val client = OkHttpClient()
            val body = FormBody.Builder().add("page", "1").add("per_page", "50").build()
            val request = Request.Builder()
                .url("${ApiAddress}chat/list")
                .post(body)
                .header("x-access-token", token)
                .build()
            withContext(Dispatchers.IO) {
                client.newCall(request).execute().use { response ->
                    val bodyStr = response.body?.string() ?: return@withContext
                    val json = JSONObject(bodyStr)
                    if (json.optBoolean("success")) {
                        val arr = json.optJSONArray("friends") ?: return@withContext
                        val list = mutableListOf<ForwardChatItem>()
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            val type = obj.optString("type", "private")
                            list.add(ForwardChatItem(
                                id = obj.optInt("id"),
                                name = obj.optString("name", obj.optString("username", "")),
                                avatar = obj.optString("avatar", ""),
                                type = type,
                                chatType = if (type == "group") 2 else 1
                            ))
                        }
                        chats = list
                    }
                }
            }
        } catch (_: Exception) {}
        isLoading = false
    }

    val filtered = chats.filter {
        it.name.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("转发") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { pd ->
        Column(modifier = Modifier.padding(pd).fillMaxSize()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("搜索...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
            )

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filtered) { chat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch {
                                        try {
                                            val client = OkHttpClient()
                                            val jsonBody = JSONObject().apply {
                                                put("message_id", messageId.toIntOrNull() ?: return@launch)
                                                put("target_chat_type", chat.chatType)
                                                put("target_chat_id", chat.id)
                                            }
                                            val request = Request.Builder()
                                                .url("${ApiAddress}chat/forward")
                                                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                                                .header("x-access-token", token)
                                                .build()
                                            withContext(Dispatchers.IO) { client.newCall(request).execute() }
                                            onForwarded()
                                        } catch (_: Exception) {
                                            Toast.makeText(context, "转发失败", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = chat.avatar,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(50.dp).clip(CircleShape)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(chat.name, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}