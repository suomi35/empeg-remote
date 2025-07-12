package com.chasinglemons.empeg.discovery

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chasinglemons.empeg.R
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoveryScreen(
    viewModel: DiscoveryViewModel,
    onPlayerSet: () -> Unit,
    snackbarHostState: SnackbarHostState
) {

    var showSettingsDialog by remember { mutableStateOf(false) }
    var manualIpEntry by remember { mutableStateOf(String()) }
    val lazyListState = rememberLazyListState()
    val discoveryListState = viewModel.discoveryFlow.collectAsStateWithLifecycle()
    var showProgressIndicator = viewModel.showProgressIndicator.collectAsStateWithLifecycle()

    if (showSettingsDialog) {
//        SettingsDialog(
//
//        )
    }

    Box(Modifier.fillMaxSize()) {

        Column {

            AnimatedVisibility(visible = true) {
                CenterAlignedTopAppBar(
                    title = { stringResource(R.string.discovery_title) },
                    navigationIcon = {
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher),
                            contentDescription = stringResource(R.string.description_empeg_remote_icon)
                        )
                    },
                    actions = {
                        IconButton(onClick = { viewModel.searchForEmpegs() }
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.description_empeg_discovery_icon)
                            )
                        }

                        IconButton(onClick = { showSettingsDialog = true }
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = stringResource(R.string.description_show_settings_icon)
                            )
                        }
                    }
                )
            }

            Column(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    textAlign = TextAlign.Center,
                    text = "Currently controlling empeg at:"
                )

                Row(
                    modifier = Modifier
                        .padding(4.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        modifier = Modifier
                            .weight(1f),
                        placeholder = {
                            Text(stringResource(R.string.discovery_ip_address_placeholder))
                        },
                        singleLine = true,
                        value = viewModel.getCurrentlyHomedPlayerIp(),
                        onValueChange = { manualIpEntry = it }, // TODO: IP address validation?!
                    )

                    TextButton(
                        onClick = { viewModel.setPlayer(manualIpEntry) }
                    ) {
                        Text(text = stringResource(R.string.discovery_ip_address_ok))
                    }
                }

                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    textAlign = TextAlign.Center,
                    text = "Discovered players"
                )

                LazyColumn(
//        contentPadding = innerPadding, // TODO: Do we need this?
                    state = lazyListState
                ) {
                    items(
                        items = discoveryListState.value,
                        key = { empeg -> empeg.ip },
                        itemContent = { item ->
                            Row(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.setPlayer(item.ip)
                                        onPlayerSet()
                                    }
                            ) {
                                Column {
                                    Text(item.name)
                                    Text("(${item.ip})")
                                }
                            }
                        }
                    )
                }
            }
        }

        if (showProgressIndicator.value) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(48.dp)
                        .wrapContentSize()
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        Timber.d(">>> LaunchedEffect searchForEmpegs()")
        viewModel.searchForEmpegs()
    }
}