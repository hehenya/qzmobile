package com.example.toolbox.ui.theme

import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class LiquidGlassConfig(
    val enabled: Boolean = true,
    val blurRadius: Float = 25f   // 模糊强度，0~50f
)

val LocalLiquidGlassConfig = compositionLocalOf { LiquidGlassConfig() }

class LiquidGlassSettings(context: Context) {
    private val prefs = context.getSharedPreferences("glass_prefs", Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = prefs.getBoolean("liquid_glass_enabled", true)
        set(value) = prefs.edit().putBoolean("liquid_glass_enabled", value).apply()

    var blurRadius: Float
        get() = prefs.getFloat("liquid_glass_blur", 25f)
        set(value) = prefs.edit().putFloat("liquid_glass_blur", value).apply()
}