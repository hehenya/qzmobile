package com.example.toolbox.message

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.toolbox.ApiAddress
import kotlinx.serialization.Serializable
import com.example.toolbox.AppJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

data class SearchUiState(
    val query: String = "",
    val results: List<SearchChatItem> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val pagination: SearchPagination? = null,
    val hasMore: Boolean = false
)

@Serializable
data class SearchChatItem(
    val type: String,
    val id: Int,
    val name: String,
    val avatar: String,
    val title: String? = null,
    val lastMessage: String? = null,
    val lastMessageTime: String? = null,
    val unreadCount: Int = 0
)

@Serializable
data class SearchPagination(
    val currentPage: Int,
    val perPage: Int,
    val total: Int,
    val pages: Int,
    val hasNext: Boolean,
    val hasPrev: Boolean
)

@Serializable
data class SearchResponse(
    val success: Boolean,
    val chats: List<SearchChatItem> = emptyList(),
    val pagination: SearchPagination? = null,
    val message: String? = null
)

class SearchViewModel(private val token: String) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val client = OkHttpClient()

    fun search(keyword: String, chatType: Int? = null) {
        if (keyword.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(query = keyword, isLoading = true, error = null, results = emptyList()) }
            try {
                val result = withContext(Dispatchers.IO) {
                    val json = JSONObject().apply {
                        put("keyword", keyword)
                        chatType?.let { put("chat_type", it) }
                        put("page", 1)
                        put("per_page", 20)
                    }
                    val request = Request.Builder()
                        .url("${ApiAddress}chat/search")
                        .post(json.toString().toRequestBody("application/json".toMediaType()))
                        .header("x-access-token", token)
                        .build()
                    val response = client.newCall(request).execute()
                    val body = response.body?.string() ?: ""
                    AppJson.json.decodeFromString<SearchResponse>(body)
                }
                if (result.success) {
                    _uiState.update {
                        it.copy(
                            results = result.chats,
                            isLoading = false,
                            pagination = result.pagination,
                            hasMore = result.pagination?.hasNext ?: false
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = result.message ?: "搜索失败") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.query.isBlank() || state.isLoadingMore || !state.hasMore) return
        val nextPage = (state.pagination?.currentPage ?: 0) + 1
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            try {
                val result = withContext(Dispatchers.IO) {
                    val json = JSONObject().apply {
                        put("keyword", state.query)
                        put("page", nextPage)
                        put("per_page", 20)
                    }
                    val request = Request.Builder()
                        .url("${ApiAddress}chat/search")
                        .post(json.toString().toRequestBody("application/json".toMediaType()))
                        .header("x-access-token", token)
                        .build()
                    val response = client.newCall(request).execute()
                    val body = response.body?.string() ?: ""
                    AppJson.json.decodeFromString<SearchResponse>(body)
                }
                if (result.success) {
                    _uiState.update {
                        it.copy(
                            results = it.results + result.chats,
                            isLoadingMore = false,
                            pagination = result.pagination,
                            hasMore = result.pagination?.hasNext ?: false
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoadingMore = false, error = result.message) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingMore = false, error = e.message) }
            }
        }
    }
}

class SearchViewModelFactory(private val token: String) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SearchViewModel(token) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}