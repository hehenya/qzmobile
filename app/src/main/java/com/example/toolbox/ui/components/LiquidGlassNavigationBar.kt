package com.example.toolbox.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

@Composable
fun LiquidGlassNavigationBar(
    modifier: Modifier = Modifier,
    hazeState: HazeState,
    enabled: Boolean,
    blurRadius: Dp = 25.dp,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.85f),
    content: @Composable RowScope.() -> Unit
) {
    val shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

    Box(modifier = modifier) {
        if (enabled) {
            NavigationBar(
                modifier = Modifier
                    .clip(shape)
                    .hazeEffect(
                        state = hazeState,
                        style = HazeStyle(
                            blurRadius = blurRadius,
                            tint = HazeTint(containerColor),  // 明确使用 tint 参数
                            noiseFactor = 0f
                        )
                    ),
                containerColor = Color.Transparent,
                tonalElevation = 0.dp,
                windowInsets = NavigationBarDefaults.windowInsets,
                content = content
            )
        } else {
            NavigationBar(
                modifier = Modifier.clip(shape),
                containerColor = containerColor,
                tonalElevation = 3.dp,
                windowInsets = NavigationBarDefaults.windowInsets,
                content = content
            )
        }
    }
}