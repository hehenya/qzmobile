/*
 * Adapted from the Murexide liquid glass theme (com.juhao.murexide.ui.theme.liquidglass).
 * Ported to ToolBox with the Haze 1.x fallback API (hazeEffect) instead of Haze 2 hazeBlur.
 */
package com.example.toolbox.ui.theme.liquidglass

import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import com.example.toolbox.ui.theme.LocalLiquidGlassBlur
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

/**
 * 参考 Murexide 的 `glassControl` 修饰符：启用液态玻璃时使用
 * backdrop 的 vibrancy + blur + lens 效果（并带按压/拖拽位移响应），
 * 未启用时退化为 1dp 边框 + haze 模糊。
 *
 * @param shape 圆角形状
 * @param liquidBackdrop 液态玻璃 backdrop，为 null 时走 haze 回退分支
 * @param hazeState haze 状态（回退分支使用）
 * @param useGlassBar 是否启用液态玻璃
 * @param isInteractive 是否需要按压/拖拽的交互位移效果（输入栏传 false）
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun Modifier.glassControl(
    shape: Shape,
    liquidBackdrop: Backdrop? = null,
    hazeState: HazeState,
    useGlassBar: Boolean,
    isInteractive: Boolean = true
): Modifier =
    if (useGlassBar && liquidBackdrop != null) {
        val liquidGlassBlur = LocalLiquidGlassBlur.current

        val color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.8f)

        val animationScope = rememberCoroutineScope()

        val interactiveHighlight = remember(animationScope) {
            InteractiveHighlight(
                animationScope = animationScope
            )
        }

        drawBackdrop(
            backdrop = liquidBackdrop,
            shape = { shape },
            effects = {
                vibrancy()
                blur(1.dp.toPx() * liquidGlassBlur)
                lens(16.dp.toPx(), 32.dp.toPx())
            },
            layerBlock = {
                val width = size.width
                val height = size.height

                val progress = interactiveHighlight.pressProgress
                val scale = lerp(1f, 1f + 4f.dp.toPx() / size.height, progress)

                val maxOffset = size.minDimension
                val initialDerivative = 0.05f
                val offset = interactiveHighlight.offset
                translationX = maxOffset * tanh(initialDerivative * offset.x / maxOffset)
                translationY = maxOffset * tanh(initialDerivative * offset.y / maxOffset)

                val maxDragScale = 4f.dp.toPx() / size.height
                val offsetAngle = atan2(offset.y, offset.x)
                scaleX =
                    scale +
                            maxDragScale * abs(cos(offsetAngle) * offset.x / size.maxDimension) *
                            (width / height).fastCoerceAtMost(1f)
                scaleY =
                    scale +
                            maxDragScale * abs(sin(offsetAngle) * offset.y / size.maxDimension) *
                            (height / width).fastCoerceAtMost(1f)
            },
            onDrawSurface = {
                drawRect(color)
            },
        )
            .then(
                if (isInteractive) {
                    Modifier
                        .then(interactiveHighlight.modifier)
                        .then(interactiveHighlight.gestureModifier)
                } else {
                    Modifier
                }
            )
    } else {
        val style = HazeMaterials.thin().copy(
            blurRadius = 32.dp,
            noiseFactor = 0f
        )

        border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.surfaceBright,
            shape = shape
        )
            .clip(shape)
            .hazeEffect(
                state = hazeState,
                style = style
            )
    }