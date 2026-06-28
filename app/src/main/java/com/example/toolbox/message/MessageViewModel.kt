package com.example.toolbox.message

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