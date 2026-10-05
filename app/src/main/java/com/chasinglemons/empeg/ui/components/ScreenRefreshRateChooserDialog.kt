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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun ScreenRefreshRateChooserDialog(
    currentRefreshRate: Int,
    buttonClick: (Int) -> Unit,
    onDismiss: () -> Unit
) {

    val swipeOptions = listOf(65, 75, 85, 100, 125, 250, 500, 1000, 2000, 5000)
    val (selectedOption, onOptionSelected) = remember { mutableIntStateOf(currentRefreshRate) }

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
                    text = "Screen refresh rate",
                    style = MaterialTheme.typography.headlineSmall
                )

                swipeOptions.forEach { rate ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .selectable(
                                selected = (rate == selectedOption),
                                onClick = { onOptionSelected(rate) },
                                role = Role.RadioButton
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (rate == selectedOption),
                            onClick = { buttonClick(rate) }
                        )
                        Text(
                            text = rate.toString(),
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
fun ScreenRefreshRateChooserDialogPreview() {
    ScreenRefreshRateChooserDialog(
        currentRefreshRate = 65,
        buttonClick = { },
        onDismiss = { }
    )
}