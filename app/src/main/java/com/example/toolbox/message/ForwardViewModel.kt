package com.example.toolbox.message

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.toolbox.ApiAddress
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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

// ---------- 数据类 ----------
data class ForwardTarget(
    val chatId: Int,
    val chatType: Int,
    val displayName: String,
    val avatarUrl: String = "",
    val isPinned: Boolean = false
) {
    val key: Pair<Int, Int> get() = Pair(chatType, chatId)
}

// ---------- UI 状态 ----------
data class ForwardUiState(
    val isOpen: Boolean = true,
    val isLoading: Boolean = false,
    val targets: List<ForwardTarget> = emptyList(),
    val query: String = "",
    val selectedKeys: Set<Pair<Int, Int>> = emptySet(),
    val sourceMsgIds: List<String> = emptyList(),
    val sourceChatType: Int = 1,
    val nextSourceIndex: Int = 0,
    val isSending: Boolean = false,
    val isLocked: Boolean = false,
    val isCompleted: Boolean = false,
    val error: String? = null
) {
    val filteredTargets: List<ForwardTarget>
        get() = if (query.isBlank()) targets
        else targets.filter { it.displayName.contains(query, ignoreCase = true) }

    val selectedTargets: List<ForwardTarget>
        get() = targets.filter { it.key in selectedKeys }

    val pendingSourceCount: Int
        get() = (sourceMsgIds.size - nextSourceIndex).coerceAtLeast(0)

    val canSend: Boolean
        get() = selectedKeys.isNotEmpty() && pendingSourceCount > 0 && !isSending && !isCompleted
}

// ---------- 事件 ----------
sealed interface ForwardEvent {
    data class SourceForwarded(val msgId: String) : ForwardEvent
    data class Completed(val recipients: List<ForwardTarget>) : ForwardEvent
}

// ---------- ViewModel ----------
class ForwardViewModel(private val token: String) : ViewModel() {
    private val _uiState = MutableStateFlow(ForwardUiState())
    val uiState: StateFlow<ForwardUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ForwardEvent>()
    val events: SharedFlow<ForwardEvent> = _events.asSharedFlow()

    fun open(sourceChatType: Int, sourceMsgIds: List<String>) {
        _uiState.value = ForwardUiState(
            isOpen = true,
            isLoading = true,
            sourceMsgIds = sourceMsgIds,
            sourceChatType = sourceChatType
        )
        loadTargets()
    }

    fun close() {
        if (_uiState.value.isSending) return
        _uiState.value = ForwardUiState(isOpen = false)
    }

    fun updateQuery(query: String) {
        _uiState.update { it.copy(query = query, error = null) }
    }

    fun retryLoad() {
        if (_uiState.value.isLoading || _uiState.value.isSending) return
        _uiState.update { it.copy(isLoading = true, error = null) }
        loadTargets()
    }

    fun toggleTarget(target: ForwardTarget) {
        _uiState.update { state ->
            if (state.isSending || state.isLocked || state.isCompleted) return@update state
            val selected = if (target.key in state.selectedKeys) {
                state.selectedKeys - target.key
            } else {
                state.selectedKeys + target.key
            }
            state.copy(selectedKeys = selected, error = null)
        }
    }

    fun send() {
        val snapshot = _uiState.value
        if (!snapshot.canSend) return

        val recipientTargets = snapshot.selectedTargets
        _uiState.update { it.copy(isSending = true, isLocked = true, error = null) }

        viewModelScope.launch {
            var index = snapshot.nextSourceIndex
            while (index < snapshot.sourceMsgIds.size) {
                val msgId = snapshot.sourceMsgIds[index]
                val success = forwardSingleMessage(msgId, snapshot.sourceChatType, recipientTargets)
                if (!success) {
                    val completedCount = index
                    _uiState.update {
                        it.copy(
                            nextSourceIndex = index,
                            isSending = false,
                            isLocked = true,
                            error = if (completedCount == 0) "转发失败" else "已转发 $completedCount/${snapshot.sourceMsgIds.size} 条，转发失败"
                        )
                    }
                    return@launch
                }

                _events.emit(ForwardEvent.SourceForwarded(msgId))
                index++
                _uiState.update { it.copy(nextSourceIndex = index) }
            }

            _uiState.update { it.copy(isSending = false, isLocked = true, isCompleted = true, error = null) }
            _events.emit(ForwardEvent.Completed(recipientTargets))
        }
    }

    private suspend fun forwardSingleMessage(
        msgId: String,
        @Suppress("UNUSED_PARAMETER") sourceChatType: Int,
        recipients: List<ForwardTarget>
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val json = JSONObject().apply {
                put("message_id", msgId.toIntOrNull() ?: return@withContext false)
                val arr = JSONArray()
                recipients.forEach { r ->
                    val obj = JSONObject()
                    obj.put("chat_type", r.chatType)
                    obj.put("chat_id", r.chatId)
                    arr.put(obj)
                }
                put("recipients", arr)
            }
            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("${ApiAddress}chat/forward")
                .post(body)
                .header("x-access-token", token)
                .build()
            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (_: Exception) {
            false
        }
    }

    private fun loadTargets() {
        viewModelScope.launch {
            try {
                val client = OkHttpClient()
                val form = okhttp3.FormBody.Builder()
                    .add("page", "1")
                    .add("per_page", "50")
                    .build()
                val request = Request.Builder()
                    .url("${ApiAddress}chat/list")
                    .post(form)
                    .header("x-access-token", token)
                    .build()
                val bodyStr = withContext(Dispatchers.IO) {
                    val resp = client.newCall(request).execute()
                    resp.body?.string() ?: ""
                }
                val json = JSONObject(bodyStr)
                val arr = json.optJSONArray("friends") ?: JSONArray()
                val list = mutableListOf<ForwardTarget>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val type = obj.optString("type", "private")
                    list.add(
                        ForwardTarget(
                            chatId = obj.optInt("id"),
                            chatType = if (type == "group") 2 else 1,
                            displayName = obj.optString("name", obj.optString("username", "")),
                            avatarUrl = obj.optString("avatar", "")
                        )
                    )
                }
                _uiState.update { it.copy(targets = list, isLoading = false, error = if (list.isEmpty()) "暂无可转发的会话" else null) }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "加载失败") }
            }
        }
    }
}