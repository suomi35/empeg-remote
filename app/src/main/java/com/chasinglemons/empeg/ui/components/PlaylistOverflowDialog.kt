package com.chasinglemons.empeg.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.chasinglemons.empeg.model.Playlist
import com.chasinglemons.empeg.model.PlaylistType

@Composable
fun PlaylistOverflowDialog(
    playlist: Playlist,
    buttonClick: (String) -> Unit,
    onDownload: () -> Unit = {},
    onDismiss: () -> Unit
) {

    Dialog(onDismissRequest = { onDismiss() }) {

        Card(
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    modifier = Modifier
                        .padding(16.dp),
                    textAlign = TextAlign.Center,
                    text = playlist.name,
                    style = MaterialTheme.typography.headlineSmall
                )

                TextButton(
                    modifier = Modifier
                        .padding(8.dp),
                    onClick = { buttonClick(playlist.enqueueURL) }
                ) {
                    Text("Enqueue")
                }

                TextButton(
                    modifier = Modifier
                        .padding(8.dp),
                    onClick = { buttonClick(playlist.appendURL) }
                ) {
                    Text("Append")
                }

                TextButton(
                    modifier = Modifier
                        .padding(8.dp),
                    onClick = { buttonClick(playlist.insertURL) }
                ) {
                    Text("Insert")
                }

                TextButton(
                    modifier = Modifier
                        .padding(8.dp),
                    onClick = { buttonClick(playlist.streamURL) }
                ) {
                    Text("Stream")
                }

                if (playlist.type == PlaylistType.TUNE) {
                    TextButton(
                        modifier = Modifier
                            .padding(8.dp),
                        onClick = { onDownload() }
                    ) {
                        Text("Download")
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun PlaylistOverflowDialogPreview() {
    PlaylistOverflowDialog(
        playlist = Playlist(
            "Scars on Broadway",
            streamURL = "",
            playURL = "",
            insertURL = "",
            enqueueURL = "",
            appendURL = "",
            url = "",
            length = "",
            type = PlaylistType.TUNE,
            artist = "",
            source = "",
        ),
        buttonClick = { },
        onDismiss = { }
    )
}