package com.example.toolbox.message

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import com.example.toolbox.ApiAddress
import com.example.toolbox.AppJson
import com.example.toolbox.InAppNotification
import com.example.toolbox.NotificationManager
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

    private val observers = mutableListOf<(type: String, chatId: String, chatType: Int, message: Message) -> Unit>()

    fun addObserver(observer: (type: String, chatId: String, chatType: Int, message: Message) -> Unit) {
        observers.add(observer)
    }

    fun removeObserver(observer: (type: String, chatId: String, chatType: Int, message: Message) -> Unit) {
        observers.remove(observer)
    }

    private var friendListUpdateListener: ((JSONObject) -> Unit)? = null

    fun setOnFriendListUpdateListener(listener: (JSONObject) -> Unit) {
        this.friendListUpdateListener = listener
    }

    private var groupMessageListener: ((JSONObject) -> Unit)? = null

    fun setOnGroupMessageListener(listener: (JSONObject) -> Unit) {
        this.groupMessageListener = listener
    }

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

            // ========== 私聊消息 ==========
            socket?.on("private_message") { args ->
                scope?.launch {
                    try {
                        val json = args[0] as JSONObject
                        val type = json.optString("type")
                        val dataObj = json.optJSONObject("data")
                        if (dataObj != null) {
                            val chatId = dataObj.optString("chat_id", "").ifEmpty {
                                if (type == "recall") {
                                    dataObj.optString("sender_id", "")
                                } else {
                                    dataObj.optString("sender_id", "").ifEmpty {
                                        dataObj.optString("receiver_id", "")
                                    }
                                }
                            }
                            val chatType = if (dataObj.has("chat_type")) {
                                dataObj.optInt("chat_type", 1)
                            } else {
                                1
                            }
                            val dataStr = dataObj.toString()
                            val message = AppJson.json.decodeFromString<Message>(dataStr)
                            mainHandler.post {
                                observers.forEach { observer ->
                                    observer(type, chatId, chatType, message)
                                }

                                if (type == "new" && !dataObj.optBoolean("is_mine", false)) {
                                    val senderName = dataObj.optString("sender_username", "未知用户")
                                    val content = dataObj.optString("content", "")
                                    val senderAvatar = dataObj.optString("sender_avatar", "")
                                    NotificationManager.show(
                                        InAppNotification(
                                            title = senderName,
                                            message = content.take(50),
                                            chatId = chatId.toIntOrNull(),
                                            chatType = chatType,
                                            avatarUrl = senderAvatar
                                        )
                                    )
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("WS", "解析私信消息失败: ${e.message}")
                    }
                }
            }

            // ========== 群聊消息 ==========
            socket?.on("group_message") { args ->
                scope?.launch {
                    try {
            
                        val json = args[0] as JSONObject
                        val type = json.optString("type")
                        val dataObj = json.optJSONObject("data") ?: return@launch
            
                        Log.d(
                            "WS_GROUP",
                            "收到事件 type=$type chatId=${dataObj.optString("chat_id")} msgId=${dataObj.optString("msg_id")}"
                        )
            
                        val chatId = dataObj.optString("chat_id").ifEmpty {
                            dataObj.optString("group_id")
                        }
            
                        val chatType = dataObj.optInt("chat_type", 2)
            
                        val message = AppJson.json.decodeFromString<Message>(
                            dataObj.toString()
                        )
            
                        mainHandler.post {
            
                            // 保留你原来的URL卡片刷新功能
                            groupMessageListener?.invoke(dataObj)
            
                            // 通知所有观察者
                            observers.toList().forEach { observer ->
                                observer(type, chatId, chatType, message)
                            }
            
                            // 新消息通知（撤回、编辑不会走这里）
                            if (type == "new" && !message.isMine) {
            
                                val senderName =
                                    dataObj.optString("sender_username", "未知用户")
            
                                val content =
                                    dataObj.optString("content", "")
            
                                val senderAvatar =
                                    dataObj.optString("sender_avatar", "")
            
                                val groupName =
                                    dataObj.optString("group_name", "")
            
                                val title =
                                    if (groupName.isNotEmpty())
                                        "$groupName - $senderName"
                                    else
                                        "群聊($chatId) $senderName"
            
                                NotificationManager.show(
                                    InAppNotification(
                                        title = title,
                                        message = content.take(50),
                                        chatId = chatId.toIntOrNull(),
                                        chatType = chatType,
                                        avatarUrl = senderAvatar
                                    )
                                )
                            }
                        }
            
                    } catch (e: Exception) {
            
                        Log.e(
                            "WS_GROUP",
                            "解析失败",
                            e
                        )
            
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