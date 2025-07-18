package com.chasinglemons.empeg.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import com.chasinglemons.empeg.R
import com.chasinglemons.empeg.util.Utils
import kotlinx.coroutines.delay

@Composable
fun LoadingAnimation(
    modifier: Modifier = Modifier,
    color: Color,
    frameDurationMillis: Long = 100L, // Default to 100ms per frame (10 FPS)
    contentScale: ContentScale = ContentScale.Fit,
    imageSize: Dp? = null // Optional fixed size for the image container // TODO: Can we fix the size so not needed?
) {
    val frames = listOf(
        R.drawable.white_sync1,
        R.drawable.white_sync2,
        R.drawable.white_sync3,
        R.drawable.white_sync4,
        R.drawable.white_sync5,
        R.drawable.white_sync6
    )

    var currentFrameIndex by remember { mutableIntStateOf(0) }

    // This LaunchedEffect will run as long as PngSequenceAnimation is in the composition
    // and will re-launch if imageResourceIds or frameDurationMillis changes.
    LaunchedEffect(key1 = frames, key2 = frameDurationMillis) {
        while (true) { // Infinite loop
            delay(frameDurationMillis)
            currentFrameIndex = (currentFrameIndex + 1) % frames.size
        }
    }

    val imageModifier = if (imageSize != null) {
        modifier.size(imageSize)
    } else {
        modifier
    }

    AsyncImage(
        model = frames[currentFrameIndex],
        contentDescription = "Animated loading image ${currentFrameIndex + 1} of ${frames.size}",
        colorFilter = ColorFilter.colorMatrix(Utils.getColorMatrix(color)),
        filterQuality = FilterQuality.None,
        modifier = imageModifier,
        contentScale = contentScale
    )
}