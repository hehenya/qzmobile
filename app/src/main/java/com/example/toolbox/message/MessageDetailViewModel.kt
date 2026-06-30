package com.example.toolbox.message

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.toolbox.ApiAddress
import com.example.toolbox.AppJson
import com.example.toolbox.DraftManager
import com.example.toolbox.community.uploadImage
import com.example.toolbox.data.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class MessageDetailViewModel(
    private val token: String,
    private val chatType: Int,  // 1: 私聊, 2: 群聊
    private val chatId: Int     // 私聊为对方用户ID，群聊为群ID
) : ViewModel() {

    private val _uiState = MutableStateFlow(MessageDetailUiState(chatType = chatType, chatId = chatId))
    val uiState: StateFlow<MessageDetailUiState> = _uiState.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    private val _recallDialog = MutableStateFlow(RecallDialogState())
    val recallDialog: StateFlow<RecallDialogState> = _recallDialog.asStateFlow()

    // 背景URL
    private val _backgroundUrl = MutableStateFlow<String?>(null)
    val backgroundUrl: StateFlow<String?> = _backgroundUrl.asStateFlow()

    private val _editDialog = MutableStateFlow(EditDialogState())
    val editDialog: StateFlow<EditDialogState> = _editDialog.asStateFlow()
    
    private val _replyTo = MutableStateFlow<Message?>(null)
    val replyTo: StateFlow<Message?> = _replyTo.asStateFlow()

    // 上传进度相关
    private val _uploadProgress = MutableStateFlow(0f)
    val uploadProgress: StateFlow<Float> = _uploadProgress.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    fun setReplyTo(message: Message?) { _replyTo.value = message }
    fun clearReplyTo() { _replyTo.value = null }

    private val client = OkHttpClient()
    private val json = AppJson.json

    init {
        // 加载草稿
        val draft = DraftManager.getDraft(chatType, chatId)
        if (draft != null) {
            _uiState.update { it.copy(inputText = draft) }
        }
        loadMessages(page = 1, isRefresh = true)
        if (chatType == 2) {
            loadGroupInfo()
        }
        loadChatBackground()
    }

    fun loadChatBackground() {
        viewModelScope.launch {
            try {
                val url = "${ApiAddress}chat/get_background"
                val requestBody = org.json.JSONObject().apply {
                    put("chat_type", chatType)
                    put("target_id", chatId)
                }.toString()
                val request = Request.Builder()
                    .url(url)
                    .header("x-access-token", token)
                    .post(requestBody.toRequestBody("application/json".toMediaType()))
                    .build()
                withContext(Dispatchers.IO) {
                    client.newCall(request).execute().use { response ->
                        val body = response.body?.string() ?: ""
                        val json = org.json.JSONObject(body)
                        if (json.optBoolean("success")) {
                            val data = json.optJSONObject("data")
                            val bgUrl = data?.optString("background_url", "") ?: ""
                            _backgroundUrl.value = bgUrl.ifEmpty { null }
                        }
                    }
                }
            } catch (_: Exception) { }
        }
    }

    fun loadMessages(page: Int, isRefresh: Boolean) {
        viewModelScope.launch {
            _uiState.update {
                if (isRefresh) it.copy(isRefreshing = true, error = null)
                else it.copy(isLoadingMore = true, error = null)
            }
            try {
                val url = "${ApiAddress}chat/messages"
                val requestObj = buildJsonObject {
                    put("chat_type", chatType)
                    put("chat_id", chatId)
                    put("page", page)
                    put("per_page", 20)
                }
                val body = requestObj.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url(url)
                    .header("x-access-token", token)
                    .addHeader("linkinfo", "true")
                    .post(body)
                    .build()
                val result = withContext(Dispatchers.IO) {
                    try {
                        val response = client.newCall(request).execute()
                        if (response.isSuccessful) {
                            val responseBody = response.body.string()
                            json.decodeFromString<GetMessagesResponse>(responseBody)
                        } else null
                    } catch (e: Exception) {
                        Log.e("MessageDetailVM", "loadMessages error", e)
                        null
                    }
                }
                if (result != null && result.status.code == 0) {
                    val hasMore = result.pagination?.let { it.page < it.pages } ?: false
                    _uiState.update { current ->
                        val newMessages = if (isRefresh) result.messages.sortedByDescending { it.sendTime }
                        else (current.messages + result.messages.sortedByDescending { it.sendTime }).distinctBy { it.msgId }
                        current.copy(
                            messages = newMessages, canSend = result.canSend, pagination = result.pagination,
                            hasMore = hasMore, otherUser = result.otherUser, relationship = result.relationship,
                            isChatExpired = result.tempChatExpired, isAdmin = result.isAdmin,
                            isRefreshing = false, isLoadingMore = false, error = null
                        )
                    }
                } else {
                    _uiState.update { it.copy(isRefreshing = false, isLoadingMore = false, error = result?.status?.msg ?: "请求失败") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isRefreshing = false, isLoadingMore = false, error = e.message) }
            }
        }
    }

    fun refresh() {
        loadMessages(page = 1, isRefresh = true)
        if (chatType == 2) loadGroupInfo()
    }

    private fun loadGroupInfo() {
        viewModelScope.launch {
            try {
                val url = "${ApiAddress}group/detail"
                val requestBody = buildJsonObject { put("group_id", chatId) }
                val request = Request.Builder()
                    .url(url).header("x-access-token", token)
                    .post(json.encodeToString(requestBody).toRequestBody("application/json".toMediaType()))
                    .build()
                val result = withContext(Dispatchers.IO) {
                    val response = client.newCall(request).execute()
                    val responseBody = response.body.string()
                    if (response.isSuccessful) {
                        try {
                            val parsed = json.decodeFromString<GroupDetailResponse>(responseBody)
                            if (parsed.success) parsed.group else null
                        } catch (_: Exception) { null }
                    } else null
                }
                if (result != null) { _uiState.update { it.copy(groupInfo = result) } }
            } catch (_: Exception) {}
        }
    }

    fun loadMore() {
        val currentState = _uiState.value
        if (currentState.isLoadingMore || !currentState.hasMore || currentState.pagination == null) return
        val nextPage = currentState.pagination.page + 1
        loadMessages(page = nextPage, isRefresh = false)
    }

    fun sendMessage() {
        val state = _uiState.value
        if (state.inputText.isBlank() && state.selectedImages.isEmpty()) {
            viewModelScope.launch { _toastMessage.emit("请输入内容或选择图片") }
            return
        }
        viewModelScope.launch {
            try {
                val url = "${ApiAddress}chat/send"
                val requestData = SendMessageRequest(
                    chatType = chatType, chatId = chatId,
                    data = MessageData(text = state.inputText, images = state.selectedImages, isMarkdown = state.isMarkdown),
                    quoteMsgId = _replyTo.value?.msgId
                )
                val bodyJson = json.encodeToString(requestData)
                val body = bodyJson.toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url(url).header("x-access-token", token)
                    .post(body).build()
                val result = withContext(Dispatchers.IO) {
                    try {
                        val response = client.newCall(request).execute()
                        val responseBody = response.body.string()
                        if (response.isSuccessful) json.decodeFromString<SendMessageResponse>(responseBody)
                        else {
                            val status = try { json.decodeFromString<ChatStatus>(responseBody) } catch (_: Exception) { ChatStatus(number = response.code, code = -1, msg = "发送失败") }
                            SendMessageResponse(status = status)
                        }
                    } catch (e: Exception) { SendMessageResponse(status = ChatStatus(-1, -1, e.message ?: "未知错误")) }
                }
                if (result.status.code == 0) {
                    _uiState.update { it.copy(inputText = "", selectedImages = emptyList()) }
                    _replyTo.value = null
                    // 发送成功清除草稿
                    DraftManager.removeDraft(chatType, chatId)
                } else {
                    _toastMessage.emit(result.status.msg)
                }
            } catch (e: Exception) {
                _toastMessage.emit(e.message ?: "发送失败")
            }
        }
    }

    fun showRecallDialog(messageId: String) { _recallDialog.update { RecallDialogState(isOpen = true, messageId = messageId) } }
    fun hideRecallDialog() { _recallDialog.update { RecallDialogState() } }

    fun recallMessage() {
        val msgId = _recallDialog.value.messageId ?: return
        viewModelScope.launch {
            try {
                val url = if (chatType == 1) "${ApiAddress}private/delete_message" else "${ApiAddress}group/recall"
                val requestBody = buildJsonObject { put("message_id", msgId) }.toString()
                val body = requestBody.toRequestBody("application/json".toMediaType())
                val request = Request.Builder().url(url).header("x-access-token", token).post(body).build()
                val result = withContext(Dispatchers.IO) {
                    try {
                        val response = client.newCall(request).execute()
                        val responseBody = response.body.string()
                        json.decodeFromString<RecallResponse>(responseBody)
                    } catch (e: Exception) { RecallResponse(success = false, message = "操作失败: ${e.message}") }
                }
                if (result.success) {
                    _toastMessage.emit(result.message ?: "撤回成功")
                    val hint = result.recall_hint ?: "你撤回了消息"
                    _uiState.update { state ->
                        state.copy(messages = state.messages.map { msg ->
                            if (msg.effectiveMsgId == msgId) msg.copy(msgDeleteTime = System.currentTimeMillis(), content = "", images = emptyList(), isRecalled = true, isSystem = false, recallHint = hint)
                            else msg
                        })
                    }
                } else { _toastMessage.emit(result.message ?: "撤回失败") }
            } catch (e: Exception) { _toastMessage.emit(e.message ?: "撤回失败") }
            finally { hideRecallDialog() }
        }
    }

    fun showEditDialog(message: Message) {
        _editDialog.update { EditDialogState(isOpen = true, message = message, newContent = message.content, newImages = message.images, isMarkdown = message.isMarkdown) }
    }
    fun toggleEditMarkdown() { _editDialog.update { it.copy(isMarkdown = !it.isMarkdown) } }
    fun hideEditDialog() { _editDialog.update { EditDialogState() } }
    fun updateEditContent(newContent: String) { _editDialog.update { it.copy(newContent = newContent) } }

    fun editMessage() {
        val dialogState = _editDialog.value
        val message = dialogState.message ?: return
        viewModelScope.launch {
            try {
                val url = if (chatType == 1) "${ApiAddress}private/edit_message" else "${ApiAddress}group/edit_message"
                val jsonObject = buildJsonObject {
                    put("message_id", message.effectiveMsgId)
                    if (chatType == 1) {
                        put("new_content", dialogState.newContent)
                        put("new_images", buildJsonArray { dialogState.newImages.forEach { add(JsonPrimitive(it)) } })
                        put("new_is_markdown", dialogState.isMarkdown)
                    } else {
                        put("content", dialogState.newContent)
                        put("images", buildJsonArray { dialogState.newImages.forEach { add(JsonPrimitive(it)) } })
                        put("is_markdown", dialogState.isMarkdown)
                    }
                }
                val bodyJson = jsonObject.toString()
                val body = bodyJson.toRequestBody("application/json".toMediaType())
                val request = Request.Builder().url(url).header("x-access-token", token).post(body).build()
                val result = withContext(Dispatchers.IO) {
                    try {
                        val response = client.newCall(request).execute()
                        val responseBody = response.body.string()
                        if (response.isSuccessful) {
                            val jsonElement = json.decodeFromString<JsonElement>(responseBody)
                            val msg = jsonElement.jsonObject["msg"]?.jsonPrimitive?.content
                            ChatStatus(number = 200, code = 0, msg = msg ?: "编辑成功")
                        } else ChatStatus(number = response.code, code = -1, msg = "编辑失败")
                    } catch (e: Exception) { ChatStatus(number = -1, code = -1, msg = "编辑失败: ${e.message}") }
                }
                if (result.code == 0) { _toastMessage.emit("编辑成功"); refresh() }
                else _toastMessage.emit(result.msg)
            } catch (e: Exception) { _toastMessage.emit(e.message ?: "编辑失败") }
            finally { hideEditDialog() }
        }
    }

    fun updateInputText(text: String) {
        _uiState.update { it.copy(inputText = text) }
        DraftManager.saveDraft(chatType, chatId, text)
    }

    fun handleImageSelected(uri: Uri?, context: Context, coroutineScope: CoroutineScope) {
        if (uiState.value.selectedImages.size >= 9) {
            viewModelScope.launch { _toastMessage.emit("最多只能上传9张图片") }
            return
        }
        if (uri == null) return
        coroutineScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    _toastMessage.emit("无法读取图片")
                    return@launch
                }
                val tempFile = File(context.cacheDir, "temp_img_${System.currentTimeMillis()}.jpg")
                FileOutputStream(tempFile).use { output -> inputStream.copyTo(output) }
                _isUploading.value = true
                _uploadProgress.value = 0f
                val url = uploadImage(tempFile.absolutePath, token, 3) { progress ->
                    _uploadProgress.value = progress / 100f
                }
                _isUploading.value = false
                if (url != null) {
                    addImage(url)
                    tempFile.delete()
                } else {
                    _toastMessage.emit("图片上传失败")
                }
            } catch (e: Exception) {
                _isUploading.value = false
                _toastMessage.emit("上传出错: ${e.message}")
            }
        }
    }

    fun addImage(imageUrl: String) {
        val current = _uiState.value.selectedImages
        if (current.size < 9) _uiState.update { it.copy(selectedImages = current + imageUrl) }
        else viewModelScope.launch { _toastMessage.emit("最多选择9张图片") }
    }

    fun removeImage(index: Int) {
        val current = _uiState.value.selectedImages.toMutableList()
        if (index in current.indices) { current.removeAt(index); _uiState.update { it.copy(selectedImages = current) } }
    }

    fun toggleMarkdown() { _uiState.update { it.copy(isMarkdown = !it.isMarkdown) } }

    fun cancelUpload() {
        _isUploading.value = false
        _uploadProgress.value = 0f
    }

    fun connectWebSocket() {
        if (token.isNotBlank()) {
            val manager = ChatSocketManager.getInstance()
            manager.removeObserver(messageObserver)
            manager.addObserver(messageObserver)
            manager.connect(token)
        }
    }

    fun disconnectWebSocket() { ChatSocketManager.getInstance().removeObserver(messageObserver) }

    private val messageObserver: (type: String, chatId: String, chatType: Int, message: Message) -> Unit = { type, pushChatId, pushChatType, message ->
        when (type) {
            "new", "edit", "recall" -> {
                val isCurrentChat = pushChatType == this.chatType && pushChatId == this.chatId.toString()
                if (isCurrentChat) {
                    when (type) {
                        "new" -> addNewMessage(message)
                        "edit" -> refresh()
                        "recall" -> removeMessage(message.msgId)
                    }
                }
            }
        }
    }

    private fun addNewMessage(message: Message) {
        _uiState.update { state ->
            if (state.messages.any { it.effectiveMsgId == message.effectiveMsgId }) state
            else state.copy(messages = listOf(message) + state.messages)
        }
    }

    private fun updateMessage(message: Message) {
        _uiState.update { state ->
            val updated = state.messages.map { if (it.effectiveMsgId == message.effectiveMsgId) message else it }
            state.copy(messages = updated)
        }
    }

    private fun removeMessage(msgId: String) {
        _uiState.update { state ->
            val updated = state.messages.map { msg ->
                if (msg.effectiveMsgId == msgId) msg.copy(msgDeleteTime = System.currentTimeMillis(), content = "", images = emptyList(), isRecalled = true, recallHint = "$msg.displayName 撤回了消息")
                else msg
            }
            state.copy(messages = updated)
        }
    }

    override fun onCleared() {
        super.onCleared()
        disconnectWebSocket()
    }
}

class MessageDetailViewModelFactory(
    private val token: String,
    private val chatType: Int,
    private val chatId: Int
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MessageDetailViewModel::class.java)) {
            return MessageDetailViewModel(token, chatType, chatId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}