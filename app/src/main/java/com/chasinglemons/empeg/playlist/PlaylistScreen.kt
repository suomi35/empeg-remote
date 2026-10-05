package com.chasinglemons.empeg.playlist

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.chasinglemons.empeg.R
import com.chasinglemons.empeg.model.Playlist
import com.chasinglemons.empeg.model.PlaylistType
import com.chasinglemons.empeg.ui.components.PlaylistOverflowDialog
import com.chasinglemons.empeg.util.Constants


@Composable
fun PlaylistScreen(
    modifier: Modifier = Modifier,
    playlist: SnapshotStateList<Playlist>,
    lensColor: Color,
    usePixelFont: Boolean,
    handleBackButton: Boolean,
    playlistClick: (String) -> Unit,
    tuneClick: (String) -> Unit,
    overflowAction: (String) -> Unit = {},
    downloadTune: (Playlist) -> Unit = {},
    backPressed: () -> Unit
) {
    val empegFont = FontFamily(Font(R.font.pixelmix))
    val empegFontBold = FontFamily(Font(R.font.pixelmix_bold))
    val lazyListState = rememberLazyListState()

    var showOverflowDialog by remember { mutableStateOf(false) }
    var currentPlaylistData by remember { mutableStateOf<Playlist?>(null) }

    BackHandler(
        enabled = handleBackButton
    ) {
        backPressed()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {

        Column {

            LazyColumn(
                state = lazyListState,
                content = {
                    itemsIndexed(
                        items = playlist,
                        key = { index, item -> "${item.url}-$index" }
                    ) { index, item ->
                        Row(
                            modifier = Modifier
                                .background(
                                    color = when (item.url == Constants.PLAYLIST_HEAD) {
                                        true -> Color.DarkGray
                                        false -> Color.Transparent
                                    }
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(
                                onClick = { tuneClick(item.playURL) }
                            ) {
                                AsyncImage(
                                    model = R.drawable.ic_play_circle,
                                    contentDescription = stringResource(R.string.description_playlist_play_button),
                                    modifier = Modifier.fillMaxSize(),
                                    colorFilter = when (item.url == Constants.PLAYLIST_HEAD) {
                                        true -> ColorFilter.tint(lensColor)
                                        false -> ColorFilter.tint(Color.White)
                                    }
                                )
                            }

                            Text(
                                modifier = Modifier
                                    .weight(1F)
                                    .clickable {
                                        when (item.type == PlaylistType.TUNE) {
                                            true -> tuneClick(item.playURL)
                                            false -> playlistClick(item.url)
                                        }
                                    },
                                text = item.name,
                                textAlign = TextAlign.Center,
                                color = when (item.url == Constants.PLAYLIST_HEAD) {
                                    true -> lensColor
                                    false -> Color.White
                                },
                                style =
                                    when (item.url == Constants.PLAYLIST_HEAD) {
                                        true -> MaterialTheme.typography.headlineMedium
                                        false -> MaterialTheme.typography.headlineSmall
                                    },
                                fontFamily =
                                    when (item.url == Constants.PLAYLIST_HEAD) {
                                        true -> {
                                            when (usePixelFont) {
                                                true -> empegFontBold
                                                false -> FontFamily.Default
                                            }
                                        }
                                        false -> {
                                            when (usePixelFont) {
                                                true -> empegFont
                                                false -> FontFamily.Default
                                            }
                                        }
                                }
                            )

                            IconButton(onClick = {
                                currentPlaylistData = item
                                showOverflowDialog = true
                            }
                            ) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription =
                                        stringResource(R.string.description_playlist_overflow_button),
                                    tint = when (item.url == Constants.PLAYLIST_HEAD) {
                                        true -> lensColor
                                        false -> Color.White
                                    }
                                )
                            }
                        }
                        if (index < playlist.lastIndex) {
                            HorizontalDivider(thickness = 1.dp, color = lensColor)
                        }
                    }
                })

            /* TODO: determine if we should show playlist nav UI with the following:
                  if (name != Constants.ALL_MUSIC && listLinks.size > 1 /* if nothing is returned (player off or not configured)*/) {
                        _showPlaylistHistory.value = true
                    } else {
                        _showPlaylistHistory.value = false
                    }
             */
        }
    }

    if (showOverflowDialog && currentPlaylistData != null) {
        PlaylistOverflowDialog(
            playlist = currentPlaylistData!!,
            buttonClick = { actionUrl ->
                overflowAction(actionUrl)
                showOverflowDialog = false
                currentPlaylistData = null
            },
            onDownload = {
                currentPlaylistData?.let { downloadTune(it) }
                showOverflowDialog = false
                currentPlaylistData = null
            },
            onDismiss = {
                showOverflowDialog = false
                currentPlaylistData = null
            }
        )
    }
}
