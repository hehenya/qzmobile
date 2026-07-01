package com.example.toolbox

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class InAppNotification(
    val title: String,
    val message: String,
    val type: Type = Type.INFO,
    val chatId: Int? = null,
    val chatType: Int? = null,
    val avatarUrl: String = ""
) {
    enum class Type { INFO, SUCCESS, WARNING, ERROR }
}

object NotificationManager {
    private val _notifications = MutableSharedFlow<InAppNotification>(replay = 1)
    val notifications = _notifications.asSharedFlow()

    fun show(notification: InAppNotification) {
        _notifications.tryEmit(notification)
    }

    fun show(title: String, message: String, type: InAppNotification.Type = InAppNotification.Type.INFO) {
        show(InAppNotification(title, message, type))
    }
}