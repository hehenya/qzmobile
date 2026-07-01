package com.example.toolbox

import android.content.Context
import android.content.SharedPreferences

object DraftManager {
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.getSharedPreferences("chat_drafts", Context.MODE_PRIVATE)
    }

    fun saveDraft(chatType: Int, chatId: Int, text: String) {
        prefs?.edit()?.putString("draft_${chatType}_$chatId", text)?.apply()
    }

    fun getDraft(chatType: Int, chatId: Int): String? {
        val draft = prefs?.getString("draft_${chatType}_$chatId", "")
        return if (draft.isNullOrBlank()) null else draft
    }

    fun removeDraft(chatType: Int, chatId: Int) {
        prefs?.edit()?.remove("draft_${chatType}_$chatId")?.apply()
    }
    fun getDraftTime(chatType: Int, chatId: Int): String {
        val key = "draft_${chatType}_${chatId}_time"
        return prefs?.getString(key, "") ?: ""
    }
}