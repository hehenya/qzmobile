package com.example.toolbox

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object CacheManager {
    private val json = Json { ignoreUnknownKeys = true }
    private const val PREF_NAME = "cache_prefs"

    private fun prefs(context: Context) = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun save(context: Context, key: String, data: String) {
        prefs(context).edit().putString(key, data).apply()
    }

    fun load(context: Context, key: String): String? {
        return prefs(context).getString(key, null)
    }

    fun saveJson(context: Context, key: String, jsonData: String) {
        save(context, key, jsonData)
    }

    fun loadJson(context: Context, key: String): String? {
        return load(context, key)
    }
}