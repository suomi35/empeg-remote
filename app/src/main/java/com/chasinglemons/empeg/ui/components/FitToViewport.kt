package com.chasinglemons.empeg.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

/** Corrections smaller than this are ignored, so a fit can never oscillate. */
private const val ScaleTolerance = 0.001f

/**
 * Shows [content] complete instead of scrolling it or re-flowing it: the content is laid out at
 * the size it was designed at, and then drawn smaller - never larger than [maxScale] - until it
 * just fits the space this composable is given.
 *
 * The faces of the player (the remote, with its artwork and spacing) are fixed-proportion
 * designs, so re-flowing them into a shorter viewport changes the design while scrolling hides
 * part of it. Scaling the whole face keeps every proportion exactly as drawn.
 *
 * Only the drawn scale changes, never the layout the content is measured with. That keeps the
 * natural size the content reports independent of the scale in use, which makes [fitScale] exact
 * and lets it settle in a single correction: rounding a scaled-down density instead would let
 * dp-to-pixel rounding creep into the result and shrink the face further than it needs to be.
 * Touch input is mapped straight back through the scale onto the buttons.
 */
@Composable
fun FitToViewport(
    modifier: Modifier = Modifier,
    maxScale: Float = 1f,
    content: @Composable () -> Unit,
) {
    var scale by remember { mutableFloatStateOf(maxScale) }

    BoxWithConstraints(modifier = modifier) {
        val viewportWidth = constraints.maxWidth
        val viewportHeight = constraints.maxHeight
        val fitsSomething = constraints.hasBoundedWidth && constraints.hasBoundedHeight

        Layout(
            content = {
                // Scaled as it is drawn, around its top left corner, so nothing below has to
                // know about the fit: the buttons keep the sizes, and the touch areas, that
                // they were designed with.
                Box(
                    Modifier.graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = TransformOrigin(0f, 0f)
                    }
                ) {
                    content()
                }
            },
            modifier = Modifier.clipToBounds()
        ) { measurables, _ ->
            val child = measurables.first()
            // Measured unconstrained in height, at the size it was designed at, so the content
            // reveals how tall it really wants to be - independent of how it will be drawn.
            val natural = child.measure(
                Constraints(maxWidth = viewportWidth, maxHeight = Constraints.Infinity)
            )

            if (fitsSomething) {
                val fitted = fitScale(
                    availableWidth = viewportWidth,
                    availableHeight = viewportHeight,
                    contentWidth = natural.width,
                    contentHeight = natural.height,
                    maxScale = maxScale,
                )
                if (abs(fitted - scale) > ScaleTolerance) {
                    // Redrawn at the fitted scale from the next pass; until then the content is
                    // clipped to the viewport, exactly as the unscaled design was.
                    scale = fitted
                }
            }

            layout(viewportWidth, viewportHeight) {
                natural.placeRelative(
                    x = ((viewportWidth - natural.width * scale) / 2f).roundToInt(),
                    y = 0
                )
            }
        }
    }
}

/**
 * The largest uniform scale at which content measuring [contentWidth] x [contentHeight] fits
 * inside [availableWidth] x [availableHeight].
 *
 * Content that already fits is left at its designed size, because [maxScale] caps the result.
 */
fun fitScale(
    availableWidth: Int,
    availableHeight: Int,
    contentWidth: Int,
    contentHeight: Int,
    maxScale: Float = 1f,
): Float {
    if (availableWidth <= 0 || availableHeight <= 0) return maxScale
    if (contentWidth <= 0 || contentHeight <= 0) return maxScale

    val widthFit = availableWidth.toFloat() / contentWidth
    val heightFit = availableHeight.toFloat() / contentHeight
    return min(maxScale, min(widthFit, heightFit))
}
