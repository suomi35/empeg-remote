package com.chasinglemons.empeg.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chasinglemons.empeg.ui.components.LoadingAnimation
import com.chasinglemons.empeg.ui.theme.EmpegRemoteTheme
import kotlinx.coroutines.launch


@Composable
fun PlaylistScreen(
    modifier: Modifier = Modifier,
    viewModel: PlaylistScreenViewModel = viewModel(),
    playlistClick: (String) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    val lensColor = viewModel.lensColor.collectAsStateWithLifecycle()
    val playlist = viewModel.playlist.collectAsStateWithLifecycle()

    Box(
        Modifier
            .fillMaxSize()
    ) {

        Column {

            LazyColumn(
//        contentPadding = innerPadding,
                state = lazyListState
            ) {
                items(
                    items = playlist.value,
                    key = { item -> item.url },
                    itemContent = { item ->
                        val currentItem by rememberUpdatedState(item)
                        if (currentItem.url == "head") {
                            Text("HEAD: ${currentItem.name}")
                        } else {
                            Text(currentItem.name)
                        }
                    }
                )
            }
            Button(
                onClick = {
                    coroutineScope.launch {
//                        viewModel.fetchPlaylist()
                    }
                }
            ) {
                Text("GET PLAYLIST")
            }
        }

        LoadingAnimation(
            color = lensColor.value,
            imageSize = 60.dp,
            modifier = Modifier
                .align(Alignment.Center)
        )
    }
}

@Preview
@Composable
fun PlaylistScreenPreview() {
    EmpegRemoteTheme {
        PlaylistScreen(
            playlistClick = {}
        )
    }
}