package com.chasinglemons.empeg.remote

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.chasinglemons.empeg.R
import com.chasinglemons.empeg.ui.components.ColorPickerDialog
import com.chasinglemons.empeg.ui.components.EmpegDisplay
import com.chasinglemons.empeg.ui.theme.EmpegRemoteTheme
import com.chasinglemons.empeg.util.Constants
import com.chasinglemons.empeg.util.Utils


@Composable
fun RemoteScreen(
    modifier: Modifier = Modifier,
    viewModel: RemoteScreenViewModel = viewModel(),
    empegIp: String,
    buttonPress: (String) -> Unit,
    lensColor: Color,
    updateLensColor: (Color) -> Unit,
    screenRefreshRate: Int,
    useKeyboard: Boolean,
    showDisplay: Boolean = true
) {
    val showKeyboard by viewModel.showKeyboard.collectAsStateWithLifecycle()
    var showColorPickerDialog by remember { mutableStateOf(false) }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    var internalTextValue by remember { mutableStateOf("") } // This will receive IME text

    if (showColorPickerDialog) {
        ColorPickerDialog(
            initialColor = lensColor,
            onChoice = {
                showColorPickerDialog = false
                updateLensColor(it)
            },
            onDismiss = { showColorPickerDialog = false }
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
//                .focusable()
//                .focusRequester(focusRequester)
//                .onFocusChanged { focusState ->
//                    isFocusTargetFocused = focusState.isFocused
//                    println("Focus changed on Box: isFocused = ${focusState.isFocused}")
//                    if (!focusState.isFocused) {
//                        // Optional: Hide keyboard if the box loses focus and you want that behavior
//                        // keyboardController?.hide()
//                    }
//                }
//                .onKeyEvent {
//                    if (it.type == KeyEventType.KeyUp) {
//                        println(">>> KEY: ${it.key}") //Key.Enter
//                        true // Indicate that the event is handled
//                    } else {
//                        false
//                    }
//                }
            ) {
                Column(
                    modifier = modifier
                        .fillMaxSize()
                ) {
                    if (showDisplay) {
                        EmpegDisplay(
                            empegIp = empegIp,
                            displayColor = lensColor,
                            refreshDelay = screenRefreshRate.toLong(),
                            onClick = { showColorPickerDialog = true }
                        )
                    }



//                    TextField(
//                        modifier = Modifier
//                            .focusRequester(focusRequester)
//                            .size(1.dp),
//                        placeholder = {
//                            Text(stringResource(R.string.search_placeholder))
//                        },
//                        singleLine = true,
//                        value = internalTextValue,
//                        onValueChange = { internalTextValue = it },
//                    )

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
                                        onClick = { buttonPress(Constants.ONE) },
                                        onLongClick = { buttonPress(Constants.ONE_LONG) }
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
                                        onClick = { buttonPress(Constants.TWO) },
                                        onLongClick = { buttonPress(Constants.TWO_LONG) }
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
                                        onClick = { buttonPress(Constants.THREE) },
                                        onLongClick = { buttonPress(Constants.THREE_LONG) }
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
                                        onClick = { buttonPress(Constants.SOURCE) },
                                        onLongClick = { buttonPress(Constants.SOURCE_LONG) }
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
                                        onClick = { buttonPress(Constants.FOUR) },
                                        onLongClick = { buttonPress(Constants.FOUR_LONG) }
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
                                        onClick = { buttonPress(Constants.FIVE) },
                                        onLongClick = { buttonPress(Constants.FIVE_LONG) }
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
                                        onClick = { buttonPress(Constants.SIX) },
                                        onLongClick = { buttonPress(Constants.SIX_LONG) }
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
                                        onClick = { buttonPress(Constants.TUNER) },
                                        onLongClick = { buttonPress(Constants.TUNER_LONG) }
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
                                        onClick = { buttonPress(Constants.SEVEN) },
                                        onLongClick = { buttonPress(Constants.SEVEN_LONG) }
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
                                        onClick = { buttonPress(Constants.EIGHT) },
                                        onLongClick = { buttonPress(Constants.EIGHT_LONG) }
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
                                        onClick = { buttonPress(Constants.NINE) },
                                        onLongClick = { buttonPress(Constants.NINE_LONG) }
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
                                        onClick = { buttonPress(Constants.SELECT_MODE) },
                                        onLongClick = { buttonPress(Constants.HIJACK_MENU) }
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
                                        onClick = { buttonPress(Constants.CANCEL) },
                                        onLongClick = { buttonPress(Constants.CANCEL_LONG) }
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
                                        onClick = { buttonPress(Constants.ZERO) },
                                        onLongClick = { buttonPress(Constants.ZERO_LONG) }
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
                                        onClick = {
                                            buttonPress(Constants.SEARCH)
                                            if (useKeyboard) {
                                                viewModel.showKeyboard(true)
                                            }
                                        },
                                        onLongClick = { buttonPress(Constants.SEARCH_LONG) }
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
                                        onClick = { buttonPress(Constants.SOUND) },
                                        onLongClick = { buttonPress(Constants.SOUND_LONG) }
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
                                        onClick = { buttonPress(Constants.PREV_TRACK) },
                                        onLongClick = { buttonPress(Constants.PREV_TRACK_LONG) }
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
                                        onClick = { buttonPress(Constants.NEXT_TRACK) },
                                        onLongClick = { buttonPress(Constants.NEXT_TRACK_LONG) }
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
                                        onClick = { buttonPress(Constants.MENU) },
                                        onLongClick = { buttonPress(Constants.MENU_LONG) }
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
                                        onClick = { buttonPress(Constants.VOL_UP) },
                                        onLongClick = { buttonPress(Constants.VOL_UP_LONG) }
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
                                        onClick = { buttonPress(Constants.INFO) },
                                        onLongClick = { buttonPress(Constants.INFO_LONG) }
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
                                        onClick = { buttonPress(Constants.VISUAL) },
                                        onLongClick = { buttonPress(Constants.VISUAL_LONG) }
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
                                        onClick = { buttonPress(Constants.PLAY) },
                                        onLongClick = { buttonPress(Constants.PLAY_LONG) }
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
                                        onClick = { buttonPress(Constants.VOL_DOWN) },
                                        onLongClick = { buttonPress(Constants.VOL_DOWN_LONG) }
                                    )
                            )
                        }
                    }
                }

                // Visually Hidden BasicTextField
                BasicTextField(
                    value = internalTextValue,
                    onValueChange = { newValue ->
                        val added = if (newValue.length > internalTextValue.length) {
                            newValue.substring(internalTextValue.length)
                        } else {
                            newValue
                        }
                        val lastChar = added.lastOrNull()?.toString().orEmpty()
                        val buttonCode = Utils.translateKeyToButton(lastChar)
                        if (buttonCode.isNotEmpty()) {
                            buttonPress(buttonCode)
                        }
                        internalTextValue = ""
                    },
                    modifier = Modifier
                        .size(1.dp)
                        .focusRequester(focusRequester)
                        .onKeyEvent { keyEvent ->
                            if (keyEvent.type == KeyEventType.KeyUp) {
                                val buttonCode = Utils.translateSpecialKeyToButton(keyEvent.key)
                                if (buttonCode.isNotEmpty()) {
                                    buttonPress(buttonCode)
                                }
                                true
                            } else {
                                false
                            }
                                    },
                    textStyle = TextStyle(color = Color.Transparent, fontSize = 1.sp), // Make text invisible
                    cursorBrush = SolidColor(Color.Transparent), // Hide cursor
//                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                viewModel.showKeyboard(false)
                                keyboardController?.hide()
                            }
                        ),
                        decorationBox = { innerTextField ->
                            // No decoration to keep it minimal
                            innerTextField()
                        }
                )
            }
    LaunchedEffect(showKeyboard) {
        if (showKeyboard == true) {
            focusRequester.requestFocus()
        }
    }
}

@Preview
@Composable
fun RemoteScreenPreview() {
    EmpegRemoteTheme {
        RemoteScreen(
            empegIp = "192.168.1.56",
            buttonPress = { },
            lensColor = Color.Red,
            updateLensColor = { },
            screenRefreshRate = 100,
            useKeyboard = true,
        )
    }
}