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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
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
    var currentImageUrl by remember(empegIp) {
        mutableStateOf("http://$empegIp/proc/empeg_screen.png")
    }
    var currentPainter by remember { mutableStateOf<Painter?>(null) }
    var showError by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(currentImageUrl) {
        showError = false
    }

    LaunchedEffect(lifecycleOwner, empegIp, refreshDelay) {
        var tickerJob: Job? = null

        fun startTicker() {
            if (tickerJob?.isActive == true) {
                return
            }
            tickerJob = launch {
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
                Lifecycle.Event.ON_RESUME -> startTicker()
                Lifecycle.Event.ON_PAUSE -> launch { pauseTickerJob() }
                else -> Unit
            }
        }

        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            startTicker()
        }

        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

        try {
            delay(Long.MAX_VALUE)
        } finally {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            tickerJob?.cancel()
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
                        showError = false
                        currentPainter = success.painter
                    },
                    onError = {
                        showError = true
                    },
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                )
            }
        }
    }
}
