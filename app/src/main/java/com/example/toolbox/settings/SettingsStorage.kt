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
        private val KEY_SCREENSHOT_HIDE_SENDER_INFO = booleanPreferencesKey("screenshot_hide_sender_info")
        private val KEY_SCREENSHOT_HIDE_MY_INFO = booleanPreferencesKey("screenshot_hide_my_info")
        private val KEY_SCREENSHOT_HIDE_SESSION_INFO = booleanPreferencesKey("screenshot_hide_session_info")
        private val KEY_SCREENSHOT_HIDE_IMAGES = booleanPreferencesKey("screenshot_hide_images")
    }

    val bubbleCornerRadiusFlow: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_BUBBLE_CORNER_RADIUS] ?: 16f
    }

    suspend fun getBubbleCornerRadius(): Float {
        return context.dataStore.data.first()[KEY_BUBBLE_CORNER_RADIUS] ?: 16f
    }

    suspend fun setBubbleCornerRadius(value: Float) {
        context.dataStore.edit { it[KEY_BUBBLE_CORNER_RADIUS] = value }
    }

    val bubbleOpacityFlow: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_BUBBLE_OPACITY] ?: 0.9f
    }

    suspend fun getBubbleOpacity(): Float {
        return context.dataStore.data.first()[KEY_BUBBLE_OPACITY] ?: 0.9f
    }

    suspend fun setBubbleOpacity(value: Float) {
        context.dataStore.edit { it[KEY_BUBBLE_OPACITY] = value }
    }

    val showMyBubbleAvatarFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SHOW_MY_BUBBLE_AVATAR] ?: true
    }

    suspend fun setShowMyBubbleAvatar(value: Boolean) {
        context.dataStore.edit { it[KEY_SHOW_MY_BUBBLE_AVATAR] = value }
    }

    val screenshotHideSenderInfoFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SCREENSHOT_HIDE_SENDER_INFO] ?: false
    }

    suspend fun getScreenshotHideSenderInfo(): Boolean = context.dataStore.data.first()[KEY_SCREENSHOT_HIDE_SENDER_INFO] ?: false

    suspend fun setScreenshotHideSenderInfo(value: Boolean) {
        context.dataStore.edit { it[KEY_SCREENSHOT_HIDE_SENDER_INFO] = value }
    }

    val screenshotHideMyInfoFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SCREENSHOT_HIDE_MY_INFO] ?: false
    }

    suspend fun getScreenshotHideMyInfo(): Boolean = context.dataStore.data.first()[KEY_SCREENSHOT_HIDE_MY_INFO] ?: false

    suspend fun setScreenshotHideMyInfo(value: Boolean) {
        context.dataStore.edit { it[KEY_SCREENSHOT_HIDE_MY_INFO] = value }
    }

    val screenshotHideSessionInfoFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SCREENSHOT_HIDE_SESSION_INFO] ?: false
    }

    suspend fun getScreenshotHideSessionInfo(): Boolean = context.dataStore.data.first()[KEY_SCREENSHOT_HIDE_SESSION_INFO] ?: false

    suspend fun setScreenshotHideSessionInfo(value: Boolean) {
        context.dataStore.edit { it[KEY_SCREENSHOT_HIDE_SESSION_INFO] = value }
    }

    val screenshotHideImagesFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SCREENSHOT_HIDE_IMAGES] ?: false
    }

    suspend fun getScreenshotHideImages(): Boolean = context.dataStore.data.first()[KEY_SCREENSHOT_HIDE_IMAGES] ?: false

    suspend fun setScreenshotHideImages(value: Boolean) {
        context.dataStore.edit { it[KEY_SCREENSHOT_HIDE_IMAGES] = value }
    }
}