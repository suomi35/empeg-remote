package com.chasinglemons.empeg.remote

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.chasinglemons.empeg.ui.components.FitToViewport
import com.chasinglemons.empeg.ui.theme.EmpegRemoteTheme

/** Padding around each button's artwork, inside its cell. */
private val ButtonPadding = 8.dp

/** Gap between the button columns. */
private val ButtonColumnGap = 4.dp

/** Gap between the button rows. */
private val ButtonRowGap = 2.dp

/** Inset of the button grid from the screen edges above and beside it. */
private val ButtonGridInset = 10.dp

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

    Box(
        Modifier
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
        // The face of the remote is a fixed-proportion design: the player's own button artwork
        // at the player's own spacing. It is a faceplate, not a list, so instead of scrolling
        // it (or re-flowing the buttons to squeeze them in) the whole face is shrunk uniformly
        // until it just fits between the app bar and the tabs.
        FitToViewport(modifier = modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                EmpegDisplay(
                    empegIp = empegIp,
                    displayColor = lensColor.value,
                    refreshDelay = 100,
                    onClick = { showColorPickerDialog = true }
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = ButtonGridInset,
                            top = ButtonGridInset,
                            end = ButtonGridInset
                        ),
                    verticalArrangement = Arrangement.spacedBy(ButtonRowGap)
                ) {
                    RemoteButtons.forEach { rowButtons ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(ButtonColumnGap)
                        ) {
                            rowButtons.forEach { button ->
                                RemoteButton(button = button, buttonPress = buttonPress)
                            }
                        }
                    }
                }
            }
        }

        // TODO: Fix for mlord's mention of "Add workaround for Empeg Remote Android app: it sends wrong codes for NextTrack and PrevTrack."
        // looks like the mistake was sending Next and Prev instead of NextTrack and PrevTrack :shrug:
    }
}

/** A button on the remote's face: its artwork, its description and the commands it sends. */
private data class RemoteButtonSpec(
    @param:DrawableRes val artwork: Int,
    @param:StringRes val description: Int,
    /** Command sent when the button is tapped. */
    val command: String,
    /** Command sent when the button is held down. */
    val longPressCommand: String,
)

/** The remote's buttons, one list per row of the face, in the player's own order. */
private val RemoteButtons: List<List<RemoteButtonSpec>> = listOf(
    listOf(
        RemoteButtonSpec(R.drawable.button_a1, R.string.desc_remote_button_a1, "One", "One.L"),
        RemoteButtonSpec(R.drawable.button_a2, R.string.desc_remote_button_a2, "Two", "Two.L"),
        RemoteButtonSpec(R.drawable.button_a3, R.string.desc_remote_button_a3, "Three", "Three.L"),
        RemoteButtonSpec(R.drawable.button_a4, R.string.desc_remote_button_a4, "Source", "Source.L"),
    ),
    listOf(
        RemoteButtonSpec(R.drawable.button_b1, R.string.desc_remote_button_b1, "Four", "Four.L"),
        RemoteButtonSpec(R.drawable.button_b2, R.string.desc_remote_button_b2, "Five", "Five.L"),
        RemoteButtonSpec(R.drawable.button_b3, R.string.desc_remote_button_b3, "Six", "Six.L"),
        RemoteButtonSpec(R.drawable.button_b4, R.string.desc_remote_button_b4, "Tuner", "Tuner.L"),
    ),
    listOf(
        RemoteButtonSpec(R.drawable.button_c1, R.string.desc_remote_button_c1, "Seven", "Seven.L"),
        RemoteButtonSpec(R.drawable.button_c2, R.string.desc_remote_button_c2, "Eight", "Eight.L"),
        RemoteButtonSpec(R.drawable.button_c3, R.string.desc_remote_button_c3, "Nine", "Nine.L"),
        RemoteButtonSpec(R.drawable.button_c4, R.string.desc_remote_button_c4, "SelectMode", "HijackMenu"),
    ),
    listOf(
        RemoteButtonSpec(R.drawable.button_d1, R.string.desc_remote_button_d1, "Cancel", "Cancel.L"),
        RemoteButtonSpec(R.drawable.button_d2, R.string.desc_remote_button_d2, "Zero", "Zero.L"),
        RemoteButtonSpec(R.drawable.button_d3, R.string.desc_remote_button_d3, "Search", "Search.L"),
        RemoteButtonSpec(R.drawable.button_d4, R.string.desc_remote_button_d4, "Sound", "Sound.L"),
    ),
    listOf(
        RemoteButtonSpec(R.drawable.button_e1, R.string.desc_remote_button_e1, "PrevTrack", "PrevTrack.L"),
        RemoteButtonSpec(R.drawable.button_e2, R.string.desc_remote_button_e2, "NextTrack", "NextTrack.L"),
        RemoteButtonSpec(R.drawable.button_e3, R.string.desc_remote_button_e3, "Menu", "Menu.L"),
        RemoteButtonSpec(R.drawable.button_e4, R.string.desc_remote_button_e4, "VolUp", "VolUp.L"),
    ),
    listOf(
        RemoteButtonSpec(R.drawable.button_f1, R.string.desc_remote_button_f1, "Info", "Info.L"),
        RemoteButtonSpec(R.drawable.button_f2, R.string.desc_remote_button_f2, "Visual", "Visual.L"),
        RemoteButtonSpec(R.drawable.button_f3, R.string.desc_remote_button_f3, "Play", "Play.L"),
        RemoteButtonSpec(R.drawable.button_f4, R.string.desc_remote_button_f4, "VolDown", "VolDown.L"),
    ),
)

@Composable
private fun RowScope.RemoteButton(
    button: RemoteButtonSpec,
    buttonPress: (String) -> Unit,
) {
    AsyncImage(
        model = button.artwork,
        contentDescription = stringResource(id = button.description),
        contentScale = ContentScale.FillWidth,
        modifier = Modifier
            .weight(1f)
            // Every piece of button art is square, so the face keeps its shape - and the
            // height FitToViewport measures - even before Coil has decoded anything.
            .aspectRatio(1f)
            .padding(ButtonPadding)
            .combinedClickable(
                onClick = { buttonPress(button.command) },
                onLongClick = { buttonPress(button.longPressCommand) }
            )
    )
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
