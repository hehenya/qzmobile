package com.example.toolbox.message

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.toolbox.ApiAddress
import com.example.toolbox.AppJson
import com.example.toolbox.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

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

    init {
        loadMessages()
        connectWebSocket()
        loadBackground()
        if (chatType == 2) {
            loadGroupInfo()   // 群聊时加载群信息
        }
    }

    // ============= WebSocket =============
    fun connectWebSocket() {
        val manager = ChatSocketManager.getInstance()
        manager.connect(token)

        manager.addObserver { type, chatIdStr, chatTypeInt, message ->
            if (chatIdStr.toIntOrNull() == chatId && chatTypeInt == chatType) {
                when (type) {
                    "new" -> {
                        if (message.effectiveMsgId !in msgIdCache) {
                            msgIdCache.add(message.effectiveMsgId)
                            _uiState.update { state ->
                                state.copy(messages = listOf(message) + state.messages)
                            }
                        }
                    }
                    "edit" -> {
                        _uiState.update { state ->
                            state.copy(messages = state.messages.map {
                                if (it.effectiveMsgId == message.effectiveMsgId) it.copy(
                                    content = message.content,
                                    isEdited = true,
                                    editTime = message.editTime
                                ) else it
                            })
                        }
                    }
                    "recall" -> {
                        _uiState.update { state ->
                            state.copy(messages = state.messages.map {
                                if (it.effectiveMsgId == message.effectiveMsgId) it.copy(
                                    isRecalled = true,
                                    recallHint = message.recallHint
                                ) else it
                            })
                        }
                    }
                }
            }
        }
    }

    // ============= 消息加载 =============
    private fun loadMessages() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = withContext(Dispatchers.IO) {
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
                    client.newCall(request).execute()
                }
                val body = response.body?.string() ?: ""
                Log.d("MESSAGES", "response: $body")
                val result = AppJson.json.decodeFromString<GetMessagesResponse>(body)
                if (result.status.code == 0) {
                    msgIdCache.addAll(result.messages.map { it.effectiveMsgId })
                    // 私聊背景可从消息接口直接获取
                    if (chatType == 1 && result.chatBackgroundUrl.isNotEmpty()) {
                        _backgroundUrl.value = result.chatBackgroundUrl
                    }
                    _uiState.update { state ->
                        state.copy(
                            messages = result.messages,
                            isLoading = false,
                            hasMore = (result.pagination?.pages ?: 1) > currentPage,
                            pagination = result.pagination,
                            canSend = result.canSend,
                            isAdmin = result.isAdmin,
                            relationship = result.relationship,
                            isChatExpired = result.tempChatExpired,
                            otherUser = result.otherUser,
                            groupInfo = if (chatType == 2) state.groupInfo else null
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = result.status.msg) }
                }
            } catch (e: Exception) {
                Log.e("MESSAGES", "加载失败", e)
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun loadMore() {
        if (!hasMore || _uiState.value.isLoadingMore) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            try {
                currentPage++
                val response = withContext(Dispatchers.IO) {
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
                    client.newCall(request).execute()
                }
                val body = response.body?.string() ?: ""
                val result = AppJson.json.decodeFromString<GetMessagesResponse>(body)
                if (result.status.code == 0) {
                    val newMessages = result.messages.filter { it.effectiveMsgId !in msgIdCache }
                    msgIdCache.addAll(newMessages.map { it.effectiveMsgId })
                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages + newMessages,
                            isLoadingMore = false,
                            hasMore = (result.pagination?.pages ?: 1) > currentPage,
                            pagination = result.pagination
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoadingMore = false, error = result.status.msg) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingMore = false, error = e.message) }
            }
        }
    }

    fun refresh() {
        currentPage = 1
        hasMore = true
        msgIdCache.clear()
        loadMessages()
    }

    // ============= 多选 =============
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
                put("msg_id", msgId)
                put("chat_type", chatType)
                put("chat_id", chatId)
            }
            val request = Request.Builder()
                .url("${ApiAddress}chat/recall")
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
                throw Exception("撤回失败")
            }
        }
    }

    // ============= 编辑 =============
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

    // ============= 发送 =============
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

    // ============= 输入 =============
    fun updateInputText(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun toggleMarkdown() {
        _uiState.update { it.copy(isMarkdown = !it.isMarkdown) }
    }

    fun handleImageSelected(uri: Uri?, context: Context, scope: kotlinx.coroutines.CoroutineScope) {
        uri?.let {
            _uiState.update { state -> state.copy(selectedImages = state.selectedImages + it.toString()) }
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

    // ============= 撤回对话框 =============
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

    // ============= 引用 =============
    fun setReplyTo(message: Message) {
        _replyTo.value = message
    }

    fun clearReplyTo() {
        _replyTo.value = null
    }

    // ============= 背景 =============
    private fun loadBackground() {
        viewModelScope.launch {
            try {
                val json = JSONObject().apply {
                    put("chat_type", chatType)
                    put("target_id", chatId)
                }
                val request = Request.Builder()
                    .url("${ApiAddress}chat/get_background")
                    .post(json.toString().toRequestBody("application/json".toMediaType()))
                    .header("x-access-token", token)
                    .build()
                val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
                val body = response.body?.string() ?: ""
                Log.d("BACKGROUND", "背景返回: $body")
                val result = AppJson.json.decodeFromString<BackgroundResponse>(body)
                if (result.success) {
                    _backgroundUrl.value = result.data?.backgroundUrl?.takeIf { it.isNotEmpty() }
                }
            } catch (e: Exception) {
                Log.e("BACKGROUND", "背景加载失败", e)
            }
        }
    }

    // ============= 群信息 =============
    private fun loadGroupInfo() {
        viewModelScope.launch {
            try {
                val json = JSONObject().apply {
                    put("group_id", chatId)
                }
                val request = Request.Builder()
                    .url("${ApiAddress}group/detail")
                    .post(json.toString().toRequestBody("application/json".toMediaType()))
                    .header("x-access-token", token)
                    .build()
                val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
                val body = response.body?.string() ?: ""
                Log.d("GROUPINFO", "群信息返回: $body")
                val result = AppJson.json.decodeFromString<GroupDetailResponse>(body)
                if (result.success && result.group != null) {
                    _uiState.update { it.copy(groupInfo = result.group) }
                }
            } catch (e: Exception) {
                Log.e("GROUPINFO", "群信息加载失败", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        ChatSocketManager.getInstance().removeObserver { _, _, _, _ -> }
    }
}

// ============= 辅助数据类 =============
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