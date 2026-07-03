package com.example.toolbox.message

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.toolbox.ApiAddress
import com.example.toolbox.community.uploadImage
import kotlinx.coroutines.CoroutineScope
import java.io.File
import java.io.FileOutputStream
import com.example.toolbox.AppJson
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

class MessageDetailViewModel(
    private val token: String,
    private val chatType: Int,
    private val chatId: Int
) : ViewModel() {

    private val _uiState = MutableStateFlow(MessageDetailUiState(chatType = chatType, chatId = chatId))
    val uiState: StateFlow<MessageDetailUiState> = _uiState.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    private val _recallDialog = MutableStateFlow(RecallDialogState())
    val recallDialog: StateFlow<RecallDialogState> = _recallDialog.asStateFlow()

    private val _editDialog = MutableStateFlow(EditDialogState())
    val editDialog: StateFlow<EditDialogState> = _editDialog.asStateFlow()

    private val _replyTo = MutableStateFlow<Message?>(null)
    val replyTo: StateFlow<Message?> = _replyTo.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _uploadProgress = MutableStateFlow(0f)
    val uploadProgress: StateFlow<Float> = _uploadProgress.asStateFlow()

    private val _backgroundUrl = MutableStateFlow<String?>(null)
    val backgroundUrl: StateFlow<String?> = _backgroundUrl.asStateFlow()

    private val client = OkHttpClient()
    private var currentPage = 1
    private var hasMore = true
    private val msgIdCache = mutableSetOf<String>()
    private var wsObserver: ((String, String, Int, Message) -> Unit)? = null

    init {
        loadMessages()
        connectWebSocket()
        loadBackground()
        if (chatType == 2) {
            loadGroupInfo()
        }
    }

    fun connectWebSocket() {

        val manager = ChatSocketManager.getInstance()
    
        manager.connect(token)
    
        wsObserver = observer@ { type, chatIdStr, chatTypeInt, message ->
    
            val incomingChatId = chatIdStr.toIntOrNull()
                ?: return@observer
    
            // 不是当前聊天直接忽略
            if (incomingChatId != chatId) return@observer
            if (chatTypeInt != chatType) return@observer
    
            when (type) {
    
                "recall" -> {
                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages.map {
                                if (it.effectiveMsgId == message.effectiveMsgId) {
                                    it.copy(
                                        isRecalled = true,
                                        recallHint = message.recallHint ?: "已撤回"
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
                    }
                }
            }
        }
    
        manager.addObserver(wsObserver!!)

        Toast.makeText(
            getApplication(),
            "Observer 已注册",
            Toast.LENGTH_SHORT
        ).show()
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
    
                    msgIdCache.clear()
                    msgIdCache.addAll(sortedMessages.map { it.effectiveMsgId })
    
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
                            error = null
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
    
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.stackTraceToString()
                    )
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

    private suspend fun recallMessageInternal(msgId: String) {
        withContext(Dispatchers.IO) {
            val json = JSONObject().apply {
                put("message_id", msgId)
            }
            val request = Request.Builder()
                .url("${ApiAddress}group/recall")
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

    fun showEditDialog(message: Message) {
        _editDialog.value = EditDialogState(isOpen = true, message = message, newContent = message.content, isMarkdown = message.isMarkdown)
    }

    fun hideEditDialog() {
        _editDialog.value = EditDialogState(isOpen = false)
    }

    fun updateEditContent(content: String) {
        _editDialog.update { it.copy(newContent = content) }
    }

    fun toggleEditMarkdown() {
        _editDialog.update { it.copy(isMarkdown = !it.isMarkdown) }
    }

    fun editMessage() {
        val state = _editDialog.value
        val message = state.message ?: return
        viewModelScope.launch {
            try {
                val json = JSONObject().apply {
                    put("msg_id", message.effectiveMsgId)
                    put("content", state.newContent)
                    put("is_markdown", state.isMarkdown)
                }
                val request = Request.Builder()
                    .url("${ApiAddress}chat/edit")
                    .post(json.toString().toRequestBody("application/json".toMediaType()))
                    .header("x-access-token", token)
                    .build()
                withContext(Dispatchers.IO) { client.newCall(request).execute() }
                _uiState.update { it.copy(messages = it.messages.map { msg ->
                    if (msg.effectiveMsgId == message.effectiveMsgId) msg.copy(content = state.newContent, isEdited = true, editTime = System.currentTimeMillis())
                    else msg
                })}
                hideEditDialog()
            } catch (e: Exception) {
                _toastMessage.emit("编辑失败: ${e.message}")
            }
        }
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
                    it.copy(inputText = "", selectedImages = emptyList(), isMarkdown = false, isSending = false)
                }
                _replyTo.value = null
            } catch (e: Exception) {
                _uiState.update { it.copy(isSending = false) }
                _toastMessage.emit("发送失败: ${e.message}")
            }
        }
    }

    fun updateInputText(text: String) {
        _uiState.update { it.copy(inputText = text) }
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
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val tempFile = File(context.cacheDir, "temp_img_${System.currentTimeMillis()}.jpg")
            FileOutputStream(tempFile).use { output ->
                inputStream?.copyTo(output)
            }
            inputStream?.close()

            val url = uploadImage(tempFile.absolutePath, token, 3) { _: Int -> }
            if (url != null) {
                _uiState.update { it.copy(selectedImages = it.selectedImages + url) }
                tempFile.delete()
            } else {
                _toastMessage.emit("图片上传失败")
            }
        } catch (e: Exception) {
            _toastMessage.emit("上传出错: ${e.message}")
        }
    }
}

    fun removeImage(index: Int) {
        _uiState.update { state ->
            val updated = state.selectedImages.toMutableList().apply { removeAt(index) }
            state.copy(selectedImages = updated)
        }
    }

    fun cancelUpload() {
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