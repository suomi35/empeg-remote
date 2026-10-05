package com.chasinglemons.empeg.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.chasinglemons.empeg.model.SwipeAction

@Composable
fun SwipeActionChooserDialog(
    currentSwipeAction: SwipeAction,
    buttonClick: (SwipeAction) -> Unit,
    onDismiss: () -> Unit
) {

    val swipeOptions = listOf(SwipeAction.PLAYLISTS, SwipeAction.GESTURES)
    val (selectedOption, onOptionSelected) = remember { mutableStateOf(currentSwipeAction) }

    Dialog(onDismissRequest = { onDismiss() }) {

        Card(
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .selectableGroup(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    modifier = Modifier
                        .padding(16.dp),
                    textAlign = TextAlign.Center,
                    text = "Swipe action",
                    style = MaterialTheme.typography.headlineSmall
                )

                swipeOptions.forEach { swipeAction ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .selectable(
                                selected = (swipeAction == selectedOption),
                                onClick = { onOptionSelected(swipeAction) },
                                role = Role.RadioButton
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (swipeAction == selectedOption),
                            onClick = { buttonClick(swipeAction) }
                        )
                        Text(
                            text = swipeAction.type,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }

                TextButton(
                    modifier = Modifier
                        .padding(8.dp),
                    onClick = { onDismiss() }
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Preview
@Composable
fun SwipeActionChooserDialogPreview() {
    SwipeActionChooserDialog(
        currentSwipeAction = SwipeAction.PLAYLISTS,
        buttonClick = { },
        onDismiss = { }
    )
}