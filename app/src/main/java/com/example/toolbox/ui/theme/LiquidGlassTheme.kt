package com.example.toolbox.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import com.kyant.backdrop.Backdrop
import kotlin.math.pow

val LocalLiquidGlassEnabled = compositionLocalOf { true }
val LocalLiquidGlassBlur = compositionLocalOf { 1f }
val LocalLiquidGlassBackdrop = compositionLocalOf<Backdrop?> { null }

fun liquidGlassContentColor(
    preferredColor: Color,
    glassColor: Color,
    backgroundColor: Color,
): Color {
    val glassAlpha = glassColor.alpha
    return Color(
        red = preferredColor.red + (backgroundColor.red - preferredColor.red) * (1 - glassAlpha).pow(2),
        green = preferredColor.green + (backgroundColor.green - preferredColor.green) * (1 - glassAlpha).pow(2),
        blue = preferredColor.blue + (backgroundColor.blue - preferredColor.blue) * (1 - glassAlpha).pow(2),
        alpha = preferredColor.alpha,
    )
}