package com.example.toolbox.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsStorage(private val context: Context) {

    companion object {
        private val KEY_BUBBLE_CORNER_RADIUS = floatPreferencesKey("bubble_corner_radius")
        private val KEY_BUBBLE_OPACITY = floatPreferencesKey("bubble_opacity")
        private val KEY_SHOW_MY_BUBBLE_AVATAR = booleanPreferencesKey("show_my_bubble_avatar")
    }

    // 气泡圆角
    val bubbleCornerRadiusFlow: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_BUBBLE_CORNER_RADIUS] ?: 16f
    }

    suspend fun getBubbleCornerRadius(): Float {
        return context.dataStore.data.first()[KEY_BUBBLE_CORNER_RADIUS] ?: 16f
    }

    suspend fun setBubbleCornerRadius(value: Float) {
        context.dataStore.edit { it[KEY_BUBBLE_CORNER_RADIUS] = value }
    }

    // 气泡不透明度
    val bubbleOpacityFlow: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_BUBBLE_OPACITY] ?: 0.9f
    }

    suspend fun getBubbleOpacity(): Float {
        return context.dataStore.data.first()[KEY_BUBBLE_OPACITY] ?: 0.9f
    }

    suspend fun setBubbleOpacity(value: Float) {
        context.dataStore.edit { it[KEY_BUBBLE_OPACITY] = value }
    }

    // 显示我的头像
    val showMyBubbleAvatarFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SHOW_MY_BUBBLE_AVATAR] ?: true
    }

    suspend fun setShowMyBubbleAvatar(value: Boolean) {
        context.dataStore.edit { it[KEY_SHOW_MY_BUBBLE_AVATAR] = value }
    }
}