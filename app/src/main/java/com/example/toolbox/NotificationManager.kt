package com.example.toolbox

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class InAppNotification(
    val title: String,
    val message: String,
    val type: Type = Type.INFO,
    val chatId: Int? = null,      // 可选，用于点击跳转
    val chatType: Int? = null,    // 1=私聊，2=群聊
    val avatarUrl: String = ""    // 发送者头像（群聊可用群头像）
) {
    enum class Type { INFO, SUCCESS, WARNING, ERROR }
}

object NotificationManager {
    private val _notifications = MutableSharedFlow<InAppNotification>()
    val notifications = _notifications.asSharedFlow()

    fun show(notification: InAppNotification) {
        _notifications.tryEmit(notification)
    }

    fun show(title: String, message: String, type: InAppNotification.Type = InAppNotification.Type.INFO) {
        show(InAppNotification(title, message, type))
    }
}