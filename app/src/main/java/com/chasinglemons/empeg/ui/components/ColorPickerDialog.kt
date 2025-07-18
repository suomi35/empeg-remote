package com.chasinglemons.empeg.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.chasinglemons.empeg.R
import com.chasinglemons.empeg.util.Utils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPickerDialog(
    initialColor: Color,
    onChoice: (Color) -> Unit,
    onDismissRequest: () -> Unit
) {
    val standardLensColors = arrayOf(
        Color(0xFF00BFFF),
        Color(0xFFE52900),
        Color(0xFFD0C700),
        Color(0xFF00DA37),
        Color(0xFFFFFFFF)
    )
    var selectedColor by remember(initialColor) { mutableStateOf(initialColor) }

    BasicAlertDialog(
        onDismissRequest = { onDismissRequest() }
    ) {
        Surface(
            shape = AlertDialogDefaults.shape,
            tonalElevation = AlertDialogDefaults.TonalElevation
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp),
                    text = "Lens Color",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                ) {

                    AsyncImage(
                        model = R.drawable.empeg_screen_init,
                        filterQuality = FilterQuality.None,
                        colorFilter = ColorFilter.colorMatrix(Utils.getColorMatrix(selectedColor)),
                        contentDescription = stringResource(id = R.string.description_empeg_display),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                HueBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 26.dp),
                    setColor = { hue ->
                        selectedColor = Color.hsv(hue, 1f, 1f)
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                Column(
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 50.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(standardLensColors) {
                            Button(
                                onClick = { selectedColor = it },
                                shape = CircleShape,
                                modifier = Modifier.requiredSize(50.dp),
                                contentPadding = PaddingValues(1.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(it.value)
                                ),
                                content = {}
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    TextButton(
                        onClick = { onChoice(selectedColor) },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(text = "Update", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}
