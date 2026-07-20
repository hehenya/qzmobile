package com.example.toolbox.message

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.toolbox.ApiAddress
import com.example.toolbox.community.HttpUpload
import kotlinx.coroutines.CoroutineScope
import java.io.File
import java.io.FileOutputStream
import com.example.toolbox.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import android.widget.Toast
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.MultipartBody
import java.util.concurrent.TimeUnit
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.toolbox.data.ActiveDay
import com.example.toolbox.data.ActiveDaysResponse
import java.time.YearMonth
import com.example.toolbox.DraftManager
import androidx.compose.runtime.DisposableEffect
import com.example.toolbox.CacheManager
import com.example.toolbox.MyApplication
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.coroutines.Job
import com.example.toolbox.AppJson
import com.example.toolbox.data.AnnouncementResponse
import kotlinx.serialization.json.Json
import com.example.toolbox.TokenManager
import com.example.toolbox.data.ScheduledMessage
import com.example.toolbox.data.ScheduleListResponse
class MessageDetailViewModel(
    private val token: String,
    private val chatType: Int,
    private val chatId: Int
) : ViewModel() {
    private val jsonParser = Json { ignoreUnknownKeys = true }
    private val _uiState = MutableStateFlow(MessageDetailUiState(chatType = chatType, chatId = chatId))
    val uiState: StateFlow<MessageDetailUiState> = _uiState.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    private val _recallDialog = MutableStateFlow(RecallDialogState())
    val recallDialog: StateFlow<RecallDialogState> = _recallDialog.asStateFlow()

    

    private val _replyTo = MutableStateFlow<Message?>(null)
    val replyTo: StateFlow<Message?> = _replyTo.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _uploadProgress = MutableStateFlow(0f)
    val uploadProgress: StateFlow<Float> = _uploadProgress.asStateFlow()

    private val _backgroundUrl = MutableStateFlow<String?>(null)
    val backgroundUrl: StateFlow<String?> = _backgroundUrl.asStateFlow()
    private var typingJob: Job? = null
    private var activeUploadCancel: (() -> Unit)? = null
    private val _typingText = MutableStateFlow<String?>(null)
    val typingText: StateFlow<String?> = _typingText.asStateFlow()
    private val client = OkHttpClient()
    private var currentPage = 1
    private var hasMore = true
    private val msgIdCache = mutableSetOf<String>()
    private var wsObserver: ((String, String, Int, Message) -> Unit)? = null
    
    private val _isLoadingAtPage = MutableStateFlow(false)
    val isLoadingAtPage: StateFlow<Boolean> = _isLoadingAtPage.asStateFlow()
    private var currentUserId: Int = TokenManager.getUserID(MyApplication.instance) 
    
    private val _atMessages = MutableStateFlow<List<Message>>(emptyList())
    val atMessages: StateFlow<List<Message>> = _atMessages.asStateFlow()

    private val _hasAtMessage = MutableStateFlow(false)
    val hasAtMessage: StateFlow<Boolean> = _hasAtMessage.asStateFlow()

    private val _targetMessageId = MutableStateFlow<String?>(null)
    val targetMessageId: StateFlow<String?> = _targetMessageId.asStateFlow()
    
    init {
        val app = MyApplication.instance
        currentUserId = if (app != null) TokenManager.getUserID(app) else 0
        Log.d("AtDebug", "currentUserId = $currentUserId")

        loadMessages()
        loadScheduledList()
        connectWebSocket()
        loadBackground()
        if (chatType == 2) {
            loadGroupInfo()
            loadLatestAnnouncement(chatId) { announcement ->
                _uiState.update { state -> state.copy(latestAnnouncement = announcement) }
            }
            viewModelScope.launch {
                _atMessages.collect { messages ->
                    _uiState.update { it.copy(atMessages = messages) }
                }
            }
            viewModelScope.launch {
                _hasAtMessage.collect { has ->
                    _uiState.update { it.copy(hasAtMessage = has) }
                }
            }
        }
        loadDraft()
    }
            

    fun connectWebSocket() {
        val manager = ChatSocketManager.getInstance()
    
        manager.connect(token)
    
        wsObserver = observer@ { type, chatIdStr, chatTypeInt, message ->
    
            val incomingChatId = chatIdStr.toIntOrNull()
                ?: return@observer

            if (incomingChatId != chatId) return@observer
            if (chatTypeInt != chatType) return@observer
    
            when (type) {
    
                "recall" -> {
                    val senderName = message.senderUsername ?: message.sender?.name ?: "用户"
                    val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                    val hint = "$senderName 在 $timeStr 撤回了一条消息"

                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages.map {
                                if (it.effectiveMsgId == message.effectiveMsgId) {
                                    it.copy(
                                        msgDeleteTime = System.currentTimeMillis(),
                                        content = "",
                                        images = emptyList(),
                                        isRecalled = true,
                                        recallHint = hint
                                    )
                                } else {
                                    it
                                }
                            }
                        )
                    }
                }
    
                "edit" -> {
                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages.map {
                                if (it.effectiveMsgId == message.effectiveMsgId) {
                                    it.copy(
                                        content = message.content,
                                        isEdited = true,
                                        editTime = message.editTime
                                            ?: System.currentTimeMillis()
                                    )
                                } else {
                                    it
                                }
                            }
                        )
                    }
    
                    viewModelScope.launch {
                        delay(2000)
                        refresh()
                    }
                }
    
                "new" -> {
                    if (message.effectiveMsgId !in msgIdCache) {
                        msgIdCache.add(message.effectiveMsgId)
    
                        _uiState.update { state ->
                            state.copy(
                                messages = listOf(message) + state.messages
                            )
                        }
                        val mentionUsers = message.mentionUsers ?: emptyList()
                        if (chatType == 2 && !message.isMine && mentionUsers.contains(currentUserId)) {
                            _atMessages.update { list -> list + message }
                            _hasAtMessage.value = true
                        }
                    }
                }
                "group_typing_status" -> {
                    _typingText.value = message.content
                }
                "group_stop_typing" -> {
                    _typingText.value = null
                }
                "private_typing_status" -> {
                    if (message.isTyping) {
                        _typingText.value = "${message.senderUsername} 正在输入..."
                    } else {
                        _typingText.value = null
                    }
                }
                "announcement" -> {
                Log.d("ANNOUNCEMENT_WS", "收到公告推送: isAnnouncement=${message.isAnnouncement}")
                
                _uiState.update { state ->
                    // 更新消息列表中的公告状态
                    val updatedMessages = state.messages.map { msg ->
                        if (msg.effectiveMsgId == message.effectiveMsgId) {
                            msg.copy(
                                isAnnouncement = message.isAnnouncement,
                                announcementSetAt = message.announcementSetAt,
                                announcementSetBy = message.announcementSetBy
                            )
                        } else {
                            msg
                        }
                    }
                    
                    // 更新最新公告
                    val latestAnnouncement = if (message.isAnnouncement == true) {
                        // 如果是设置公告，使用推送的消息
                        message
                    } else {
                        // 如果是取消公告，从消息列表中重新找最新的公告
                        updatedMessages
                            .filter { it.isAnnouncement == true }
                            .sortedByDescending { it.sendTime }
                            .firstOrNull()
                    }
                    
                    state.copy(
                        messages = updatedMessages,
                        latestAnnouncement = latestAnnouncement
                    )
                }
                
                val hint = if (message.isAnnouncement == true) {
                    val setBy = message.announcementSetBy?.username ?: "管理员"
                    "$setBy 设置了置顶消息"
                } else {
                    "公告已取消"
                }
                viewModelScope.launch { _toastMessage.emit(hint) }
            }
                "link_update" -> {
                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages.map {
                                if (it.effectiveMsgId == message.effectiveMsgId) {
                                    it.copy(linkInfo = message.linkInfo)
                                } else {
                                    it
                                }
                            }
                        )
                    }
                }
            }
        }
    
        manager.addObserver(wsObserver!!)
        viewModelScope.launch { _toastMessage.emit("Observer 已注册") }
    }
        

    private fun loadMessages() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null
                )
            }
    
            try {
                val result = withContext(Dispatchers.IO) {
    
                    val json = JSONObject().apply {
                        put("chat_type", chatType)
                        put("chat_id", chatId)
                        put("page", currentPage)
                        put("per_page", 20)
                    }
    
                    val request = Request.Builder()
                        .url("${ApiAddress}chat/messages")
                        .post(json.toString().toRequestBody("application/json".toMediaType()))
                        .header("x-access-token", token)
                        .header("timeis", "true")
                        .header("linkinfo", "true")
                        .header("isnew", "true")
                        .build()
    
                    client.newCall(request).execute().use { response ->
    
                        Log.d("CHAT_LOAD", "HTTP=${response.code}")
    
                        if (!response.isSuccessful) {
                            throw IOException("HTTP ${response.code}")
                        }
    
                        val body = response.body?.string().orEmpty()
    
                        Log.d("CHAT_LOAD", "Body=${body.take(500)}")
    
                        AppJson.json.decodeFromString<GetMessagesResponse>(body)
                    }
                }
    
                Log.d(
                    "CHAT_LOAD",
                    "status=${result.status.code}, msgCount=${result.messages.size}"
                )
    
                if (result.status.code == 0) {
    
                    val sortedMessages =
                        result.messages.sortedByDescending { it.sendTime }
                        val cacheKey = "messages_${chatType}_${chatId}"
                        try {
                            val jsonStr = AppJson.json.encodeToString(ListSerializer(Message.serializer()), sortedMessages)
                            CacheManager.save(MyApplication.instance, cacheKey, jsonStr)
                        } catch (_: Exception) {}
                    msgIdCache.clear()
                    msgIdCache.addAll(sortedMessages.map { it.effectiveMsgId })
                    val newAtMessages = if (currentUserId > 0) {
                        sortedMessages.filter { msg ->
                            chatType == 2 && !msg.isMine && (msg.mentionUsers?.contains(currentUserId) == true)
                        }
                    } else {
                        emptyList()
                    }
                    val merged = (_atMessages.value + newAtMessages).distinctBy { it.effectiveMsgId }
                    _atMessages.value = merged
                    _hasAtMessage.value = merged.isNotEmpty()
                    Toast.makeText(MyApplication.instance, "newAtMessages=${newAtMessages.size}, merged=${merged.size}", Toast.LENGTH_SHORT).show()
                    if (chatType == 1 && result.chatBackgroundUrl.isNotEmpty()) {
                        _backgroundUrl.value = result.chatBackgroundUrl
                    }
    
                    _uiState.update { state ->
                        state.copy(
                            messages = sortedMessages,
                            isLoading = false,
                            hasMore = (result.pagination?.pages ?: 1) > currentPage,
                            pagination = result.pagination,
                            canSend = result.canSend,
                            isAdmin = result.isAdmin,
                            relationship = result.relationship,
                            isChatExpired = result.tempChatExpired,
                            otherUser = result.otherUser,
                            groupInfo = if (chatType == 2) state.groupInfo else null,
                            error = null,
                            hasScheduled = result.hasScheduled 
                        )
                    }
    
                    Log.d(
                        "CHAT_LOAD",
                        "UI Updated. messages=${sortedMessages.size}"
                    )
    
                } else {
    
                    Log.e(
                        "CHAT_LOAD",
                        "Server Error: ${result.status.msg}"
                    )
    
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.status.msg
                        )
                    }
                }
    
            } catch (e: Exception) {
                Log.e("CHAT_LOAD", "loadMessages Exception", e)
                val cacheKey = "messages_${chatType}_${chatId}"
                val cachedStr = CacheManager.load(MyApplication.instance, cacheKey)
                if (cachedStr != null && _uiState.value.messages.isEmpty()) {
                    try {
                        val cached = AppJson.json.decodeFromString<List<Message>>(cachedStr)
                        _uiState.update { it.copy(messages = cached, isLoading = false) }
                    } catch (_: Exception) {
                        _uiState.update { it.copy(isLoading = false, error = "无法连接到轻昼服务器，请检查网络配置！") }
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "无法连接到轻昼服务器，请检查网络配置！") }
                }
            }
        }
    }

    fun loadMore() {
        if (!hasMore || _uiState.value.isLoadingMore) return
    
        viewModelScope.launch {
    
            _uiState.update {
                it.copy(isLoadingMore = true)
            }
    
            try {
    
                val nextPage = currentPage + 1
    
                val result = withContext(Dispatchers.IO) {
    
                    val json = JSONObject().apply {
                        put("chat_type", chatType)
                        put("chat_id", chatId)
                        put("page", nextPage)
                        put("per_page", 20)
                    }
    
                    val request = Request.Builder()
                        .url("${ApiAddress}chat/messages")
                        .post(json.toString().toRequestBody("application/json".toMediaType()))
                        .header("x-access-token", token)
                        .header("timeis", "true")
                        .header("linkinfo", "true")
                        .build()
    
                    client.newCall(request).execute().use { response ->
    
                        Log.d("LOAD_MORE", "HTTP=${response.code}")
    
                        if (!response.isSuccessful) {
                            throw IOException("HTTP ${response.code}")
                        }
    
                        val body = response.body?.string().orEmpty()
    
                        if (body.isBlank()) {
                            throw IOException("Response Body is empty")
                        }
    
                        Log.d("LOAD_MORE", "Body=${body.take(500)}")
    
                        AppJson.json.decodeFromString<GetMessagesResponse>(body)
                    }
                }
    
                Log.d(
                    "LOAD_MORE",
                    "status=${result.status.code}, page=$nextPage, msgCount=${result.messages.size}"
                )
    
                if (result.status.code == 0) {
    
                    currentPage = nextPage
    
                    val newMessages = result.messages
                        .filter { it.effectiveMsgId !in msgIdCache }
                        .sortedByDescending { it.sendTime }
    
                    msgIdCache.addAll(newMessages.map { it.effectiveMsgId })
    
                    hasMore = (result.pagination?.pages ?: 1) > currentPage
    
                    Log.d(
                        "LOAD_MORE",
                        "UI before=${_uiState.value.messages.size}, add=${newMessages.size}"
                    )
    
                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages + newMessages,
                            isLoadingMore = false,
                            hasMore = hasMore,
                            pagination = result.pagination,
                            error = null
                        )
                    }
    
                } else {
    
                    Log.e("LOAD_MORE", "Server Error: ${result.status.msg}")
    
                    _uiState.update {
                        it.copy(
                            isLoadingMore = false,
                            error = result.status.msg
                        )
                    }
    
                    _toastMessage.emit("加载更多失败：${result.status.msg}")
                }
    
            } catch (e: Exception) {
    
                Log.e("LOAD_MORE", "loadMore Exception", e)
    
                _uiState.update {
                    it.copy(
                        isLoadingMore = false,
                        error = e.stackTraceToString()
                    )
                }
    
                _toastMessage.emit("加载更多异常：${e.javaClass.simpleName}")
            }
        }
    }
    fun refresh() {
        currentPage = 1
        hasMore = true
        msgIdCache.clear()
        loadMessages()
    }

    fun enterSelectionMode(message: Message) {
        _uiState.update {
            it.copy(selectionMode = true, selectedMessages = setOf(message.effectiveMsgId))
        }
    }

    fun toggleMessageSelection(message: Message) {
        _uiState.update { state ->
            if (!state.selectionMode) return@update state
            val id = message.effectiveMsgId
            val newSelected = if (id in state.selectedMessages) state.selectedMessages - id else state.selectedMessages + id
            if (newSelected.isEmpty()) state.copy(selectionMode = false, selectedMessages = emptySet())
            else state.copy(selectedMessages = newSelected)
        }
    }

    fun exitSelectionMode() {
        _uiState.update { it.copy(selectionMode = false, selectedMessages = emptySet()) }
    }

    fun recallSelectedMessages() {
        val selected = _uiState.value.selectedMessages
        if (selected.isEmpty()) return
        viewModelScope.launch {
            val toRecall = _uiState.value.messages.filter { it.effectiveMsgId in selected && !it.isRecalled && it.isMine }
            toRecall.forEach { message ->
                try {
                    recallMessageInternal(message.effectiveMsgId)
                } catch (e: Exception) {
                    _toastMessage.emit("撤回失败: ${e.message}")
                }
            }
            exitSelectionMode()
        }
    }
    fun loadScheduledList() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingScheduled = true) }
            try {
                val body = JSONObject().apply {
                    put("chat_type", chatType)
                    put("chat_id", chatId)
                    put("page", 1)
                    put("per_page", 20)
                }
                val request = Request.Builder()
                    .url("${ApiAddress}chat/scheduled_list")
                    .post(body.toString().toRequestBody("application/json".toMediaType()))
                    .header("x-access-token", token)
                    .build()
                
                withContext(Dispatchers.IO) {
                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val resp = AppJson.json.decodeFromString<ScheduleListResponse>(response.body?.string() ?: "")
                            if (resp.success) {
                                _uiState.update { 
                                    it.copy(scheduledMessages = resp.messages, hasScheduled = resp.messages.isNotEmpty()) 
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("Schedule", "loadScheduledList error", e)
            } finally {
                _uiState.update { it.copy(isLoadingScheduled = false) }
            }
        }
    }

    // 创建定时消息
    fun scheduleMessage(scheduledAt: String, text: String, images: List<String>) {
        viewModelScope.launch {
            try {
                val data = JSONObject().apply {
                    put("text", text)
                    put("images", JSONArray(images))
                    put("is_markdown", _uiState.value.isMarkdown)
                }
                val body = JSONObject().apply {
                    put("chat_type", chatType)
                    put("chat_id", chatId)
                    put("scheduled_at", scheduledAt)
                    put("data", data)
                }
                val request = Request.Builder()
                    .url("${ApiAddress}chat/schedule_send")
                    .post(body.toString().toRequestBody("application/json".toMediaType()))
                    .header("x-access-token", token)
                    .build()
                
                withContext(Dispatchers.IO) { client.newCall(request).execute() }
                
                // 成功后清空输入框并刷新列表
                _uiState.update { it.copy(inputText = "", selectedImages = emptyList()) }
                loadScheduledList()
                _toastMessage.emit("定时消息已设置")
            } catch (e: Exception) {
                _toastMessage.emit("设置定时消息失败: ${e.message}")
            }
        }
    }

    // 取消定时消息
    fun cancelScheduledMessage(id: Int) {
        viewModelScope.launch {
            try {
                val body = JSONObject().apply { put("scheduled_id", id) }
                val request = Request.Builder()
                    .url("${ApiAddress}chat/cancel_schedule")
                    .post(body.toString().toRequestBody("application/json".toMediaType()))
                    .header("x-access-token", token)
                    .build()
                
                withContext(Dispatchers.IO) { client.newCall(request).execute() }
                loadScheduledList() // 刷新列表
                _toastMessage.emit("已取消定时消息")
            } catch (e: Exception) {
                _toastMessage.emit("取消失败: ${e.message}")
            }
        }
    }
}
    private suspend fun recallMessageInternal(msgId: String) {
        withContext(Dispatchers.IO) {
            val json = JSONObject().apply {
                put("message_id", msgId)
            }
            val url = if (chatType == 1) "${ApiAddress}private/delete_message" else "${ApiAddress}group/recall"
            val request = Request.Builder()
                .url(url)
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .header("x-access-token", token)
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                _uiState.update { state ->
                    state.copy(messages = state.messages.map {
                        if (it.effectiveMsgId == msgId) it.copy(isRecalled = true, recallHint = "你撤回了一条消息")
                        else it
                    })
                }
            } else {
                _toastMessage.emit("撤回失败，请重试")
            }
        }
    }
    fun deleteMessage(message: Message) {
        viewModelScope.launch {
            try {
                val body = JSONObject().apply {
                    put("chat_type", chatType)
                    put("message_id", message.id ?: message.msgId.toIntOrNull() ?: return@launch)
                }
                val request = Request.Builder()
                    .url("${ApiAddress}chat/delete_message")
                    .post(body.toString().toRequestBody("application/json".toMediaType()))
                    .header("x-access-token", token)
                    .build()
                withContext(Dispatchers.IO) { client.newCall(request).execute() }
                _uiState.update { it.copy(messages = it.messages.filter { m -> m.effectiveMsgId != message.effectiveMsgId }) }
                _toastMessage.emit("已删除")
            } catch (e: Exception) {
                _toastMessage.emit("删除失败")
            }
        }
    }
    // 设置/取消公告
    fun toggleAnnouncement(messageId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val url = "${ApiAddress}group/set_announcement"
                val json = JSONObject().apply { put("message_id", messageId.toIntOrNull() ?: 0) }
                val request = Request.Builder()
                    .url(url)
                    .header("x-access-token", token)
                    .post(json.toString().toRequestBody("application/json".toMediaType()))
                    .build()
                
                withContext(Dispatchers.IO) {
                    client.newCall(request).execute().use { response ->
                        val body = response.body?.string() ?: ""
                        val result = JSONObject(body)
                        val success = result.optBoolean("success")
                        
                        withContext(Dispatchers.Main) {
                            if (success) {
                                // 更新UI状态：先取消所有消息的公告标记，再设置新公告
                                _uiState.update { state ->
                                    val updatedMessages = state.messages.map { msg ->
                                        if (msg.effectiveMsgId == messageId) {
                                            // 切换这条消息的公告状态
                                            msg.copy(isAnnouncement = !(msg.isAnnouncement == true))
                                        } else if (msg.isAnnouncement == true) {
                                            // 取消其他消息的公告状态
                                            msg.copy(isAnnouncement = false)
                                        } else {
                                            msg
                                        }
                                    }
                                    state.copy(messages = updatedMessages)
                                }
                            }
                            onResult(success)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ToggleAnnouncement", "Error toggling announcement", e)
                withContext(Dispatchers.Main) { onResult(false) }
            }
        }
    }

    // 获取最新公告
    fun loadLatestAnnouncement(groupId: Int, onResult: (Message?) -> Unit) {
    viewModelScope.launch {
        try {
            val url = "${ApiAddress}group/latest_announcement"
            val json = JSONObject().apply { put("group_id", groupId) }
            val request = Request.Builder()
                .url(url)
                .header("x-access-token", token)
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()

            withContext(Dispatchers.IO) {
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string() ?: ""
                    val result = AppJson.json.decodeFromString<AnnouncementResponse>(body)
                    withContext(Dispatchers.Main) {
                        onResult(result.announcement)
                    }
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) { onResult(null) }
        }
    }
}

    
    private val _mentionUsers = MutableStateFlow<List<Int>>(emptyList())
    val mentionUsers: StateFlow<List<Int>> = _mentionUsers.asStateFlow()

    fun addMentionUser(userId: Int, username: String) {
    _mentionUsers.update { list ->
        if (userId !in list) list + userId else list
    }
}
        
    


    fun clearMentionUsers() {
        _mentionUsers.value = emptyList()
    }
    fun sendMessage() {
        val state = _uiState.value
        val text = state.inputText.trim()
        val images = state.selectedImages
        if (text.isEmpty() && images.isEmpty()) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true) }
            try {
                val data = JSONObject().apply {
                    put("text", text)
                    put("images", org.json.JSONArray(images))
                    put("is_markdown", state.isMarkdown)
                    if (chatType == 2 && _mentionUsers.value.isNotEmpty()) {
                        put("mention_users", org.json.JSONArray(_mentionUsers.value))
                    }
                }
                val body = JSONObject().apply {
                    put("chat_type", chatType)
                    put("chat_id", chatId)
                    put("data", data)
                    _replyTo.value?.let { put("quote_msg_id", it.effectiveMsgId) }
                }
                val request = Request.Builder()
                    .url("${ApiAddress}chat/send")
                    .post(body.toString().toRequestBody("application/json".toMediaType()))
                    .header("x-access-token", token)
                    .build()
                
                withContext(Dispatchers.IO) { client.newCall(request).execute() }
                
                _uiState.update {
                    it.copy(
                        inputText = "",
                        selectedImages = emptyList(),
                        isMarkdown = false,
                        isSending = false
                    )
                }
                _replyTo.value = null
                clearMentionUsers()
                DraftManager.removeDraft(chatType, chatId)
            } catch (e: Exception) {
                _uiState.update { it.copy(isSending = false) }
                _toastMessage.emit("发送失败: ${e.message}")
            }
        }
    }
    fun startEditMessage(message: Message) {
        _uiState.update {
            it.copy(
                editingMessage = message,
                editingContent = message.content,
                editingImages = message.images,
                editingIsMarkdown = message.isMarkdown
            )
        }
    }
    
    fun cancelEditMessage() {
        _uiState.update {
            it.copy(
                editingMessage = null,
                editingContent = "",
                editingImages = emptyList(),
                editingIsMarkdown = false
            )
        }
    }
    
    fun updateEditingContent(content: String) {
        _uiState.update { it.copy(editingContent = content) }
    }
    
    fun removeEditingImage(index: Int) {
        val current = _uiState.value.editingImages.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _uiState.update { it.copy(editingImages = current) }
        }
    }
    
    fun addEditingImage(url: String) {
        _uiState.update { it.copy(editingImages = it.editingImages + url) }
    }
    
    fun toggleEditingMarkdown() {
        _uiState.update { it.copy(editingIsMarkdown = !it.editingIsMarkdown) }
    }
    
    fun submitEditMessage() {
        val state = _uiState.value
        val message = state.editingMessage ?: return
    
        viewModelScope.launch {
            try {
                val json = JSONObject().apply {
                    put("message_id", message.effectiveMsgId)
                    put("content", state.editingContent)
                    put("images", org.json.JSONArray(state.editingImages))
                    put("is_markdown", state.editingIsMarkdown)
                }
                val request = Request.Builder()
                    .url("${ApiAddress}group/edit_message")
                    .post(json.toString().toRequestBody("application/json".toMediaType()))
                    .header("x-access-token", token)
                    .build()
                withContext(Dispatchers.IO) { client.newCall(request).execute() }
                _uiState.update { it.copy(messages = it.messages.map { msg ->
                    if (msg.effectiveMsgId == message.effectiveMsgId) msg.copy(
                        content = state.editingContent,
                        images = state.editingImages,
                        isMarkdown = state.editingIsMarkdown,
                        isEdited = true,
                        editTime = System.currentTimeMillis()
                    )
                    else msg
                })}
                cancelEditMessage()
            } catch (e: Exception) {
                _toastMessage.emit("编辑失败: ${e.message}")
            }
        }
    }
    fun updateInputText(text: String) {
        _uiState.update { it.copy(inputText = text) }
        DraftManager.saveDraft(chatType, chatId, text)
    }
    fun onInputTextChanged(text: String) {
        updateInputText(text)
        typingJob?.cancel()
        if (text.isNotEmpty()) {
            ChatSocketManager.getInstance().sendTypingStatus(chatType, chatId, true)
            typingJob = viewModelScope.launch {
                delay(3000)
                ChatSocketManager.getInstance().sendTypingStatus(chatType, chatId, false)
            }
        }
    }

    fun toggleMarkdown() {
        _uiState.update { it.copy(isMarkdown = !it.isMarkdown) }
    }

    fun handleImageSelected(uri: Uri?, context: Context, coroutineScope: CoroutineScope) {
        if (uiState.value.selectedImages.size >= 20) {
            viewModelScope.launch { _toastMessage.emit("最多只能上传20张图片") }
            return
        }
        if (uri == null) return

        coroutineScope.launch {
            _isUploading.value = true
            _uploadProgress.value = 0f
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val tempFile = File(context.cacheDir, "temp_img_${System.currentTimeMillis()}.jpg")
                FileOutputStream(tempFile).use { output ->
                    inputStream?.copyTo(output)
                }
                inputStream?.close()

                val uploader = HttpUpload(
                    panUrl = "${ApiAddress}upload_image",
                    token = token
                )
                activeUploadCancel = { uploader.cancelUpload() }

                val resultJson = withContext(Dispatchers.IO) {
                    uploader.uploadFile(
                        filePath = tempFile.absolutePath,
                        status = 3,
                        onProgress = { progress ->
                            _uploadProgress.value = progress / 100f
                        }
                    )
                }
                activeUploadCancel = null

                val url = parseUploadResult(resultJson)
                if (url != null) {
                    if (_uiState.value.editingMessage != null) {
                        _uiState.update { it.copy(editingImages = it.editingImages + url) }
                    } else {
                        _uiState.update { it.copy(selectedImages = it.selectedImages + url) }
                    }
                    tempFile.delete()
                } else {
                    _toastMessage.emit("图片上传失败")
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    _toastMessage.emit("上传已取消")
                } else {
                    _toastMessage.emit("上传出错: ${e.message}")
                }
            } finally {
                activeUploadCancel = null
                _isUploading.value = false
                _uploadProgress.value = 0f
            }
        }
    }

    private fun parseUploadResult(resultJson: String): String? {
    return try {
        val jsonObject = JSONObject(resultJson)
        val imageUrl = jsonObject.optString("image_url")
        if (imageUrl.isNotEmpty()) {
            if (imageUrl.startsWith("http")) {
                imageUrl
            } else {
                "${ApiAddress}uploads/$imageUrl"
            }
        } else {
            null
        }
    } catch (_: Exception) {
        null
    }
}

    fun removeImage(index: Int) {
        _uiState.update { state ->
            val updated = state.selectedImages.toMutableList().apply { removeAt(index) }
            state.copy(selectedImages = updated)
        }
    }

    fun cancelUpload() {
        activeUploadCancel?.invoke()
        activeUploadCancel = null
        _isUploading.value = false
        _uploadProgress.value = 0f
    }

    fun showRecallDialog(msgId: String) {
        _recallDialog.value = RecallDialogState(isOpen = true, messageId = msgId)
    }

    fun hideRecallDialog() {
        _recallDialog.value = RecallDialogState(isOpen = false)
    }

    fun recallMessage() {
        val msgId = _recallDialog.value.messageId ?: return
        viewModelScope.launch {
            recallMessageInternal(msgId)
            hideRecallDialog()
        }
    }

    fun setReplyTo(message: Message) {
        _replyTo.value = message
    }

    fun clearReplyTo() {
        _replyTo.value = null
    }

    private fun loadBackground() {
        viewModelScope.launch {
    
            try {
    
                val result = withContext(Dispatchers.IO) {
    
                    val json = JSONObject().apply {
                        put("chat_type", chatType)
                        put("target_id", chatId)
                    }
    
                    val request = Request.Builder()
                        .url("${ApiAddress}chat/get_background")
                        .post(json.toString().toRequestBody("application/json".toMediaType()))
                        .header("x-access-token", token)
                        .build()
    
                    client.newCall(request).execute().use { response ->
    
                        if (!response.isSuccessful) {
                            throw IOException("HTTP ${response.code}")
                        }
    
                        val body = response.body?.string().orEmpty()
    
                        Log.d("CHAT_BG", body)
    
                        AppJson.json.decodeFromString<BackgroundResponse>(body)
                    }
                }
    
                if (result.success) {
    
                    _backgroundUrl.value =
                        result.data?.backgroundUrl?.takeIf { it.isNotBlank() }
    
                    Log.d(
                        "CHAT_BG",
                        "背景加载成功：${result.data?.backgroundUrl}"
                    )
    
                } else {
    
                    Log.e(
                        "CHAT_BG",
                        "背景接口返回失败：${result.message}"
                    )
    
                }
    
            } catch (e: Exception) {
    
                Log.e("CHAT_BG", "loadBackground", e)
    
            }
        }
    }
    private fun loadDraft() {
        val draft = DraftManager.getDraft(chatType, chatId)
        if (!draft.isNullOrBlank()) {
            _uiState.update { it.copy(inputText = draft) }
        }
    }
    private val _emojiPanelVisible = MutableStateFlow(false)
    val emojiPanelVisible: StateFlow<Boolean> = _emojiPanelVisible.asStateFlow()

    private val _emojis = MutableStateFlow<List<EmojiItem>>(emptyList())
    val emojis: StateFlow<List<EmojiItem>> = _emojis.asStateFlow()

    private val _isLoadingEmojis = MutableStateFlow(false)
    val isLoadingEmojis: StateFlow<Boolean> = _isLoadingEmojis.asStateFlow()

    fun toggleEmojiPanel() {
        _emojiPanelVisible.value = !_emojiPanelVisible.value
        if (_emojiPanelVisible.value && _emojis.value.isEmpty()) loadEmojis()
    }

    fun hideEmojiPanel() {
        _emojiPanelVisible.value = false
    }

    private fun loadEmojis() {
        viewModelScope.launch {
            _isLoadingEmojis.value = true
            try {
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url("${ApiAddress}sticker/list")
                    .get()
                    .header("x-access-token", token)
                    .build()
                withContext(Dispatchers.IO) {
                    client.newCall(request).execute().use { response ->
                        val body = response.body?.string()
                        if (response.isSuccessful && body != null) {
                            val json = JSONObject(body)
                            if (json.optBoolean("success")) {
                                val arr = json.optJSONArray("stickers") ?: return@use
                                val list = mutableListOf<EmojiItem>()
                                for (i in 0 until arr.length()) {
                                    val obj = arr.getJSONObject(i)
                                    list.add(EmojiItem(obj.optInt("id"), obj.optString("url"), obj.optInt("type")))
                                }
                                _emojis.value = list
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _toastMessage.emit("加载表情失败")
            } finally {
                _isLoadingEmojis.value = false
            }
        }
    }

    fun sendEmoji(emoji: EmojiItem) {
        viewModelScope.launch {
            try {
                val body = JSONObject().apply {
                    put("chat_type", chatType)
                    put("chat_id", chatId)
                    put("data", JSONObject().apply {
                        put("is_sticker", true)
                        put("sticker_id", emoji.id)
                    })
                }
                val request = Request.Builder()
                    .url("${ApiAddress}chat/send")
                    .post(body.toString().toRequestBody("application/json".toMediaType()))
                    .header("x-access-token", token)
                    .build()
                withContext(Dispatchers.IO) { client.newCall(request).execute() }
                hideEmojiPanel()
            } catch (e: Exception) {
                _toastMessage.emit("发送表情失败")
            }
        }
    }
    fun collectImageAsSticker(message: Message) {
        viewModelScope.launch {
            try {
                val imageUrl = message.images.firstOrNull() ?: return@launch
                val body = JSONObject().apply { put("url", imageUrl) }
                val request = Request.Builder()
                    .url("${ApiAddress}sticker/collect")
                    .post(body.toString().toRequestBody("application/json".toMediaType()))
                    .header("x-access-token", token)
                    .build()
                withContext(Dispatchers.IO) { client.newCall(request).execute() }
                _toastMessage.emit("已收藏为表情")
            } catch (e: Exception) {
                _toastMessage.emit("收藏失败")
            }
        }
    }
    fun collectSticker(message: Message) {
        viewModelScope.launch {
            try {
                val stickerUrl = message.content.ifEmpty { message.images.firstOrNull() ?: return@launch }
                val body = JSONObject().apply { put("url", stickerUrl) }
                val request = Request.Builder()
                    .url("${ApiAddress}sticker/collect")
                    .post(body.toString().toRequestBody("application/json".toMediaType()))
                    .header("x-access-token", token)
                    .build()
                withContext(Dispatchers.IO) { client.newCall(request).execute() }
                _toastMessage.emit("收藏成功")
            } catch (e: Exception) {
                _toastMessage.emit("收藏失败")
            }
        }
    }

    fun deleteSticker(message: Message) {
        viewModelScope.launch {
            try {
                val stickerUrl = message.content.ifEmpty { message.images.firstOrNull() ?: return@launch }
                val body = JSONObject().apply { put("url", stickerUrl) }
                val request = Request.Builder()
                    .url("${ApiAddress}sticker/delete")
                    .post(body.toString().toRequestBody("application/json".toMediaType()))
                    .header("x-access-token", token)
                    .build()
                withContext(Dispatchers.IO) { client.newCall(request).execute() }
                _toastMessage.emit("已删除")
            } catch (e: Exception) {
                _toastMessage.emit("删除失败")
            }
        }
    }
    fun deleteEmoji(emoji: EmojiItem) {
        viewModelScope.launch {
            try {
                val body = JSONObject().apply { put("sticker_id", emoji.id) }
                val request = Request.Builder()
                    .url("${ApiAddress}sticker/delete")
                    .post(body.toString().toRequestBody("application/json".toMediaType()))
                    .header("x-access-token", token)
                    .build()
                withContext(Dispatchers.IO) { client.newCall(request).execute() }
                _emojis.value = _emojis.value.filter { it.id != emoji.id }
                _toastMessage.emit("已删除")
            } catch (e: Exception) {
                _toastMessage.emit("删除失败")
            }
        }
    }
    fun forwardMessage(message: Message, targetChatType: Int, targetChatId: Int) {
        viewModelScope.launch {
            try {
                val body = JSONObject().apply {
                    put("message_id", message.id ?: message.msgId.toIntOrNull() ?: return@launch)
                    put("target_chat_type", targetChatType)
                    put("target_chat_id", targetChatId)
                }
                val request = Request.Builder()
                    .url("${ApiAddress}chat/forward")
                    .post(body.toString().toRequestBody("application/json".toMediaType()))
                    .header("x-access-token", token)
                    .build()
                withContext(Dispatchers.IO) { client.newCall(request).execute() }
                _toastMessage.emit("转发成功")
            } catch (e: Exception) {
                _toastMessage.emit("转发失败")
            }
        }
    }
    




    

    fun handleNewMessage(message: Message) {
    val mentionUsers = message.mentionUsers ?: emptyList()
    if (mentionUsers.contains(currentUserId)) {
        _atMessages.update { list ->
            if (list.none { it.effectiveMsgId == message.effectiveMsgId }) {
                list + message
            } else list
        }
        _hasAtMessage.value = true
    }
    }

    fun clearAtMessages() {
        _atMessages.value = emptyList()
        _hasAtMessage.value = false
        _targetMessageId.value = null
    }

    fun jumpToAtMessage(messageId: String) {
    viewModelScope.launch {
        _isLoadingAtPage.value = true
        try {
            // 提前转换并检查 messageId
            val msgIdInt = messageId.toIntOrNull()
            if (msgIdInt == null || msgIdInt <= 0) {
                _toastMessage.emit("消息 ID 无效")
                _isLoadingAtPage.value = false
                return@launch
            }

            val client = OkHttpClient()
            val json = JSONObject().apply {
                put("chat_type", chatType)
                put("chat_id", chatId)
                put("around_msg_id", msgIdInt)
                put("per_page", 20)
            }
            val request = Request.Builder()
                .url("${ApiAddress}chat/messages")
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .header("x-access-token", token)
                .build()

            withContext(Dispatchers.IO) {
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string() ?: ""
                    val result = AppJson.json.decodeFromString<GetMessagesResponse>(body)
                    withContext(Dispatchers.Main) {
                        if (result.status.code == 0) {
                            val newMessages = result.messages.sortedByDescending { it.sendTime }
                            val mergedList = (_uiState.value.messages + newMessages)
                                .distinctBy { it.effectiveMsgId }
                                .sortedByDescending { it.sendTime }

                            _uiState.update { state ->
                                state.copy(
                                    messages = mergedList,
                                    pagination = result.pagination
                                )
                            }
                            _targetMessageId.value = messageId

                            // 清除本地和服务器标记
                            clearAtMessages()
                            clearMentionOnServer(msgIdInt)

                            // 调试 Toast：跳转成功
                            _toastMessage.emit("跳转成功，消息已加载")
                        } else {
                            _toastMessage.emit("定位失败: ${result.status.msg}")
                        }
                        _isLoadingAtPage.value = false
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AT_MESSAGE", "跳转失败", e)
            _isLoadingAtPage.value = false
            _toastMessage.emit("网络错误，跳转失败")
        }
    }
}
    fun clearTargetMessageId() {
        _targetMessageId.value = null
    }
    private fun clearMentionOnServer(messageId: Int) {
        viewModelScope.launch {
            try {
                val json = JSONObject().apply {
                    put("message_id", messageId)
                }
                val request = Request.Builder()
                    .url("${ApiAddress}chat/clear_mention")
                    .post(json.toString().toRequestBody("application/json".toMediaType()))
                    .header("x-access-token", token)
                    .build()
                withContext(Dispatchers.IO) {
                    client.newCall(request).execute()
                }
            } catch (e: Exception) {
                // 静默失败，不影响使用
            }
        }
    }
    private val _activeDays = MutableStateFlow<List<ActiveDay>>(emptyList())
    val activeDays: StateFlow<List<ActiveDay>> = _activeDays.asStateFlow()

    private val _showHeatmap = MutableStateFlow(false)
    val showHeatmap: StateFlow<Boolean> = _showHeatmap.asStateFlow()

    private val _heatmapYearMonth = MutableStateFlow(YearMonth.now())
    val heatmapYearMonth: StateFlow<YearMonth> = _heatmapYearMonth.asStateFlow()

    private val _isLoadingActiveDays = MutableStateFlow(false)
    val isLoadingActiveDays: StateFlow<Boolean> = _isLoadingActiveDays.asStateFlow()
    private val loadedYearMonths = mutableSetOf<YearMonth>()
    fun loadActiveDays(yearMonth: YearMonth = YearMonth.now()) {
        if (yearMonth in loadedYearMonths) return  
        viewModelScope.launch {
            _isLoadingActiveDays.value = true
            try {
                val client = OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .build()
                
                val jsonBody = JSONObject().apply {
                    put("chat_type", chatType)
                    put("chat_id", chatId)
                    put("page", 1)
                    put("per_page", 31) 
                }.toString()
                
                val requestBody = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url("${ApiAddress}chat/active_days")
                    .post(requestBody)
                    .addHeader("x-access-token", token)
                    .build()
                
                withContext(Dispatchers.IO) {
                    client.newCall(request).execute().use { response ->
                        val body = response.body?.string()
                        if (response.isSuccessful && body != null) {
                            val result = jsonParser.decodeFromString<ActiveDaysResponse>(body)
                            if (result.success) {
                                _activeDays.value = result.activeDays
                                loadedYearMonths.add(yearMonth)  
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ActiveDays", "加载活跃天数失败", e)
            } finally {
                _isLoadingActiveDays.value = false
            }
        }
    }
    fun showHeatmap(dateString: String? = null) {
        val yearMonth = if (dateString != null) {
            parseDateString(dateString)
        } else {
            YearMonth.now()
        }
        _heatmapYearMonth.value = yearMonth
        loadActiveDays(yearMonth)
        _showHeatmap.value = true
    }
    
    private fun parseDateString(dateString: String): YearMonth {
        return try {
            val cleaned = dateString
                .replace("年", "-")
                .replace("月", "-")
                .replace("日", "")
            val parts = cleaned.split("-").filter { it.isNotBlank() }
    
            if (parts.size == 2) {
                val month = parts[0].toIntOrNull() ?: return YearMonth.now()
                YearMonth.of(YearMonth.now().year, month)
            } else if (parts.size >= 3) {
                val year = parts[0].toIntOrNull() ?: return YearMonth.now()
                val month = parts[1].toIntOrNull() ?: return YearMonth.now()
                YearMonth.of(year, month)
            } else {
                YearMonth.now()
            }
        } catch (e: Exception) {
            YearMonth.now()
        }
    }
    fun hideHeatmap() {
        _showHeatmap.value = false
    }
    fun previousMonth() {
        val newMonth = _heatmapYearMonth.value.minusMonths(1)
        _heatmapYearMonth.value = newMonth
        loadActiveDays(newMonth)
    }
    fun nextMonth() {
        val newMonth = _heatmapYearMonth.value.plusMonths(1)
        if (!newMonth.isAfter(YearMonth.now())) {
            _heatmapYearMonth.value = newMonth
            loadActiveDays(newMonth)
        }
    }
        private fun loadGroupInfo() {

            viewModelScope.launch {
        
                try {
        
                    val result = withContext(Dispatchers.IO) {
        
                        val json = JSONObject().apply {
                            put("group_id", chatId)
                        }
        
                        val request = Request.Builder()
                            .url("${ApiAddress}group/detail")
                            .post(json.toString().toRequestBody("application/json".toMediaType()))
                            .header("x-access-token", token)
                            .build()
        
                        client.newCall(request).execute().use { response ->
        
                            if (!response.isSuccessful) {
                                throw IOException("HTTP ${response.code}")
                            }
        
                            val body = response.body?.string().orEmpty()
        
                            Log.d("GROUP_INFO", body)
        
                            AppJson.json.decodeFromString<GroupDetailResponse>(body)
                        }
        
                    }
        
                    if (result.success && result.group != null) {
        
                        _uiState.update {
        
                            it.copy(
                                groupInfo = result.group
                            )
        
                        }
        
                        Log.d(
                            "GROUP_INFO",
                            "群资料加载成功：${result.group.name}"
                        )
        
                    } else {
        
                        Log.e(
                            "GROUP_INFO",
                            "接口返回失败：${result.message}"
                        )
        
                    }
        
                } catch (e: Exception) {
        
                    Log.e("GROUP_INFO", "loadGroupInfo", e)
        
                }
        
            }
        
        }

    override fun onCleared() {
        super.onCleared()
        wsObserver?.let {
            ChatSocketManager.getInstance().removeObserver(it)
        }
    }
}


@Serializable
data class BackgroundResponse(
    val success: Boolean,
    val data: BackgroundData? = null,
    val message: String? = null
)

@Serializable
data class BackgroundData(
    @SerialName("chat_type") val chatType: Int,
    @SerialName("target_id") val targetId: Int,
    @SerialName("background_url") val backgroundUrl: String = ""
)


class MessageDetailViewModelFactory(
    private val token: String,
    private val chatType: Int,
    private val chatId: Int
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MessageDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MessageDetailViewModel(token, chatType, chatId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

