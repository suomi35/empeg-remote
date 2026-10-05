package com.chasinglemons.empeg.ui.components

import android.graphics.Point
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.chasinglemons.empeg.R
import com.chasinglemons.empeg.model.CircularButton
import com.chasinglemons.empeg.model.RectangularButton
import com.chasinglemons.empeg.ui.theme.EmpegRemoteTheme
import com.chasinglemons.empeg.util.Constants
import com.chasinglemons.empeg.util.Utils
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun FasciaScreen(
    empegIp: String,
    refreshDelay: Long,
    modifier: Modifier = Modifier,
    lensColor: Color,
    showDisplayBoard: Boolean,
    onClick: (String) -> Unit
) {
    val containerWidthDp = getContainerWidthInDpFromLocalWindowInfo()
    val vfdPaddingStart: Dp = (containerWidthDp / 4 + 18.dp)
    val vfdPaddingEnd: Dp = (containerWidthDp / 4)
    var currentImageUrl by remember(empegIp) { mutableStateOf("http://$empegIp/proc/empeg_screen.png") }
    var currentPainter by remember { mutableStateOf<Painter?>(null) }
    var showError by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var fasciaSize by remember { mutableStateOf(IntSize.Zero) }

    fun Color.withAlpha(alpha: Float): Color {
        // Clamp the alpha value to be within the valid range [0.0, 1.0]
        val clampedAlpha = alpha.coerceIn(0.0f, 1.0f)
        return this.copy(alpha = clampedAlpha)
    }

    fun calculateClickTarget(
        clickX: Float,
        clickY: Float,
        fasciaDimensions: IntSize,
        longPress: Boolean) {
        val translatedTouchPoint = Utils.translateTouchPoint(clickX, clickY, fasciaDimensions)

        val buttons = arrayOf(
            CircularButton(center = Point(475, 102), radius = 19f, Constants.KNOB, Constants.KNOB_LONG), // add this first to check before knob left/right since it's on top of them
            RectangularButton(x = 66f, y = 39f, width = 30f, height = 30f, Constants.TOP, Constants.TOP_LONG),
            RectangularButton(x = 24f, y = 70f, width = 57f, height = 33f, Constants.LEFT, Constants.LEFT_LONG),
            RectangularButton(x = 81f, y = 70f, width = 57f, height = 33f, Constants.RIGHT, Constants.RIGHT_LONG),
            RectangularButton(x = 63f, y = 103f, width = 37f, height = 46f, Constants.BOTTOM, Constants.BOTTOM_LONG),
            RectangularButton(x = 409f, y = 0f, width = 66f, height = 172f, Constants.KNOB_LEFT, null),
            RectangularButton(x = 475f, y = 0f, width = 66f, height = 172f, Constants.KNOB_RIGHT, null)
        )

        for (buttonArea in buttons) {
            when (buttonArea) {
                is CircularButton -> {
                    if (buttonArea.contains(translatedTouchPoint)) {
                        when (longPress) {
                            true -> buttonArea.onLongClickCommand?.let { onClick(it) }
                            false -> buttonArea.onClickCommand?.let { onClick(it) }
                        }
                        break
                    }
                }
                is RectangularButton -> {
                    if (buttonArea.contains(translatedTouchPoint)) {
                        when (longPress) {
                            true -> buttonArea.onLongClickCommand?.let { onClick(it) }
                            false -> buttonArea.onClickCommand?.let { onClick(it) }
                        }
                        break
                    }
                }
            }
        }
    }

    LaunchedEffect(currentImageUrl) {
        showError = false
    }

    LaunchedEffect(lifecycleOwner, empegIp, refreshDelay) {
        var tickerJob: Job? = null

        fun startTicker() {
            if (tickerJob?.isActive == true) {
                return
            }
            tickerJob = launch { // Launch a child coroutine for the ticking
                while (isActive) {
                    currentImageUrl = when (currentImageUrl.endsWith("?")) {
                        true -> "http://$empegIp/proc/empeg_screen.png"
                        false -> "http://$empegIp/proc/empeg_screen.png?"
                    }
                    delay(refreshDelay)
                }
            }
        }

        suspend fun pauseTickerJob() {
            if (tickerJob?.isActive == true) {
                tickerJob?.cancelAndJoin()
            }
        }

        val lifecycleObserver = LifecycleEventObserver { _: LifecycleOwner, event: Lifecycle.Event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    startTicker()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    // Launch in the scope of LaunchedEffect to allow suspension
                    launch { pauseTickerJob() }
                }
                // ON_DESTROY will lead to the LaunchedEffect being cancelled,
                // and its finally block will handle cleanup.
                else -> Unit
            }
        }

        // Initial check: if already resumed when effect starts
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            startTicker()
        }

        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

        // The 'finally' block of LaunchedEffect serves as its onDispose for this setup
        try {
            // Keep LaunchedEffect running. It will suspend indefinitely here if not
            // already cancelled by leaving composition.
            // A common way to keep it alive is to await something that never completes,
            // or simply let it run if its only job is to manage the observer and child coroutine.
            // In this case, its main job is done by setting up the observer and child job.
            // We just need it to stay active to keep the observer registered.
            // A delay without a loop will suspend and keep the coroutine alive until cancellation.
            delay(Long.MAX_VALUE)
        } finally {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            tickerJob?.cancel() // Ensure the child job is also cancelled
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .height(IntrinsicSize.Min)
            .systemBarsPadding()
            .navigationBarsPadding()
            .background(Color.Black)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .background(Color.Black)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            if (showDisplayBoard) {
                AsyncImage(
                    model = R.drawable.fascia_board,
                    alpha = 0.5f,
                    contentDescription = stringResource(id = R.string.description_empeg_display),
                    modifier = Modifier
                        .fillMaxWidth()
                )
            }

            when (showError) {
                true -> {
                    AsyncImage(
                        model = R.drawable.not_found,
                        filterQuality = FilterQuality.None,
                        colorFilter = ColorFilter.colorMatrix(Utils.getColorMatrix(Color.White)),
                        contentDescription = stringResource(id = R.string.description_empeg_display),
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.Center)
                            .padding(
                                start = vfdPaddingStart,
                                end = vfdPaddingEnd
                            )
                    )
                }

                false -> {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(currentImageUrl)
                            .memoryCachePolicy(CachePolicy.DISABLED)
                            .diskCachePolicy(CachePolicy.DISABLED)
                            .build(),
                        contentDescription = stringResource(R.string.description_empeg_display),
                        filterQuality = FilterQuality.None,
                        placeholder = currentPainter,
                        colorFilter = ColorFilter.colorMatrix(Utils.getColorMatrix(Color.White)),
                        onSuccess = { success ->
//                        println(">>> EmpegDisplay(onSuccess)")
                            showError = false
                            currentPainter = success.painter
                        },
                        onError = {
//                        println(">>> EmpegDisplay(onError)")
                            showError = true
                        },
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.Center)
                            .padding(
                                start = vfdPaddingStart,
                                end = vfdPaddingEnd
                            )
                    )
                }
            }

            Box(
                // TODO: Change this to a shape?
                modifier = Modifier
                    .matchParentSize()
                    .background(lensColor.withAlpha(0.4f)),
            )

            AsyncImage(
                model = R.drawable.fascia_transparent,
                contentDescription = stringResource(id = R.string.description_empeg_display),
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates -> fasciaSize = coordinates.size }
                    .pointerInput(fasciaSize) {
                        if (fasciaSize == IntSize.Zero) return@pointerInput
                        detectTapGestures(
                            onLongPress = { offset ->
                                calculateClickTarget(
                                    clickX = offset.x,
                                    clickY = offset.y,
                                    fasciaDimensions = fasciaSize,
                                    longPress = true)
                            },
                            onTap = { offset ->
                                calculateClickTarget(
                                    clickX = offset.x,
                                    clickY = offset.y,
                                    fasciaDimensions = fasciaSize,
                                    longPress = false)
                            }
                        )
                    }
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .background(Color.Black)
        )
    }
}

@Preview
@Composable
fun FasciaScreenPreview() {
    EmpegRemoteTheme {
        FasciaScreen(
            empegIp = "192.168.1.56",
            refreshDelay = 1000,
            lensColor = Color.Blue,
            showDisplayBoard = true,
            onClick = { }
        )
    }
}