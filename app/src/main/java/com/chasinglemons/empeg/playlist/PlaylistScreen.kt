package com.chasinglemons.empeg.playlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chasinglemons.empeg.empegapi.EmpegItem
import com.chasinglemons.empeg.model.Playlist
import com.chasinglemons.empeg.model.PlaylistStatus
import com.chasinglemons.empeg.ui.components.LoadingAnimation
import com.chasinglemons.empeg.ui.theme.EmpegRemoteTheme


@Composable
fun PlaylistScreen(
    modifier: Modifier = Modifier,
    viewModel: PlaylistScreenViewModel = viewModel(),
    playlistClick: (String) -> Unit,
) {
    val lazyListState = rememberLazyListState()
    val lensColor = viewModel.lensColor.collectAsStateWithLifecycle()
    val playlist = viewModel.playlist.collectAsStateWithLifecycle()
    val status = viewModel.status.collectAsStateWithLifecycle()
    val title = viewModel.title.collectAsStateWithLifecycle()
    val canGoUp = viewModel.canGoUp.collectAsStateWithLifecycle()
    val empegIp = viewModel.empegIp.collectAsStateWithLifecycle()

    // Read the playlist the first time the tab is shown.
    LaunchedEffect(viewModel) {
        viewModel.loadIfNeeded()
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {

        Column(Modifier.fillMaxSize()) {

            PlaylistHeader(
                title = title.value,
                itemCount = playlist.value.size,
                canGoUp = canGoUp.value,
                onUp = { viewModel.goUp() }
            )

            LazyColumn(
//        contentPadding = innerPadding,
                state = lazyListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(
                    items = playlist.value,
                    itemContent = { item ->
                        val currentItem by rememberUpdatedState(item)
                        PlaylistRow(
                            item = currentItem,
                            onClick = {
                                viewModel.onItemClick(currentItem)
                                playlistClick(currentItem.name)
                            }
                        )
                        HorizontalDivider()
                    }
                )
            }

            PlaylistFooter(
                status = status.value,
                itemCount = playlist.value.size,
                empegIp = empegIp.value
            )
        }

        // Only shown while the player is being asked for the playlist.
        if (status.value == PlaylistStatus.LOADING) {
            LoadingAnimation(
                color = lensColor.value,
                imageSize = 60.dp,
                modifier = Modifier
                    .align(Alignment.Center)
            )
        }
    }
}

/**
 * Header of the tab: the playlist currently shown, how many items it holds
 * and, for sub playlists, a way back to the parent.
 */
@Composable
private fun PlaylistHeader(
    title: String,
    itemCount: Int,
    canGoUp: Boolean,
    onUp: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (canGoUp) {
            IconButton(onClick = onUp) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Up one playlist"
                )
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = if (itemCount == 1) "1 item" else "$itemCount items",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * One playlist entry: tapping a sub playlist browses into it, tapping a tune
 * plays it; [Playlist.length] is the duration for tunes and the number of
 * contained items for playlists.
 */
@Composable
private fun PlaylistRow(
    item: Playlist,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyLarge,
                color = if (item.type == EmpegItem.TYPE_PLAYLIST) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = item.length,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        val subtitle = item.subtitle()
        if (subtitle.isNotEmpty()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Explains an empty playlist or a player that could not be reached. */
@Composable
private fun PlaylistFooter(
    status: PlaylistStatus,
    itemCount: Int,
    empegIp: String,
) {
    when {
        status == PlaylistStatus.ERROR -> Text(
            text = "Could not read the playlist from $empegIp",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(16.dp)
        )

        status == PlaylistStatus.LOADED && itemCount == 0 -> Text(
            text = "No items in this playlist",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp)
        )
    }
}

/** "Artist — Source" for tunes, "Playlist" for sub playlists. */
private fun Playlist.subtitle(): String {
    if (type == EmpegItem.TYPE_PLAYLIST) return "Playlist"
    return listOf(artist, source)
        .filter { it.isNotBlank() }
        .joinToString(" — ")
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