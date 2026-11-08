package com.example.toolbox.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import androidx.compose.foundation.background

@Composable
fun GlassBottomNavigationBar(
    modifier: Modifier = Modifier,
    hazeState: HazeState,
    enabled: Boolean,
    blurRadius: Dp = 24.dp,
    selectedIndex: Int,
    itemCount: Int,
    surfaceColor: Color = Color.White.copy(alpha = 0.15f),
    indicatorColor: Color = Color.White.copy(alpha = 0.3f),
    content: @Composable RowScope.() -> Unit
) {
    val shape = RoundedCornerShape(32.dp)
    val indicatorInset = 5.dp
    val barHeight = 64.dp
    val indicatorHeight = barHeight - indicatorInset * 2
    val indicatorShape = RoundedCornerShape(26.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, bottom = 10.dp, top = 8.dp)
            .shadow(elevation = 2.dp, shape = shape, clip = false)
            .then(
                if (enabled) {
                    Modifier
                        .clip(shape)
                        .hazeEffect(
                            state = hazeState,
                            style = HazeStyle(
                                blurRadius = blurRadius,
                                tint = HazeTint(surfaceColor),
                                noiseFactor = 0f
                            )
                        )
                } else {
                    Modifier
                        .clip(shape)
                        .background(surfaceColor)
                }
            )
            .height(barHeight)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val contentInset = 12.dp
            val itemWidth = (maxWidth - contentInset * 2) / itemCount
            val selectionWidth = itemWidth + (contentInset - indicatorInset) * 2

            val progress by animateFloatAsState(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 220, easing = LinearOutSlowInEasing),
                label = "indicator"
            )

            // 选中指示器
            if (enabled) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = itemWidth * selectedIndex + indicatorInset)
                        .width(selectionWidth)
                        .height(indicatorHeight)
                        .graphicsLayer {
                            scaleX = progress
                            scaleY = progress
                            transformOrigin = TransformOrigin.Center
                        }
                        .clip(indicatorShape)
                        .hazeEffect(
                            state = hazeState,
                            style = HazeStyle(
                                blurRadius = blurRadius / 2,
                                tint = HazeTint(indicatorColor),
                                noiseFactor = 0f
                            )
                        )
                )
            } else {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = itemWidth * selectedIndex + indicatorInset)
                        .width(selectionWidth)
                        .height(indicatorHeight)
                        .graphicsLayer {
                            scaleX = progress
                            scaleY = progress
                            transformOrigin = TransformOrigin.Center
                        }
                        .clip(indicatorShape)
                        .background(indicatorColor)
                )
            }

            // 导航项
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = contentInset),
                verticalAlignment = Alignment.CenterVertically
            ) {
                content()
            }
        }
    }
}