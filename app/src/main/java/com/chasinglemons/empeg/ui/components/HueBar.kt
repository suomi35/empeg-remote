package com.chasinglemons.empeg.ui.components

import android.graphics.Bitmap
import android.graphics.Color.HSVToColor
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toRect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun HueBar(
    setColor: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val interactionSource = remember {
        MutableInteractionSource()
    }
    val pressOffset = remember {
        mutableStateOf(Offset.Zero)
    }
    Canvas(
        modifier = modifier
            .height(50.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .emitDragGesture(interactionSource)
    ) {
        val drawScopeSize = size
        val bitmap = createBitmap(size.width.toInt(), size.height.toInt())
        val hueCanvas = android.graphics.Canvas(bitmap)
        val huePanel = RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
        val hueColors = IntArray((huePanel.width()).toInt())
        var hue = 0f
        for (i in hueColors.indices) {
            hueColors[i] = HSVToColor(floatArrayOf(hue, 1f, 1f))
            hue += 360f / hueColors.size
        }
        val linePaint = Paint()
        linePaint.strokeWidth = 0F
        for (i in hueColors.indices) {
            linePaint.color = hueColors[i]
            hueCanvas.drawLine(i.toFloat(), 0F, i.toFloat(), huePanel.bottom, linePaint)
        }
        drawBitmap(
            bitmap = bitmap,
            panel = huePanel
        )
        fun pointToHue(pointX: Float): Float {
            val width = huePanel.width()
            val x = when {
                pointX < huePanel.left -> 0F
                pointX > huePanel.right -> width
                else -> pointX - huePanel.left
            }
            return x * 360f / width
        }

        scope.collectForPress(interactionSource) { pressPosition ->
            val pressPos = pressPosition.x.coerceIn(0f..drawScopeSize.width)
            pressOffset.value = Offset(pressPos, 0f)
            val selectedHue = pointToHue(pressPos)
            setColor(selectedHue)
        }

//        drawCircle(
//            Color.White,
//            radius = size.height / 2,
//            center = Offset(pressOffset.value.x, size.height / 2),
//            style = Stroke(
//                width = 2.dp.toPx()
//            )
//        )
    }
}

fun CoroutineScope.collectForPress(
    interactionSource: InteractionSource,
    setOffset: (Offset) -> Unit
) {
    launch {
        interactionSource.interactions.collect { interaction ->
            (interaction as? PressInteraction.Press)
                ?.pressPosition
                ?.let(setOffset)
        }
    }
}

private fun Modifier.emitDragGesture(
    interactionSource: MutableInteractionSource
): Modifier = composed {
    val scope = rememberCoroutineScope()
    pointerInput(Unit) {
        detectDragGestures { input, _ ->
            scope.launch {
                interactionSource.emit(PressInteraction.Press(input.position))
            }
        }
    }.clickable(interactionSource, null) {
    }
}

private fun DrawScope.drawBitmap(
    bitmap: Bitmap,
    panel: RectF
) {
    drawIntoCanvas {
        it.nativeCanvas.drawBitmap(
            bitmap,
            null,
            panel.toRect(),
            null
        )
    }
}