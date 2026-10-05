package com.chasinglemons.empeg.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.chasinglemons.empeg.model.SwipeAction

@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,
    lensColor: Color,
    updateLensColor: (Color) -> Unit,
    persistentNotification: Boolean,
    updatePersistentNotification: (Boolean) -> Unit,
    keepScreenOn: Boolean,
    updateKeepScreenOn: (Boolean) -> Unit,
    vibrate: Boolean,
    updateVibrate: (Boolean) -> Unit,
    discoveryTimeout: Int,
    updateDiscoveryTimeout: (Int) -> Unit,
    swipeAction: SwipeAction,
    updateSwipeAction: (SwipeAction) -> Unit,
    screenRefreshRate: Int,
    updateScreenRefreshRate: (Int) -> Unit,
    usePixelFont: Boolean,
    updateUsePixelFont: (Boolean) -> Unit,
    showDisplay: Boolean,
    updateShowDisplay: (Boolean) -> Unit,
    useKeyboard: Boolean,
    updateUseKeyboard: (Boolean) -> Unit,
    showDisplayBoard: Boolean,
    updateShowDisplayBoard: (Boolean) -> Unit,
) {
    var showColorPickerDialog by remember { mutableStateOf(false) }
    var showSwipeActionChooserDialog by remember { mutableStateOf(false) }
    var showScreenRefreshRateChooserDialog by remember { mutableStateOf(false) }

    Dialog(
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = { onDismiss() }
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                item {
                    SettingsSectionTitle("General")
                }
                item {
                    BooleanPreference(
                        title = "Persistent notification",
                        summary = "Keep the app in the notification area for quick access/control",
                        checked = persistentNotification,
                        onCheckedChange = { newValue ->
                            updatePersistentNotification(newValue)
                        }
                    )
                }

                item {
                    BooleanPreference(
                        title = "Keep screen on",
                        summary = "Keep the screen on while the app is active",
                        checked = keepScreenOn,
                        onCheckedChange = { newValue ->
                            updateKeepScreenOn(newValue)
                        }
                    )
                }

                item {
                    BooleanPreference(
                        title = "Vibrate on touch",
                        summary = "Vibrate when buttons are touched",
                        checked = vibrate,
                        onCheckedChange = { newValue ->
                            updateVibrate(newValue)
                        }
                    )
                }

                item { SettingsDivider() }

                item {
                    SettingsSectionTitle("Landscape fascia view")
                }

                item {
                    BooleanPreference(
                        title = "Show display circuit board",
                        summary = "This will allow the circuit board to show through the lens",
                        checked = showDisplayBoard,
                        onCheckedChange = { newValue ->
                            updateShowDisplayBoard(newValue)
                        }
                    )
                }

                item {
                    SettingsSectionTitle("Remote")
                }

                item {
                    BooleanPreference(
                        title = "Show display",
                        summary = "The Empeg screen is visible on the remote",
                        checked = showDisplay,
                        onCheckedChange = { newValue ->
                            updateShowDisplay(newValue)
                        }
                    )
                }

                item {
                    PreferenceItem(
                        title = "Lens color",
                        summary = "Choose the lens color for your Empeg",
                        onClick = {
                            showColorPickerDialog = true
                        }
                    )
                }

                item {
                    PreferenceItem(
                        title = "Screen refresh rate",
                        summary = "Choose how often the screen refreshes",
                        onClick = {
                            showScreenRefreshRateChooserDialog = true
                        }
                    )
                }

                item {
                    BooleanPreference(
                        title = "Show soft keyboard tab",
                        summary = "A soft keyboard tab will appear in the lower left corner of the remote for use with searching",
                        checked = useKeyboard,
                        onCheckedChange = { newValue ->
                            updateUseKeyboard(newValue)
                        }
                    )
                }

                item { SettingsDivider() }

                item {
                    SettingsSectionTitle("Playlists")
                }

                item {
                    BooleanPreference(
                        title = "Use pixel font",
                        summary = "Emulate the Empeg font for playlists",
                        checked = usePixelFont,
                        onCheckedChange = { newValue ->
                            updateUsePixelFont(newValue)
                        }
                    )
                }

                item {
                    PreferenceItem(
                        title = "Swipe action",
                        summary = when (swipeAction) {
                            SwipeAction.GESTURES -> "Pager swipe disabled so gestures can be used"
                            else -> "Swipe between Remote and Playlists"
                        },
                        onClick = {
                            showSwipeActionChooserDialog = true
                        }
                    )
                }

                item { SettingsDivider() }

                item {
                    SettingsSectionTitle("Discovery")
                }

                item {
                    SliderPreference(
                        title = "Discovery timeout",
                        value = discoveryTimeout.toFloat(),
                        onValueChange = { newValue ->
                            updateDiscoveryTimeout(newValue.toInt())
                        },
                        summarySuffix = "sec",
                        valueRange = 2f..30f,
                        steps = 28
                    )
                }
            }

            if (showSwipeActionChooserDialog) {
                SwipeActionChooserDialog(
                    currentSwipeAction = swipeAction,
                    buttonClick = {
                        updateSwipeAction(it)
                        showSwipeActionChooserDialog = false
                    },
                    onDismiss = {
                        showSwipeActionChooserDialog = false
                    }
                )
            }

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

            if (showScreenRefreshRateChooserDialog) {
                ScreenRefreshRateChooserDialog(
                    currentRefreshRate = screenRefreshRate,
                    buttonClick = {
                        showScreenRefreshRateChooserDialog = false
                        updateScreenRefreshRate(it)
                    },
                    onDismiss = { showScreenRefreshRateChooserDialog = false }
                )
            }
        }
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
}

