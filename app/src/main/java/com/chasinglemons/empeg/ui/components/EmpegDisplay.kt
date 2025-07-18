package com.chasinglemons.empeg.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.chasinglemons.empeg.R
import com.chasinglemons.empeg.util.Utils
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun EmpegDisplay(
    empegIp: String,
    refreshDelay: Long,
    modifier: Modifier = Modifier,
    displayColor: Color,
    onClick: () -> Unit
) {
    var currentImageUrl by remember { mutableStateOf("http://$empegIp/proc/empeg_screen.png") }
    var currentPainter by remember { mutableStateOf<Painter?>(null) }
    var showError by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    // LaunchedEffect will manage lifecycle observation and the ticker job
    LaunchedEffect(lifecycleOwner) { // Keyed to lifecycleOwner to setup once
        var tickerJob: Job? = null
        var internalTickCount = 0

        fun startTicker() {
            if (tickerJob?.isActive == true) {
                return
            }
            tickerJob = launch { // Launch a child coroutine for the ticking
                while (isActive) {
                    internalTickCount++ // This will not overflow until 9,223,372,036,854,775,807

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

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(128.dp)
            .padding(12.dp)
            .background(Color.Black)
            .clickable {
                onClick()
            }
    ) {
        when (showError) {
            true -> {
                AsyncImage(
                    model = R.drawable.not_found,
                    filterQuality = FilterQuality.None,
                    colorFilter = ColorFilter.colorMatrix(Utils.getColorMatrix(displayColor)),
                    contentDescription = stringResource(id = R.string.description_empeg_display),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
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
                    colorFilter = ColorFilter.colorMatrix(Utils.getColorMatrix(displayColor)),
                    onSuccess = { success ->
//                        println(">>> EmpegDisplay(onSuccess)")
                        showError = false
                        currentPainter = success.painter
                    },
                    onError = {
//                        println(">>> EmpegDisplay(onError)")
                        showError = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                )
            }
        }
    }
}