package com.chasinglemons.empeg.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.chasinglemons.empeg.R

@Composable
fun SearchDialog(
    onDismiss: () -> Unit,
    onSearch: (String) -> Unit
) {
    var searchTerm by remember { mutableStateOf(String()) }
    val focusRequester = remember { FocusRequester() }

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
                    text = stringResource(R.string.search_title),
                    style = MaterialTheme.typography.headlineSmall
                )

                TextField(
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .fillMaxWidth(),
                    placeholder = {
                        Text(stringResource(R.string.search_placeholder))
                    },
                    singleLine = true,
                    value = searchTerm,
                    onValueChange = { searchTerm = it },
                )

                TextButton(
                    modifier = Modifier
                        .padding(8.dp),
                    onClick = { onSearch(searchTerm) }
                ) {
                    Text(stringResource(R.string.ok))
                }
            }
        }
    }
}

@Preview
@Composable
fun SearchDialogPreview() {
    SearchDialog(
        onDismiss = { },
        onSearch = { }
    )
}