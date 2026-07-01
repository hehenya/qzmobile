package com.example.toolbox.message

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import com.example.toolbox.ApiAddress
import com.example.toolbox.AppJson
import com.example.toolbox.data.Message
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.URISyntaxException

object ChatSocketManager {
    private var instance: WebSocketManager? = null

    fun getInstance(): WebSocketManager {
        if (instance == null) {
            instance = WebSocketManager()
        }
        return instance!!
    }

    fun disconnect() {
        instance?.disconnect()
        instance = null
    }
}

class WebSocketManager internal constructor() {
    private var socket: Socket? = null
    private var currentToken: String? = null

    private var heartbeatThread: HandlerThread? = null
    private var heartbeatHandler: Handler? = null
    private val heartbeatRunnable = object : Runnable {
        override fun run() {
            if (socket?.connected() == true) {
                socket?.emit("heartbeat", JSONObject())
            }
            heartbeatHandler?.postDelayed(this, 30000)
        }
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var scope: CoroutineScope? = null

    private val _authState = MutableStateFlow(false)
    val authState: StateFlow<Boolean> = _authState.asStateFlow()

    // 原有的消息回调（用于聊天页面）
    private val observers = mutableListOf<(type: String, chatId: String, chatType: Int, message: Message) -> Unit>()

    fun addObserver(observer: (type: String, chatId: String, chatType: Int, message: Message) -> Unit) {
        observers.add(observer)
    }

    fun removeObserver(observer: (type: String, chatId: String, chatType: Int, message: Message) -> Unit) {
        observers.remove(observer)
    }

    // 好友列表更新回调
    private var friendListUpdateListener: ((JSONObject) -> Unit)? = null

    fun setOnFriendListUpdateListener(listener: (JSONObject) -> Unit) {
        this.friendListUpdateListener = listener
    }

    // 群聊消息回调（用于会话列表更新）
    private var groupMessageListener: ((JSONObject) -> Unit)? = null

    fun setOnGroupMessageListener(listener: (JSONObject) -> Unit) {
        this.groupMessageListener = listener
    }

    // 群聊列表更新回调
    private var groupListUpdateListener: ((JSONObject) -> Unit)? = null

    fun setOnGroupListUpdateListener(listener: (JSONObject) -> Unit) {
        this.groupListUpdateListener = listener
    }

    fun connect(token: String) {
        if (socket?.connected() == true && currentToken == token) {
            Log.d("WS", "已经连接，复用现有连接")
            return
        }

        if (currentToken != token || socket == null) {
            disconnect()
        }

        this.currentToken = token

        if (heartbeatThread == null) {
            heartbeatThread = HandlerThread("WS-Heartbeat").apply { start() }
            heartbeatHandler = Handler(heartbeatThread!!.looper)
        }

        if (scope == null) {
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        }

        try {
            val opts = IO.Options().apply {
                transports = arrayOf("websocket")
                reconnection = true
            }

            val wsUrl = ApiAddress.replace("http://", "ws://").replace("https://", "wss://")
            socket = IO.socket("${wsUrl}?type=2", opts)

            socket?.on(Socket.EVENT_CONNECT) {
                Log.d("WS", "连接成功")
                authenticate()
                heartbeatHandler?.post(heartbeatRunnable)
                _authState.value = false
            }

            socket?.on(Socket.EVENT_DISCONNECT) {
                _authState.value = false
            }

            socket?.on(Socket.EVENT_CONNECT_ERROR) {
                Log.e("WS", "连接失败: ${it.firstOrNull()}")
            }

            socket?.on("auth_success") {
                _authState.value = true
            }

            socket?.on("auth_error") { args ->
                val message = (args[0] as JSONObject).optString("message")
                Log.e("WS", "认证失败: $message")
            }

            socket?.on("private_message") { args ->
                scope?.launch {
                    try {
                        val json = args[0] as JSONObject
                        val type = json.optString("type")
                        val dataObj = json.optJSONObject("data")
                        if (dataObj != null) {
                            val chatId = dataObj.optString("chat_id", "")
                            val chatType = dataObj.optInt("chat_type", 0)
                            val dataStr = dataObj.toString()
                            val message = AppJson.json.decodeFromString<Message>(dataStr)
                            mainHandler.post {
                                observers.forEach { observer ->
                                    observer(type, chatId, chatType, message)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("WS", "解析私信消息失败: ${e.message}")
                    }
                }
            }

            socket?.on("group_message") { args ->
                scope?.launch {
                    try {
                        val json = args[0] as JSONObject
                        val type = json.optString("type")
                        val dataObj = json.optJSONObject("data")
                        if (dataObj != null) {
                            // 传递给群聊消息监听器（用于更新会话列表）
                            mainHandler.post {
                                groupMessageListener?.invoke(dataObj)
                            }
                            // 同时也传递给原有的 observers（用于聊天页面）
                            val chatId = dataObj.optString("chat_id", "")
                            val chatType = dataObj.optInt("chat_type", 0)
                            val dataStr = dataObj.toString()
                            val message = AppJson.json.decodeFromString<Message>(dataStr)
                            mainHandler.post {
                                observers.forEach { observer ->
                                    observer(type, chatId, chatType, message)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("WS", "解析群聊消息失败: ${e.message}")
                    }
                }
            }

            socket?.on("friend_list_update") { args ->
                try {
                    val data = args[0] as? JSONObject
                    if (data != null) {
                        mainHandler.post {
                            friendListUpdateListener?.invoke(data)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("WS", "解析 friend_list_update 失败: ${e.message}")
                }
            }

            socket?.on("group_list_update") { args ->
                try {
                    val data = args[0] as? JSONObject
                    if (data != null) {
                        mainHandler.post {
                            groupListUpdateListener?.invoke(data)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("WS", "解析 group_list_update 失败: ${e.message}")
                }
            }

            socket?.connect()
        } catch (e: URISyntaxException) {
            Log.e("WS", "连接地址错误", e)
        }
    }

    private fun authenticate() {
        val authData = JSONObject().apply {
            put("token", currentToken)
        }
        socket?.emit("authenticate", authData)
    }

    fun disconnect() {
        heartbeatHandler?.removeCallbacks(heartbeatRunnable)
        heartbeatThread?.quitSafely()
        heartbeatThread = null
        heartbeatHandler = null

        scope?.cancel()
        scope = null

        socket?.disconnect()
        socket?.off()
        socket = null
    }
}