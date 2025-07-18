package com.chasinglemons.empeg.remote

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.chasinglemons.empeg.R
import com.chasinglemons.empeg.ui.components.ColorPickerDialog
import com.chasinglemons.empeg.ui.components.EmpegDisplay
import com.chasinglemons.empeg.ui.components.LoadingAnimation
import com.chasinglemons.empeg.ui.theme.EmpegRemoteTheme


@Composable
fun RemoteScreen(
    modifier: Modifier = Modifier,
    viewModel: RemoteScreenViewModel = viewModel(),
    empegIp: String,
    buttonPress: (String) -> Unit,
) {
    val lensColor = viewModel.lensColor.collectAsStateWithLifecycle()
    var showColorPickerDialog by remember { mutableStateOf(false) }

    if (showColorPickerDialog) {
        ColorPickerDialog(
            initialColor = lensColor.value,
            onChoice = {
                showColorPickerDialog = false
                viewModel.updateLensColor(it)
            },
            onDismissRequest = { showColorPickerDialog = false }
        )
    }

            Box(Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFFE3E3E3),
                            Color(0xFFBCBCBC),
                        )
                    )
                )
            ) {
                Column(
                    modifier = modifier
                        .fillMaxSize()
                ) {
                    EmpegDisplay(
                        empegIp = empegIp,
                        displayColor = lensColor.value,
                        refreshDelay = 100,
                        onClick = { showColorPickerDialog = true }
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        Modifier
                            .padding(
                                start = 10.dp,
                                top = 10.dp,
                                end = 10.dp,
                            ),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        val itemPadding = 8.dp
                        item {
                            AsyncImage(
                                model = R.drawable.button_a1,
                                contentDescription = stringResource(id = R.string.desc_remote_button_a1),
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(itemPadding)
                                    .combinedClickable(
                                        onClick = { buttonPress("One") },
                                        onLongClick = { buttonPress("One.L") }
                                    )
                            )
                        }

                        item {
                            AsyncImage(
                                model = R.drawable.button_a2,
                                contentDescription = stringResource(id = R.string.desc_remote_button_a2),
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(itemPadding)
                                    .combinedClickable(
                                        onClick = { buttonPress("Two") },
                                        onLongClick = { buttonPress("Two.L") }
                                    )
                            )
                        }

                        item {
                            AsyncImage(
                                model = R.drawable.button_a3,
                                contentDescription = stringResource(id = R.string.desc_remote_button_a3),
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(itemPadding)
                                    .combinedClickable(
                                        onClick = { buttonPress("Three") },
                                        onLongClick = { buttonPress("Three.L") }
                                    )
                            )
                        }

                        item {
                            AsyncImage(
                                model = R.drawable.button_a4,
                                contentDescription = stringResource(id = R.string.desc_remote_button_a4),
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(itemPadding)
                                    .combinedClickable(
                                        onClick = { buttonPress("Source") },
                                        onLongClick = { buttonPress("Source.L") }
                                    )
                            )
                        }

                        item {
                            AsyncImage(
                                model = R.drawable.button_b1,
                                contentDescription = stringResource(id = R.string.desc_remote_button_b1),
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(itemPadding)
                                    .combinedClickable(
                                        onClick = { buttonPress("Four") },
                                        onLongClick = { buttonPress("Four.L") }
                                    )
                            )
                        }

                        item {
                            AsyncImage(
                                model = R.drawable.button_b2,
                                contentDescription = stringResource(id = R.string.desc_remote_button_b2),
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(itemPadding)
                                    .combinedClickable(
                                        onClick = { buttonPress("Five") },
                                        onLongClick = { buttonPress("Five.L") }
                                    )
                            )
                        }

                        item {
                            AsyncImage(
                                model = R.drawable.button_b3,
                                contentDescription = stringResource(id = R.string.desc_remote_button_b3),
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(itemPadding)
                                    .combinedClickable(
                                        onClick = { buttonPress("Six") },
                                        onLongClick = { buttonPress("Six.L") }
                                    )
                            )
                        }

                        item {
                            AsyncImage(
                                model = R.drawable.button_b4,
                                contentDescription = stringResource(id = R.string.desc_remote_button_b4),
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(itemPadding)
                                    .combinedClickable(
                                        onClick = { buttonPress("Tuner") },
                                        onLongClick = { buttonPress("Tuner.L") }
                                    )
                            )
                        }

                        item {
                            AsyncImage(
                                model = R.drawable.button_c1,
                                contentDescription = stringResource(id = R.string.desc_remote_button_c1),
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(itemPadding)
                                    .combinedClickable(
                                        onClick = { buttonPress("Seven") },
                                        onLongClick = { buttonPress("Seven.L") }
                                    )
                            )
                        }

                        item {
                            AsyncImage(
                                model = R.drawable.button_c2,
                                contentDescription = stringResource(id = R.string.desc_remote_button_c2),
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(itemPadding)
                                    .combinedClickable(
                                        onClick = { buttonPress("Eight") },
                                        onLongClick = { buttonPress("Eight.L") }
                                    )
                            )
                        }

                        item {
                            AsyncImage(
                                model = R.drawable.button_c3,
                                contentDescription = stringResource(id = R.string.desc_remote_button_c3),
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(itemPadding)
                                    .combinedClickable(
                                        onClick = { buttonPress("Nine") },
                                        onLongClick = { buttonPress("Nine.L") }
                                    )
                            )
                        }

                        item {
                            AsyncImage(
                                model = R.drawable.button_c4,
                                contentDescription = stringResource(id = R.string.desc_remote_button_c4),
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(itemPadding)
                                    .combinedClickable(
                                        onClick = { buttonPress("SelectMode") },
                                        onLongClick = { buttonPress("HijackMenu") }
                                    )
                            )
                        }

                        item {
                            AsyncImage(
                                model = R.drawable.button_d1,
                                contentDescription = stringResource(id = R.string.desc_remote_button_d1),
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(itemPadding)
                                    .combinedClickable(
                                        onClick = { buttonPress("Cancel") },
                                        onLongClick = { buttonPress("Cancel.L") }
                                    )
                            )
                        }

                        item {
                            AsyncImage(
                                model = R.drawable.button_d2,
                                contentDescription = stringResource(id = R.string.desc_remote_button_d2),
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(itemPadding)
                                    .combinedClickable(
                                        onClick = { buttonPress("Zero") },
                                        onLongClick = { buttonPress("Zero.L") }
                                    )
                            )
                        }

                        item {
                            AsyncImage(
                                model = R.drawable.button_d3,
                                contentDescription = stringResource(id = R.string.desc_remote_button_d3),
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(itemPadding)
                                    .combinedClickable(
                                        onClick = { buttonPress("Search") },
                                        onLongClick = { buttonPress("Search.L") }
                                    )
                            )
                        }

                        item {
                            AsyncImage(
                                model = R.drawable.button_d4,
                                contentDescription = stringResource(id = R.string.desc_remote_button_d4),
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(itemPadding)
                                    .combinedClickable(
                                        onClick = { buttonPress("Sound") },
                                        onLongClick = { buttonPress("Sound.L") }
                                    )
                            )
                        }

                        item {
                            AsyncImage(
                                model = R.drawable.button_e1,
                                contentDescription = stringResource(id = R.string.desc_remote_button_e1),
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(itemPadding)
                                    .combinedClickable(
                                        onClick = { buttonPress("PrevTrack") },
                                        onLongClick = { buttonPress("PrevTrack.L") }
                                    )
                            )
                        }

                        item {
                            AsyncImage(
                                model = R.drawable.button_e2,
                                contentDescription = stringResource(id = R.string.desc_remote_button_e2),
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(itemPadding)
                                    .combinedClickable(
                                        onClick = { buttonPress("NextTrack") },
                                        onLongClick = { buttonPress("NextTrack.L") }
                                    )
                            )
                        }

                        item {
                            AsyncImage(
                                model = R.drawable.button_e3,
                                contentDescription = stringResource(id = R.string.desc_remote_button_e3),
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(itemPadding)
                                    .combinedClickable(
                                        onClick = { buttonPress("Menu") },
                                        onLongClick = { buttonPress("Menu.L") }
                                    )
                            )
                        }

                        item {
                            AsyncImage(
                                model = R.drawable.button_e4,
                                contentDescription = stringResource(id = R.string.desc_remote_button_e4),
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(itemPadding)
                                    .combinedClickable(
                                        onClick = { buttonPress("VolUp") },
                                        onLongClick = { buttonPress("VolUp.L") }
                                    )
                            )
                        }

                        item {
                            AsyncImage(
                                model = R.drawable.button_f1,
                                contentDescription = stringResource(id = R.string.desc_remote_button_f1),
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(itemPadding)
                                    .combinedClickable(
                                        onClick = { buttonPress("Info") },
                                        onLongClick = { buttonPress("Info.L") }
                                    )
                            )
                        }

                        item {
                            AsyncImage(
                                model = R.drawable.button_f2,
                                contentDescription = stringResource(id = R.string.desc_remote_button_f2),
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(itemPadding)
                                    .combinedClickable(
                                        onClick = { buttonPress("Visual") },
                                        onLongClick = { buttonPress("Visual.L") }
                                    )
                            )
                        }

                        item {
                            AsyncImage(
                                model = R.drawable.button_f3,
                                contentDescription = stringResource(id = R.string.desc_remote_button_f3),
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(itemPadding)
                                    .combinedClickable(
                                        onClick = { buttonPress("Play") },
                                        onLongClick = { buttonPress("Play.L") }
                                    )
                            )
                        }

                        item {
                            AsyncImage(
                                model = R.drawable.button_f4,
                                contentDescription = stringResource(id = R.string.desc_remote_button_f4),
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(itemPadding)
                                    .combinedClickable(
                                        onClick = { buttonPress("VolDown") },
                                        onLongClick = { buttonPress("VolDown.L") }
                                    )
                            )
                        }
                    }
                }

            // TODO: Fix for mlord's mention of "Add workaround for Empeg Remote Android app: it sends wrong codes for NextTrack and PrevTrack."
                // looks like the mistake was sending Next and Prev instead of NextTrack and PrevTrack :shrug:

            }
}

@Preview
@Composable
fun RemoteScreenPreview() {
    EmpegRemoteTheme {
        RemoteScreen(
            empegIp = "192.168.1.56",
            buttonPress = { }
        )
    }
}