package com.example.toolbox.message

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.toolbox.ApiAddress
import com.example.toolbox.AppJson
import com.example.toolbox.data.Friend
import com.example.toolbox.data.FriendsResponse
import com.example.toolbox.data.Pagination
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class MessageUiState(
    val friends: List<Friend> = emptyList(),
    val pagination: Pagination? = null,
    val hasMore: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

class MessageViewModel(
    private val token: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(MessageUiState())
    val uiState: StateFlow<MessageUiState> = _uiState.asStateFlow()

    private val client = OkHttpClient()

    init {
        loadFriends(page = 1, isRefresh = true)
        connectWebSocket()
    }

    fun refresh() {
        if (_uiState.value.isRefreshing) return
        loadFriends(page = 1, isRefresh = true)
    }

    fun loadMore() {
        val currentState = _uiState.value
        if (currentState.isLoadingMore || !currentState.hasMore) return
        val nextPage = (currentState.pagination?.currentPage ?: 0) + 1
        loadFriends(page = nextPage, isRefresh = false)
    }

    private fun loadFriends(page: Int, isRefresh: Boolean) {
        viewModelScope.launch {
            _uiState.update {
                if (isRefresh) it.copy(isRefreshing = true, isLoading = true, error = null)
                else it.copy(isLoadingMore = true, error = null)
            }

            try {
                val result = withContext(Dispatchers.IO) {
                    val requestBody = FormBody.Builder()
                        .add("page", page.toString())
                        .add("per_page", "20")
                        .build()

                    val request = Request.Builder()
                        .url("${ApiAddress}chat/list")
                        .post(requestBody)
                        .header("x-access-token", token)
                        .build()

                    val response = client.newCall(request).execute()
                    if (!response.isSuccessful) {
                        return@withContext null
                    }

                    val responseBody = response.body.string()
                    AppJson.json.decodeFromString<FriendsResponse>(responseBody)
                }

                if (result != null && result.success) {
                    _uiState.update { current ->
                        val newFriends = if (isRefresh) result.friends
                        else current.friends + result.friends
                        current.copy(
                            friends = newFriends,
                            pagination = result.pagination,
                            hasMore = result.pagination.currentPage < result.pagination.pages,
                            isRefreshing = false,
                            isLoadingMore = false,
                            isLoading = false,
                            error = null
                        )
                    }
                } else {
                    _uiState.update { it.copy(
                        isRefreshing = false,
                        isLoadingMore = false,
                        isLoading = false,
                        error = "请求失败或数据为空"
                    ) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isRefreshing = false,
                    isLoadingMore = false,
                    isLoading = false,
                    error = e.message
                ) }
            }
        }
    }

    fun connectWebSocket() {
        if (token.isNotBlank()) {
            val manager = ChatSocketManager.getInstance()
            manager.connect(token)

            // 好友列表更新
            manager.setOnFriendListUpdateListener { data ->
                try {
                    val friendsArray = data.optJSONArray("friends") ?: return@setOnFriendListUpdateListener
                    val newPrivateChats = mutableListOf<Friend>()
                    for (i in 0 until friendsArray.length()) {
                        val friendJson = friendsArray.getJSONObject(i)
                        newPrivateChats.add(
                            Friend(
                                id = friendJson.getInt("id").toString(),
                                username = friendJson.getString("username"),
                                avatar = friendJson.optString("avatar", ""),
                                lastMessage = friendJson.optString("last_message", null),
                                lastMessageTime = friendJson.optString("last_message_time", null),
                                unreadCount = friendJson.optInt("unread_count", 0),
                                type = "friend",
                                name = friendJson.getString("username"),
                                title = friendJson.optString("title", ""),
                                titleStatus = friendJson.optInt("title_status", 0)
                            )
                        )
                    }
                    // 保留现有群聊，更新私聊
                    _uiState.update { current ->
                        val groupChats = current.friends.filter { it.type == "group" }
                        current.copy(friends = groupChats + newPrivateChats)
                    }
                } catch (e: Exception) {
                    Log.e("MessageViewModel", "解析 friend_list_update 失败", e)
                }
            }

            // 群聊列表更新
            manager.setOnGroupListUpdateListener { data ->
                try {
                    val groupsArray = data.optJSONArray("groups") ?: return@setOnGroupListUpdateListener
                    val newGroupChats = mutableListOf<Friend>()
                    for (i in 0 until groupsArray.length()) {
                        val groupJson = groupsArray.getJSONObject(i)
                        newGroupChats.add(
                            Friend(
                                id = groupJson.getInt("group_id").toString(),
                                username = groupJson.optString("name", "群聊"),
                                avatar = groupJson.optString("avatar", ""),
                                lastMessage = groupJson.optString("last_message", null),
                                lastMessageTime = groupJson.optString("last_message_time", null),
                                unreadCount = groupJson.optInt("unread_count", 0),
                                type = "group",
                                name = groupJson.optString("name", "群聊"),
                                title = "",
                                titleStatus = 0
                            )
                        )
                    }
                    // 保留现有私聊，更新群聊
                    _uiState.update { current ->
                        val privateChats = current.friends.filter { it.type == "friend" }
                        current.copy(friends = privateChats + newGroupChats)
                    }
                } catch (e: Exception) {
                    Log.e("MessageViewModel", "解析 group_list_update 失败", e)
                }
            }
        }
    }
}

class MessageViewModelFactory(private val token: String) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MessageViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MessageViewModel(token) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}