@Composable
fun PreferenceItem(
    title: String,
    summary: String? = null,
    onClick: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (summary != null) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (trailingContent != null) {
            Spacer(modifier = Modifier.width(16.dp))
            trailingContent()
        }
    }
}

@Composable
fun BooleanPreference(
    title: String,
    summary: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    PreferenceItem(
        title = title,
        summary = summary,
        onClick = { if (enabled) onCheckedChange(!checked) },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
        }
    )
}

//@Composable
//fun TextPreference(
//    title: String,
//    summary: String? = null,
//    value: String,
//    onValueChange: (String) -> Unit, // This lambda should also handle saving to SharedPreferences
//    dialogTitle: String,
//    dialogLabel: String,
//    keyboardType: KeyboardType = KeyboardType.Text,
//    enabled: Boolean = true
//) {
//    var showDialog by remember { mutableStateOf(false) }
//    var currentTextValue by remember(value) { mutableStateOf(value) } // Local state for dialog editing
//
//    PreferenceItem(
//        title = title,
//        summary = summary ?: value.ifEmpty { "Not set" }, // Show current value if no summary
//        onClick = { if (enabled) showDialog = true }
//    )
//
//    if (showDialog) {
//        AlertDialog(
//            onDismissRequest = { showDialog = false },
//            title = { Text(dialogTitle) },
//            text = {
//                OutlinedTextField(
//                    value = currentTextValue,
//                    onValueChange = { currentTextValue = it },
//                    label = { Text(dialogLabel) },
//                    singleLine = true,
//                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
//                    modifier = Modifier.fillMaxWidth()
//                )
//            },
//            confirmButton = {
//                TextButton(
//                    onClick = {
//                        onValueChange(currentTextValue) // Call the provided lambda to update state and save
//                        showDialog = false
//                    }
//                ) {
//                    Text("OK")
//                }
//            },
//            dismissButton = {
//                TextButton(onClick = { showDialog = false }) {
//                    Text("Cancel")
//                }
//            }
//        )
//    }
//}

@Composable
fun SliderPreference(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int, // Number of discrete steps (gaps between points). 0 for continuous.
    summarySuffix: String = "",
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)

            Text(
                text = "${String.format(Locale.current.platformLocale, "%.0f", value)} $summarySuffix",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps,
                modifier = Modifier
                    .padding(10.dp)
                    .widthIn(min = 100.dp), // Give slider some minimum width
                enabled = enabled
            )
        }
    }
}
