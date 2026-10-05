package com.chasinglemons.empeg.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.chasinglemons.empeg.R
import com.chasinglemons.empeg.util.Constants

@Composable
fun SendMessageDialog(
    initialDuration: Int = 9,
    onSendMessage: (Pair<Int, String>) -> Unit,
    onDismiss: () -> Unit
) {
    var message by remember { mutableStateOf(String()) }
    val values = remember { (1..60).map { it.toString() } }
    val valuesPickerState = rememberPickerState()
    val units = remember { listOf(Constants.SECONDS, Constants.MINUTES) }
    val unitsPickerState = rememberPickerState()

    fun calculateSeconds(): Int {
        return when (unitsPickerState.selectedItem == Constants.MINUTES) {
            true -> valuesPickerState.selectedItem.toInt() * 60
            false -> valuesPickerState.selectedItem.toInt()
        }
    }

    Dialog(onDismissRequest = { onDismiss() }) {

        Card(
            modifier = Modifier
                .height(IntrinsicSize.Min),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    modifier = Modifier
                        .padding(18.dp),
                    text = stringResource(R.string.overflow_menu_item_send_message),
                    style = MaterialTheme.typography.headlineLarge
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        modifier = Modifier
                            .padding(16.dp),
                        text = stringResource(R.string.send_message_duration_label),
                    )
                    Picker(
                        state = valuesPickerState,
                        items = values,
                        visibleItemsCount = 3,
                        startIndex = (initialDuration - 1).coerceIn(0, values.lastIndex),
                        modifier = Modifier.weight(0.3f),
                        textModifier = Modifier.padding(8.dp),
                    )
                    Picker(
                        state = unitsPickerState,
                        items = units,
                        visibleItemsCount = 3,
                        modifier = Modifier.weight(0.7f),
                        textModifier = Modifier.padding(8.dp),
                    )
                }

                TextField(
                    modifier = Modifier
                        .fillMaxWidth(),
                    placeholder = {
                        Text(stringResource(R.string.send_message_placeholder))
                    },
                    singleLine = true,
                    value = message,
                    onValueChange = { message = it },
                )
                Row(
                    modifier = Modifier
                        .padding(top = 16.dp),
                ) {
                    TextButton(
                        onClick = { onDismiss() },
                    ) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    TextButton(
                        onClick = {
                            onSendMessage(Pair(calculateSeconds(), message))
                            onDismiss()
                        },
                        enabled = message.isNotEmpty()
                    ) {
                        Text("Send")
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun SendMessageDialogPreview() {
    SendMessageDialog(
        onSendMessage = {},
        onDismiss = {}
    )
}